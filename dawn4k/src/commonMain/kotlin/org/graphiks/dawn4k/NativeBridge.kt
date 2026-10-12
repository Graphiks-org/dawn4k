package org.graphiks.dawn4k

/**
 * Minimal bridge to the backend's native runtime, for platform integrators
 * (window/surface bridges). NOT part of the WebGPU contract: the handles are
 * raw native pointers valid only while the owning context/device is open, and
 * callers own synchronization and lifetime. Native calls execute directly on
 * their caller; this bridge exposes handles only.
 */
interface NativeBridge {
    /** The raw `WGPUInstance` pointer of this context, or 0 once closed. */
    fun instanceHandle(): Long
}
