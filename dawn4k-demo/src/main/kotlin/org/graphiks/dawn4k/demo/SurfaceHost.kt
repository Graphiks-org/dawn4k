package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge

/** Owns the native presentation target for the Dawn surface's lifetime. */
internal interface SurfaceHost : AutoCloseable {
    val backend: DawnBackend
    fun pixelSize(): Pair<Int, Int>
    fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface
}
