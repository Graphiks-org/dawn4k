package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUIndexFormat_Undefined
import org.graphiks.dawn4k.native.WGPULoadOp_Undefined
import org.graphiks.dawn4k.native.WGPUStoreOp_Undefined
import org.graphiks.webgpu.*
import kotlin.test.Test
import kotlin.test.assertEquals

/** Checks every Kotlin entry against its named Dawn constant, independently of numeric values. */
class EnumsMappingTest {
    @Test
    fun absentEnumsUseNativeUndefined() {
        assertEquals(WGPULoadOp_Undefined, (null as GPULoadOp?).toNativeOrUndefined)
        assertEquals(WGPUStoreOp_Undefined, (null as GPUStoreOp?).toNativeOrUndefined)
        assertEquals(WGPUIndexFormat_Undefined, (null as GPUIndexFormat?).toNativeOrUndefined)
        GPULoadOp.entries.forEach { assertEquals(it.expectedNativeLoadOp(), (it as GPULoadOp?).toNativeOrUndefined) }
        GPUStoreOp.entries.forEach { assertEquals(it.expectedNativeStoreOp(), (it as GPUStoreOp?).toNativeOrUndefined) }
        GPUIndexFormat.entries.forEach { assertEquals(it.expectedNativeIndexFormat(), (it as GPUIndexFormat?).toNativeOrUndefined) }
    }

    @Test
    fun enumValuesMatchDawnConstants() {
        GPUTextureFormat.entries.forEach { assertEquals(it.expectedNativeTextureFormat(), it.value, it.name) }
        GPUTextureDimension.entries.forEach { assertEquals(it.expectedNativeTextureDimension(), it.value, it.name) }
        GPUTextureViewDimension.entries.forEach { assertEquals(it.expectedNativeTextureViewDimension(), it.value, it.name) }
        GPUTextureAspect.entries.forEach { assertEquals(it.expectedNativeTextureAspect(), it.value, it.name) }
        GPUAddressMode.entries.forEach { assertEquals(it.expectedNativeAddressMode(), it.value, it.name) }
        GPUFilterMode.entries.forEach { assertEquals(it.expectedNativeFilterMode(), it.value, it.name) }
        GPUMipmapFilterMode.entries.forEach { assertEquals(it.expectedNativeMipmapFilterMode(), it.value, it.name) }
        GPUCompareFunction.entries.forEach { assertEquals(it.expectedNativeCompareFunction(), it.value, it.name) }
        GPUQueryType.entries.forEach { assertEquals(it.expectedNativeQueryType(), it.value, it.name) }
        GPUPrimitiveTopology.entries.forEach { assertEquals(it.expectedNativePrimitiveTopology(), it.value, it.name) }
        GPUIndexFormat.entries.forEach { assertEquals(it.expectedNativeIndexFormat(), it.value, it.name) }
        GPUFrontFace.entries.forEach { assertEquals(it.expectedNativeFrontFace(), it.value, it.name) }
        GPUCullMode.entries.forEach { assertEquals(it.expectedNativeCullMode(), it.value, it.name) }
        GPUVertexStepMode.entries.forEach { assertEquals(it.expectedNativeVertexStepMode(), it.value, it.name) }
        GPUVertexFormat.entries.forEach { assertEquals(it.expectedNativeVertexFormat(), it.value, it.name) }
        GPUBlendFactor.entries.forEach { assertEquals(it.expectedNativeBlendFactor(), it.value, it.name) }
        GPUBlendOperation.entries.forEach { assertEquals(it.expectedNativeBlendOperation(), it.value, it.name) }
        GPUStencilOperation.entries.forEach { assertEquals(it.expectedNativeStencilOperation(), it.value, it.name) }
        GPUTextureSampleType.entries.forEach { assertEquals(it.expectedNativeTextureSampleType(), it.value, it.name) }
        GPUSamplerBindingType.entries.forEach { assertEquals(it.expectedNativeSamplerBindingType(), it.value, it.name) }
        GPUStorageTextureAccess.entries.forEach { assertEquals(it.expectedNativeStorageTextureAccess(), it.value, it.name) }
        GPUTextureSwizzleSource.entries.forEach { assertEquals(it.expectedNativeComponentSwizzle(), it.toNativeComponentSwizzle(), it.name) }
        GPULoadOp.entries.forEach { assertEquals(it.expectedNativeLoadOp(), it.value, it.name) }
        GPUStoreOp.entries.forEach { assertEquals(it.expectedNativeStoreOp(), it.value, it.name) }
        GPUErrorFilter.entries.forEach { assertEquals(it.expectedNativeErrorFilter(), it.value, it.name) }
    }

    @Test
    fun textureUsagePreservesEveryCombination() {
        val flags = listOf(
            GPUTextureUsage.CopySrc,
            GPUTextureUsage.CopyDst,
            GPUTextureUsage.TextureBinding,
            GPUTextureUsage.StorageBinding,
            GPUTextureUsage.RenderAttachment,
            GPUTextureUsage.TransientAttachment,
        )
        for (bits in 0 until (1 shl flags.size)) {
            val usage = flags.filterIndexed { index, _ -> bits and (1 shl index) != 0 }
                .fold(GPUTextureUsage.None) { combined, flag -> combined or flag }
            assertEquals(usage.expectedNativeTextureUsage(), usage.value, "combination $bits")
        }
    }

    @Test
    fun colorWritePreservesEveryCombination() {
        val flags = listOf(GPUColorWrite.Red, GPUColorWrite.Green, GPUColorWrite.Blue, GPUColorWrite.Alpha)
        for (bits in 0 until (1 shl flags.size)) {
            val mask = flags.filterIndexed { index, _ -> bits and (1 shl index) != 0 }
                .fold(GPUColorWrite.None) { combined, flag -> combined or flag }
            assertEquals(mask.expectedNativeColorWriteMask(), mask.value, "combination $bits")
        }
    }
}
