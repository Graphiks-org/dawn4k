package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeoutOrNull
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.native.WGPUAdapter
import org.graphiks.dawn4k.native.WGPUBackendType_Metal
import org.graphiks.dawn4k.native.WGPUBackendType_Undefined
import org.graphiks.dawn4k.native.WGPUBackendType_Vulkan
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUDevice
import org.graphiks.dawn4k.native.WGPUDeviceDescriptor
import org.graphiks.dawn4k.native.WGPUDeviceLostCallback
import org.graphiks.dawn4k.native.WGPUDeviceLostCallbackInfo
import org.graphiks.dawn4k.native.WGPUFeatureLevel_Undefined
import org.graphiks.dawn4k.native.WGPUInstance
import org.graphiks.dawn4k.native.WGPUInstanceDescriptor
import org.graphiks.dawn4k.native.WGPUPowerPreference_Undefined
import org.graphiks.dawn4k.native.WGPURequestAdapterCallback
import org.graphiks.dawn4k.native.WGPURequestAdapterCallbackInfo
import org.graphiks.dawn4k.native.WGPURequestAdapterOptions
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus_Success
import org.graphiks.dawn4k.native.WGPURequestDeviceCallback
import org.graphiks.dawn4k.native.WGPURequestDeviceCallbackInfo
import org.graphiks.dawn4k.native.WGPURequestDeviceStatus
import org.graphiks.dawn4k.native.WGPURequestDeviceStatus_Success
import org.graphiks.dawn4k.native.WGPUStringView
import org.graphiks.dawn4k.native.WGPUUncapturedErrorCallback
import org.graphiks.dawn4k.native.WGPUUncapturedErrorCallbackInfo
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.dawn4k.native.wgpuAdapterRelease
import org.graphiks.dawn4k.native.wgpuAdapterRequestDevice
import org.graphiks.dawn4k.native.wgpuCreateInstance
import org.graphiks.dawn4k.native.wgpuDeviceGetQueue
import org.graphiks.dawn4k.native.wgpuDeviceRelease
import org.graphiks.dawn4k.native.wgpuInstanceProcessEvents
import org.graphiks.dawn4k.native.wgpuInstanceRelease
import org.graphiks.dawn4k.native.wgpuInstanceRequestAdapter
import org.graphiks.dawn4k.native.wgpuQueueRelease
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope

/**
 * The raw Dawn runtime of one context: it owns the WGPUInstance and the native
 * dispatcher, discovers adapters and devices, and progresses the instance's
 * events while callback-bearing operations are in flight.
 *
 * Ownership model:
 * - every native call runs on the owned dispatcher worker;
 * - every async callback copies its borrowed data (string views included)
 *   inside the callback, then posts the settled result to the worker;
 * - every awaited native outcome owns its native reference and goes through a
 *   [PendingOperation] directly, so an abandoned wait never leaks the late
 *   delivery: it is released on the worker;
 * - the periodic event pump runs only while callback registrations are open,
 *   and its job and worker close cleanly at runtime close;
 * - [close] abandons in-flight waits with a close diagnostic, waits for the
 *   late deliveries to settle, releases the instance, and closes the
 *   dispatcher. It is idempotent and safe to reenter from the worker.
 *
 * Device sessions must be closed before the runtime.
 */
internal class DawnRuntime internal constructor(internal val config: DawnConfig) : AutoCloseable {

    internal val dispatcher: NativeDispatcher = createNativeDispatcher()

    /** Completed once the close choreography started; safe to read from any thread. */
    private val closedMarker = CompletableDeferred<Unit>()

    /** Completed on the worker once the instance is released and the pump is stopped. */
    private val teardownSettled = CompletableDeferred<Unit>()

    /** The periodic pump lives here; the scope holds no thread of its own. */
    private val pumpScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Worker-confined state — mutated only inside dispatcher tasks.
    private var instance: WGPUInstance? = null
    private var closed = false
    private var closeInitiatedFromWorker = false
    private var outstandingCallbacks = 0
    private val openOperations = mutableListOf<PendingOperation<*>>()
    private var pumpJob: Job? = null

    init {
        try {
            instance = dispatcher.call { createInstanceOnWorker() }
        } catch (failure: Throwable) {
            dispatcher.close()
            throw failure
        }
    }

    /** Opens one device session: adapter request, device request, queue fetch. */
    suspend fun openSession(): DeviceSession {
        if (closedMarker.isCompleted) throw DawnRuntimeClosedException()
        val adapter = awaitAdapter()
        return try {
            val outcome = awaitDevice(adapter)
            dispatcher.call { buildSessionOnWorker(adapter, outcome) }
        } catch (failure: Throwable) {
            // No session will own this adapter after all. If the runtime's
            // teardown already closed the dispatcher, the instance teardown has
            // overtaken this wait and the native ownership went with it.
            try {
                dispatcher.call { wgpuAdapterRelease(adapter) }
            } catch (dispatcherClosed: IllegalStateException) {
                // Nothing left to release through.
            }
            throw failure
        }
    }

    /** Progresses the instance's events once, on the dispatcher worker. */
    suspend fun drainEvents() {
        if (closedMarker.isCompleted) throw DawnRuntimeClosedException()
        dispatcher.awaitCall {
            val current = instance ?: throw DawnRuntimeClosedException()
            wgpuInstanceProcessEvents(current)
        }
    }

    override fun close() {
        if (closedMarker.isCompleted) return
        // FIFO probe: a post from the worker is queued, never inline. When the
        // close task below runs through the queue, the probe has already run;
        // when the close is reentered from the worker, it has not — that is
        // how the choreography detects it must not join its own worker.
        val probe = CompletableDeferred<Unit>()
        dispatcher.post { probe.complete(Unit) }
        dispatcher.call { beginCloseOnWorker(reentrant = !probe.isCompleted) }
        if (!probe.isCompleted) {
            // Reentrant close from the worker: initiate only. The late
            // deliveries settle through the queue and finish the teardown —
            // including the dispatcher close, initiated from the worker.
            return
        }
        // Foreign thread: let the late deliveries settle (the pump keeps
        // progressing the instance while the queue is still open), then close
        // the dispatcher. The timeout is a wedged-driver escape hatch only.
        runBlocking {
            withTimeoutOrNull(CLOSE_SETTLE_TIMEOUT_MS) { teardownSettled.await() }
        }
        dispatcher.close()
    }

    /**
     * Test visibility: callback registrations still open (one per in-flight
     * adapter/device request). Routed through the dispatcher while the
     * teardown has not settled; once it has, the completion of
     * [teardownSettled] publishes the terminal value to every reader.
     */
    internal fun debugOpenCallbacks(): Int =
        if (teardownSettled.isCompleted) outstandingCallbacks
        else dispatcher.call { outstandingCallbacks }

    // --- Adapter discovery -------------------------------------------------

    private suspend fun awaitAdapter(): WGPUAdapter {
        val operation = dispatcher.call { issueAdapterRequest() }
        val outcome = operation.await().getOrThrow()
        return outcome.adapter ?: throw DawnRequestAdapterException(outcome.status, outcome.message)
    }

    /** Worker: registers the callback, issues the native request, arms the pump. */
    private fun issueAdapterRequest(): PendingOperation<AdapterOutcome> {
        if (closed) throw DawnRuntimeClosedException()
        val operation = PendingOperation<AdapterOutcome> { outcome ->
            // Runs on the worker: every completion is posted there.
            outcome.adapter?.let { wgpuAdapterRelease(it) }
        }
        // Self-referenced by its own callback; assigned before the request that
        // can trigger it is issued, so the capture is never null when it fires.
        var registration: CallbackRegistration<WGPURequestAdapterCallback>? = null
        registration = WGPURequestAdapterCallback.register(
            policy = CallbackPolicy.ONCE,
            callback = { status, adapter, message, _ ->
                // Callback thread: copy the borrowed view before returning.
                val outcome = AdapterOutcome(status, adapter, message.copyToString())
                dispatcher.post { settleAdapterRequest(operation, registration!!, outcome) }
            },
        )
        openOperations += operation
        outstandingCallbacks += 1
        try {
            memoryScope { allocator ->
                val options = WGPURequestAdapterOptions.allocate(allocator)
                initRequestAdapterOptions(options)
                val callbackInfo = WGPURequestAdapterCallbackInfo.allocate(
                    allocator = allocator,
                    mode = WGPUCallbackMode_AllowProcessEvents,
                    registration = registration,
                ).also { it.nextInChain = null }
                wgpuInstanceRequestAdapter(allocator, instance, options, callbackInfo)
            }
        } catch (failure: Throwable) {
            openOperations.remove(operation)
            outstandingCallbacks -= 1
            registration.close()
            throw failure
        }
        ensurePump()
        return operation
    }

    /** Worker: the posted settle of the adapter callback. */
    private fun settleAdapterRequest(
        operation: PendingOperation<AdapterOutcome>,
        registration: CallbackRegistration<WGPURequestAdapterCallback>,
        outcome: AdapterOutcome,
    ) {
        openOperations.remove(operation)
        outstandingCallbacks -= 1
        registration.close()
        // The waiter registered before this task could run (single queue); a
        // vanished waiter makes the rejected outcome release its adapter above.
        operation.complete(Result.success(outcome))
        settleTeardownIfClosing()
    }

    // --- Device request ----------------------------------------------------

    private suspend fun awaitDevice(adapter: WGPUAdapter): DeviceOutcome {
        val operation = dispatcher.call { issueDeviceRequest(adapter) }
        val outcome = operation.await().getOrThrow()
        if (outcome.status != WGPURequestDeviceStatus_Success || outcome.device == null) {
            dispatcher.call { releaseDeviceOutcome(outcome) }
            throw DawnRequestDeviceException(outcome.status, outcome.message)
        }
        return outcome
    }

    /** Worker: installs the device callbacks, issues the request, arms the pump. */
    private fun issueDeviceRequest(adapter: WGPUAdapter): PendingOperation<DeviceOutcome> {
        if (closed) throw DawnRuntimeClosedException()
        val callbacks = DeviceCallbacks()
        val operation = PendingOperation<DeviceOutcome> { outcome ->
            // Runs on the worker: every completion is posted there.
            releaseDeviceOutcome(outcome)
        }
        // Self-referenced by its own callback; assigned before the request that
        // can trigger it is issued, so the capture is never null when it fires.
        var registration: CallbackRegistration<WGPURequestDeviceCallback>? = null
        registration = WGPURequestDeviceCallback.register(
            policy = CallbackPolicy.ONCE,
            callback = { status, device, message, _ ->
                val outcome = DeviceOutcome(status, device, message.copyToString(), callbacks)
                dispatcher.post { settleDeviceRequest(operation, registration!!, outcome) }
            },
        )
        val lostRegistration = WGPUDeviceLostCallback.register(
            policy = CallbackPolicy.ONCE,
            callback = { _, reason, message, _ ->
                val info = DawnDeviceLost(deviceLostReason(reason), message.copyToString())
                dispatcher.post { callbacks.handleLoss(info) }
            },
        )
        val uncapturedRegistration = WGPUUncapturedErrorCallback.register(
            // Uncaptured errors have no callback mode and fire on their
            // registration until it is closed — the repeated registration.
            policy = CallbackPolicy.REPEATING,
            callback = { _, type, message, _ ->
                val error = uncapturedError(type, message.copyToString())
                if (error != null) {
                    dispatcher.post { callbacks.handleUncapturedError(error) }
                }
            },
        )
        callbacks.deviceLostRegistration = lostRegistration
        callbacks.uncapturedErrorRegistration = uncapturedRegistration
        // A FailedCreation loss must abandon this wait before it registers.
        callbacks.deviceOperation = operation
        openOperations += operation
        outstandingCallbacks += 1
        try {
            memoryScope { allocator ->
                val lostInfo = WGPUDeviceLostCallbackInfo.allocate(
                    allocator = allocator,
                    mode = WGPUCallbackMode_AllowProcessEvents,
                    registration = lostRegistration,
                ).also { it.nextInChain = null }
                val uncapturedInfo = WGPUUncapturedErrorCallbackInfo.allocate(
                    allocator = allocator,
                    registration = uncapturedRegistration,
                ).also { it.nextInChain = null }
                val descriptor = WGPUDeviceDescriptor.allocate(allocator)
                initDeviceDescriptorDefaults(descriptor, lostInfo, uncapturedInfo)
                val callbackInfo = WGPURequestDeviceCallbackInfo.allocate(
                    allocator = allocator,
                    mode = WGPUCallbackMode_AllowProcessEvents,
                    registration = registration,
                ).also { it.nextInChain = null }
                wgpuAdapterRequestDevice(allocator, adapter, descriptor, callbackInfo)
            }
        } catch (failure: Throwable) {
            openOperations.remove(operation)
            outstandingCallbacks -= 1
            callbacks.deviceOperation = null
            registration.close()
            callbacks.close()
            throw failure
        }
        ensurePump()
        return operation
    }

    /** Worker: the posted settle of the device callback. */
    private fun settleDeviceRequest(
        operation: PendingOperation<DeviceOutcome>,
        registration: CallbackRegistration<WGPURequestDeviceCallback>,
        outcome: DeviceOutcome,
    ) {
        openOperations.remove(operation)
        outstandingCallbacks -= 1
        registration.close()
        outcome.callbacks.deviceOperation = null
        operation.complete(Result.success(outcome))
        settleTeardownIfClosing()
    }

    /** Worker: releases a device outcome nobody consumed. */
    private fun releaseDeviceOutcome(outcome: DeviceOutcome) {
        outcome.device?.let { wgpuDeviceRelease(it) }
        outcome.callbacks.close()
    }

    /** Worker: fetches the queue and hands the callbacks over to the session. */
    private fun buildSessionOnWorker(adapter: WGPUAdapter, outcome: DeviceOutcome): DeviceSession {
        val device = checkNotNull(outcome.device)
        val queue = wgpuDeviceGetQueue(device)
            ?: throw IllegalStateException("wgpuDeviceGetQueue returned no queue")
        return DeviceSession(this, device, queue, adapter, outcome.callbacks)
    }

    // --- Event pump ---------------------------------------------------------

    /** Worker: starts the periodic pump unless one already ticks. */
    private fun ensurePump() {
        val running = pumpJob
        if (running != null && running.isActive) return
        pumpJob = pumpScope.launch {
            try {
                // One ProcessEvents per tick, executed on the worker, only
                // while callback registrations are open.
                while (dispatcher.call { pumpOnce() }) {
                    delay(PUMP_TICK_MS)
                }
            } catch (dispatcherClosed: IllegalStateException) {
                // The runtime closed the dispatcher between two ticks: the
                // teardown settled the deliveries it could, and this loop owns
                // nothing left to pump.
            }
        }
    }

    /** Worker: one pump iteration; true when another tick is needed. */
    private fun pumpOnce(): Boolean {
        // The instance is null only after the teardown released it.
        val current = instance ?: return exitPumpOnWorker()
        if (outstandingCallbacks == 0) return exitPumpOnWorker()
        wgpuInstanceProcessEvents(current)
        // Keep ticking through a close while deliveries are still outstanding:
        // their settles are what releases the instance and finishes the teardown.
        return if (outstandingCallbacks > 0) true else exitPumpOnWorker()
    }

    /**
     * Worker: this pump has decided not to tick again. The job reference is
     * cleared here, inside the deciding worker task, before the pump coroutine
     * completes on its own Default thread — a settle→re-arm adjacency must
     * never observe a stale-active pump through [ensurePump]'s active-job
     * check, skip the launch, and leave a fresh request without a
     * ProcessEvents source.
     */
    private fun exitPumpOnWorker(): Boolean {
        pumpJob = null
        return false
    }

    // --- Close choreography --------------------------------------------------

    /** Worker: initiates the teardown; the settle tasks finish it when needed. */
    private fun beginCloseOnWorker(reentrant: Boolean) {
        if (closed) return
        closed = true
        closeInitiatedFromWorker = reentrant
        closedMarker.complete(Unit)
        val cause = DawnRuntimeClosedException()
        openOperations.forEach { operation -> operation.abandon(cause) }
        openOperations.clear()
        if (outstandingCallbacks == 0) {
            finishTeardownOnWorker()
        }
    }

    /** Worker: called at the end of every settle while closing. */
    private fun settleTeardownIfClosing() {
        if (closed && outstandingCallbacks == 0) {
            finishTeardownOnWorker()
        }
    }

    /** Worker: stops the pump, releases the instance, publishes the teardown. */
    private fun finishTeardownOnWorker() {
        pumpJob?.cancel()
        pumpJob = null
        instance?.let { wgpuInstanceRelease(it) }
        instance = null
        teardownSettled.complete(Unit)
        if (closeInitiatedFromWorker) {
            // Initiated from the worker: initiate only; the loop drains and
            // exits by itself.
            dispatcher.close()
        }
    }

    /** Worker: creates the instance with its C defaults. */
    private fun createInstanceOnWorker(): WGPUInstance {
        memoryScope { allocator ->
            val descriptor = WGPUInstanceDescriptor.allocate(allocator)
            // WGPU_INSTANCE_DESCRIPTOR_INIT equivalent: the allocator does not
            // zero memory on every platform, so every field is explicit.
            descriptor.nextInChain = null
            descriptor.requiredFeatureCount = 0uL
            descriptor.requiredFeatures = null
            descriptor.requiredLimits = null
            return wgpuCreateInstance(descriptor)
                ?: throw IllegalStateException("wgpuCreateInstance returned no instance")
        }
    }

    // --- Descriptor defaults (C *_INIT macro equivalents) --------------------

    /** WGPU_REQUEST_ADAPTER_OPTIONS_INIT equivalent, plus the requested backend. */
    private fun initRequestAdapterOptions(options: WGPURequestAdapterOptions) {
        options.nextInChain = null
        options.featureLevel = WGPUFeatureLevel_Undefined
        options.powerPreference = WGPUPowerPreference_Undefined
        options.forceFallbackAdapter = 0u
        options.backendType = when (config.backend) {
            null -> WGPUBackendType_Undefined
            DawnBackend.Metal -> WGPUBackendType_Metal
            DawnBackend.Vulkan -> WGPUBackendType_Vulkan
        }
        options.compatibleSurface = null
    }

    /** WGPU_DEVICE_DESCRIPTOR_INIT equivalent, with the two callback infos installed. */
    private fun initDeviceDescriptorDefaults(
        descriptor: WGPUDeviceDescriptor,
        lostInfo: WGPUDeviceLostCallbackInfo,
        uncapturedInfo: WGPUUncapturedErrorCallbackInfo,
    ) {
        descriptor.nextInChain = null
        initStringViewDefaults(descriptor.label)
        descriptor.requiredFeatureCount = 0uL
        descriptor.requiredFeatures = null
        descriptor.requiredLimits = null
        val defaultQueue = descriptor.defaultQueue
        defaultQueue.nextInChain = null
        initStringViewDefaults(defaultQueue.label)
        descriptor.deviceLostCallbackInfo = lostInfo
        descriptor.uncapturedErrorCallbackInfo = uncapturedInfo
    }

    /** WGPU_STRING_VIEW_INIT equivalent: no data, NUL-terminated sentinel. */
    private fun initStringViewDefaults(view: WGPUStringView) {
        view.data = null
        view.length = WGPU_STRLEN
    }

    private companion object {
        /** One pump tick while callback registrations are open. */
        const val PUMP_TICK_MS = 1L

        /** Close-settling bound; only a wedged driver reaches it. */
        const val CLOSE_SETTLE_TIMEOUT_MS = 10_000L
    }
}

/** The settled outcome of a native adapter request; owns its adapter. */
private class AdapterOutcome(
    val status: WGPURequestAdapterStatus,
    val adapter: WGPUAdapter?,
    val message: String,
)

/** The settled outcome of a native device request; owns its device. */
private class DeviceOutcome(
    val status: WGPURequestDeviceStatus,
    val device: WGPUDevice?,
    val message: String,
    val callbacks: DeviceCallbacks,
)

/** webgpu.h `WGPU_STRLEN` (SIZE_MAX): the NUL-terminated length sentinel. */
private const val WGPU_STRLEN: ULong = ULong.MAX_VALUE

/**
 * Copies the borrowed native string into a Kotlin string. Must be called
 * inside the callback, before returning to native code.
 */
private fun WGPUStringView.copyToString(): String {
    val data = this.data ?: return ""
    return if (length == WGPU_STRLEN) {
        data.toKString() ?: ""
    } else {
        data.toKString(length) ?: ""
    }
}
