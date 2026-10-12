package org.graphiks.dawn4k.internal

import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.ArrayBuffer

/**
 * Wraps [address] into a borrowed [ArrayBuffer] view of [size] bytes: a view,
 * not a copy — the memory is owned by the native mapping and must never be
 * freed by the view. The view is invalid once the buffer is unmapped; never
 * read it afterwards.
 */
internal expect fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer

/**
 * Returns the native address of [data] at byte [offset] for an upload downcall,
 * or — when [data] is not directly addressable on this platform (a heap-backed
 * segment) — copies [size] bytes into an [allocator]-owned temporary buffer and
 * returns that instead. The returned address is valid for the duration of one
 * downcall, as long as [data] stays alive.
 */
internal expect fun uploadAddress(allocator: MemoryAllocator, data: ArrayBuffer, offset: ULong, size: ULong): NativeAddress

/**
 * A mapped range that does not fit the host's signed address space cannot be
 * exposed as an [ArrayBuffer]; reject it before any FFI call touches memory.
 */
internal fun requireRepresentableSize(size: ULong) {
    if (size > Long.MAX_VALUE.toULong()) {
        throw IllegalArgumentException("Range size $size is not representable as a host address range")
    }
}
