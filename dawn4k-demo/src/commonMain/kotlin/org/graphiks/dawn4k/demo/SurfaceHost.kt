package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.webgpu.GPUTextureFormat

/** Typed presentation boundary; implementations execute native calls directly. */
internal interface PresentationSurface : AutoCloseable {
    val textureFormat: GPUTextureFormat
    val width: Int
    val height: Int
    fun configureForAdapter(adapterHandle: Long, requiredFormat: GPUTextureFormat? = null): SurfaceConfiguration
    fun configure(width: Int, height: Int)
    fun acquireFrame(): BorrowedSurfaceTexture
    fun present(texture: BorrowedSurfaceTexture)
}

/** Owns the native presentation target for the Dawn surface's lifetime. */
internal interface SurfaceHost : AutoCloseable {
    val backend: DawnBackend
    fun pixelSize(): Pair<Int, Int>
    fun createSurface(bridge: NativeBridge, deviceHandle: Long): PresentationSurface
}
