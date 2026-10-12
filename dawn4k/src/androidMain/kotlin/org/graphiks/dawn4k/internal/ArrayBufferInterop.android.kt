package org.graphiks.dawn4k.internal

import java.nio.ByteBuffer
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.AndroidArrayBuffer
import org.graphiks.webgpu.ArrayBuffer

internal actual fun borrowedArrayBuffer(address: NativeAddress, size: ULong): ArrayBuffer {
    // Mirrors the JVM guards: reject the unrepresentable and the null before
    // any memory is touched. The public wrap factory is a borrowed view
    // (webgpu-api's BorrowedNativeArrayBuffer) that never frees the mapped
    // range and bounds-checks every offset against the size.
    requireRepresentableSize(size)
    require(address.rawValue != 0L) { "Cannot wrap a null address" }
    return ArrayBuffer.wrap(address.rawValue, size)
}

internal actual fun uploadAddress(
    allocator: MemoryAllocator,
    data: ArrayBuffer,
    offset: ULong,
    size: ULong,
): NativeAddress {
    // A direct ByteBuffer carrier is addressable in place: its native address
    // is the hidden-API accessor every direct-buffer consumer on Android uses,
    // with the same tolerated status as sun.misc.Unsafe.
    val carrier = (data as? AndroidArrayBuffer)?.buffer
    if (carrier != null && carrier.isDirect) {
        val base = directBufferAddress(carrier)
        if (base != 0L) {
            return NativeAddress(base + offset.toLong())
        }
    }
    // Borrowed views keep their address internal to webgpu-api, and a blocked
    // or absent accessor must degrade instead of crashing: copy the requested
    // slice through the public reads into allocator-owned memory, valid for
    // the duration of one downcall.
    val temp = allocator.allocateBuffer(size)
    for (index in 0uL until size) {
        temp.writeByte(data.getByte(offset + index), index)
    }
    return temp.handler
}

/**
 * Reads the native address of a direct [ByteBuffer] through the platform's
 * hidden accessor. Returns `0` when the accessor is unavailable — the caller
 * degrades to the copy path instead of reaching for a null address.
 */
@Suppress("PrivateApi")
private fun directBufferAddress(buffer: ByteBuffer): Long = try {
    buffer.javaClass.getMethod("address").invoke(buffer) as Long
} catch (_: ReflectiveOperationException) {
    0L
}
