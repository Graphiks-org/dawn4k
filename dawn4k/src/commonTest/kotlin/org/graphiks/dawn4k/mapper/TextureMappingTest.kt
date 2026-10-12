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
import org.graphiks.dawn4k.native.WGPUSType_TextureBindingViewDimension
import org.graphiks.dawn4k.native.WGPUTextureAspect_All
import org.graphiks.dawn4k.native.WGPUTextureAspect_DepthOnly
import org.graphiks.dawn4k.native.WGPUTextureAspect_StencilOnly
import org.graphiks.dawn4k.native.WGPUTextureComponentSwizzleDescriptor
import org.graphiks.dawn4k.native.WGPUTextureBindingViewDimension
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
        val mapped = GPUTextureFormat.entries.map { it.value }
        assertEquals(GPUTextureFormat.entries.size, mapped.toSet().size)
        assertEquals(WGPUTextureFormat_RGBA8Unorm, GPUTextureFormat.RGBA8Unorm.value)
        assertEquals(WGPUTextureFormat_Depth32Float, GPUTextureFormat.Depth32Float.value)
        assertEquals(WGPUTextureFormat_BGRA8Unorm, GPUTextureFormat.BGRA8Unorm.value)
        assertEquals(WGPUTextureFormat_ASTC12x12UnormSrgb, GPUTextureFormat.ASTC12x12UnormSrgb.value)
    }

    @Test
    fun textureDimensionTableIsExhaustive() {
        assertEquals(WGPUTextureDimension_1D, GPUTextureDimension.OneD.value)
        assertEquals(WGPUTextureDimension_2D, GPUTextureDimension.TwoD.value)
        assertEquals(WGPUTextureDimension_3D, GPUTextureDimension.ThreeD.value)
    }

    @Test
    fun textureViewDimensionTableIsExhaustive() {
        assertEquals(WGPUTextureViewDimension_1D, GPUTextureViewDimension.OneD.value)
        assertEquals(WGPUTextureViewDimension_2D, GPUTextureViewDimension.TwoD.value)
        assertEquals(WGPUTextureViewDimension_2DArray, GPUTextureViewDimension.TwoDArray.value)
        assertEquals(WGPUTextureViewDimension_Cube, GPUTextureViewDimension.Cube.value)
        assertEquals(WGPUTextureViewDimension_CubeArray, GPUTextureViewDimension.CubeArray.value)
        assertEquals(WGPUTextureViewDimension_3D, GPUTextureViewDimension.ThreeD.value)
    }

    @Test
    fun textureAspectTableIsExhaustive() {
        assertEquals(WGPUTextureAspect_All, GPUTextureAspect.All.value)
        assertEquals(WGPUTextureAspect_StencilOnly, GPUTextureAspect.StencilOnly.value)
        assertEquals(WGPUTextureAspect_DepthOnly, GPUTextureAspect.DepthOnly.value)
    }

    @Test
    fun addressModeTableIsExhaustive() {
        assertEquals(WGPUAddressMode_ClampToEdge, GPUAddressMode.ClampToEdge.value)
        assertEquals(WGPUAddressMode_Repeat, GPUAddressMode.Repeat.value)
        assertEquals(WGPUAddressMode_MirrorRepeat, GPUAddressMode.MirrorRepeat.value)
    }

    @Test
    fun filterModeTableIsExhaustive() {
        assertEquals(WGPUFilterMode_Nearest, GPUFilterMode.Nearest.value)
        assertEquals(WGPUFilterMode_Linear, GPUFilterMode.Linear.value)
    }

    @Test
    fun mipmapFilterModeTableIsExhaustive() {
        assertEquals(WGPUMipmapFilterMode_Nearest, GPUMipmapFilterMode.Nearest.value)
        assertEquals(WGPUMipmapFilterMode_Linear, GPUMipmapFilterMode.Linear.value)
    }

    @Test
    fun compareFunctionTableIsExhaustive() {
        assertEquals(WGPUCompareFunction_Never, GPUCompareFunction.Never.value)
        assertEquals(WGPUCompareFunction_Less, GPUCompareFunction.Less.value)
        assertEquals(WGPUCompareFunction_Equal, GPUCompareFunction.Equal.value)
        assertEquals(WGPUCompareFunction_LessEqual, GPUCompareFunction.LessEqual.value)
        assertEquals(WGPUCompareFunction_Greater, GPUCompareFunction.Greater.value)
        assertEquals(WGPUCompareFunction_NotEqual, GPUCompareFunction.NotEqual.value)
        assertEquals(WGPUCompareFunction_GreaterEqual, GPUCompareFunction.GreaterEqual.value)
        assertEquals(WGPUCompareFunction_Always, GPUCompareFunction.Always.value)
    }

    @Test
    fun queryTypeTableIsExhaustive() {
        assertEquals(WGPUQueryType_Occlusion, GPUQueryType.Occlusion.value)
        assertEquals(WGPUQueryType_Timestamp, GPUQueryType.Timestamp.value)
    }

    @Test
    fun primitiveTopologyTableIsExhaustive() {
        assertEquals(WGPUPrimitiveTopology_PointList, GPUPrimitiveTopology.PointList.value)
        assertEquals(WGPUPrimitiveTopology_LineList, GPUPrimitiveTopology.LineList.value)
        assertEquals(WGPUPrimitiveTopology_LineStrip, GPUPrimitiveTopology.LineStrip.value)
        assertEquals(WGPUPrimitiveTopology_TriangleList, GPUPrimitiveTopology.TriangleList.value)
        assertEquals(WGPUPrimitiveTopology_TriangleStrip, GPUPrimitiveTopology.TriangleStrip.value)
    }

    @Test
    fun indexFormatTableIsExhaustive() {
        assertEquals(WGPUIndexFormat_Uint16, GPUIndexFormat.Uint16.value)
        assertEquals(WGPUIndexFormat_Uint32, GPUIndexFormat.Uint32.value)
    }

    @Test
    fun frontFaceTableIsExhaustive() {
        assertEquals(WGPUFrontFace_CCW, GPUFrontFace.CCW.value)
        assertEquals(WGPUFrontFace_CW, GPUFrontFace.CW.value)
    }

    @Test
    fun cullModeTableIsExhaustive() {
        assertEquals(WGPUCullMode_None, GPUCullMode.None.value)
        assertEquals(WGPUCullMode_Front, GPUCullMode.Front.value)
        assertEquals(WGPUCullMode_Back, GPUCullMode.Back.value)
    }

    @Test
    fun vertexStepModeTableIsExhaustive() {
        assertEquals(WGPUVertexStepMode_Vertex, GPUVertexStepMode.Vertex.value)
        assertEquals(WGPUVertexStepMode_Instance, GPUVertexStepMode.Instance.value)
    }

    @Test
    fun vertexFormatTableMapsEveryEntryDistinctly() {
        val mapped = GPUVertexFormat.entries.map { it.value }
        assertEquals(GPUVertexFormat.entries.size, mapped.toSet().size)
        assertEquals(WGPUVertexFormat_Float32x3, GPUVertexFormat.Float32x3.value)
        assertEquals(WGPUVertexFormat_Unorm8x4BGRA, GPUVertexFormat.Unorm8x4BGRA.value)
        // The WebGPU name differs from the Dawn name for the packed unorm format.
        assertEquals(WGPUVertexFormat_Unorm10_10_10_2, GPUVertexFormat.Unorm1010102.value)
    }

    @Test
    fun blendFactorTableIsExhaustive() {
        assertEquals(WGPUBlendFactor_Zero, GPUBlendFactor.Zero.value)
        assertEquals(WGPUBlendFactor_One, GPUBlendFactor.One.value)
        assertEquals(WGPUBlendFactor_Src, GPUBlendFactor.Src.value)
        assertEquals(WGPUBlendFactor_OneMinusSrc, GPUBlendFactor.OneMinusSrc.value)
        assertEquals(WGPUBlendFactor_SrcAlpha, GPUBlendFactor.SrcAlpha.value)
        assertEquals(WGPUBlendFactor_Dst, GPUBlendFactor.Dst.value)
    }

    @Test
    fun blendOperationTableIsExhaustive() {
        assertEquals(WGPUBlendOperation_Add, GPUBlendOperation.Add.value)
        assertEquals(WGPUBlendOperation_Subtract, GPUBlendOperation.Subtract.value)
        assertEquals(WGPUBlendOperation_ReverseSubtract, GPUBlendOperation.ReverseSubtract.value)
        assertEquals(WGPUBlendOperation_Min, GPUBlendOperation.Min.value)
        assertEquals(WGPUBlendOperation_Max, GPUBlendOperation.Max.value)
    }

    @Test
    fun stencilOperationTableIsExhaustive() {
        assertEquals(WGPUStencilOperation_Keep, GPUStencilOperation.Keep.value)
        assertEquals(WGPUStencilOperation_Zero, GPUStencilOperation.Zero.value)
        assertEquals(WGPUStencilOperation_Replace, GPUStencilOperation.Replace.value)
        assertEquals(WGPUStencilOperation_IncrementWrap, GPUStencilOperation.IncrementWrap.value)
    }

    @Test
    fun textureSampleTypeTableIsExhaustive() {
        assertEquals(WGPUTextureSampleType_BindingNotUsed, GPUTextureSampleType.BindingNotUsed.value)
        assertEquals(WGPUTextureSampleType_Float, GPUTextureSampleType.Float.value)
        assertEquals(WGPUTextureSampleType_UnfilterableFloat, GPUTextureSampleType.UnfilterableFloat.value)
        assertEquals(WGPUTextureSampleType_Depth, GPUTextureSampleType.Depth.value)
        assertEquals(WGPUTextureSampleType_Sint, GPUTextureSampleType.Sint.value)
        assertEquals(WGPUTextureSampleType_Uint, GPUTextureSampleType.Uint.value)
    }

    @Test
    fun samplerBindingTypeTableIsExhaustive() {
        assertEquals(WGPUSamplerBindingType_BindingNotUsed, GPUSamplerBindingType.BindingNotUsed.value)
        assertEquals(WGPUSamplerBindingType_Filtering, GPUSamplerBindingType.Filtering.value)
        assertEquals(WGPUSamplerBindingType_NonFiltering, GPUSamplerBindingType.NonFiltering.value)
        assertEquals(WGPUSamplerBindingType_Comparison, GPUSamplerBindingType.Comparison.value)
    }

    @Test
    fun storageTextureAccessTableIsExhaustive() {
        assertEquals(WGPUStorageTextureAccess_BindingNotUsed, GPUStorageTextureAccess.BindingNotUsed.value)
        assertEquals(WGPUStorageTextureAccess_WriteOnly, GPUStorageTextureAccess.WriteOnly.value)
        assertEquals(WGPUStorageTextureAccess_ReadOnly, GPUStorageTextureAccess.ReadOnly.value)
        assertEquals(WGPUStorageTextureAccess_ReadWrite, GPUStorageTextureAccess.ReadWrite.value)
    }

    @Test
    fun colorWriteMaskCombinesFlags() {
        assertEquals(WGPUColorWriteMask_Red, GPUColorWrite.Red.value)
        assertEquals(
            WGPUColorWriteMask_Red or WGPUColorWriteMask_Green or WGPUColorWriteMask_Blue,
            (GPUColorWrite.Red or GPUColorWrite.Green or GPUColorWrite.Blue).value,
        )
        assertEquals(WGPUColorWriteMask_All, GPUColorWrite.All.value)
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
    fun absentTextureBindingViewDimensionChainsNothing() = memoryScope { allocator ->
        val native = allocator.allocateTextureDescriptor(
            TextureDescriptor(
                size = Extent3D(4u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.TextureBinding,
            ),
        )
        assertNull(native.nextInChain)
    }

    @Test
    fun explicitTextureBindingViewDimensionReachesTheNativeDescriptor() = memoryScope { allocator ->
        val native = allocator.allocateTextureDescriptor(
            TextureDescriptor(
                size = Extent3D(width = 4u, height = 4u, depthOrArrayLayers = 6u),
                format = GPUTextureFormat.RGBA8Unorm,
                usage = GPUTextureUsage.TextureBinding,
                textureBindingViewDimension = GPUTextureViewDimension.Cube,
            ),
        )
        val chain = native.nextInChain ?: fail("an explicit texture binding view dimension must reach Dawn")
        assertEquals(WGPUSType_TextureBindingViewDimension, chain.sType)
        assertNull(chain.next)
        val dimension = WGPUTextureBindingViewDimension(chain.handler)
        assertEquals(WGPUTextureViewDimension_Cube, dimension.textureBindingViewDimension)
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
            (GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc).value,
        )
    }
}
