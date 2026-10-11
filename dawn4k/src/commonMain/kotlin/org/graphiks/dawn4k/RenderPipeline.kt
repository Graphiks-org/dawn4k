package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.transferOwnership
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.mapper.allocateRenderPipelineDescriptor
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUCreatePipelineAsyncStatus
import org.graphiks.dawn4k.native.WGPUCreatePipelineAsyncStatus_Success
import org.graphiks.dawn4k.native.WGPUCreateRenderPipelineAsyncCallback
import org.graphiks.dawn4k.native.WGPUCreateRenderPipelineAsyncCallbackInfo
import org.graphiks.dawn4k.native.WGPURenderPipeline
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.dawn4k.native.wgpuDeviceCreateRenderPipeline
import org.graphiks.dawn4k.native.wgpuDeviceCreateRenderPipelineAsync
import org.graphiks.dawn4k.native.wgpuRenderPipelineGetBindGroupLayout
import org.graphiks.dawn4k.native.wgpuRenderPipelineRelease
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor

/**
 * A raw [GPURenderPipeline] backed by a Dawn `WGPURenderPipeline`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnRenderPipeline internal constructor(
    internal val session: DeviceSession,
    handle: WGPURenderPipeline,
    label: String,
) : GPURenderPipeline {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuRenderPipelineRelease(it) })
    internal val handle: WGPURenderPipeline
        get() { session.requireOpen(); return reference.handle }

    /**
     * The owned bind group layout at [index]. The native call returns a NEW
     * reference; the returned [DawnBindGroupLayout] owns it, and its [close]
     * releases it.
     */
    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout {
        val layoutHandle = wgpuRenderPipelineGetBindGroupLayout(handle, index)
            ?: throw IllegalStateException("wgpuRenderPipelineGetBindGroupLayout returned no layout")
        return DawnBindGroupLayout(session, layoutHandle, "")
    }

    override fun close() {
        reference.close()
    }
}


/** Creates a caller-owned [DawnRenderPipeline] on [this] session. */
internal fun DeviceSession.createRenderPipeline(descriptor: GPURenderPipelineDescriptor): DawnRenderPipeline =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateRenderPipelineDescriptor(descriptor)
            val handle = wgpuDeviceCreateRenderPipeline(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateRenderPipeline returned no pipeline")
            transferOwnership(handle, release = { wgpuRenderPipelineRelease(it) }) { DawnRenderPipeline(this, it, descriptor.label) }
        }
    }

/**
 * Creates a [DawnRenderPipeline] asynchronously, resolving the returned [Result]
 * when the caller explicitly progresses the callback through
 * [org.graphiks.dawn4k.DawnContext.processEvents]. A rejected creation returns
 * [Result.failure] with a [DawnPipelineException]. A creation issued against a
 * closed runtime returns failure; closure with an outstanding operation is
 * refused. Coroutine cancellation stays a cancellation.
 */
internal suspend fun DeviceSession.createRenderPipelineAsync(
    descriptor: GPURenderPipelineDescriptor,
): Result<DawnRenderPipeline> = try {
    createRenderPipelineAsyncOnSession(descriptor)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}

/** Issues the native creation and maps its settled outcome onto the contract. */
private suspend fun DeviceSession.createRenderPipelineAsyncOnSession(
    descriptor: GPURenderPipelineDescriptor,
): Result<DawnRenderPipeline> {
    val label = descriptor.label
    val operation = runtime.pending<RenderPipelineOutcome>(this) { outcome ->
        // A rejected result is released during explicit callback progression.
        outcome.pipeline?.let { wgpuRenderPipelineRelease(it) }
    }
    run {
        var registration: CallbackRegistration<WGPUCreateRenderPipelineAsyncCallback>? = null
        runtime.beginSubdeviceOperation(
            operation = operation,
            issue = {
                registration = WGPUCreateRenderPipelineAsyncCallback.register(
                    policy = CallbackPolicy.ONCE,
                    callback = { status, pipeline, message, _ ->
                        // Callback thread: copy the borrowed message before returning.
                        val outcome = RenderPipelineOutcome(status, pipeline, message.copyToString())
                        runtime.postCallback {
                            runtime.finishSubdeviceOperation(operation, registration!!, Result.success(outcome))
                        }
                    },
                )
                memoryScope { allocator ->
                    val native = allocator.allocateRenderPipelineDescriptor(descriptor)
                    val callbackInfo = WGPUCreateRenderPipelineAsyncCallbackInfo.allocate(
                        allocator = allocator,
                        mode = WGPUCallbackMode_AllowProcessEvents,
                        registration = registration,
                    ).also { it.nextInChain = null }
                    wgpuDeviceCreateRenderPipelineAsync(allocator, this.handle, native, callbackInfo)
                }
            },
            closeRegistration = { registration?.close() },
        )
    }
    val result = operation.await()
    try {
    val outcome = result.getOrThrow()
    val handle = outcome.pipeline
    if (outcome.status != WGPUCreatePipelineAsyncStatus_Success || handle == null) {
        handle?.let { wgpuRenderPipelineRelease(it) }
        return Result.failure(DawnPipelineException(outcome.status, outcome.message))
    }
    return Result.success(transferOwnership(handle, release = { wgpuRenderPipelineRelease(it) }) {
        DawnRenderPipeline(this, it, label)
    })
    } finally { operation.acceptOwnership() }
}

/** The settled outcome of a native async render pipeline creation; owns its pipeline. */
private class RenderPipelineOutcome(
    val status: WGPUCreatePipelineAsyncStatus,
    val pipeline: WGPURenderPipeline?,
    val message: String,
)
