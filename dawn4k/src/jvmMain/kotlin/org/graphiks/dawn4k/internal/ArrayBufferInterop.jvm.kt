package org.graphiks.dawn4k.internal

import java.lang.foreign.MemorySegment
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.ArrayBuffer

internal actual fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer {
    // Reject an unrepresentable size before touching FFI: a size beyond
    // Long.MAX_VALUE cannot be expressed as a MemorySegment byte length, and a
    // null address or zero-size range must never become a dereferenced pointer.
    requireRepresentableSize(size)
    require(address.rawValue != 0L) { "Cannot wrap a null address" }
    val segment = MemorySegment.ofAddress(address.rawValue).reinterpret(size.toLong())
    return ArrayBuffer.wrap(segment)
}
