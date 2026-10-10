package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.uploadAddress
import org.graphiks.dawn4k.mapper.dataSlice
import org.graphiks.dawn4k.mapper.initFrom
import org.graphiks.dawn4k.native.WGPUComputePassEncoder
import org.graphiks.dawn4k.native.WGPUStringView
import org.graphiks.dawn4k.native.wgpuComputePassEncoderDispatchWorkgroups
import org.graphiks.dawn4k.native.wgpuComputePassEncoderDispatchWorkgroupsIndirect
import org.graphiks.dawn4k.native.wgpuComputePassEncoderEnd
import org.graphiks.dawn4k.native.wgpuComputePassEncoderInsertDebugMarker
import org.graphiks.dawn4k.native.wgpuComputePassEncoderPopDebugGroup
import org.graphiks.dawn4k.native.wgpuComputePassEncoderPushDebugGroup
import org.graphiks.dawn4k.native.wgpuComputePassEncoderRelease
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetBindGroup
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetImmediates
import org.graphiks.dawn4k.native.wgpuComputePassEncoderSetPipeline
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUComputePassEncoder
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUIndex32
import org.graphiks.webgpu.GPUSize32
import org.graphiks.webgpu.GPUSize64

/**
 * The pass and its parent encoder require exclusive caller use, without scheduling.
 * A raw [GPUComputePassEncoder] borrowed from its owning command encoder. It has
 * no [close]: the pass handle is released when [end] is called, or by the session
 * teardown if the pass is never ended.
 *
 * [end] releases the native handle, so any command after it would dereference a
 * freed handle. A Kotlin [ended] flag refuses both a second [end] and any later
 * command with an [IllegalStateException] before a downcall — unlike the command
 * encoder's [DawnCommandEncoder.finish], whose "finish twice" validation error is
 * deliberately left to Dawn (the encoder handle is only released by [close]).
 */
class DawnComputePassEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUComputePassEncoder,
) : GPUComputePassEncoder {

    override var label: String = ""

    /** Set once by [end]; a command after it would use a released native handle. */
    private var ended = false

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuComputePassEncoderRelease(handle) })
    }

    private fun requireOpen() {
        check(!ended) { "the compute pass encoder has already been ended" }
    }

    override fun setPipeline(pipeline: GPUComputePipeline) {
        requireOpen()
        val dawn = pipeline.requireDawnComputePipeline(session)
        wgpuComputePassEncoderSetPipeline(handle, dawn.handle)
    }

    override fun dispatchWorkgroups(workgroupCountX: GPUSize32, workgroupCountY: GPUSize32, workgroupCountZ: GPUSize32) {
        requireOpen()
        run {
            wgpuComputePassEncoderDispatchWorkgroups(handle, workgroupCountX, workgroupCountY, workgroupCountZ)
        }
    }

    override fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        requireOpen()
        val dawn = indirectBuffer.requireDawnBuffer(session)
        run {
            wgpuComputePassEncoderDispatchWorkgroupsIndirect(handle, dawn.handle, indirectOffset)
        }
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        requireOpen()
        val dawn = bindGroup?.requireDawnBindGroup(session)
        run {
            memoryScope { allocator ->
                wgpuComputePassEncoderSetBindGroup(
                    handle,
                    index,
                    dawn?.handle,
                    dynamicOffsetsData.size.toULong(),
                    allocator.dynamicOffsetsBuffer(dynamicOffsetsData),
                )
            }
        }
    }

    override fun setImmediates(rangeOffset: GPUSize32, data: ArrayBuffer, dataOffset: GPUSize64, dataSize: GPUSize64?) {
        // A real SetImmediates downcall; Dawn validates the range against the
        // pipeline layout's immediate size and the device's maxImmediateSize.
        requireOpen()
        val slice = dataSlice(data.size, dataOffset, dataSize)
        run {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuComputePassEncoderSetImmediates(handle, rangeOffset, address, slice.size)
            }
        }
    }

    override fun pushDebugGroup(groupLabel: String) {
        requireOpen()
        run {
            memoryScope { allocator ->
                wgpuComputePassEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel))
            }
        }
    }

    override fun popDebugGroup() {
        requireOpen()
        wgpuComputePassEncoderPopDebugGroup(handle)
    }

    override fun insertDebugMarker(markerLabel: String) {
        requireOpen()
        run {
            memoryScope { allocator ->
                wgpuComputePassEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel))
            }
        }
    }

    override fun end() {
        requireOpen()
        ended = true
        wgpuComputePassEncoderEnd(handle)
        session.resources.release(this)
    }
}

/**
 * Writes [offsets] into an allocator-allocated UInt array; null when empty (the
 * C default `dynamicOffsets = NULL, dynamicOffsetCount = 0`).
 */
internal fun MemoryAllocator.dynamicOffsetsBuffer(offsets: List<UInt>): NativeAddress? =
    if (offsets.isEmpty()) {
        null
    } else {
        allocateBuffer((offsets.size * 4).toULong()).apply { writeUInts(offsets.toUIntArray()) }.handler
    }

/** Allocates a `WGPU_STRING_VIEW_INIT` pointing at [label] for a debug command. */
internal fun MemoryAllocator.allocateLabel(label: String): WGPUStringView =
    WGPUStringView.allocate(this).also { it.initFrom(label, this) }
