package org.graphiks.dawn4k.internal

import kotlinx.cinterop.ExperimentalForeignApi
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.ArrayBuffer

@OptIn(ExperimentalForeignApi::class)
internal actual fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer {
    // Consistent with the JVM view: an unrepresentable size and a null address
    // are rejected before a pointer is ever produced, and the public wrap
    // factory is a borrowed view (it never frees the mapped memory).
    requireRepresentableSize(size)
    val pointer = requireNotNull(address.pointer) { "Cannot wrap a null address" }
    return ArrayBuffer.wrap(pointer, size)
}
