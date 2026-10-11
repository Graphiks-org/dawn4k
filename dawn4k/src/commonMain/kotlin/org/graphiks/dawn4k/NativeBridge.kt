package org.graphiks.dawn4k

/**
 * Minimal bridge to the backend's native runtime, for platform integrators
 * (window/surface bridges). NOT part of the WebGPU contract: the handles are
 * raw native pointers valid only while the owning context/device is open, and
 * callers own synchronization and lifetime. [call] is only an inline legacy alias.
 */
interface NativeBridge {
    /** Executes inline; this bridge supplies neither synchronization nor scheduling. */
    @Deprecated("Native calls run on the caller; call does not synchronize or dispatch")
    fun <T> call(block: () -> T): T

    /** The raw `WGPUInstance` pointer of this context, or 0 once closed. */
    fun instanceHandle(): Long
}
