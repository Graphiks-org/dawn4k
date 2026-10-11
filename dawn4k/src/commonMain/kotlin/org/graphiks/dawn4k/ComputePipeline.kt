package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
import org.graphiks.dawn4k.internal.transferOwnership
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.mapper.allocateComputePipelineDescriptor
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUComputePipeline
import org.graphiks.dawn4k.native.WGPUCreateComputePipelineAsyncCallback
import org.graphiks.dawn4k.native.WGPUCreateComputePipelineAsyncCallbackInfo
import org.graphiks.dawn4k.native.WGPUCreatePipelineAsyncStatus
import org.graphiks.dawn4k.native.WGPUCreatePipelineAsyncStatus_Success
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.dawn4k.native.wgpuComputePipelineGetBindGroupLayout
import org.graphiks.dawn4k.native.wgpuComputePipelineRelease
import org.graphiks.dawn4k.native.wgpuDeviceCreateComputePipeline
import org.graphiks.dawn4k.native.wgpuDeviceCreateComputePipelineAsync
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUComputePipelineDescriptor

/**
 * A raw [GPUComputePipeline] backed by a Dawn `WGPUComputePipeline`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnComputePipeline internal constructor(
    internal val session: DeviceSession,
    handle: WGPUComputePipeline,
    label: String,
) : GPUComputePipeline {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuComputePipelineRelease(it) })
    internal val handle: WGPUComputePipeline
        get() { session.requireOpen(); return reference.handle }

    /**
     * The owned bind group layout at [index]. The native call returns a NEW
     * reference; the returned [DawnBindGroupLayout] owns it, and its [close]
     * releases it.
     */
    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout {
        val layoutHandle = wgpuComputePipelineGetBindGroupLayout(handle, index)
            ?: throw IllegalStateException("wgpuComputePipelineGetBindGroupLayout returned no layout")
        return DawnBindGroupLayout(session, layoutHandle, "")
    }

    override fun close() {
        reference.close()
    }
}

/** Creates a caller-owned [DawnComputePipeline] on [this] session. */
internal fun DeviceSession.createComputePipeline(descriptor: GPUComputePipelineDescriptor): DawnComputePipeline =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateComputePipelineDescriptor(descriptor)
            val handle = wgpuDeviceCreateComputePipeline(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateComputePipeline returned no pipeline")
            transferOwnership(handle, release = { wgpuComputePipelineRelease(it) }) { DawnComputePipeline(this, it, descriptor.label) }
        }
    }

/**
 * Creates a [DawnComputePipeline] asynchronously, resolving the returned [Result]
 * when the caller explicitly progresses the callback through
 * [org.graphiks.dawn4k.DawnContext.processEvents]. A rejected creation returns
 * [Result.failure] with a [DawnPipelineException]. A creation issued against a
 * closed runtime returns failure; closure with an outstanding operation is
 * refused. Coroutine cancellation stays a cancellation.
 */
internal suspend fun DeviceSession.createComputePipelineAsync(
    descriptor: GPUComputePipelineDescriptor,
): Result<DawnComputePipeline> = try {
    createComputePipelineAsyncOnSession(descriptor)
} catch (cancellation: CancellationException) {
    throw cancellation
} catch (failure: Throwable) {
    Result.failure(failure)
}

/** Issues the native creation and maps its settled outcome onto the contract. */
private suspend fun DeviceSession.createComputePipelineAsyncOnSession(
    descriptor: GPUComputePipelineDescriptor,
): Result<DawnComputePipeline> {
    val label = descriptor.label
    val operation = runtime.pending<ComputePipelineOutcome>(this) { outcome ->
        // A rejected result is released during explicit callback progression.
        outcome.pipeline?.let { wgpuComputePipelineRelease(it) }
    }
    run {
        var registration: CallbackRegistration<WGPUCreateComputePipelineAsyncCallback>? = null
        runtime.beginSubdeviceOperation(
            operation = operation,
            issue = {
                registration = WGPUCreateComputePipelineAsyncCallback.register(
                    policy = CallbackPolicy.ONCE,
                    callback = { status, pipeline, message, _ ->
                        // Callback thread: copy the borrowed message before returning.
                        val outcome = ComputePipelineOutcome(status, pipeline, message.copyToString())
                        runtime.postCallback {
                            runtime.finishSubdeviceOperation(operation, registration!!, Result.success(outcome))
                        }
                    },
                )
                memoryScope { allocator ->
                    val native = allocator.allocateComputePipelineDescriptor(descriptor)
                    val callbackInfo = WGPUCreateComputePipelineAsyncCallbackInfo.allocate(
                        allocator = allocator,
                        mode = WGPUCallbackMode_AllowProcessEvents,
                        registration = registration,
                    ).also { it.nextInChain = null }
                    wgpuDeviceCreateComputePipelineAsync(allocator, this.handle, native, callbackInfo)
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
        handle?.let { wgpuComputePipelineRelease(it) }
        return Result.failure(DawnPipelineException(outcome.status, outcome.message))
    }
    return Result.success(transferOwnership(handle, release = { wgpuComputePipelineRelease(it) }) {
        DawnComputePipeline(this, it, label)
    })
    } finally { operation.acceptOwnership() }
}

/** The settled outcome of a native async pipeline creation; owns its pipeline. */
private class ComputePipelineOutcome(
    val status: WGPUCreatePipelineAsyncStatus,
    val pipeline: WGPUComputePipeline?,
    val message: String,
)

/** A compute pipeline creation rejected by the native layer. */
internal class DawnPipelineException(
    val status: WGPUCreatePipelineAsyncStatus,
    message: String,
) : IllegalStateException("the compute pipeline creation failed (status=$status): $message")
