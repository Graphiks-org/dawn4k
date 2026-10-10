package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUAddressMode_ClampToEdge
import org.graphiks.dawn4k.native.WGPUAddressMode_MirrorRepeat
import org.graphiks.dawn4k.native.WGPUAddressMode_Repeat
import org.graphiks.dawn4k.native.WGPUBlendFactor_Dst
import org.graphiks.dawn4k.native.WGPUBlendFactor_One
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusSrc
import org.graphiks.dawn4k.native.WGPUBlendFactor_Src
import org.graphiks.dawn4k.native.WGPUBlendFactor_SrcAlpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_Zero
import org.graphiks.dawn4k.native.WGPUBlendOperation_Add
import org.graphiks.dawn4k.native.WGPUBlendOperation_Max
import org.graphiks.dawn4k.native.WGPUBlendOperation_Min
import org.graphiks.dawn4k.native.WGPUBlendOperation_ReverseSubtract
import org.graphiks.dawn4k.native.WGPUBlendOperation_Subtract
import org.graphiks.dawn4k.native.WGPUColorWriteMask_All
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Blue
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Green
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Red
import org.graphiks.dawn4k.native.WGPUCompareFunction_Always
import org.graphiks.dawn4k.native.WGPUCompareFunction_Equal
import org.graphiks.dawn4k.native.WGPUCompareFunction_Greater
import org.graphiks.dawn4k.native.WGPUCompareFunction_GreaterEqual
import org.graphiks.dawn4k.native.WGPUCompareFunction_Less
import org.graphiks.dawn4k.native.WGPUCompareFunction_LessEqual
import org.graphiks.dawn4k.native.WGPUCompareFunction_Never
import org.graphiks.dawn4k.native.WGPUCompareFunction_NotEqual
import org.graphiks.dawn4k.native.WGPUCompareFunction_Undefined
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_B
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_One
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_R
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_Zero
import org.graphiks.dawn4k.native.WGPUCullMode_Back
import org.graphiks.dawn4k.native.WGPUCullMode_Front
import org.graphiks.dawn4k.native.WGPUCullMode_None
import org.graphiks.dawn4k.native.WGPUFilterMode_Linear
import org.graphiks.dawn4k.native.WGPUFilterMode_Nearest
import org.graphiks.dawn4k.native.WGPUFrontFace_CCW
import org.graphiks.dawn4k.native.WGPUFrontFace_CW
import org.graphiks.dawn4k.native.WGPUIndexFormat_Uint16
import org.graphiks.dawn4k.native.WGPUIndexFormat_Uint32
import org.graphiks.dawn4k.native.WGPUMipmapFilterMode_Linear
import org.graphiks.dawn4k.native.WGPUMipmapFilterMode_Nearest
import org.graphiks.dawn4k.native.WGPUOptionalBool_False
import org.graphiks.dawn4k.native.WGPUOptionalBool_True
import org.graphiks.dawn4k.native.WGPUOptionalBool_Undefined
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_LineList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_LineStrip
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_PointList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_TriangleList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_TriangleStrip
import org.graphiks.dawn4k.native.WGPUQueryType_Occlusion
import org.graphiks.dawn4k.native.WGPUQueryType_Timestamp
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_Comparison
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_Filtering
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_NonFiltering
import org.graphiks.dawn4k.native.WGPUStencilOperation_IncrementWrap
import org.graphiks.dawn4k.native.WGPUStencilOperation_Keep
import org.graphiks.dawn4k.native.WGPUStencilOperation_Replace
import org.graphiks.dawn4k.native.WGPUStencilOperation_Zero
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_ReadOnly
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_ReadWrite
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_WriteOnly
import org.graphiks.dawn4k.native.WGPUSType_TextureComponentSwizzleDescriptor
import org.graphiks.dawn4k.native.WGPUTextureAspect_All
import org.graphiks.dawn4k.native.WGPUTextureAspect_DepthOnly
import org.graphiks.dawn4k.native.WGPUTextureAspect_StencilOnly
import org.graphiks.dawn4k.native.WGPUTextureComponentSwizzleDescriptor
import org.graphiks.dawn4k.native.WGPUTextureDimension_1D
import org.graphiks.dawn4k.native.WGPUTextureDimension_2D
import org.graphiks.dawn4k.native.WGPUTextureDimension_3D
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC12x12UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BGRA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth32Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_Undefined
import org.graphiks.dawn4k.native.WGPUTextureSampleType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Depth
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Float
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Sint
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Uint
import org.graphiks.dawn4k.native.WGPUTextureSampleType_UnfilterableFloat
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_1D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_2D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_2DArray
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_3D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Cube
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_CubeArray
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Undefined
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float32x3
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm10_10_10_2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm8x4BGRA
import org.graphiks.dawn4k.native.WGPUVertexStepMode_Instance
import org.graphiks.dawn4k.native.WGPUVertexStepMode_Vertex
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUAddressMode
import org.graphiks.webgpu.GPUBlendFactor
import org.graphiks.webgpu.GPUBlendOperation
import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUCullMode
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUFrontFace
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUPrimitiveTopology
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUSamplerBindingType
import org.graphiks.webgpu.GPUStencilOperation
import org.graphiks.webgpu.GPUStorageTextureAccess
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureSwizzle
import org.graphiks.webgpu.GPUTextureSwizzleSource
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.GPUVertexStepMode
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.fail

/**
 * Pure mapping tests for the texture/sampler/render/query families: every
 * conversion table maps its Kotlin value through its named Dawn constant (never
 * assuming the numeric values coincide), the descriptor mappers fill every field
 * explicitly, and the texture-view swizzle chains only when the mapping is
 * non-identity.
 */
class TextureMappingTest {

    @Test
    fun textureFormatTableMapsEveryEntryDistinctly() {
        val mapped = GPUTextureFormat.entries.map { it.toNativeTextureFormat() }
        assertEquals(GPUTextureFormat.entries.size, mapped.toSet().size)
        assertEquals(WGPUTextureFormat_RGBA8Unorm, GPUTextureFormat.RGBA8Unorm.toNativeTextureFormat())
        assertEquals(WGPUTextureFormat_Depth32Float, GPUTextureFormat.Depth32Float.toNativeTextureFormat())
        assertEquals(WGPUTextureFormat_BGRA8Unorm, GPUTextureFormat.BGRA8Unorm.toNativeTextureFormat())
        assertEquals(WGPUTextureFormat_ASTC12x12UnormSrgb, GPUTextureFormat.ASTC12x12UnormSrgb.toNativeTextureFormat())
    }

    @Test
    fun textureDimensionTableIsExhaustive() {
        assertEquals(WGPUTextureDimension_1D, GPUTextureDimension.OneD.toNativeTextureDimension())
        assertEquals(WGPUTextureDimension_2D, GPUTextureDimension.TwoD.toNativeTextureDimension())
        assertEquals(WGPUTextureDimension_3D, GPUTextureDimension.ThreeD.toNativeTextureDimension())
    }

    @Test
    fun textureViewDimensionTableIsExhaustive() {
        assertEquals(WGPUTextureViewDimension_1D, GPUTextureViewDimension.OneD.toNativeTextureViewDimension())
        assertEquals(WGPUTextureViewDimension_2D, GPUTextureViewDimension.TwoD.toNativeTextureViewDimension())
        assertEquals(WGPUTextureViewDimension_2DArray, GPUTextureViewDimension.TwoDArray.toNativeTextureViewDimension())
        assertEquals(WGPUTextureViewDimension_Cube, GPUTextureViewDimension.Cube.toNativeTextureViewDimension())
        assertEquals(WGPUTextureViewDimension_CubeArray, GPUTextureViewDimension.CubeArray.toNativeTextureViewDimension())
        assertEquals(WGPUTextureViewDimension_3D, GPUTextureViewDimension.ThreeD.toNativeTextureViewDimension())
    }

    @Test
    fun textureAspectTableIsExhaustive() {
        assertEquals(WGPUTextureAspect_All, GPUTextureAspect.All.toNativeTextureAspect())
        assertEquals(WGPUTextureAspect_StencilOnly, GPUTextureAspect.StencilOnly.toNativeTextureAspect())
        assertEquals(WGPUTextureAspect_DepthOnly, GPUTextureAspect.DepthOnly.toNativeTextureAspect())
    }

    @Test
    fun addressModeTableIsExhaustive() {
        assertEquals(WGPUAddressMode_ClampToEdge, GPUAddressMode.ClampToEdge.toNativeAddressMode())
        assertEquals(WGPUAddressMode_Repeat, GPUAddressMode.Repeat.toNativeAddressMode())
        assertEquals(WGPUAddressMode_MirrorRepeat, GPUAddressMode.MirrorRepeat.toNativeAddressMode())
    }

    @Test
    fun filterModeTableIsExhaustive() {
        assertEquals(WGPUFilterMode_Nearest, GPUFilterMode.Nearest.toNativeFilterMode())
        assertEquals(WGPUFilterMode_Linear, GPUFilterMode.Linear.toNativeFilterMode())
    }

    @Test
    fun mipmapFilterModeTableIsExhaustive() {
        assertEquals(WGPUMipmapFilterMode_Nearest, GPUMipmapFilterMode.Nearest.toNativeMipmapFilterMode())
        assertEquals(WGPUMipmapFilterMode_Linear, GPUMipmapFilterMode.Linear.toNativeMipmapFilterMode())
    }

    @Test
    fun compareFunctionTableIsExhaustive() {
        assertEquals(WGPUCompareFunction_Never, GPUCompareFunction.Never.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_Less, GPUCompareFunction.Less.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_Equal, GPUCompareFunction.Equal.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_LessEqual, GPUCompareFunction.LessEqual.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_Greater, GPUCompareFunction.Greater.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_NotEqual, GPUCompareFunction.NotEqual.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_GreaterEqual, GPUCompareFunction.GreaterEqual.toNativeCompareFunction())
        assertEquals(WGPUCompareFunction_Always, GPUCompareFunction.Always.toNativeCompareFunction())
    }

    @Test
    fun queryTypeTableIsExhaustive() {
        assertEquals(WGPUQueryType_Occlusion, GPUQueryType.Occlusion.toNativeQueryType())
        assertEquals(WGPUQueryType_Timestamp, GPUQueryType.Timestamp.toNativeQueryType())
    }

    @Test
    fun primitiveTopologyTableIsExhaustive() {
        assertEquals(WGPUPrimitiveTopology_PointList, GPUPrimitiveTopology.PointList.toNativePrimitiveTopology())
        assertEquals(WGPUPrimitiveTopology_LineList, GPUPrimitiveTopology.LineList.toNativePrimitiveTopology())
        assertEquals(WGPUPrimitiveTopology_LineStrip, GPUPrimitiveTopology.LineStrip.toNativePrimitiveTopology())
        assertEquals(WGPUPrimitiveTopology_TriangleList, GPUPrimitiveTopology.TriangleList.toNativePrimitiveTopology())
        assertEquals(WGPUPrimitiveTopology_TriangleStrip, GPUPrimitiveTopology.TriangleStrip.toNativePrimitiveTopology())
    }

    @Test
    fun indexFormatTableIsExhaustive() {
        assertEquals(WGPUIndexFormat_Uint16, GPUIndexFormat.Uint16.toNativeIndexFormat())
        assertEquals(WGPUIndexFormat_Uint32, GPUIndexFormat.Uint32.toNativeIndexFormat())
    }

    @Test
    fun frontFaceTableIsExhaustive() {
        assertEquals(WGPUFrontFace_CCW, GPUFrontFace.CCW.toNativeFrontFace())
        assertEquals(WGPUFrontFace_CW, GPUFrontFace.CW.toNativeFrontFace())
    }

    @Test
    fun cullModeTableIsExhaustive() {
        assertEquals(WGPUCullMode_None, GPUCullMode.None.toNativeCullMode())
        assertEquals(WGPUCullMode_Front, GPUCullMode.Front.toNativeCullMode())
        assertEquals(WGPUCullMode_Back, GPUCullMode.Back.toNativeCullMode())
    }

    @Test
    fun vertexStepModeTableIsExhaustive() {
        assertEquals(WGPUVertexStepMode_Vertex, GPUVertexStepMode.Vertex.toNativeVertexStepMode())
        assertEquals(WGPUVertexStepMode_Instance, GPUVertexStepMode.Instance.toNativeVertexStepMode())
    }

    @Test
    fun vertexFormatTableMapsEveryEntryDistinctly() {
        val mapped = GPUVertexFormat.entries.map { it.toNativeVertexFormat() }
        assertEquals(GPUVertexFormat.entries.size, mapped.toSet().size)
        assertEquals(WGPUVertexFormat_Float32x3, GPUVertexFormat.Float32x3.toNativeVertexFormat())
        assertEquals(WGPUVertexFormat_Unorm8x4BGRA, GPUVertexFormat.Unorm8x4BGRA.toNativeVertexFormat())
        // The WebGPU name differs from the Dawn name for the packed unorm format.
        assertEquals(WGPUVertexFormat_Unorm10_10_10_2, GPUVertexFormat.Unorm1010102.toNativeVertexFormat())
    }

    @Test
    fun blendFactorTableIsExhaustive() {
        assertEquals(WGPUBlendFactor_Zero, GPUBlendFactor.Zero.toNativeBlendFactor())
        assertEquals(WGPUBlendFactor_One, GPUBlendFactor.One.toNativeBlendFactor())
        assertEquals(WGPUBlendFactor_Src, GPUBlendFactor.Src.toNativeBlendFactor())
        assertEquals(WGPUBlendFactor_OneMinusSrc, GPUBlendFactor.OneMinusSrc.toNativeBlendFactor())
        assertEquals(WGPUBlendFactor_SrcAlpha, GPUBlendFactor.SrcAlpha.toNativeBlendFactor())
        assertEquals(WGPUBlendFactor_Dst, GPUBlendFactor.Dst.toNativeBlendFactor())
    }

    @Test
    fun blendOperationTableIsExhaustive() {
        assertEquals(WGPUBlendOperation_Add, GPUBlendOperation.Add.toNativeBlendOperation())
        assertEquals(WGPUBlendOperation_Subtract, GPUBlendOperation.Subtract.toNativeBlendOperation())
        assertEquals(WGPUBlendOperation_ReverseSubtract, GPUBlendOperation.ReverseSubtract.toNativeBlendOperation())
        assertEquals(WGPUBlendOperation_Min, GPUBlendOperation.Min.toNativeBlendOperation())
        assertEquals(WGPUBlendOperation_Max, GPUBlendOperation.Max.toNativeBlendOperation())
    }

    @Test
    fun stencilOperationTableIsExhaustive() {
        assertEquals(WGPUStencilOperation_Keep, GPUStencilOperation.Keep.toNativeStencilOperation())
        assertEquals(WGPUStencilOperation_Zero, GPUStencilOperation.Zero.toNativeStencilOperation())
        assertEquals(WGPUStencilOperation_Replace, GPUStencilOperation.Replace.toNativeStencilOperation())
        assertEquals(WGPUStencilOperation_IncrementWrap, GPUStencilOperation.IncrementWrap.toNativeStencilOperation())
    }

    @Test
    fun textureSampleTypeTableIsExhaustive() {
        assertEquals(WGPUTextureSampleType_BindingNotUsed, GPUTextureSampleType.BindingNotUsed.toNativeTextureSampleType())
        assertEquals(WGPUTextureSampleType_Float, GPUTextureSampleType.Float.toNativeTextureSampleType())
        assertEquals(WGPUTextureSampleType_UnfilterableFloat, GPUTextureSampleType.UnfilterableFloat.toNativeTextureSampleType())
        assertEquals(WGPUTextureSampleType_Depth, GPUTextureSampleType.Depth.toNativeTextureSampleType())
        assertEquals(WGPUTextureSampleType_Sint, GPUTextureSampleType.Sint.toNativeTextureSampleType())
        assertEquals(WGPUTextureSampleType_Uint, GPUTextureSampleType.Uint.toNativeTextureSampleType())
    }

    @Test
    fun samplerBindingTypeTableIsExhaustive() {
        assertEquals(WGPUSamplerBindingType_BindingNotUsed, GPUSamplerBindingType.BindingNotUsed.toNativeSamplerBindingType())
        assertEquals(WGPUSamplerBindingType_Filtering, GPUSamplerBindingType.Filtering.toNativeSamplerBindingType())
        assertEquals(WGPUSamplerBindingType_NonFiltering, GPUSamplerBindingType.NonFiltering.toNativeSamplerBindingType())
        assertEquals(WGPUSamplerBindingType_Comparison, GPUSamplerBindingType.Comparison.toNativeSamplerBindingType())
    }

    @Test
    fun storageTextureAccessTableIsExhaustive() {
        assertEquals(WGPUStorageTextureAccess_BindingNotUsed, GPUStorageTextureAccess.BindingNotUsed.toNativeStorageTextureAccess())
        assertEquals(WGPUStorageTextureAccess_WriteOnly, GPUStorageTextureAccess.WriteOnly.toNativeStorageTextureAccess())
        assertEquals(WGPUStorageTextureAccess_ReadOnly, GPUStorageTextureAccess.ReadOnly.toNativeStorageTextureAccess())
        assertEquals(WGPUStorageTextureAccess_ReadWrite, GPUStorageTextureAccess.ReadWrite.toNativeStorageTextureAccess())
    }

    @Test
    fun colorWriteMaskCombinesFlags() {
        assertEquals(WGPUColorWriteMask_Red, GPUColorWrite.Red.toNativeColorWriteMask())
        assertEquals(
            WGPUColorWriteMask_Red or WGPUColorWriteMask_Green or WGPUColorWriteMask_Blue,
            (GPUColorWrite.Red or GPUColorWrite.Green or GPUColorWrite.Blue).toNativeColorWriteMask(),
        )
        assertEquals(WGPUColorWriteMask_All, GPUColorWrite.All.toNativeColorWriteMask())
    }

    @Test
    fun optionalBoolTableIsTriState() {
        assertEquals(WGPUOptionalBool_Undefined, null.toNativeOptionalBool())
        assertEquals(WGPUOptionalBool_True, true.toNativeOptionalBool())
        assertEquals(WGPUOptionalBool_False, false.toNativeOptionalBool())
    }

    @Test
    fun samplerDescriptorMapsCompareNullAndAnisotropy() = memoryScope { allocator ->
        val native = allocator.allocateSamplerDescriptor(
            SamplerDescriptor(
                addressModeU = GPUAddressMode.Repeat,
                compare = null,
                maxAnisotropy = 8u,
                lodMaxClamp = 16f,
            ),
        )
        assertEquals(WGPUAddressMode_Repeat, native.addressModeU)
        assertEquals(WGPUCompareFunction_Undefined, native.compare)
        assertEquals(8u, native.maxAnisotropy)
        assertEquals(16f, native.lodMaxClamp)
    }

    @Test
    fun samplerDescriptorMapsAComparisonSampler() = memoryScope { allocator ->
        val native = allocator.allocateSamplerDescriptor(SamplerDescriptor(compare = GPUCompareFunction.LessEqual))
        assertEquals(WGPUCompareFunction_LessEqual, native.compare)
    }

    @Test
    fun textureViewDescriptorMapsNullFieldsToDefaults() = memoryScope { allocator ->
        val native = allocator.allocateTextureViewDescriptor(
            TextureViewDescriptor(
                format = null,
                dimension = null,
                mipLevelCount = null,
                arrayLayerCount = null,
            ),
        )
        assertEquals(WGPUTextureFormat_Undefined, native.format)
        assertEquals(WGPUTextureViewDimension_Undefined, native.dimension)
        assertEquals(WGPU_MIP_LEVEL_COUNT_UNDEFINED, native.mipLevelCount)
        assertEquals(WGPU_ARRAY_LAYER_COUNT_UNDEFINED, native.arrayLayerCount)
        assertEquals(WGPUTextureAspect_All, native.aspect)
    }

    @Test
    fun textureViewDescriptorMapsAspectAndMipRange() = memoryScope { allocator ->
        val native = allocator.allocateTextureViewDescriptor(
            TextureViewDescriptor(
                aspect = GPUTextureAspect.DepthOnly,
                dimension = GPUTextureViewDimension.TwoD,
                baseMipLevel = 1u,
                mipLevelCount = 2u,
                baseArrayLayer = 3u,
                arrayLayerCount = 4u,
            ),
        )
        assertEquals(WGPUTextureAspect_DepthOnly, native.aspect)
        assertEquals(WGPUTextureViewDimension_2D, native.dimension)
        assertEquals(1u, native.baseMipLevel)
        assertEquals(2u, native.mipLevelCount)
        assertEquals(3u, native.baseArrayLayer)
        assertEquals(4u, native.arrayLayerCount)
    }

    @Test
    fun identitySwizzleChainsNothing() = memoryScope { allocator ->
        val native = allocator.allocateTextureViewDescriptor(TextureViewDescriptor())
        assertNull(native.nextInChain)
    }

    @Test
    fun nonIdentitySwizzleChainsTheSwizzleDescriptor() = memoryScope { allocator ->
        val native = allocator.allocateTextureViewDescriptor(
            TextureViewDescriptor(
                swizzle = GPUTextureSwizzle(
                    red = GPUTextureSwizzleSource.Blue,
                    green = GPUTextureSwizzleSource.Zero,
                    blue = GPUTextureSwizzleSource.One,
                    alpha = GPUTextureSwizzleSource.Red,
                ),
            ),
        )
        val chain = native.nextInChain ?: fail("a non-identity swizzle must chain a descriptor")
        assertEquals(WGPUSType_TextureComponentSwizzleDescriptor, chain.sType)
        assertNull(chain.next)
        val swizzle = WGPUTextureComponentSwizzleDescriptor(chain.handler)
        assertEquals(WGPUComponentSwizzle_B, swizzle.swizzle.r)
        assertEquals(WGPUComponentSwizzle_Zero, swizzle.swizzle.g)
        assertEquals(WGPUComponentSwizzle_One, swizzle.swizzle.b)
        assertEquals(WGPUComponentSwizzle_R, swizzle.swizzle.a)
    }

    @Test
    fun textureDescriptorMapsMetadataAndViewFormats() = memoryScope { allocator ->
        val native = allocator.allocateTextureDescriptor(
            TextureDescriptor(
                size = Extent3D(width = 8u, height = 4u, depthOrArrayLayers = 2u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.TextureBinding,
                mipLevelCount = 3u,
                sampleCount = 4u,
                dimension = GPUTextureDimension.ThreeD,
                viewFormats = listOf(GPUTextureFormat.RGBA8Unorm, GPUTextureFormat.BGRA8Unorm),
            ),
        )
        assertEquals(8u, native.size.width)
        assertEquals(4u, native.size.height)
        assertEquals(2u, native.size.depthOrArrayLayers)
        assertEquals(WGPUTextureFormat_RGBA8Unorm, native.format)
        assertEquals(WGPUTextureDimension_3D, native.dimension)
        assertEquals(3u, native.mipLevelCount)
        assertEquals(4u, native.sampleCount)
        assertEquals(2uL, native.viewFormatCount)
        val formats = native.viewFormats ?: fail("the view-format array was not allocated")
        val buffer = MemoryBuffer(formats, 8uL)
        assertEquals(WGPUTextureFormat_RGBA8Unorm, buffer.readUInt(0uL))
        assertEquals(WGPUTextureFormat_BGRA8Unorm, buffer.readUInt(4uL))
    }

    @Test
    fun querySetDescriptorMapsTypeAndCount() = memoryScope { allocator ->
        val native = allocator.allocateQuerySetDescriptor(QuerySetDescriptor(type = GPUQueryType.Occlusion, count = 2u))
        assertEquals(WGPUQueryType_Occlusion, native.type)
        assertEquals(2u, native.count)
    }

    @Test
    fun textureUsageTableCombinesFlags() {
        assertEquals(
            org.graphiks.dawn4k.native.WGPUTextureUsage_RenderAttachment or org.graphiks.dawn4k.native.WGPUTextureUsage_CopySrc,
            (GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc).toNativeTextureUsage(),
        )
    }
}
