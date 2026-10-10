package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.selects.select
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.mapper.allocateRequestAdapterOptions
import org.graphiks.dawn4k.mapper.initNull
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPURequestAdapterOptions

/** One instance, with caller-owned downcalls and explicitly progressed callback settlements. */
internal class DawnRuntime internal constructor(internal val config: DawnConfig) : AutoCloseable {
    private val lock = SynchronizedObject()
    private val mailbox = CallbackMailbox()
    private val operations = AsyncOperationRegistry()
    private val adapters = mutableSetOf<Long>()
    private val sessions = mutableSetOf<DeviceSession>()
    private val teardowns = mutableSetOf<DeviceSession>()
    private val rejectedDevices = mutableSetOf<DeviceCallbacks>()
    private val closedMarker = CompletableDeferred<Unit>()
    private var processing = false
    private var instance: WGPUInstance? = null

    init {
        preparePlatformLibraries()
        instance = createInstance()
    }

    internal fun requireOpen() {
        if (closedMarker.isCompleted) throw DawnRuntimeClosedException()
    }

    /** Consumers must stop/join their producers; this guard is not a use/close lock. */
    internal fun requireCanClose(owner: Any? = null) {
        lock.withLock { check(!processing) { "cannot close during processEvents()" } }
        check(!operations.hasPending(owner) && (owner != null || mailbox.isEmpty())) {
            "native operations are pending; continue processEvents() before closing"
        }
    }

    internal fun postCallback(action: () -> Unit) = mailbox.post(action)

    internal fun <T> pending(owner: Any, releaseRejected: (T) -> Unit): PendingOperation<T> {
        requireOpen()
        lateinit var operation: PendingOperation<T>
        operation = PendingOperation { outcome ->
            postCallback {
                try { releaseRejected(outcome) } finally { operation.acceptOwnership() }
            }
        }
        operations.register(owner, operation)
        operation.ownershipCompletion.invokeOnCompletion { operations.ownershipSettled(operation) }
        return operation
    }

    private fun nativeSettled(operation: PendingOperation<*>) {
        operation.nativeTerminal()
        operations.nativeSettled(operation)
    }

    internal fun <T> beginSubdeviceOperation(
        operation: PendingOperation<T>,
        issue: () -> Unit,
        closeRegistration: () -> Unit,
    ) {
        requireOpen()
        try { issue() } catch (failure: Throwable) {
            closeRegistration()
            nativeSettled(operation)
            operation.acceptOwnership()
            throw failure
        }
    }

    internal fun <T> finishSubdeviceOperation(
        operation: PendingOperation<T>,
        registration: CallbackRegistration<*>,
        result: Result<T>,
    ) {
        registration.close()
        // Mark native-terminal only after the trampoline returns, even on inline delivery.
        registration.onQuiescent { nativeSettled(operation) }
        operation.complete(result)
    }

    fun processEvents() {
        lock.withLock {
            requireOpen()
            check(!processing) { "processEvents() is already active" }
            processing = true
        }
        try {
            mailbox.drain()
            wgpuInstanceProcessEvents(checkNotNull(instance))
            mailbox.drain()
        } finally { lock.withLock { processing = false } }
    }

    suspend fun drainEvents() = processEvents()

    fun hasPendingOperations(): Boolean = operations.hasPending() || !mailbox.isEmpty() ||
        lock.withLock { teardowns.isNotEmpty() || rejectedDevices.isNotEmpty() }

    internal fun debugOpenCallbacks(): Int = operations.nativeCount()
    internal fun currentInstance(): WGPUInstance? = lock.withLock { instance }

    override fun close() {
        if (closedMarker.isCompleted) return
        requireCanClose()
        val released = lock.withLock {
            check(adapters.isEmpty() && sessions.isEmpty() && rejectedDevices.isEmpty()) {
                "context has open children; close devices and adapters first"
            }
            instance.also { instance = null; closedMarker.complete(Unit) }
        }
        released?.let { wgpuInstanceRelease(it) }
    }

    internal fun releaseAdapter(adapter: WGPUAdapter) {
        lock.withLock { adapters.remove(adapter.handler.rawValue) }
    }

    internal fun registerSession(session: DeviceSession) { lock.withLock { sessions += session } }
    internal fun beginTeardown(session: DeviceSession) { lock.withLock { teardowns += session } }
    internal fun finishTeardown(session: DeviceSession) {
        lock.withLock { teardowns.remove(session); sessions.remove(session) }
    }

    internal suspend fun awaitDeviceLoss(callbacks: DeviceCallbacks): DawnDeviceLost {
        if (callbacks.lost.isCompleted) return callbacks.lost.await()
        requireOpen()
        return select {
            callbacks.lost.onAwait { it }
            closedMarker.onAwait { throw DawnRuntimeClosedException() }
        }
    }

    internal suspend fun requestAdapter(options: GPURequestAdapterOptions?): WGPUAdapter {
        val operation = pending<AdapterOutcome>(this) { outcome ->
            outcome.adapter?.let { wgpuAdapterRelease(it) }
        }
        var registration: CallbackRegistration<WGPURequestAdapterCallback>? = null
        beginSubdeviceOperation(operation, issue = {
            registration = WGPURequestAdapterCallback.register(policy = CallbackPolicy.ONCE) { status, adapter, message, _ ->
                val outcome = AdapterOutcome(status, adapter, message.copyToString())
                postCallback { finishSubdeviceOperation(operation, registration!!, Result.success(outcome)) }
            }
            memoryScope { allocator ->
                val native = allocator.allocateRequestAdapterOptions(options, config.backend)
                val info = WGPURequestAdapterCallbackInfo.allocate(allocator, WGPUCallbackMode_AllowProcessEvents, registration)
                    .also { it.nextInChain = null }
                wgpuInstanceRequestAdapter(allocator, instance, native, info)
            }
        }, closeRegistration = { registration?.close() })
        val result = operation.await()
        try {
            val outcome = result.getOrThrow()
            val adapter = outcome.adapter ?: throw DawnRequestAdapterException(outcome.status, outcome.message)
            lock.withLock { adapters += adapter.handler.rawValue }
            return adapter
        } finally { operation.acceptOwnership() }
    }

    suspend fun openSession(): DeviceSession {
        val adapter = requestAdapter(null)
        try {
            val session = openSessionOnAdapter(adapter) { _, _ -> }
            return session
        } finally {
            wgpuAdapterRelease(adapter)
            releaseAdapter(adapter)
        }
    }

    internal suspend fun openSessionOnAdapter(
        adapter: WGPUAdapter,
        configure: (WGPUDeviceDescriptor, MemoryAllocator) -> Unit,
    ): DeviceSession {
        val callbacks = DeviceCallbacks()
        val operation = pending<DeviceOutcome>(adapter.handler.rawValue) { releaseDeviceOutcome(it) }
        var registration: CallbackRegistration<WGPURequestDeviceCallback>? = null
        beginSubdeviceOperation(operation, issue = {
            registration = WGPURequestDeviceCallback.register(policy = CallbackPolicy.ONCE) { status, device, message, _ ->
                val outcome = DeviceOutcome(status, device, message.copyToString(), callbacks)
                postCallback { finishSubdeviceOperation(operation, registration!!, Result.success(outcome)) }
            }
            callbacks.deviceLostRegistration = WGPUDeviceLostCallback.register(policy = CallbackPolicy.ONCE) { _, reason, message, _ ->
                val info = DawnDeviceLost(deviceLostReason(reason), message.copyToString())
                postCallback { callbacks.handleLoss(info) }
            }
            callbacks.uncapturedErrorRegistration = WGPUUncapturedErrorCallback.register(policy = CallbackPolicy.REPEATING) { _, type, message, _ ->
                val error = uncapturedError(type, message.copyToString())
                if (error != null) postCallback { callbacks.handleUncapturedError(error) }
            }
            memoryScope { allocator ->
                val lost = WGPUDeviceLostCallbackInfo.allocate(allocator, WGPUCallbackMode_AllowProcessEvents, checkNotNull(callbacks.deviceLostRegistration))
                    .also { it.nextInChain = null }
                val uncaptured = WGPUUncapturedErrorCallbackInfo.allocate(allocator, checkNotNull(callbacks.uncapturedErrorRegistration))
                    .also { it.nextInChain = null }
                val descriptor = WGPUDeviceDescriptor.allocate(allocator)
                initDeviceDescriptorDefaults(descriptor, lost, uncaptured)
                configure(descriptor, allocator)
                if (config.implicitDeviceSynchronization) {
                    check(wgpuAdapterHasFeature(adapter, WGPUFeatureName_ImplicitDeviceSynchronization) != 0u) {
                        "Dawn adapter does not support ImplicitDeviceSynchronization"
                    }
                    val count = descriptor.requiredFeatureCount.toInt()
                    val existing = UIntArray(count)
                    if (count > 0) org.graphiks.kffi.MemoryBuffer(checkNotNull(descriptor.requiredFeatures), count.toULong() * 4uL).readUInts(existing)
                    val features = (existing.toList() + WGPUFeatureName_ImplicitDeviceSynchronization).distinct().toUIntArray()
                    val memory = allocator.allocateBuffer(features.size.toULong() * 4uL)
                    memory.writeUInts(features)
                    descriptor.requiredFeatureCount = features.size.toULong()
                    descriptor.requiredFeatures = memory.handler
                }
                val info = WGPURequestDeviceCallbackInfo.allocate(allocator, WGPUCallbackMode_AllowProcessEvents, registration)
                    .also { it.nextInChain = null }
                wgpuAdapterRequestDevice(allocator, adapter, descriptor, info)
            }
        }, closeRegistration = { registration?.close(); callbacks.close() })
        val result = operation.await()
        try {
            val outcome = result.getOrThrow()
            if (outcome.status != WGPURequestDeviceStatus_Success || outcome.device == null) {
                releaseDeviceOutcome(outcome)
                throw DawnRequestDeviceException(outcome.status, outcome.message)
            }
            wgpuAdapterAddRef(adapter)
            try {
                val queue = wgpuDeviceGetQueue(outcome.device)
                    ?: error("wgpuDeviceGetQueue returned no queue")
                return DeviceSession(this, outcome.device, queue, adapter, callbacks)
            } catch (failure: Throwable) {
                wgpuAdapterRelease(adapter)
                releaseDeviceOutcome(outcome)
                throw failure
            }
        } finally { operation.acceptOwnership() }
    }

    private fun releaseDeviceOutcome(outcome: DeviceOutcome) {
        // Dawn release may enqueue the loss callback; keep routes until loss is delivered.
        outcome.device?.let {
            lock.withLock { rejectedDevices += outcome.callbacks }
            outcome.callbacks.lost.invokeOnCompletion {
                postCallback { lock.withLock { rejectedDevices.remove(outcome.callbacks) } }
            }
            wgpuDeviceRelease(it)
        }
        if (outcome.device == null) outcome.callbacks.close()
    }

    private fun createInstance(): WGPUInstance = memoryScope { allocator ->
        val descriptor = WGPUInstanceDescriptor.allocate(allocator)
        descriptor.nextInChain = null
        descriptor.requiredFeatureCount = 0uL
        descriptor.requiredFeatures = null
        descriptor.requiredLimits = null
        wgpuCreateInstance(descriptor) ?: error("wgpuCreateInstance returned no instance")
    }

    private fun initDeviceDescriptorDefaults(
        descriptor: WGPUDeviceDescriptor,
        lost: WGPUDeviceLostCallbackInfo,
        uncaptured: WGPUUncapturedErrorCallbackInfo,
    ) {
        descriptor.nextInChain = null
        descriptor.label.initNull()
        descriptor.requiredFeatureCount = 0uL
        descriptor.requiredFeatures = null
        descriptor.requiredLimits = null
        descriptor.defaultQueue.nextInChain = null
        descriptor.defaultQueue.label.initNull()
        descriptor.deviceLostCallbackInfo = lost
        descriptor.uncapturedErrorCallbackInfo = uncaptured
    }
}

private class AdapterOutcome(val status: WGPURequestAdapterStatus, val adapter: WGPUAdapter?, val message: String)
private class DeviceOutcome(val status: WGPURequestDeviceStatus, val device: WGPUDevice?, val message: String, val callbacks: DeviceCallbacks)

private const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

/** Copy inside the callback, before native borrowed memory becomes invalid. */
internal fun WGPUStringView.copyToString(): String {
    val data = this.data ?: return ""
    return if (length == WGPU_STRLEN) data.toKString() ?: "" else data.toKString(length) ?: ""
}
