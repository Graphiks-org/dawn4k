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
 * A raw [GPUComputePassEncoder] borrowed from its owning command encoder. It has
 * no [close]: the pass handle is released when [end] is called, or by the session
 * teardown if the pass is never ended.
 */
class DawnComputePassEncoder internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUComputePassEncoder,
) : GPUComputePassEncoder {

    override var label: String = ""

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuComputePassEncoderRelease(handle) })
    }

    override fun setPipeline(pipeline: GPUComputePipeline) {
        val dawn = pipeline.requireDawnComputePipeline(session)
        session.runtime.dispatcher.call { wgpuComputePassEncoderSetPipeline(handle, dawn.handle) }
    }

    override fun dispatchWorkgroups(workgroupCountX: GPUSize32, workgroupCountY: GPUSize32, workgroupCountZ: GPUSize32) {
        session.runtime.dispatcher.call {
            wgpuComputePassEncoderDispatchWorkgroups(handle, workgroupCountX, workgroupCountY, workgroupCountZ)
        }
    }

    override fun dispatchWorkgroupsIndirect(indirectBuffer: GPUBuffer, indirectOffset: GPUSize64) {
        val dawn = indirectBuffer.requireDawnBuffer(session)
        session.runtime.dispatcher.call {
            wgpuComputePassEncoderDispatchWorkgroupsIndirect(handle, dawn.handle, indirectOffset)
        }
    }

    override fun setBindGroup(index: GPUIndex32, bindGroup: GPUBindGroup?, dynamicOffsetsData: List<UInt>) {
        val dawn = bindGroup?.requireDawnBindGroup(session)
        session.runtime.dispatcher.call {
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
        val slice = dataSlice(data.size, dataOffset, dataSize)
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val address = uploadAddress(allocator, data, slice.offset, slice.size)
                wgpuComputePassEncoderSetImmediates(handle, rangeOffset, address, slice.size)
            }
        }
    }

    override fun pushDebugGroup(groupLabel: String) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuComputePassEncoderPushDebugGroup(handle, allocator.allocateLabel(groupLabel))
            }
        }
    }

    override fun popDebugGroup() {
        session.runtime.dispatcher.call { wgpuComputePassEncoderPopDebugGroup(handle) }
    }

    override fun insertDebugMarker(markerLabel: String) {
        session.runtime.dispatcher.call {
            memoryScope { allocator ->
                wgpuComputePassEncoderInsertDebugMarker(handle, allocator.allocateLabel(markerLabel))
            }
        }
    }

    override fun end() {
        session.runtime.dispatcher.call { wgpuComputePassEncoderEnd(handle) }
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
