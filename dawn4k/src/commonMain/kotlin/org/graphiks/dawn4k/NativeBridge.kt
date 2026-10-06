package org.graphiks.dawn4k

/**
 * Minimal bridge to the backend's native runtime, for platform integrators
 * (window/surface bridges). NOT part of the WebGPU contract: the handles are
 * raw native pointers valid only while the owning context/device is open, and
 * [call] must be used to touch them (the dispatcher worker owns them).
 */
interface NativeBridge {
    /** Runs [block] on the backend's native worker thread; returns its result. */
    fun <T> call(block: () -> T): T

    /** The raw `WGPUInstance` pointer of this context, or 0 once closed. */
    fun instanceHandle(): Long
}
