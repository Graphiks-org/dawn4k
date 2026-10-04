package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUAddressMode
import org.graphiks.dawn4k.native.WGPUAddressMode_ClampToEdge
import org.graphiks.dawn4k.native.WGPUAddressMode_MirrorRepeat
import org.graphiks.dawn4k.native.WGPUAddressMode_Repeat
import org.graphiks.dawn4k.native.WGPUBlendFactor
import org.graphiks.dawn4k.native.WGPUBlendFactor_Constant
import org.graphiks.dawn4k.native.WGPUBlendFactor_Dst
import org.graphiks.dawn4k.native.WGPUBlendFactor_DstAlpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_One
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusConstant
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusDst
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusDstAlpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusSrc
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusSrc1
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusSrc1Alpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_OneMinusSrcAlpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_Src
import org.graphiks.dawn4k.native.WGPUBlendFactor_Src1
import org.graphiks.dawn4k.native.WGPUBlendFactor_Src1Alpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_SrcAlpha
import org.graphiks.dawn4k.native.WGPUBlendFactor_SrcAlphaSaturated
import org.graphiks.dawn4k.native.WGPUBlendFactor_Zero
import org.graphiks.dawn4k.native.WGPUBlendOperation
import org.graphiks.dawn4k.native.WGPUBlendOperation_Add
import org.graphiks.dawn4k.native.WGPUBlendOperation_Max
import org.graphiks.dawn4k.native.WGPUBlendOperation_Min
import org.graphiks.dawn4k.native.WGPUBlendOperation_ReverseSubtract
import org.graphiks.dawn4k.native.WGPUBlendOperation_Subtract
import org.graphiks.dawn4k.native.WGPUColorWriteMask
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Alpha
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Blue
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Green
import org.graphiks.dawn4k.native.WGPUColorWriteMask_None
import org.graphiks.dawn4k.native.WGPUColorWriteMask_Red
import org.graphiks.dawn4k.native.WGPUCompareFunction
import org.graphiks.dawn4k.native.WGPUCompareFunction_Always
import org.graphiks.dawn4k.native.WGPUCompareFunction_Equal
import org.graphiks.dawn4k.native.WGPUCompareFunction_Greater
import org.graphiks.dawn4k.native.WGPUCompareFunction_GreaterEqual
import org.graphiks.dawn4k.native.WGPUCompareFunction_Less
import org.graphiks.dawn4k.native.WGPUCompareFunction_LessEqual
import org.graphiks.dawn4k.native.WGPUCompareFunction_Never
import org.graphiks.dawn4k.native.WGPUCompareFunction_NotEqual
import org.graphiks.dawn4k.native.WGPUComponentSwizzle
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_A
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_B
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_G
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_One
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_R
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_Zero
import org.graphiks.dawn4k.native.WGPUCullMode
import org.graphiks.dawn4k.native.WGPUCullMode_Back
import org.graphiks.dawn4k.native.WGPUCullMode_Front
import org.graphiks.dawn4k.native.WGPUCullMode_None
import org.graphiks.dawn4k.native.WGPUFilterMode
import org.graphiks.dawn4k.native.WGPUFilterMode_Linear
import org.graphiks.dawn4k.native.WGPUFilterMode_Nearest
import org.graphiks.dawn4k.native.WGPUFrontFace
import org.graphiks.dawn4k.native.WGPUFrontFace_CCW
import org.graphiks.dawn4k.native.WGPUFrontFace_CW
import org.graphiks.dawn4k.native.WGPUIndexFormat
import org.graphiks.dawn4k.native.WGPUIndexFormat_Undefined
import org.graphiks.dawn4k.native.WGPUIndexFormat_Uint16
import org.graphiks.dawn4k.native.WGPUIndexFormat_Uint32
import org.graphiks.dawn4k.native.WGPULoadOp
import org.graphiks.dawn4k.native.WGPULoadOp_Clear
import org.graphiks.dawn4k.native.WGPULoadOp_Load
import org.graphiks.dawn4k.native.WGPULoadOp_Undefined
import org.graphiks.dawn4k.native.WGPUMipmapFilterMode
import org.graphiks.dawn4k.native.WGPUMipmapFilterMode_Linear
import org.graphiks.dawn4k.native.WGPUMipmapFilterMode_Nearest
import org.graphiks.dawn4k.native.WGPUOptionalBool
import org.graphiks.dawn4k.native.WGPUOptionalBool_False
import org.graphiks.dawn4k.native.WGPUOptionalBool_True
import org.graphiks.dawn4k.native.WGPUOptionalBool_Undefined
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_LineList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_LineStrip
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_PointList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_TriangleList
import org.graphiks.dawn4k.native.WGPUPrimitiveTopology_TriangleStrip
import org.graphiks.dawn4k.native.WGPUQueryType
import org.graphiks.dawn4k.native.WGPUQueryType_Occlusion
import org.graphiks.dawn4k.native.WGPUQueryType_Timestamp
import org.graphiks.dawn4k.native.WGPUSamplerBindingType
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_Comparison
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_Filtering
import org.graphiks.dawn4k.native.WGPUSamplerBindingType_NonFiltering
import org.graphiks.dawn4k.native.WGPUStencilOperation
import org.graphiks.dawn4k.native.WGPUStencilOperation_DecrementClamp
import org.graphiks.dawn4k.native.WGPUStencilOperation_DecrementWrap
import org.graphiks.dawn4k.native.WGPUStencilOperation_IncrementClamp
import org.graphiks.dawn4k.native.WGPUStencilOperation_IncrementWrap
import org.graphiks.dawn4k.native.WGPUStencilOperation_Invert
import org.graphiks.dawn4k.native.WGPUStencilOperation_Keep
import org.graphiks.dawn4k.native.WGPUStencilOperation_Replace
import org.graphiks.dawn4k.native.WGPUStencilOperation_Zero
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess
import org.graphiks.dawn4k.native.WGPUStoreOp
import org.graphiks.dawn4k.native.WGPUStoreOp_Discard
import org.graphiks.dawn4k.native.WGPUStoreOp_Store
import org.graphiks.dawn4k.native.WGPUStoreOp_Undefined
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_ReadOnly
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_ReadWrite
import org.graphiks.dawn4k.native.WGPUStorageTextureAccess_WriteOnly
import org.graphiks.dawn4k.native.WGPUTextureAspect
import org.graphiks.dawn4k.native.WGPUTextureAspect_All
import org.graphiks.dawn4k.native.WGPUTextureAspect_DepthOnly
import org.graphiks.dawn4k.native.WGPUTextureAspect_StencilOnly
import org.graphiks.dawn4k.native.WGPUTextureDimension
import org.graphiks.dawn4k.native.WGPUTextureDimension_1D
import org.graphiks.dawn4k.native.WGPUTextureDimension_2D
import org.graphiks.dawn4k.native.WGPUTextureDimension_3D
import org.graphiks.dawn4k.native.WGPUTextureFormat
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x10Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x10UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x5Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x5UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x6Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x6UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC10x8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC12x10Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC12x10UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC12x12Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC12x12UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC4x4Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC4x4UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC5x4Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC5x4UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC5x5Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC5x5UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC6x5Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC6x5UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC6x6Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC6x6UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x5Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x5UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x6Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x6UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ASTC8x8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC1RGBAUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC1RGBAUnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC2RGBAUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC2RGBAUnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC3RGBAUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC3RGBAUnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC4RUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC4RSnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC5RGUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC5RGSnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC6HRGBFloat
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC6HRGBUfloat
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC7RGBAUnorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BC7RGBAUnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_BGRA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_BGRA8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth16Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth24Plus
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth24PlusStencil8
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth32Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_Depth32FloatStencil8
import org.graphiks.dawn4k.native.WGPUTextureFormat_EACR11Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_EACR11Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_EACRG11Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_EACRG11Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGB8A1Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGB8A1UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGB8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGB8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGBA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_ETC2RGBA8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_R16Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_R16Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R16Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_R16Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R16Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_R32Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_R32Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R32Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R8Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R8Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_R8Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_R8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG11B10Ufloat
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG16Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG16Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG16Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG16Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG16Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG32Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG32Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG32Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG8Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG8Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG8Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RG8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGB10A2Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGB10A2Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGB9E5Ufloat
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA16Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA16Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA16Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA16Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA16Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA32Float
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA32Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA32Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Sint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Snorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Uint
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8Unorm
import org.graphiks.dawn4k.native.WGPUTextureFormat_RGBA8UnormSrgb
import org.graphiks.dawn4k.native.WGPUTextureFormat_Stencil8
import org.graphiks.dawn4k.native.WGPUTextureSampleType
import org.graphiks.dawn4k.native.WGPUTextureSampleType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Depth
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Float
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Sint
import org.graphiks.dawn4k.native.WGPUTextureSampleType_Uint
import org.graphiks.dawn4k.native.WGPUTextureSampleType_UnfilterableFloat
import org.graphiks.dawn4k.native.WGPUTextureUsage
import org.graphiks.dawn4k.native.WGPUTextureUsage_CopyDst
import org.graphiks.dawn4k.native.WGPUTextureUsage_CopySrc
import org.graphiks.dawn4k.native.WGPUTextureUsage_None
import org.graphiks.dawn4k.native.WGPUTextureUsage_RenderAttachment
import org.graphiks.dawn4k.native.WGPUTextureUsage_StorageBinding
import org.graphiks.dawn4k.native.WGPUTextureUsage_TextureBinding
import org.graphiks.dawn4k.native.WGPUTextureUsage_TransientAttachment
import org.graphiks.dawn4k.native.WGPUTextureViewDimension
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_1D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_2D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_2DArray
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_3D
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_Cube
import org.graphiks.dawn4k.native.WGPUTextureViewDimension_CubeArray
import org.graphiks.dawn4k.native.WGPUVertexFormat
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float16
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float16x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float16x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float32
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float32x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float32x3
import org.graphiks.dawn4k.native.WGPUVertexFormat_Float32x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint16
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint16x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint16x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint32
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint32x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint32x3
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint32x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint8
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint8x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Sint8x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm16
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm16x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm16x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm8
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm8x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Snorm8x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint16
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint16x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint16x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint32
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint32x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint32x3
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint32x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint8
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint8x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Uint8x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm10_10_10_2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm16
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm16x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm16x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm8
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm8x2
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm8x4
import org.graphiks.dawn4k.native.WGPUVertexFormat_Unorm8x4BGRA
import org.graphiks.dawn4k.native.WGPUVertexStepMode
import org.graphiks.dawn4k.native.WGPUVertexStepMode_Instance
import org.graphiks.dawn4k.native.WGPUVertexStepMode_Vertex
import org.graphiks.webgpu.GPUAddressMode
import org.graphiks.webgpu.GPUBlendFactor
import org.graphiks.webgpu.GPUBlendOperation
import org.graphiks.webgpu.GPUColorWrite
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUCullMode
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUFrontFace
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUPrimitiveTopology
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUSamplerBindingType
import org.graphiks.webgpu.GPUStencilOperation
import org.graphiks.webgpu.GPUStorageTextureAccess
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureSampleType
import org.graphiks.webgpu.GPUTextureSwizzleSource
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUVertexFormat
import org.graphiks.webgpu.GPUVertexStepMode

/**
 * Explicit conversion table Kotlin `GPUTextureFormat` -> native `WGPUTextureFormat`.
 * Every entry maps through its named Dawn constant; the numeric values are never
 * assumed to coincide, and an unknown value has no arbitrary fallback (the sealed
 * Kotlin enum makes the table exhaustive at compile time).
 */
internal fun GPUTextureFormat.toNativeTextureFormat(): WGPUTextureFormat = when (this) {
    GPUTextureFormat.R8Unorm -> WGPUTextureFormat_R8Unorm
    GPUTextureFormat.R8Snorm -> WGPUTextureFormat_R8Snorm
    GPUTextureFormat.R8Uint -> WGPUTextureFormat_R8Uint
    GPUTextureFormat.R8Sint -> WGPUTextureFormat_R8Sint
    GPUTextureFormat.R16Unorm -> WGPUTextureFormat_R16Unorm
    GPUTextureFormat.R16Snorm -> WGPUTextureFormat_R16Snorm
    GPUTextureFormat.R16Uint -> WGPUTextureFormat_R16Uint
    GPUTextureFormat.R16Sint -> WGPUTextureFormat_R16Sint
    GPUTextureFormat.R16Float -> WGPUTextureFormat_R16Float
    GPUTextureFormat.RG8Unorm -> WGPUTextureFormat_RG8Unorm
    GPUTextureFormat.RG8Snorm -> WGPUTextureFormat_RG8Snorm
    GPUTextureFormat.RG8Uint -> WGPUTextureFormat_RG8Uint
    GPUTextureFormat.RG8Sint -> WGPUTextureFormat_RG8Sint
    GPUTextureFormat.R32Float -> WGPUTextureFormat_R32Float
    GPUTextureFormat.R32Uint -> WGPUTextureFormat_R32Uint
    GPUTextureFormat.R32Sint -> WGPUTextureFormat_R32Sint
    GPUTextureFormat.RG16Unorm -> WGPUTextureFormat_RG16Unorm
    GPUTextureFormat.RG16Snorm -> WGPUTextureFormat_RG16Snorm
    GPUTextureFormat.RG16Uint -> WGPUTextureFormat_RG16Uint
    GPUTextureFormat.RG16Sint -> WGPUTextureFormat_RG16Sint
    GPUTextureFormat.RG16Float -> WGPUTextureFormat_RG16Float
    GPUTextureFormat.RGBA8Unorm -> WGPUTextureFormat_RGBA8Unorm
    GPUTextureFormat.RGBA8UnormSrgb -> WGPUTextureFormat_RGBA8UnormSrgb
    GPUTextureFormat.RGBA8Snorm -> WGPUTextureFormat_RGBA8Snorm
    GPUTextureFormat.RGBA8Uint -> WGPUTextureFormat_RGBA8Uint
    GPUTextureFormat.RGBA8Sint -> WGPUTextureFormat_RGBA8Sint
    GPUTextureFormat.BGRA8Unorm -> WGPUTextureFormat_BGRA8Unorm
    GPUTextureFormat.BGRA8UnormSrgb -> WGPUTextureFormat_BGRA8UnormSrgb
    GPUTextureFormat.RGB10A2Uint -> WGPUTextureFormat_RGB10A2Uint
    GPUTextureFormat.RGB10A2Unorm -> WGPUTextureFormat_RGB10A2Unorm
    GPUTextureFormat.RG11B10Ufloat -> WGPUTextureFormat_RG11B10Ufloat
    GPUTextureFormat.RGB9E5Ufloat -> WGPUTextureFormat_RGB9E5Ufloat
    GPUTextureFormat.RG32Float -> WGPUTextureFormat_RG32Float
    GPUTextureFormat.RG32Uint -> WGPUTextureFormat_RG32Uint
    GPUTextureFormat.RG32Sint -> WGPUTextureFormat_RG32Sint
    GPUTextureFormat.RGBA16Unorm -> WGPUTextureFormat_RGBA16Unorm
    GPUTextureFormat.RGBA16Snorm -> WGPUTextureFormat_RGBA16Snorm
    GPUTextureFormat.RGBA16Uint -> WGPUTextureFormat_RGBA16Uint
    GPUTextureFormat.RGBA16Sint -> WGPUTextureFormat_RGBA16Sint
    GPUTextureFormat.RGBA16Float -> WGPUTextureFormat_RGBA16Float
    GPUTextureFormat.RGBA32Float -> WGPUTextureFormat_RGBA32Float
    GPUTextureFormat.RGBA32Uint -> WGPUTextureFormat_RGBA32Uint
    GPUTextureFormat.RGBA32Sint -> WGPUTextureFormat_RGBA32Sint
    GPUTextureFormat.Stencil8 -> WGPUTextureFormat_Stencil8
    GPUTextureFormat.Depth16Unorm -> WGPUTextureFormat_Depth16Unorm
    GPUTextureFormat.Depth24Plus -> WGPUTextureFormat_Depth24Plus
    GPUTextureFormat.Depth24PlusStencil8 -> WGPUTextureFormat_Depth24PlusStencil8
    GPUTextureFormat.Depth32Float -> WGPUTextureFormat_Depth32Float
    GPUTextureFormat.Depth32FloatStencil8 -> WGPUTextureFormat_Depth32FloatStencil8
    GPUTextureFormat.BC1RGBAUnorm -> WGPUTextureFormat_BC1RGBAUnorm
    GPUTextureFormat.BC1RGBAUnormSrgb -> WGPUTextureFormat_BC1RGBAUnormSrgb
    GPUTextureFormat.BC2RGBAUnorm -> WGPUTextureFormat_BC2RGBAUnorm
    GPUTextureFormat.BC2RGBAUnormSrgb -> WGPUTextureFormat_BC2RGBAUnormSrgb
    GPUTextureFormat.BC3RGBAUnorm -> WGPUTextureFormat_BC3RGBAUnorm
    GPUTextureFormat.BC3RGBAUnormSrgb -> WGPUTextureFormat_BC3RGBAUnormSrgb
    GPUTextureFormat.BC4RUnorm -> WGPUTextureFormat_BC4RUnorm
    GPUTextureFormat.BC4RSnorm -> WGPUTextureFormat_BC4RSnorm
    GPUTextureFormat.BC5RGUnorm -> WGPUTextureFormat_BC5RGUnorm
    GPUTextureFormat.BC5RGSnorm -> WGPUTextureFormat_BC5RGSnorm
    GPUTextureFormat.BC6HRGBUfloat -> WGPUTextureFormat_BC6HRGBUfloat
    GPUTextureFormat.BC6HRGBFloat -> WGPUTextureFormat_BC6HRGBFloat
    GPUTextureFormat.BC7RGBAUnorm -> WGPUTextureFormat_BC7RGBAUnorm
    GPUTextureFormat.BC7RGBAUnormSrgb -> WGPUTextureFormat_BC7RGBAUnormSrgb
    GPUTextureFormat.ETC2RGB8Unorm -> WGPUTextureFormat_ETC2RGB8Unorm
    GPUTextureFormat.ETC2RGB8UnormSrgb -> WGPUTextureFormat_ETC2RGB8UnormSrgb
    GPUTextureFormat.ETC2RGB8A1Unorm -> WGPUTextureFormat_ETC2RGB8A1Unorm
    GPUTextureFormat.ETC2RGB8A1UnormSrgb -> WGPUTextureFormat_ETC2RGB8A1UnormSrgb
    GPUTextureFormat.ETC2RGBA8Unorm -> WGPUTextureFormat_ETC2RGBA8Unorm
    GPUTextureFormat.ETC2RGBA8UnormSrgb -> WGPUTextureFormat_ETC2RGBA8UnormSrgb
    GPUTextureFormat.EACR11Unorm -> WGPUTextureFormat_EACR11Unorm
    GPUTextureFormat.EACR11Snorm -> WGPUTextureFormat_EACR11Snorm
    GPUTextureFormat.EACRG11Unorm -> WGPUTextureFormat_EACRG11Unorm
    GPUTextureFormat.EACRG11Snorm -> WGPUTextureFormat_EACRG11Snorm
    GPUTextureFormat.ASTC4x4Unorm -> WGPUTextureFormat_ASTC4x4Unorm
    GPUTextureFormat.ASTC4x4UnormSrgb -> WGPUTextureFormat_ASTC4x4UnormSrgb
    GPUTextureFormat.ASTC5x4Unorm -> WGPUTextureFormat_ASTC5x4Unorm
    GPUTextureFormat.ASTC5x4UnormSrgb -> WGPUTextureFormat_ASTC5x4UnormSrgb
    GPUTextureFormat.ASTC5x5Unorm -> WGPUTextureFormat_ASTC5x5Unorm
    GPUTextureFormat.ASTC5x5UnormSrgb -> WGPUTextureFormat_ASTC5x5UnormSrgb
    GPUTextureFormat.ASTC6x5Unorm -> WGPUTextureFormat_ASTC6x5Unorm
    GPUTextureFormat.ASTC6x5UnormSrgb -> WGPUTextureFormat_ASTC6x5UnormSrgb
    GPUTextureFormat.ASTC6x6Unorm -> WGPUTextureFormat_ASTC6x6Unorm
    GPUTextureFormat.ASTC6x6UnormSrgb -> WGPUTextureFormat_ASTC6x6UnormSrgb
    GPUTextureFormat.ASTC8x5Unorm -> WGPUTextureFormat_ASTC8x5Unorm
    GPUTextureFormat.ASTC8x5UnormSrgb -> WGPUTextureFormat_ASTC8x5UnormSrgb
    GPUTextureFormat.ASTC8x6Unorm -> WGPUTextureFormat_ASTC8x6Unorm
    GPUTextureFormat.ASTC8x6UnormSrgb -> WGPUTextureFormat_ASTC8x6UnormSrgb
    GPUTextureFormat.ASTC8x8Unorm -> WGPUTextureFormat_ASTC8x8Unorm
    GPUTextureFormat.ASTC8x8UnormSrgb -> WGPUTextureFormat_ASTC8x8UnormSrgb
    GPUTextureFormat.ASTC10x5Unorm -> WGPUTextureFormat_ASTC10x5Unorm
    GPUTextureFormat.ASTC10x5UnormSrgb -> WGPUTextureFormat_ASTC10x5UnormSrgb
    GPUTextureFormat.ASTC10x6Unorm -> WGPUTextureFormat_ASTC10x6Unorm
    GPUTextureFormat.ASTC10x6UnormSrgb -> WGPUTextureFormat_ASTC10x6UnormSrgb
    GPUTextureFormat.ASTC10x8Unorm -> WGPUTextureFormat_ASTC10x8Unorm
    GPUTextureFormat.ASTC10x8UnormSrgb -> WGPUTextureFormat_ASTC10x8UnormSrgb
    GPUTextureFormat.ASTC10x10Unorm -> WGPUTextureFormat_ASTC10x10Unorm
    GPUTextureFormat.ASTC10x10UnormSrgb -> WGPUTextureFormat_ASTC10x10UnormSrgb
    GPUTextureFormat.ASTC12x10Unorm -> WGPUTextureFormat_ASTC12x10Unorm
    GPUTextureFormat.ASTC12x10UnormSrgb -> WGPUTextureFormat_ASTC12x10UnormSrgb
    GPUTextureFormat.ASTC12x12Unorm -> WGPUTextureFormat_ASTC12x12Unorm
    GPUTextureFormat.ASTC12x12UnormSrgb -> WGPUTextureFormat_ASTC12x12UnormSrgb
}

/** Explicit conversion table Kotlin `GPUTextureDimension` -> native `WGPUTextureDimension`. */
internal fun GPUTextureDimension.toNativeTextureDimension(): WGPUTextureDimension = when (this) {
    GPUTextureDimension.OneD -> WGPUTextureDimension_1D
    GPUTextureDimension.TwoD -> WGPUTextureDimension_2D
    GPUTextureDimension.ThreeD -> WGPUTextureDimension_3D
}

/** Explicit conversion table Kotlin `GPUTextureViewDimension` -> native `WGPUTextureViewDimension`. */
internal fun GPUTextureViewDimension.toNativeTextureViewDimension(): WGPUTextureViewDimension = when (this) {
    GPUTextureViewDimension.OneD -> WGPUTextureViewDimension_1D
    GPUTextureViewDimension.TwoD -> WGPUTextureViewDimension_2D
    GPUTextureViewDimension.TwoDArray -> WGPUTextureViewDimension_2DArray
    GPUTextureViewDimension.Cube -> WGPUTextureViewDimension_Cube
    GPUTextureViewDimension.CubeArray -> WGPUTextureViewDimension_CubeArray
    GPUTextureViewDimension.ThreeD -> WGPUTextureViewDimension_3D
}

/** Explicit conversion table Kotlin `GPUTextureAspect` -> native `WGPUTextureAspect`. */
internal fun GPUTextureAspect.toNativeTextureAspect(): WGPUTextureAspect = when (this) {
    GPUTextureAspect.All -> WGPUTextureAspect_All
    GPUTextureAspect.StencilOnly -> WGPUTextureAspect_StencilOnly
    GPUTextureAspect.DepthOnly -> WGPUTextureAspect_DepthOnly
}

/**
 * Explicit conversion table Kotlin `GPUTextureUsage` -> native `WGPUTextureUsage`.
 * Each bit is mapped through its named constant.
 */
internal fun GPUTextureUsage.toNativeTextureUsage(): WGPUTextureUsage {
    val value = this.value
    var usage = WGPUTextureUsage_None
    if (value and GPUTextureUsage.CopySrc.value != 0uL) usage = usage or WGPUTextureUsage_CopySrc
    if (value and GPUTextureUsage.CopyDst.value != 0uL) usage = usage or WGPUTextureUsage_CopyDst
    if (value and GPUTextureUsage.TextureBinding.value != 0uL) usage = usage or WGPUTextureUsage_TextureBinding
    if (value and GPUTextureUsage.StorageBinding.value != 0uL) usage = usage or WGPUTextureUsage_StorageBinding
    if (value and GPUTextureUsage.RenderAttachment.value != 0uL) usage = usage or WGPUTextureUsage_RenderAttachment
    if (value and GPUTextureUsage.TransientAttachment.value != 0uL) usage = usage or WGPUTextureUsage_TransientAttachment
    return usage
}

/** Explicit conversion table Kotlin `GPUAddressMode` -> native `WGPUAddressMode`. */
internal fun GPUAddressMode.toNativeAddressMode(): WGPUAddressMode = when (this) {
    GPUAddressMode.ClampToEdge -> WGPUAddressMode_ClampToEdge
    GPUAddressMode.Repeat -> WGPUAddressMode_Repeat
    GPUAddressMode.MirrorRepeat -> WGPUAddressMode_MirrorRepeat
}

/** Explicit conversion table Kotlin `GPUFilterMode` -> native `WGPUFilterMode`. */
internal fun GPUFilterMode.toNativeFilterMode(): WGPUFilterMode = when (this) {
    GPUFilterMode.Nearest -> WGPUFilterMode_Nearest
    GPUFilterMode.Linear -> WGPUFilterMode_Linear
}

/** Explicit conversion table Kotlin `GPUMipmapFilterMode` -> native `WGPUMipmapFilterMode`. */
internal fun GPUMipmapFilterMode.toNativeMipmapFilterMode(): WGPUMipmapFilterMode = when (this) {
    GPUMipmapFilterMode.Nearest -> WGPUMipmapFilterMode_Nearest
    GPUMipmapFilterMode.Linear -> WGPUMipmapFilterMode_Linear
}

/** Explicit conversion table Kotlin `GPUCompareFunction` -> native `WGPUCompareFunction`. */
internal fun GPUCompareFunction.toNativeCompareFunction(): WGPUCompareFunction = when (this) {
    GPUCompareFunction.Never -> WGPUCompareFunction_Never
    GPUCompareFunction.Less -> WGPUCompareFunction_Less
    GPUCompareFunction.Equal -> WGPUCompareFunction_Equal
    GPUCompareFunction.LessEqual -> WGPUCompareFunction_LessEqual
    GPUCompareFunction.Greater -> WGPUCompareFunction_Greater
    GPUCompareFunction.NotEqual -> WGPUCompareFunction_NotEqual
    GPUCompareFunction.GreaterEqual -> WGPUCompareFunction_GreaterEqual
    GPUCompareFunction.Always -> WGPUCompareFunction_Always
}

/** Explicit conversion table Kotlin `GPUQueryType` -> native `WGPUQueryType`. */
internal fun GPUQueryType.toNativeQueryType(): WGPUQueryType = when (this) {
    GPUQueryType.Occlusion -> WGPUQueryType_Occlusion
    GPUQueryType.Timestamp -> WGPUQueryType_Timestamp
}

/** Explicit conversion table Kotlin `GPUPrimitiveTopology` -> native `WGPUPrimitiveTopology`. */
internal fun GPUPrimitiveTopology.toNativePrimitiveTopology(): WGPUPrimitiveTopology = when (this) {
    GPUPrimitiveTopology.PointList -> WGPUPrimitiveTopology_PointList
    GPUPrimitiveTopology.LineList -> WGPUPrimitiveTopology_LineList
    GPUPrimitiveTopology.LineStrip -> WGPUPrimitiveTopology_LineStrip
    GPUPrimitiveTopology.TriangleList -> WGPUPrimitiveTopology_TriangleList
    GPUPrimitiveTopology.TriangleStrip -> WGPUPrimitiveTopology_TriangleStrip
}

/** Explicit conversion table Kotlin `GPUIndexFormat` -> native `WGPUIndexFormat`. */
internal fun GPUIndexFormat.toNativeIndexFormat(): WGPUIndexFormat = when (this) {
    GPUIndexFormat.Uint16 -> WGPUIndexFormat_Uint16
    GPUIndexFormat.Uint32 -> WGPUIndexFormat_Uint32
}

/** Explicit conversion table Kotlin `GPUFrontFace` -> native `WGPUFrontFace`. */
internal fun GPUFrontFace.toNativeFrontFace(): WGPUFrontFace = when (this) {
    GPUFrontFace.CCW -> WGPUFrontFace_CCW
    GPUFrontFace.CW -> WGPUFrontFace_CW
}

/** Explicit conversion table Kotlin `GPUCullMode` -> native `WGPUCullMode`. */
internal fun GPUCullMode.toNativeCullMode(): WGPUCullMode = when (this) {
    GPUCullMode.None -> WGPUCullMode_None
    GPUCullMode.Front -> WGPUCullMode_Front
    GPUCullMode.Back -> WGPUCullMode_Back
}

/** Explicit conversion table Kotlin `GPUVertexStepMode` -> native `WGPUVertexStepMode`. */
internal fun GPUVertexStepMode.toNativeVertexStepMode(): WGPUVertexStepMode = when (this) {
    GPUVertexStepMode.Vertex -> WGPUVertexStepMode_Vertex
    GPUVertexStepMode.Instance -> WGPUVertexStepMode_Instance
}

/** Explicit conversion table Kotlin `GPUVertexFormat` -> native `WGPUVertexFormat`. */
internal fun GPUVertexFormat.toNativeVertexFormat(): WGPUVertexFormat = when (this) {
    GPUVertexFormat.Uint8 -> WGPUVertexFormat_Uint8
    GPUVertexFormat.Uint8x2 -> WGPUVertexFormat_Uint8x2
    GPUVertexFormat.Uint8x4 -> WGPUVertexFormat_Uint8x4
    GPUVertexFormat.Sint8 -> WGPUVertexFormat_Sint8
    GPUVertexFormat.Sint8x2 -> WGPUVertexFormat_Sint8x2
    GPUVertexFormat.Sint8x4 -> WGPUVertexFormat_Sint8x4
    GPUVertexFormat.Unorm8 -> WGPUVertexFormat_Unorm8
    GPUVertexFormat.Unorm8x2 -> WGPUVertexFormat_Unorm8x2
    GPUVertexFormat.Unorm8x4 -> WGPUVertexFormat_Unorm8x4
    GPUVertexFormat.Snorm8 -> WGPUVertexFormat_Snorm8
    GPUVertexFormat.Snorm8x2 -> WGPUVertexFormat_Snorm8x2
    GPUVertexFormat.Snorm8x4 -> WGPUVertexFormat_Snorm8x4
    GPUVertexFormat.Uint16 -> WGPUVertexFormat_Uint16
    GPUVertexFormat.Uint16x2 -> WGPUVertexFormat_Uint16x2
    GPUVertexFormat.Uint16x4 -> WGPUVertexFormat_Uint16x4
    GPUVertexFormat.Sint16 -> WGPUVertexFormat_Sint16
    GPUVertexFormat.Sint16x2 -> WGPUVertexFormat_Sint16x2
    GPUVertexFormat.Sint16x4 -> WGPUVertexFormat_Sint16x4
    GPUVertexFormat.Unorm16 -> WGPUVertexFormat_Unorm16
    GPUVertexFormat.Unorm16x2 -> WGPUVertexFormat_Unorm16x2
    GPUVertexFormat.Unorm16x4 -> WGPUVertexFormat_Unorm16x4
    GPUVertexFormat.Snorm16 -> WGPUVertexFormat_Snorm16
    GPUVertexFormat.Snorm16x2 -> WGPUVertexFormat_Snorm16x2
    GPUVertexFormat.Snorm16x4 -> WGPUVertexFormat_Snorm16x4
    GPUVertexFormat.Float16 -> WGPUVertexFormat_Float16
    GPUVertexFormat.Float16x2 -> WGPUVertexFormat_Float16x2
    GPUVertexFormat.Float16x4 -> WGPUVertexFormat_Float16x4
    GPUVertexFormat.Float32 -> WGPUVertexFormat_Float32
    GPUVertexFormat.Float32x2 -> WGPUVertexFormat_Float32x2
    GPUVertexFormat.Float32x3 -> WGPUVertexFormat_Float32x3
    GPUVertexFormat.Float32x4 -> WGPUVertexFormat_Float32x4
    GPUVertexFormat.Uint32 -> WGPUVertexFormat_Uint32
    GPUVertexFormat.Uint32x2 -> WGPUVertexFormat_Uint32x2
    GPUVertexFormat.Uint32x3 -> WGPUVertexFormat_Uint32x3
    GPUVertexFormat.Uint32x4 -> WGPUVertexFormat_Uint32x4
    GPUVertexFormat.Sint32 -> WGPUVertexFormat_Sint32
    GPUVertexFormat.Sint32x2 -> WGPUVertexFormat_Sint32x2
    GPUVertexFormat.Sint32x3 -> WGPUVertexFormat_Sint32x3
    GPUVertexFormat.Sint32x4 -> WGPUVertexFormat_Sint32x4
    GPUVertexFormat.Unorm1010102 -> WGPUVertexFormat_Unorm10_10_10_2
    GPUVertexFormat.Unorm8x4BGRA -> WGPUVertexFormat_Unorm8x4BGRA
}

/** Explicit conversion table Kotlin `GPUBlendFactor` -> native `WGPUBlendFactor`. */
internal fun GPUBlendFactor.toNativeBlendFactor(): WGPUBlendFactor = when (this) {
    GPUBlendFactor.Zero -> WGPUBlendFactor_Zero
    GPUBlendFactor.One -> WGPUBlendFactor_One
    GPUBlendFactor.Src -> WGPUBlendFactor_Src
    GPUBlendFactor.OneMinusSrc -> WGPUBlendFactor_OneMinusSrc
    GPUBlendFactor.SrcAlpha -> WGPUBlendFactor_SrcAlpha
    GPUBlendFactor.OneMinusSrcAlpha -> WGPUBlendFactor_OneMinusSrcAlpha
    GPUBlendFactor.Dst -> WGPUBlendFactor_Dst
    GPUBlendFactor.OneMinusDst -> WGPUBlendFactor_OneMinusDst
    GPUBlendFactor.DstAlpha -> WGPUBlendFactor_DstAlpha
    GPUBlendFactor.OneMinusDstAlpha -> WGPUBlendFactor_OneMinusDstAlpha
    GPUBlendFactor.SrcAlphaSaturated -> WGPUBlendFactor_SrcAlphaSaturated
    GPUBlendFactor.Constant -> WGPUBlendFactor_Constant
    GPUBlendFactor.OneMinusConstant -> WGPUBlendFactor_OneMinusConstant
    GPUBlendFactor.Src1 -> WGPUBlendFactor_Src1
    GPUBlendFactor.OneMinusSrc1 -> WGPUBlendFactor_OneMinusSrc1
    GPUBlendFactor.Src1Alpha -> WGPUBlendFactor_Src1Alpha
    GPUBlendFactor.OneMinusSrc1Alpha -> WGPUBlendFactor_OneMinusSrc1Alpha
}

/** Explicit conversion table Kotlin `GPUBlendOperation` -> native `WGPUBlendOperation`. */
internal fun GPUBlendOperation.toNativeBlendOperation(): WGPUBlendOperation = when (this) {
    GPUBlendOperation.Add -> WGPUBlendOperation_Add
    GPUBlendOperation.Subtract -> WGPUBlendOperation_Subtract
    GPUBlendOperation.ReverseSubtract -> WGPUBlendOperation_ReverseSubtract
    GPUBlendOperation.Min -> WGPUBlendOperation_Min
    GPUBlendOperation.Max -> WGPUBlendOperation_Max
}

/** Explicit conversion table Kotlin `GPUStencilOperation` -> native `WGPUStencilOperation`. */
internal fun GPUStencilOperation.toNativeStencilOperation(): WGPUStencilOperation = when (this) {
    GPUStencilOperation.Keep -> WGPUStencilOperation_Keep
    GPUStencilOperation.Zero -> WGPUStencilOperation_Zero
    GPUStencilOperation.Replace -> WGPUStencilOperation_Replace
    GPUStencilOperation.Invert -> WGPUStencilOperation_Invert
    GPUStencilOperation.IncrementClamp -> WGPUStencilOperation_IncrementClamp
    GPUStencilOperation.DecrementClamp -> WGPUStencilOperation_DecrementClamp
    GPUStencilOperation.IncrementWrap -> WGPUStencilOperation_IncrementWrap
    GPUStencilOperation.DecrementWrap -> WGPUStencilOperation_DecrementWrap
}

/** Explicit conversion table Kotlin `GPUTextureSampleType` -> native `WGPUTextureSampleType`. */
internal fun GPUTextureSampleType.toNativeTextureSampleType(): WGPUTextureSampleType = when (this) {
    GPUTextureSampleType.BindingNotUsed -> WGPUTextureSampleType_BindingNotUsed
    GPUTextureSampleType.Float -> WGPUTextureSampleType_Float
    GPUTextureSampleType.UnfilterableFloat -> WGPUTextureSampleType_UnfilterableFloat
    GPUTextureSampleType.Depth -> WGPUTextureSampleType_Depth
    GPUTextureSampleType.Sint -> WGPUTextureSampleType_Sint
    GPUTextureSampleType.Uint -> WGPUTextureSampleType_Uint
}

/** Explicit conversion table Kotlin `GPUSamplerBindingType` -> native `WGPUSamplerBindingType`. */
internal fun GPUSamplerBindingType.toNativeSamplerBindingType(): WGPUSamplerBindingType = when (this) {
    GPUSamplerBindingType.BindingNotUsed -> WGPUSamplerBindingType_BindingNotUsed
    GPUSamplerBindingType.Filtering -> WGPUSamplerBindingType_Filtering
    GPUSamplerBindingType.NonFiltering -> WGPUSamplerBindingType_NonFiltering
    GPUSamplerBindingType.Comparison -> WGPUSamplerBindingType_Comparison
}

/** Explicit conversion table Kotlin `GPUStorageTextureAccess` -> native `WGPUStorageTextureAccess`. */
internal fun GPUStorageTextureAccess.toNativeStorageTextureAccess(): WGPUStorageTextureAccess = when (this) {
    GPUStorageTextureAccess.BindingNotUsed -> WGPUStorageTextureAccess_BindingNotUsed
    GPUStorageTextureAccess.WriteOnly -> WGPUStorageTextureAccess_WriteOnly
    GPUStorageTextureAccess.ReadOnly -> WGPUStorageTextureAccess_ReadOnly
    GPUStorageTextureAccess.ReadWrite -> WGPUStorageTextureAccess_ReadWrite
}

/** Explicit conversion table Kotlin `GPUColorWrite` -> native `WGPUColorWriteMask`. */
internal fun GPUColorWrite.toNativeColorWriteMask(): WGPUColorWriteMask {
    val value = this.value
    var mask = WGPUColorWriteMask_None
    if (value and GPUColorWrite.Red.value != 0uL) mask = mask or WGPUColorWriteMask_Red
    if (value and GPUColorWrite.Green.value != 0uL) mask = mask or WGPUColorWriteMask_Green
    if (value and GPUColorWrite.Blue.value != 0uL) mask = mask or WGPUColorWriteMask_Blue
    if (value and GPUColorWrite.Alpha.value != 0uL) mask = mask or WGPUColorWriteMask_Alpha
    return mask
}

/**
 * Explicit conversion of a WebGPU optional boolean (`Boolean?`) to the native
 * tri-state `WGPUOptionalBool`: null is the C default `Undefined`, and a
 * concrete value is `True` or `False`.
 */
internal fun Boolean?.toNativeOptionalBool(): WGPUOptionalBool = when (this) {
    null -> WGPUOptionalBool_Undefined
    true -> WGPUOptionalBool_True
    false -> WGPUOptionalBool_False
}

/** Explicit conversion table `GPUTextureSwizzleSource` -> native `WGPUComponentSwizzle`. */
internal fun GPUTextureSwizzleSource.toNativeComponentSwizzle(): WGPUComponentSwizzle = when (this) {
    GPUTextureSwizzleSource.Red -> WGPUComponentSwizzle_R
    GPUTextureSwizzleSource.Green -> WGPUComponentSwizzle_G
    GPUTextureSwizzleSource.Blue -> WGPUComponentSwizzle_B
    GPUTextureSwizzleSource.Alpha -> WGPUComponentSwizzle_A
    GPUTextureSwizzleSource.Zero -> WGPUComponentSwizzle_Zero
    GPUTextureSwizzleSource.One -> WGPUComponentSwizzle_One
}

/** Explicit conversion table Kotlin `GPULoadOp` -> native `WGPULoadOp`. */
internal fun GPULoadOp.toNativeLoadOp(): WGPULoadOp = when (this) {
    GPULoadOp.Load -> WGPULoadOp_Load
    GPULoadOp.Clear -> WGPULoadOp_Clear
}

/** Explicit conversion table Kotlin `GPUStoreOp` -> native `WGPUStoreOp`. */
internal fun GPUStoreOp.toNativeStoreOp(): WGPUStoreOp = when (this) {
    GPUStoreOp.Store -> WGPUStoreOp_Store
    GPUStoreOp.Discard -> WGPUStoreOp_Discard
}

/** The native "no explicit load op" value (C `WGPU_LOAD_OP_INIT`). */
internal val GPULoadOp?.toNativeOrUndefined: WGPULoadOp
    get() = this?.toNativeLoadOp() ?: WGPULoadOp_Undefined

/** The native "no explicit store op" value (C `WGPU_STORE_OP_INIT`). */
internal val GPUStoreOp?.toNativeOrUndefined: WGPUStoreOp
    get() = this?.toNativeStoreOp() ?: WGPUStoreOp_Undefined

/** The native "no explicit index format" value (C `WGPU_INDEX_FORMAT_INIT`). */
internal val GPUIndexFormat?.toNativeOrUndefined: WGPUIndexFormat
    get() = this?.toNativeIndexFormat() ?: WGPUIndexFormat_Undefined

/** The native "no explicit texture view format" value. */
internal const val WGPU_MIP_LEVEL_COUNT_UNDEFINED: UInt = 0xFFFFFFFFu

/** The native "no explicit array layer count" value. */
internal const val WGPU_ARRAY_LAYER_COUNT_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_WHOLE_SIZE` (SIZE_MAX): the "to the end of the buffer" size sentinel. */
internal const val WGPU_WHOLE_SIZE: ULong = ULong.MAX_VALUE

/** webgpu.h `WGPU_DEPTH_SLICE_UNDEFINED`: only 3D color attachments define a depth slice. */
internal const val WGPU_DEPTH_SLICE_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_COPY_STRIDE_UNDEFINED`: an absent bytesPerRow / rowsPerImage stride. */
internal const val WGPU_COPY_STRIDE_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_QUERY_SET_INDEX_UNDEFINED`: an absent timestamp write index. */
internal const val WGPU_QUERY_SET_INDEX_UNDEFINED: UInt = 0xFFFFFFFFu
