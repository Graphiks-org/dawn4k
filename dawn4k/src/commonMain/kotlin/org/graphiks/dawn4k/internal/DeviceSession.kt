package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import org.graphiks.dawn4k.DawnQueue
import org.graphiks.dawn4k.native.WGPUAdapter
import org.graphiks.dawn4k.native.WGPUDevice
import org.graphiks.dawn4k.native.WGPUDeviceLostCallback
import org.graphiks.dawn4k.native.WGPUQueue
import org.graphiks.dawn4k.native.WGPUUncapturedErrorCallback
import org.graphiks.dawn4k.native.wgpuAdapterRelease
import org.graphiks.dawn4k.native.wgpuDeviceRelease
import org.graphiks.dawn4k.native.wgpuDeviceDestroy
import org.graphiks.dawn4k.native.wgpuQueueRelease
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.webgpu.GPUError

/**
 * One raw device session: it owns the adapter, device and queue references it
 * was opened with, and the [ResourceRegistry] of every reference acquired
 * afterwards.
 *
 * The queue is an owned reference even without a public close: it is
 * registered with the registry, and the registry releases it — with every
 * other owned reference — before the device and adapter references, on the
 * closing caller. Device/adapter releases follow explicitly progressed loss.
 *
 * Device sessions must be closed before their runtime.
 */
internal class DeviceSession internal constructor(
    internal val runtime: DawnRuntime,
    internal val handle: WGPUDevice,
    internal val queueHandle: WGPUQueue,
    internal val adapter: WGPUAdapter,
    internal val callbacks: DeviceCallbacks,
) : AutoCloseable {

    internal val resources: ResourceRegistry = ResourceRegistry()

    /**
     * The session's owned queue, wrapped once and cached for the session's
     * lifetime: the wrapper only borrows the already-owned [queueHandle] (it
     * never releases it), so caching is cheap and ownership-safe. A
     * [DawnDevice] captures the same instance, so its label stays stable
     * across [DawnDevice.queue] accesses.
     */
    internal val queue: DawnQueue by lazy { DawnQueue(this, queueHandle) }

    init {
        resources.own(queueHandle, destroy = null, release = { wgpuQueueRelease(queueHandle) })
        runtime.registerSession(this)
    }

    private val lock = SynchronizedObject()
    private var closed = false

    /** State guard, not a use/close barrier: consumers must join users first. */
    internal fun requireOpen() {
        lock.withLock { check(!closed) { "the device session is closed" } }
    }

    override fun close() {
        if (lock.withLock { closed }) return
        runtime.requireCanClose(this)
        val firstClose = lock.withLock { if (closed) false else { closed = true; true } }
        if (!firstClose) return
        runtime.beginTeardown(this)
        callbacks.lost.invokeOnCompletion {
            runtime.postCallback {
                try { wgpuDeviceRelease(handle) } finally {
                    try { wgpuAdapterRelease(adapter) } finally { runtime.finishTeardown(this) }
                }
            }
        }
        try { resources.close() } finally { wgpuDeviceDestroy(handle) }
    }
}

/**
 * The per-device callback choreography installed during the device request:
 * the uncaptured-error and device-lost registrations, the uncaptured errors
 * observed so far, and the terminal loss marker.
 *
 * Handlers run during explicit event progression. State is protected by short
 * locks; sinks, route revocation and quiescence hooks run outside those locks.
 */
internal class DeviceCallbacks internal constructor() {

    private val lock = SynchronizedObject()
    private var sink: ((GPUError) -> Unit)? = null

    /** Terminal marker: completed with the factual loss once both callback routes are proven stopped. */
    internal val lost = CompletableDeferred<DawnDeviceLost>()

    /** Uncaptured errors observed so far, in arrival order. */
    private val errors = mutableListOf<DawnNativeError>()
    internal val uncapturedErrors: List<DawnNativeError> get() = lock.withLock { errors.toList() }

    /**
     * Sink the public device routes its descriptor's uncaptured-error callback
     * into; the stored [uncapturedErrors] list above stays the runtime's own
     * record. Installation and lookup are synchronized; invocation is not.
     */
    internal var uncapturedErrorSink: ((GPUError) -> Unit)?
        get() = lock.withLock { sink }
        set(value) { lock.withLock { sink = value } }

    private var lostRoute: CallbackRegistration<WGPUDeviceLostCallback>? = null
    private var errorRoute: CallbackRegistration<WGPUUncapturedErrorCallback>? = null
    internal var deviceLostRegistration: CallbackRegistration<WGPUDeviceLostCallback>?
        get() = lock.withLock { lostRoute }
        set(value) { lock.withLock { lostRoute = value } }
    internal var uncapturedErrorRegistration: CallbackRegistration<WGPUUncapturedErrorCallback>?
        get() = lock.withLock { errorRoute }
        set(value) { lock.withLock { errorRoute = value } }

    private var lossHandled = false
    private var routesClosed = false

    /** Factually lost, terminal only once both routes are proven stopped. */
    internal fun handleLoss(info: DawnDeviceLost) {
        val first = lock.withLock { if (lossHandled) false else { lossHandled = true; true } }
        if (!first) return
        close()
        // Terminal only after both routes are closed and quiescent: the
        // actions run inline when already quiescent, otherwise on the thread
        // that observes the last in-flight delivery returning.
        val uncaptured = uncapturedErrorRegistration
        val deviceLost = deviceLostRegistration
        if (uncaptured != null && deviceLost != null) {
            uncaptured.onQuiescent {
                deviceLost.onQuiescent {
                    lost.complete(info)
                }
            }
        } else {
            lost.complete(info)
        }
    }

    /** Records an observed uncaptured error, then invokes a snapshot of the sink. */
    internal fun handleUncapturedError(error: DawnNativeError) {
        val target = lock.withLock { errors += error; sink }
        target?.let { sink ->
            try {
                sink(error.gpuError)
            } catch (failure: Throwable) {
                // The error is already recorded above; a user callback failure
                // must not take down the dispatch loop.
            }
        }
    }

    /** Revokes both callback routes; idempotent and outside the monitor. */
    internal fun close() {
        val routes = lock.withLock {
            if (routesClosed) null else {
                routesClosed = true
                lostRoute to errorRoute
            }
        } ?: return
        routes.first?.close()
        routes.second?.close()
    }
}
