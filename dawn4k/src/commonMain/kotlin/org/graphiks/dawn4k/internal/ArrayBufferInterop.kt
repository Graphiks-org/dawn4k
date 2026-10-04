package org.graphiks.dawn4k.internal

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
 * A mapped range that does not fit the host's signed address space cannot be
 * exposed as an [ArrayBuffer]; reject it before any FFI call touches memory.
 */
internal fun requireRepresentableSize(size: ULong) {
    if (size > Long.MAX_VALUE.toULong()) {
        throw IllegalArgumentException("Range size $size is not representable as a host address range")
    }
}
