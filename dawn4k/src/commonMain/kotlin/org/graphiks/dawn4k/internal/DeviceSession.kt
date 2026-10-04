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
 * other owned reference — before the device and adapter references, all on
 * the runtime's dispatcher. Closing is idempotent.
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

    internal val resources: ResourceRegistry = ResourceRegistry(runtime.dispatcher)

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
    }

    /** Worker-confined; every close routes through the dispatcher. */
    private var closed = false

    override fun close() {
        runtime.dispatcher.call {
            if (closed) return@call
            closed = true
            try {
                // Owned refs release first, before the device and adapter refs;
                // the Destroyed loss may fire during the device release below.
                resources.close()
            } finally {
                wgpuDeviceRelease(handle)
                wgpuAdapterRelease(adapter)
                callbacks.close()
            }
        }
    }
}

/**
 * The per-device callback choreography installed during the device request:
 * the uncaptured-error and device-lost registrations, the uncaptured errors
 * observed so far, the in-flight request wait a loss abandons, and the
 * terminal loss marker.
 *
 * Every handler runs on the runtime's dispatcher (posted by the callbacks,
 * which copy their borrowed data first). All mutable state is worker-confined
 * except the loss marker — a thread-safe primitive completed by the quiescence
 * proof, which may fire on any thread — and [uncapturedErrorSink], which is
 * installed on the caller thread and read only on the worker.
 */
internal class DeviceCallbacks internal constructor() {

    /** Terminal marker: completed with the factual loss once both callback routes are proven stopped. */
    internal val lost = CompletableDeferred<DawnDeviceLost>()

    /** Uncaptured errors observed so far, in arrival order. */
    internal val uncapturedErrors = mutableListOf<DawnNativeError>()

    /**
     * Sink the public device routes its descriptor's uncaptured-error callback
     * into; the stored [uncapturedErrors] list above stays the runtime's own
     * record. Not worker-confined: it is installed on the CALLER thread when
     * the public device wraps the session (the thread that requested the
     * device). Its safety is the dispatcher-ordered error routing — the write
     * completes before the device is handed back to the caller, and the only
     * reader, [handleUncapturedError], always runs on the dispatcher worker,
     * ordered after that write by the queue the caller's subsequent operations
     * and the error callback's post both pass through.
     */
    internal var uncapturedErrorSink: ((GPUError) -> Unit)? = null

    internal var deviceLostRegistration: CallbackRegistration<WGPUDeviceLostCallback>? = null
    internal var uncapturedErrorRegistration: CallbackRegistration<WGPUUncapturedErrorCallback>? = null

    /** The in-flight device request wait that a device loss abandons with its diagnostic. */
    internal var deviceOperation: PendingOperation<*>? = null

    private var lossHandled = false
    private var routesClosed = false

    /** Worker: a device loss abandons the request wait, then goes terminal once the routes are proven stopped. */
    internal fun handleLoss(info: DawnDeviceLost) {
        if (lossHandled) return
        lossHandled = true
        deviceOperation?.abandon(DawnDeviceLostException(info))
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

    /** Worker: stores an observed uncaptured error, then hands it to the public sink. */
    internal fun handleUncapturedError(error: DawnNativeError) {
        uncapturedErrors += error
        uncapturedErrorSink?.let { sink ->
            try {
                sink(error.gpuError)
            } catch (failure: Throwable) {
                // The error is already recorded above; a user callback failure
                // must not take down the dispatch loop.
            }
        }
    }

    /** Worker: revokes both callback routes; idempotent. */
    internal fun close() {
        if (routesClosed) return
        routesClosed = true
        deviceLostRegistration?.close()
        uncapturedErrorRegistration?.close()
    }
}
