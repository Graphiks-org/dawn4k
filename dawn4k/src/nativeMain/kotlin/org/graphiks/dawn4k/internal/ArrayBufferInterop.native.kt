package org.graphiks.dawn4k.internal

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toLong
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.OpaquePointerArrayBuffer

@OptIn(ExperimentalForeignApi::class)
internal actual fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer {
    // Consistent with the JVM view: an unrepresentable size and a null address
    // are rejected before a pointer is ever produced, and the public wrap
    // factory is a borrowed view (it never frees the mapped memory).
    requireRepresentableSize(size)
    val pointer = requireNotNull(address.pointer) { "Cannot wrap a null address" }
    return ArrayBuffer.wrap(pointer, size)
}

@OptIn(ExperimentalForeignApi::class)
internal actual fun uploadAddress(
    allocator: MemoryAllocator,
    data: ArrayBuffer,
    offset: ULong,
    size: ULong,
): NativeAddress {
    // Every native ArrayBuffer is backed by an opaque pointer (owned native heap
    // or a borrowed mapped view): it is always directly addressable.
    val buffer = data as OpaquePointerArrayBuffer
    return NativeAddress(buffer.pointer.toLong() + offset.toLong())
}
