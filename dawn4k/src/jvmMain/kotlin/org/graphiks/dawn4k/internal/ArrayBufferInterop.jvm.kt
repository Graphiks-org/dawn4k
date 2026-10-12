package org.graphiks.dawn4k.internal

import java.lang.foreign.MemorySegment
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.JvmArrayBuffer

internal actual fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer {
    // Reject an unrepresentable size before touching FFI: a size beyond
    // Long.MAX_VALUE cannot be expressed as a MemorySegment byte length, and a
    // null address or zero-size range must never become a dereferenced pointer.
    requireRepresentableSize(size)
    require(address.rawValue != 0L) { "Cannot wrap a null address" }
    val segment = MemorySegment.ofAddress(address.rawValue).reinterpret(size.toLong())
    return ArrayBuffer.wrap(segment)
}

internal actual fun uploadAddress(
    allocator: MemoryAllocator,
    data: ArrayBuffer,
    offset: ULong,
    size: ULong,
): NativeAddress {
    val segment = (data as JvmArrayBuffer).buffer
    val base = try {
        segment.address()
    } catch (_: UnsupportedOperationException) {
        // A heap-backed segment has no stable address: copy the requested slice
        // into allocator-owned temporary memory for the duration of the downcall.
        val temp = allocator.allocateBuffer(size)
        MemorySegment.copy(
            segment,
            offset.toLong(),
            MemorySegment.ofAddress(temp.handler.rawValue).reinterpret(size.toLong()),
            0L,
            size.toLong(),
        )
        return temp.handler
    }
    return NativeAddress(base + offset.toLong())
}
