package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
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
    internal val handle: WGPUComputePipeline,
    label: String,
) : GPUComputePipeline {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuComputePipelineRelease(handle) })
    }

    /**
     * The owned bind group layout at [index]. The native call returns a NEW
     * reference; the returned [DawnBindGroupLayout] owns it, and its [close]
     * releases it.
     */
    override fun getBindGroupLayout(index: UInt): GPUBindGroupLayout {
        val layoutHandle = session.runtime.dispatcher.call { wgpuComputePipelineGetBindGroupLayout(handle, index) }
            ?: throw IllegalStateException("wgpuComputePipelineGetBindGroupLayout returned no layout")
        return DawnBindGroupLayout(session, layoutHandle, "")
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Creates a [DawnComputePipeline] on [this] session and registers its reference. */
internal fun DeviceSession.createComputePipeline(descriptor: GPUComputePipelineDescriptor): DawnComputePipeline =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val native = allocator.allocateComputePipelineDescriptor(descriptor, this)
            val handle = wgpuDeviceCreateComputePipeline(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateComputePipeline returned no pipeline")
            DawnComputePipeline(this, handle, descriptor.label)
        }
    }

/** Refuses a foreign or foreign-session compute pipeline before its handle is read. */
internal fun GPUComputePipeline.requireDawnComputePipeline(owner: DeviceSession): DawnComputePipeline {
    val dawn = this as? DawnComputePipeline
        ?: throw IllegalArgumentException("the compute pipeline does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the compute pipeline belongs to a different device session" }
    return dawn
}

/**
 * Creates a [DawnComputePipeline] asynchronously, resolving the returned [Result]
 * once the native callback fires (progressed by the runtime's event pump — no
 * fixed sleep). A rejected creation returns [Result.failure] with a
 * [DawnPipelineException].
 */
internal suspend fun DeviceSession.createComputePipelineAsync(
    descriptor: GPUComputePipelineDescriptor,
): Result<DawnComputePipeline> {
    val operation = PendingOperation<ComputePipelineOutcome> { outcome ->
        // Runs on the worker: a late delivery nobody consumed releases its pipeline.
        outcome.pipeline?.let { wgpuComputePipelineRelease(it) }
    }
    runtime.dispatcher.call {
        var registration: CallbackRegistration<WGPUCreateComputePipelineAsyncCallback>? = null
        runtime.beginSubdeviceOperation(
            operation = operation,
            issue = {
                registration = WGPUCreateComputePipelineAsyncCallback.register(
                    policy = CallbackPolicy.ONCE,
                    callback = { status, pipeline, message, _ ->
                        // Callback thread: copy the borrowed message before returning.
                        val outcome = ComputePipelineOutcome(status, pipeline, message.copyToString())
                        runtime.dispatcher.post {
                            runtime.finishSubdeviceOperation(operation, registration!!, Result.success(outcome))
                        }
                    },
                )
                memoryScope { allocator ->
                    val native = allocator.allocateComputePipelineDescriptor(descriptor, this)
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
    val outcome = operation.await().getOrThrow()
    val handle = outcome.pipeline
    if (outcome.status != WGPUCreatePipelineAsyncStatus_Success || handle == null) {
        handle?.let { runtime.dispatcher.call { wgpuComputePipelineRelease(it) } }
        return Result.failure(DawnPipelineException(outcome.status, outcome.message))
    }
    return Result.success(DawnComputePipeline(this, handle, descriptor.label))
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
