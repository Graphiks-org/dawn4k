package org.graphiks.dawn4k.toolkit

import org.graphiks.dawn4k.DawnTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor

/** Creates an ordinary application-owned texture. Call on the session owner and close it after use. */
fun DawnSession.createRenderTexture(
    width: UInt,
    height: UInt,
    format: GPUTextureFormat,
    usage: GPUTextureUsage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
): DawnTexture {
    require(width > 0u && height > 0u) { "render texture dimensions must be positive" }
    owner.checkThread()
    return device.createTexture(TextureDescriptor(size = Extent3D(width, height), format = format, usage = usage)) as DawnTexture
}
