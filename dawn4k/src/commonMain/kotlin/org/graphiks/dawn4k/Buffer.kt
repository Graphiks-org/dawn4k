package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.ByteRange
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.internal.borrowedArrayBuffer
import org.graphiks.dawn4k.internal.checkedRange
import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.internal.requireRepresentableSize
import org.graphiks.dawn4k.mapper.allocateBufferDescriptor
import org.graphiks.dawn4k.mapper.toNativeMode
import org.graphiks.dawn4k.native.WGPUBuffer
import org.graphiks.dawn4k.native.WGPUBufferMapCallback
import org.graphiks.dawn4k.native.WGPUBufferMapCallbackInfo
import org.graphiks.dawn4k.native.WGPUBufferMapState
import org.graphiks.dawn4k.native.WGPUBufferMapState_Mapped
import org.graphiks.dawn4k.native.WGPUBufferMapState_Pending
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUMapAsyncStatus
import org.graphiks.dawn4k.native.WGPUMapAsyncStatus_Success
import org.graphiks.dawn4k.native.WGPUQueueWorkDoneCallback
import org.graphiks.dawn4k.native.WGPUQueueWorkDoneCallbackInfo
import org.graphiks.dawn4k.native.WGPUQueueWorkDoneStatus
import org.graphiks.dawn4k.native.WGPUQueueWorkDoneStatus_Success
import org.graphiks.dawn4k.native.wgpuBufferDestroy
import org.graphiks.dawn4k.native.wgpuBufferGetConstMappedRange
import org.graphiks.dawn4k.native.wgpuBufferGetMappedRange
import org.graphiks.dawn4k.native.wgpuBufferGetMapState
import org.graphiks.dawn4k.native.wgpuBufferMapAsync
import org.graphiks.dawn4k.native.wgpuBufferRelease
import org.graphiks.dawn4k.native.wgpuBufferUnmap
import org.graphiks.dawn4k.native.wgpuDeviceCreateBuffer
import org.graphiks.dawn4k.native.wgpuQueueOnSubmittedWorkDone
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUSize64
import org.graphiks.webgpu.GPUSize64Out

/**
 * A raw [GPUBuffer] backed by a Dawn `WGPUBuffer`.
 *
 * Mapping is real: [getMappedRange] wraps the actual mapped memory (a borrowed
 * view, never a CPU copy). The returned [ArrayBuffer] is valid only while the
 * buffer is mapped — after [unmap] (or [close]) its memory is returned to the
 * device and the view must not be read.
 *
 * The object is single-threaded, matching the WebGPU object model: map, unmap
 * and close must not race one another.
 */
class DawnBuffer internal constructor(
    private val session: DeviceSession,
    internal val handle: WGPUBuffer,
    descriptor: GPUBufferDescriptor,
) : GPUBuffer {

    override val size: GPUSize64Out = descriptor.size

    override val usage: Set<GPUBufferUsage> =
        GPUBufferUsage.entries.filterTo(mutableSetOf()) { (descriptor.usage.value and it.value) != 0uL }

    override var label: String = descriptor.label

    /** The range mapped by the last successful [mapAsync]; null when unmapped. */
    private var mappedRange: ByteRange? = null

    /** Whether the current mapping is writable (mapped for write). */
    private var mappedWritable: Boolean = false

    init {
        if (descriptor.mappedAtCreation) {
            // A mapped-at-creation buffer starts mapped for the whole range,
            // writable.
            mappedRange = ByteRange(0uL, descriptor.size)
            mappedWritable = true
        }
        session.resources.own(
            key = this,
            destroy = { wgpuBufferDestroy(handle) },
            release = { wgpuBufferRelease(handle) },
        )
    }

    override val mapState: GPUBufferMapState
        get() = mapStateOf(session.runtime.dispatcher.call { wgpuBufferGetMapState(handle) })

    override suspend fun mapAsync(
        mode: GPUMapMode,
        offset: GPUSize64,
        size: GPUSize64?,
    ): Result<Unit> {
        // Normalize the range; alignment (offset % 8, size % 4) and overlap
        // constraints are deliberately NOT checked here — they must surface as
        // the native validation error, not a Kotlin exception.
        val range = checkedRange(this.size, offset, size)
        val operation = PendingOperation<Unit> { }
        session.runtime.dispatcher.call {
            var registration: CallbackRegistration<WGPUBufferMapCallback>? = null
            session.runtime.beginSubdeviceOperation(
                operation = operation,
                issue = {
                    registration = WGPUBufferMapCallback.register(policy = CallbackPolicy.ONCE) { status, message, _ ->
                        val outcome = BufferMapOutcome(status, message.copyToString())
                        session.runtime.dispatcher.post {
                            session.runtime.finishSubdeviceOperation(
                                operation,
                                registration!!,
                                outcome.toResult(),
                            )
                        }
                    }
                    memoryScope { allocator ->
                        val callbackInfo = WGPUBufferMapCallbackInfo.allocate(
                            allocator = allocator,
                            mode = WGPUCallbackMode_AllowProcessEvents,
                            registration = registration,
                        ).also { it.nextInChain = null }
                        wgpuBufferMapAsync(
                            allocator = allocator,
                            buffer = handle,
                            mode = mode.toNativeMode(),
                            offset = range.offset,
                            size = range.size,
                            callbackInfo = callbackInfo,
                        )
                    }
                },
                closeRegistration = { registration?.close() },
            )
        }
        val result = operation.await()
        if (result.isSuccess) {
            mappedRange = range
            mappedWritable = mode.value == GPUMapMode.Write.value
        }
        return result
    }

    override fun getMappedRange(offset: GPUSize64, size: GPUSize64?): ArrayBuffer {
        val mapped = mappedRange
            ?: throw IllegalStateException("the buffer is not mapped; call mapAsync and await it first")
        val range = checkedRange(mapped.size, offset, size)
        if (range.size == 0uL) return ArrayBuffer.allocate(0uL)
        requireRepresentableSize(range.size)
        val absoluteOffset = mapped.offset + range.offset
        val writable = mappedWritable
        val address = session.runtime.dispatcher.call {
            // Dawn's C API takes an absolute buffer offset and separates the
            // writable and const variants: a read mapping only exposes a const
            // range, so pick the variant matching the mapping's writability.
            if (writable) {
                wgpuBufferGetMappedRange(handle, absoluteOffset, range.size)
            } else {
                wgpuBufferGetConstMappedRange(handle, absoluteOffset, range.size)
            }
        } ?: throw IllegalStateException(
            "the mapped range ${range.offset}..${range.offset + range.size} is not accessible",
        )
        return borrowedArrayBuffer(address, range.size)
    }

    override fun unmap() {
        mappedRange = null
        session.runtime.dispatcher.call { wgpuBufferUnmap(handle) }
    }

    override fun close() {
        session.resources.destroy(this)
    }
}

/** The native completion of one buffer map: the copied status and message. */
private class BufferMapOutcome(val status: WGPUMapAsyncStatus, val message: String) {
    fun toResult(): Result<Unit> =
        if (status == WGPUMapAsyncStatus_Success) Result.success(Unit)
        else Result.failure(DawnBufferMapException(status, message))
}

/** The native completion of one queue work-done wait: the copied status and message. */
private class WorkDoneOutcome(val status: WGPUQueueWorkDoneStatus, val message: String) {
    fun toResult(): Result<Unit> =
        if (status == WGPUQueueWorkDoneStatus_Success) Result.success(Unit)
        else Result.failure(
            IllegalStateException("the queue work-done callback failed (status=$status): $message"),
        )
}

/** A buffer map rejected by the native layer with a validation/aborted status. */
internal class DawnBufferMapException(
    val status: WGPUMapAsyncStatus,
    message: String,
) : IllegalStateException("the buffer map failed (status=$status): $message")

/** `WGPUBufferMapState` (C) -> `GPUBufferMapState` (WebGPU). */
private fun mapStateOf(state: WGPUBufferMapState): GPUBufferMapState = when (state) {
    WGPUBufferMapState_Pending -> GPUBufferMapState.Pending
    WGPUBufferMapState_Mapped -> GPUBufferMapState.Mapped
    else -> GPUBufferMapState.Unmapped
}

/**
 * Creates a [DawnBuffer] on [this] session and registers it with the session's
 * resource registry, so the session releases the native reference on teardown.
 */
internal fun DeviceSession.createBuffer(descriptor: GPUBufferDescriptor): DawnBuffer =
    runtime.dispatcher.call {
        memoryScope { allocator ->
            val nativeDescriptor = allocator.allocateBufferDescriptor(descriptor)
            val handle = wgpuDeviceCreateBuffer(this.handle, nativeDescriptor)
                ?: throw IllegalStateException("wgpuDeviceCreateBuffer returned no buffer")
            DawnBuffer(this, handle, descriptor)
        }
    }

/**
 * Suspends until every work submitted to the session's queue so far is done.
 * Routed through the runtime's event pump via a queue work-done callback.
 */
internal suspend fun DeviceSession.onSubmittedWorkDone(): Result<Unit> {
    val operation = PendingOperation<Unit> { }
    runtime.dispatcher.call {
        var registration: CallbackRegistration<WGPUQueueWorkDoneCallback>? = null
        runtime.beginSubdeviceOperation(
            operation = operation,
            issue = {
                registration = WGPUQueueWorkDoneCallback.register(policy = CallbackPolicy.ONCE) { status, message, _ ->
                    val outcome = WorkDoneOutcome(status, message.copyToString())
                    runtime.dispatcher.post {
                        runtime.finishSubdeviceOperation(operation, registration!!, outcome.toResult())
                    }
                }
                memoryScope { allocator ->
                    val callbackInfo = WGPUQueueWorkDoneCallbackInfo.allocate(
                        allocator = allocator,
                        mode = WGPUCallbackMode_AllowProcessEvents,
                        registration = registration,
                    ).also { it.nextInChain = null }
                    wgpuQueueOnSubmittedWorkDone(allocator, queueHandle, callbackInfo)
                }
            },
            closeRegistration = { registration?.close() },
        )
    }
    return operation.await()
}
