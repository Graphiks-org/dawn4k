package org.graphiks.dawn4k.native

import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.Callback
import org.graphiks.kffi.CallbackExceptionHandler
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.CallbackRuntimeApi
import org.graphiks.kffi.CallbackType
import org.graphiks.kffi.PreparedCallbackRegistration
import org.graphiks.kffi.UnsafeCallbackRearmApi
import org.graphiks.kffi.CString
import org.graphiks.kffi.ArrayHolder
import org.graphiks.kffi.MemoryAllocator
import kotlin.OptIn

typealias WGPUBufferUsage = ULong
const val WGPUBufferUsage_None : WGPUBufferUsage = 0uL
const val WGPUBufferUsage_MapRead : WGPUBufferUsage = 1uL
const val WGPUBufferUsage_MapWrite : WGPUBufferUsage = 2uL
const val WGPUBufferUsage_CopySrc : WGPUBufferUsage = 4uL
const val WGPUBufferUsage_CopyDst : WGPUBufferUsage = 8uL
const val WGPUBufferUsage_Index : WGPUBufferUsage = 16uL
const val WGPUBufferUsage_Vertex : WGPUBufferUsage = 32uL
const val WGPUBufferUsage_Uniform : WGPUBufferUsage = 64uL
const val WGPUBufferUsage_Storage : WGPUBufferUsage = 128uL
const val WGPUBufferUsage_Indirect : WGPUBufferUsage = 256uL
const val WGPUBufferUsage_QueryResolve : WGPUBufferUsage = 512uL
const val WGPUBufferUsage_TexelBuffer : WGPUBufferUsage = 1024uL

typealias WGPUColorWriteMask = ULong
const val WGPUColorWriteMask_None : WGPUColorWriteMask = 0uL
const val WGPUColorWriteMask_Red : WGPUColorWriteMask = 1uL
const val WGPUColorWriteMask_Green : WGPUColorWriteMask = 2uL
const val WGPUColorWriteMask_Blue : WGPUColorWriteMask = 4uL
const val WGPUColorWriteMask_Alpha : WGPUColorWriteMask = 8uL
const val WGPUColorWriteMask_All : WGPUColorWriteMask = 15uL

typealias WGPUHeapProperty = ULong
const val WGPUHeapProperty_None : WGPUHeapProperty = 0uL
const val WGPUHeapProperty_DeviceLocal : WGPUHeapProperty = 1uL
const val WGPUHeapProperty_HostVisible : WGPUHeapProperty = 2uL
const val WGPUHeapProperty_HostCoherent : WGPUHeapProperty = 4uL
const val WGPUHeapProperty_HostUncached : WGPUHeapProperty = 8uL
const val WGPUHeapProperty_HostCached : WGPUHeapProperty = 16uL

typealias WGPUMapMode = ULong
const val WGPUMapMode_None : WGPUMapMode = 0uL
const val WGPUMapMode_Read : WGPUMapMode = 1uL
const val WGPUMapMode_Write : WGPUMapMode = 2uL

typealias WGPUShaderStage = ULong
const val WGPUShaderStage_None : WGPUShaderStage = 0uL
const val WGPUShaderStage_Vertex : WGPUShaderStage = 1uL
const val WGPUShaderStage_Fragment : WGPUShaderStage = 2uL
const val WGPUShaderStage_Compute : WGPUShaderStage = 4uL

typealias WGPUTextureUsage = ULong
const val WGPUTextureUsage_None : WGPUTextureUsage = 0uL
const val WGPUTextureUsage_CopySrc : WGPUTextureUsage = 1uL
const val WGPUTextureUsage_CopyDst : WGPUTextureUsage = 2uL
const val WGPUTextureUsage_TextureBinding : WGPUTextureUsage = 4uL
const val WGPUTextureUsage_StorageBinding : WGPUTextureUsage = 8uL
const val WGPUTextureUsage_RenderAttachment : WGPUTextureUsage = 16uL
const val WGPUTextureUsage_TransientAttachment : WGPUTextureUsage = 32uL
const val WGPUTextureUsage_StorageAttachment : WGPUTextureUsage = 64uL

expect interface WGPUStringView {
    var data: CString?
    var length: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUStringView
        fun allocate(allocator: MemoryAllocator): WGPUStringView
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStringView) -> Unit): ArrayHolder<WGPUStringView>
    }
}

expect value class WGPUAdapter(val handler: NativeAddress)

expect value class WGPUBindGroup(val handler: NativeAddress)

expect value class WGPUBindGroupLayout(val handler: NativeAddress)

expect value class WGPUBuffer(val handler: NativeAddress)

expect value class WGPUCommandBuffer(val handler: NativeAddress)

expect value class WGPUCommandEncoder(val handler: NativeAddress)

expect value class WGPUComputePassEncoder(val handler: NativeAddress)

expect value class WGPUComputePipeline(val handler: NativeAddress)

expect value class WGPUDevice(val handler: NativeAddress)

expect value class WGPUExternalTexture(val handler: NativeAddress)

expect value class WGPUInstance(val handler: NativeAddress)

expect value class WGPUPipelineLayout(val handler: NativeAddress)

expect value class WGPUQuerySet(val handler: NativeAddress)

expect value class WGPUQueue(val handler: NativeAddress)

expect value class WGPURenderBundle(val handler: NativeAddress)

expect value class WGPURenderBundleEncoder(val handler: NativeAddress)

expect value class WGPURenderPassEncoder(val handler: NativeAddress)

expect value class WGPURenderPipeline(val handler: NativeAddress)

expect value class WGPUResourceTable(val handler: NativeAddress)

expect value class WGPUSampler(val handler: NativeAddress)

expect value class WGPUShaderModule(val handler: NativeAddress)

expect value class WGPUSharedBufferMemory(val handler: NativeAddress)

expect value class WGPUSharedFence(val handler: NativeAddress)

expect value class WGPUSharedTextureMemory(val handler: NativeAddress)

expect value class WGPUSurface(val handler: NativeAddress)

expect value class WGPUTexelBufferView(val handler: NativeAddress)

expect value class WGPUTexture(val handler: NativeAddress)

expect value class WGPUTextureView(val handler: NativeAddress)

typealias WGPUAdapterType = UInt
const val WGPUAdapterType_DiscreteGPU : WGPUAdapterType = 1u
const val WGPUAdapterType_IntegratedGPU : WGPUAdapterType = 2u
const val WGPUAdapterType_CPU : WGPUAdapterType = 3u
const val WGPUAdapterType_Unknown : WGPUAdapterType = 4u
const val WGPUAdapterType_Force32 : WGPUAdapterType = 2147483647u

typealias WGPUAddressMode = UInt
const val WGPUAddressMode_Undefined : WGPUAddressMode = 0u
const val WGPUAddressMode_ClampToEdge : WGPUAddressMode = 1u
const val WGPUAddressMode_Repeat : WGPUAddressMode = 2u
const val WGPUAddressMode_MirrorRepeat : WGPUAddressMode = 3u
const val WGPUAddressMode_Force32 : WGPUAddressMode = 2147483647u

typealias WGPUAlphaMode = UInt
const val WGPUAlphaMode_Opaque : WGPUAlphaMode = 1u
const val WGPUAlphaMode_Premultiplied : WGPUAlphaMode = 2u
const val WGPUAlphaMode_Unpremultiplied : WGPUAlphaMode = 3u
const val WGPUAlphaMode_Force32 : WGPUAlphaMode = 2147483647u

typealias WGPUBackendType = UInt
const val WGPUBackendType_Undefined : WGPUBackendType = 0u
const val WGPUBackendType_Null : WGPUBackendType = 1u
const val WGPUBackendType_WebGPU : WGPUBackendType = 2u
const val WGPUBackendType_D3D11 : WGPUBackendType = 3u
const val WGPUBackendType_D3D12 : WGPUBackendType = 4u
const val WGPUBackendType_Metal : WGPUBackendType = 5u
const val WGPUBackendType_Vulkan : WGPUBackendType = 6u
const val WGPUBackendType_OpenGL : WGPUBackendType = 7u
const val WGPUBackendType_OpenGLES : WGPUBackendType = 8u
const val WGPUBackendType_Force32 : WGPUBackendType = 2147483647u

typealias WGPUBlendFactor = UInt
const val WGPUBlendFactor_Undefined : WGPUBlendFactor = 0u
const val WGPUBlendFactor_Zero : WGPUBlendFactor = 1u
const val WGPUBlendFactor_One : WGPUBlendFactor = 2u
const val WGPUBlendFactor_Src : WGPUBlendFactor = 3u
const val WGPUBlendFactor_OneMinusSrc : WGPUBlendFactor = 4u
const val WGPUBlendFactor_SrcAlpha : WGPUBlendFactor = 5u
const val WGPUBlendFactor_OneMinusSrcAlpha : WGPUBlendFactor = 6u
const val WGPUBlendFactor_Dst : WGPUBlendFactor = 7u
const val WGPUBlendFactor_OneMinusDst : WGPUBlendFactor = 8u
const val WGPUBlendFactor_DstAlpha : WGPUBlendFactor = 9u
const val WGPUBlendFactor_OneMinusDstAlpha : WGPUBlendFactor = 10u
const val WGPUBlendFactor_SrcAlphaSaturated : WGPUBlendFactor = 11u
const val WGPUBlendFactor_Constant : WGPUBlendFactor = 12u
const val WGPUBlendFactor_OneMinusConstant : WGPUBlendFactor = 13u
const val WGPUBlendFactor_Src1 : WGPUBlendFactor = 14u
const val WGPUBlendFactor_OneMinusSrc1 : WGPUBlendFactor = 15u
const val WGPUBlendFactor_Src1Alpha : WGPUBlendFactor = 16u
const val WGPUBlendFactor_OneMinusSrc1Alpha : WGPUBlendFactor = 17u
const val WGPUBlendFactor_Force32 : WGPUBlendFactor = 2147483647u

typealias WGPUBlendOperation = UInt
const val WGPUBlendOperation_Undefined : WGPUBlendOperation = 0u
const val WGPUBlendOperation_Add : WGPUBlendOperation = 1u
const val WGPUBlendOperation_Subtract : WGPUBlendOperation = 2u
const val WGPUBlendOperation_ReverseSubtract : WGPUBlendOperation = 3u
const val WGPUBlendOperation_Min : WGPUBlendOperation = 4u
const val WGPUBlendOperation_Max : WGPUBlendOperation = 5u
const val WGPUBlendOperation_Force32 : WGPUBlendOperation = 2147483647u

typealias WGPUBufferBindingType = UInt
const val WGPUBufferBindingType_BindingNotUsed : WGPUBufferBindingType = 0u
const val WGPUBufferBindingType_Undefined : WGPUBufferBindingType = 1u
const val WGPUBufferBindingType_Uniform : WGPUBufferBindingType = 2u
const val WGPUBufferBindingType_Storage : WGPUBufferBindingType = 3u
const val WGPUBufferBindingType_ReadOnlyStorage : WGPUBufferBindingType = 4u
const val WGPUBufferBindingType_Force32 : WGPUBufferBindingType = 2147483647u

typealias WGPUBufferMapState = UInt
const val WGPUBufferMapState_Unmapped : WGPUBufferMapState = 1u
const val WGPUBufferMapState_Pending : WGPUBufferMapState = 2u
const val WGPUBufferMapState_Mapped : WGPUBufferMapState = 3u
const val WGPUBufferMapState_Force32 : WGPUBufferMapState = 2147483647u

typealias WGPUCallbackMode = UInt
const val WGPUCallbackMode_WaitAnyOnly : WGPUCallbackMode = 1u
const val WGPUCallbackMode_AllowProcessEvents : WGPUCallbackMode = 2u
const val WGPUCallbackMode_AllowSpontaneous : WGPUCallbackMode = 3u
const val WGPUCallbackMode_Force32 : WGPUCallbackMode = 2147483647u

typealias WGPUCallbackStatus = UInt
const val WGPUCallbackStatus_Success : WGPUCallbackStatus = 1u
const val WGPUCallbackStatus_Error : WGPUCallbackStatus = 2u
const val WGPUCallbackStatus_Force32 : WGPUCallbackStatus = 2147483647u

typealias WGPUColorSpacePrimariesDawn = UInt
const val WGPUColorSpacePrimariesDawn_SRGB : WGPUColorSpacePrimariesDawn = 1u
const val WGPUColorSpacePrimariesDawn_Rec709 : WGPUColorSpacePrimariesDawn = 1u
const val WGPUColorSpacePrimariesDawn_Rec601 : WGPUColorSpacePrimariesDawn = 2u
const val WGPUColorSpacePrimariesDawn_Rec2020 : WGPUColorSpacePrimariesDawn = 3u
const val WGPUColorSpacePrimariesDawn_DisplayP3 : WGPUColorSpacePrimariesDawn = 4u
const val WGPUColorSpacePrimariesDawn_Force32 : WGPUColorSpacePrimariesDawn = 2147483647u

typealias WGPUColorSpaceTransferDawn = UInt
const val WGPUColorSpaceTransferDawn_Identity : WGPUColorSpaceTransferDawn = 1u
const val WGPUColorSpaceTransferDawn_SRGB : WGPUColorSpaceTransferDawn = 2u
const val WGPUColorSpaceTransferDawn_DisplayP3 : WGPUColorSpaceTransferDawn = 3u
const val WGPUColorSpaceTransferDawn_SMPTE_170M : WGPUColorSpaceTransferDawn = 4u
const val WGPUColorSpaceTransferDawn_HLG : WGPUColorSpaceTransferDawn = 5u
const val WGPUColorSpaceTransferDawn_PQ : WGPUColorSpaceTransferDawn = 6u
const val WGPUColorSpaceTransferDawn_BT_1886 : WGPUColorSpaceTransferDawn = 7u
const val WGPUColorSpaceTransferDawn_Force32 : WGPUColorSpaceTransferDawn = 2147483647u

typealias WGPUColorSpaceYCbCrMatrixDawn = UInt
const val WGPUColorSpaceYCbCrMatrixDawn_Identity : WGPUColorSpaceYCbCrMatrixDawn = 1u
const val WGPUColorSpaceYCbCrMatrixDawn_Rec601 : WGPUColorSpaceYCbCrMatrixDawn = 2u
const val WGPUColorSpaceYCbCrMatrixDawn_Rec709 : WGPUColorSpaceYCbCrMatrixDawn = 3u
const val WGPUColorSpaceYCbCrMatrixDawn_Rec2020 : WGPUColorSpaceYCbCrMatrixDawn = 4u
const val WGPUColorSpaceYCbCrMatrixDawn_Force32 : WGPUColorSpaceYCbCrMatrixDawn = 2147483647u

typealias WGPUColorSpaceYCbCrRangeDawn = UInt
const val WGPUColorSpaceYCbCrRangeDawn_Identity : WGPUColorSpaceYCbCrRangeDawn = 1u
const val WGPUColorSpaceYCbCrRangeDawn_Narrow : WGPUColorSpaceYCbCrRangeDawn = 2u
const val WGPUColorSpaceYCbCrRangeDawn_Full : WGPUColorSpaceYCbCrRangeDawn = 3u
const val WGPUColorSpaceYCbCrRangeDawn_Force32 : WGPUColorSpaceYCbCrRangeDawn = 2147483647u

typealias WGPUCompareFunction = UInt
const val WGPUCompareFunction_Undefined : WGPUCompareFunction = 0u
const val WGPUCompareFunction_Never : WGPUCompareFunction = 1u
const val WGPUCompareFunction_Less : WGPUCompareFunction = 2u
const val WGPUCompareFunction_Equal : WGPUCompareFunction = 3u
const val WGPUCompareFunction_LessEqual : WGPUCompareFunction = 4u
const val WGPUCompareFunction_Greater : WGPUCompareFunction = 5u
const val WGPUCompareFunction_NotEqual : WGPUCompareFunction = 6u
const val WGPUCompareFunction_GreaterEqual : WGPUCompareFunction = 7u
const val WGPUCompareFunction_Always : WGPUCompareFunction = 8u
const val WGPUCompareFunction_Force32 : WGPUCompareFunction = 2147483647u

typealias WGPUCompilationInfoRequestStatus = UInt
const val WGPUCompilationInfoRequestStatus_Success : WGPUCompilationInfoRequestStatus = 1u
const val WGPUCompilationInfoRequestStatus_CallbackCancelled : WGPUCompilationInfoRequestStatus = 2u
const val WGPUCompilationInfoRequestStatus_Force32 : WGPUCompilationInfoRequestStatus = 2147483647u

typealias WGPUCompilationMessageType = UInt
const val WGPUCompilationMessageType_Error : WGPUCompilationMessageType = 1u
const val WGPUCompilationMessageType_Warning : WGPUCompilationMessageType = 2u
const val WGPUCompilationMessageType_Info : WGPUCompilationMessageType = 3u
const val WGPUCompilationMessageType_Force32 : WGPUCompilationMessageType = 2147483647u

typealias WGPUComponentSwizzle = UInt
const val WGPUComponentSwizzle_Undefined : WGPUComponentSwizzle = 0u
const val WGPUComponentSwizzle_Zero : WGPUComponentSwizzle = 1u
const val WGPUComponentSwizzle_One : WGPUComponentSwizzle = 2u
const val WGPUComponentSwizzle_R : WGPUComponentSwizzle = 3u
const val WGPUComponentSwizzle_G : WGPUComponentSwizzle = 4u
const val WGPUComponentSwizzle_B : WGPUComponentSwizzle = 5u
const val WGPUComponentSwizzle_A : WGPUComponentSwizzle = 6u
const val WGPUComponentSwizzle_Force32 : WGPUComponentSwizzle = 2147483647u

typealias WGPUCompositeAlphaMode = UInt
const val WGPUCompositeAlphaMode_Auto : WGPUCompositeAlphaMode = 0u
const val WGPUCompositeAlphaMode_Opaque : WGPUCompositeAlphaMode = 1u
const val WGPUCompositeAlphaMode_Premultiplied : WGPUCompositeAlphaMode = 2u
const val WGPUCompositeAlphaMode_Unpremultiplied : WGPUCompositeAlphaMode = 3u
const val WGPUCompositeAlphaMode_Inherit : WGPUCompositeAlphaMode = 4u
const val WGPUCompositeAlphaMode_Force32 : WGPUCompositeAlphaMode = 2147483647u

typealias WGPUCreatePipelineAsyncStatus = UInt
const val WGPUCreatePipelineAsyncStatus_Success : WGPUCreatePipelineAsyncStatus = 1u
const val WGPUCreatePipelineAsyncStatus_CallbackCancelled : WGPUCreatePipelineAsyncStatus = 2u
const val WGPUCreatePipelineAsyncStatus_ValidationError : WGPUCreatePipelineAsyncStatus = 3u
const val WGPUCreatePipelineAsyncStatus_InternalError : WGPUCreatePipelineAsyncStatus = 4u
const val WGPUCreatePipelineAsyncStatus_Force32 : WGPUCreatePipelineAsyncStatus = 2147483647u

typealias WGPUCullMode = UInt
const val WGPUCullMode_Undefined : WGPUCullMode = 0u
const val WGPUCullMode_None : WGPUCullMode = 1u
const val WGPUCullMode_Front : WGPUCullMode = 2u
const val WGPUCullMode_Back : WGPUCullMode = 3u
const val WGPUCullMode_Force32 : WGPUCullMode = 2147483647u

typealias WGPUDeviceLostReason = UInt
const val WGPUDeviceLostReason_Unknown : WGPUDeviceLostReason = 1u
const val WGPUDeviceLostReason_Destroyed : WGPUDeviceLostReason = 2u
const val WGPUDeviceLostReason_CallbackCancelled : WGPUDeviceLostReason = 3u
const val WGPUDeviceLostReason_FailedCreation : WGPUDeviceLostReason = 4u
const val WGPUDeviceLostReason_Force32 : WGPUDeviceLostReason = 2147483647u

typealias WGPUErrorFilter = UInt
const val WGPUErrorFilter_Validation : WGPUErrorFilter = 1u
const val WGPUErrorFilter_OutOfMemory : WGPUErrorFilter = 2u
const val WGPUErrorFilter_Internal : WGPUErrorFilter = 3u
const val WGPUErrorFilter_Force32 : WGPUErrorFilter = 2147483647u

typealias WGPUErrorType = UInt
const val WGPUErrorType_NoError : WGPUErrorType = 1u
const val WGPUErrorType_Validation : WGPUErrorType = 2u
const val WGPUErrorType_OutOfMemory : WGPUErrorType = 3u
const val WGPUErrorType_Internal : WGPUErrorType = 4u
const val WGPUErrorType_Unknown : WGPUErrorType = 5u
const val WGPUErrorType_Force32 : WGPUErrorType = 2147483647u

typealias WGPUExternalTextureRotation = UInt
const val WGPUExternalTextureRotation_Rotate0Degrees : WGPUExternalTextureRotation = 1u
const val WGPUExternalTextureRotation_Rotate90Degrees : WGPUExternalTextureRotation = 2u
const val WGPUExternalTextureRotation_Rotate180Degrees : WGPUExternalTextureRotation = 3u
const val WGPUExternalTextureRotation_Rotate270Degrees : WGPUExternalTextureRotation = 4u
const val WGPUExternalTextureRotation_Force32 : WGPUExternalTextureRotation = 2147483647u

typealias WGPUFeatureLevel = UInt
const val WGPUFeatureLevel_Undefined : WGPUFeatureLevel = 0u
const val WGPUFeatureLevel_Compatibility : WGPUFeatureLevel = 1u
const val WGPUFeatureLevel_Core : WGPUFeatureLevel = 2u
const val WGPUFeatureLevel_Force32 : WGPUFeatureLevel = 2147483647u

typealias WGPUFeatureName = UInt
const val WGPUFeatureName_CoreFeaturesAndLimits : WGPUFeatureName = 1u
const val WGPUFeatureName_DepthClipControl : WGPUFeatureName = 2u
const val WGPUFeatureName_Depth32FloatStencil8 : WGPUFeatureName = 3u
const val WGPUFeatureName_TextureCompressionBC : WGPUFeatureName = 4u
const val WGPUFeatureName_TextureCompressionBCSliced3D : WGPUFeatureName = 5u
const val WGPUFeatureName_TextureCompressionETC2 : WGPUFeatureName = 6u
const val WGPUFeatureName_TextureCompressionASTC : WGPUFeatureName = 7u
const val WGPUFeatureName_TextureCompressionASTCSliced3D : WGPUFeatureName = 8u
const val WGPUFeatureName_TimestampQuery : WGPUFeatureName = 9u
const val WGPUFeatureName_IndirectFirstInstance : WGPUFeatureName = 10u
const val WGPUFeatureName_ShaderF16 : WGPUFeatureName = 11u
const val WGPUFeatureName_RG11B10UfloatRenderable : WGPUFeatureName = 12u
const val WGPUFeatureName_BGRA8UnormStorage : WGPUFeatureName = 13u
const val WGPUFeatureName_Float32Filterable : WGPUFeatureName = 14u
const val WGPUFeatureName_Float32Blendable : WGPUFeatureName = 15u
const val WGPUFeatureName_ClipDistances : WGPUFeatureName = 16u
const val WGPUFeatureName_DualSourceBlending : WGPUFeatureName = 17u
const val WGPUFeatureName_Subgroups : WGPUFeatureName = 18u
const val WGPUFeatureName_TextureFormatsTier1 : WGPUFeatureName = 19u
const val WGPUFeatureName_TextureFormatsTier2 : WGPUFeatureName = 20u
const val WGPUFeatureName_PrimitiveIndex : WGPUFeatureName = 21u
const val WGPUFeatureName_TextureComponentSwizzle : WGPUFeatureName = 22u
const val WGPUFeatureName_SubgroupSizeControl : WGPUFeatureName = 23u
const val WGPUFeatureName_TextureCompressionUnaligned : WGPUFeatureName = 24u
const val WGPUFeatureName_DawnInternalUsages : WGPUFeatureName = 327680u
const val WGPUFeatureName_DawnMultiPlanarFormats : WGPUFeatureName = 327681u
const val WGPUFeatureName_DawnNative : WGPUFeatureName = 327682u
const val WGPUFeatureName_ChromiumExperimentalTimestampQueryInsidePasses : WGPUFeatureName = 327683u
const val WGPUFeatureName_ImplicitDeviceSynchronization : WGPUFeatureName = 327684u
const val WGPUFeatureName_TransientAttachments : WGPUFeatureName = 327686u
const val WGPUFeatureName_MSAARenderToSingleSampled : WGPUFeatureName = 327687u
const val WGPUFeatureName_D3D11MultithreadProtected : WGPUFeatureName = 327688u
const val WGPUFeatureName_ANGLETextureSharing : WGPUFeatureName = 327689u
const val WGPUFeatureName_PixelLocalStorageCoherent : WGPUFeatureName = 327690u
const val WGPUFeatureName_PixelLocalStorageNonCoherent : WGPUFeatureName = 327691u
const val WGPUFeatureName_Unorm16TextureFormats : WGPUFeatureName = 327692u
const val WGPUFeatureName_MultiPlanarFormatExtendedUsages : WGPUFeatureName = 327693u
const val WGPUFeatureName_MultiPlanarFormatP010 : WGPUFeatureName = 327694u
const val WGPUFeatureName_HostMappedPointer : WGPUFeatureName = 327695u
const val WGPUFeatureName_MultiPlanarRenderTargets : WGPUFeatureName = 327696u
const val WGPUFeatureName_MultiPlanarFormatNv12a : WGPUFeatureName = 327697u
const val WGPUFeatureName_FramebufferFetch : WGPUFeatureName = 327698u
const val WGPUFeatureName_BufferMapExtendedUsages : WGPUFeatureName = 327699u
const val WGPUFeatureName_AdapterPropertiesMemoryHeaps : WGPUFeatureName = 327700u
const val WGPUFeatureName_AdapterPropertiesD3D : WGPUFeatureName = 327701u
const val WGPUFeatureName_AdapterPropertiesVk : WGPUFeatureName = 327702u
const val WGPUFeatureName_DawnFormatCapabilities : WGPUFeatureName = 327703u
const val WGPUFeatureName_DawnDrmFormatCapabilities : WGPUFeatureName = 327704u
const val WGPUFeatureName_MultiPlanarFormatNv16 : WGPUFeatureName = 327705u
const val WGPUFeatureName_MultiPlanarFormatNv24 : WGPUFeatureName = 327706u
const val WGPUFeatureName_MultiPlanarFormatP210 : WGPUFeatureName = 327707u
const val WGPUFeatureName_MultiPlanarFormatP410 : WGPUFeatureName = 327708u
const val WGPUFeatureName_SharedTextureMemoryVkDedicatedAllocation : WGPUFeatureName = 327709u
const val WGPUFeatureName_SharedTextureMemoryAHardwareBuffer : WGPUFeatureName = 327710u
const val WGPUFeatureName_SharedTextureMemoryDmaBuf : WGPUFeatureName = 327711u
const val WGPUFeatureName_SharedTextureMemoryOpaqueFD : WGPUFeatureName = 327712u
const val WGPUFeatureName_SharedTextureMemoryZirconHandle : WGPUFeatureName = 327713u
const val WGPUFeatureName_SharedTextureMemoryDXGISharedHandle : WGPUFeatureName = 327714u
const val WGPUFeatureName_SharedTextureMemoryD3D11Texture2D : WGPUFeatureName = 327715u
const val WGPUFeatureName_SharedTextureMemoryIOSurface : WGPUFeatureName = 327716u
const val WGPUFeatureName_SharedTextureMemoryEGLImage : WGPUFeatureName = 327717u
const val WGPUFeatureName_SharedFenceVkSemaphoreOpaqueFD : WGPUFeatureName = 327718u
const val WGPUFeatureName_SharedFenceSyncFD : WGPUFeatureName = 327719u
const val WGPUFeatureName_SharedFenceVkSemaphoreZirconHandle : WGPUFeatureName = 327720u
const val WGPUFeatureName_SharedFenceDXGISharedHandle : WGPUFeatureName = 327721u
const val WGPUFeatureName_SharedFenceMTLSharedEvent : WGPUFeatureName = 327722u
const val WGPUFeatureName_SharedBufferMemoryD3D12Resource : WGPUFeatureName = 327723u
const val WGPUFeatureName_StaticSamplers : WGPUFeatureName = 327724u
const val WGPUFeatureName_YCbCrVulkanSamplers : WGPUFeatureName = 327725u
const val WGPUFeatureName_ShaderModuleCompilationOptions : WGPUFeatureName = 327726u
const val WGPUFeatureName_DawnLoadResolveTexture : WGPUFeatureName = 327727u
const val WGPUFeatureName_DawnPartialLoadResolveTexture : WGPUFeatureName = 327728u
const val WGPUFeatureName_MultiDrawIndirect : WGPUFeatureName = 327729u
const val WGPUFeatureName_DawnTexelCopyBufferRowAlignment : WGPUFeatureName = 327730u
const val WGPUFeatureName_FlexibleTextureViews : WGPUFeatureName = 327731u
const val WGPUFeatureName_ChromiumExperimentalSubgroupMatrix : WGPUFeatureName = 327732u
const val WGPUFeatureName_SharedFenceEGLSync : WGPUFeatureName = 327733u
const val WGPUFeatureName_DawnDeviceAllocatorControl : WGPUFeatureName = 327734u
const val WGPUFeatureName_AdapterPropertiesWGPU : WGPUFeatureName = 327735u
const val WGPUFeatureName_SharedBufferMemoryFromWindowsHandle : WGPUFeatureName = 327736u
const val WGPUFeatureName_SharedTextureMemoryD3D12Resource : WGPUFeatureName = 327737u
const val WGPUFeatureName_ChromiumExperimentalSamplingResourceTable : WGPUFeatureName = 327738u
const val WGPUFeatureName_AtomicVec2uMinMax : WGPUFeatureName = 327739u
const val WGPUFeatureName_Unorm16FormatsForExternalTexture : WGPUFeatureName = 327740u
const val WGPUFeatureName_OpaqueYCbCrAndroidForExternalTexture : WGPUFeatureName = 327741u
const val WGPUFeatureName_Unorm16Filterable : WGPUFeatureName = 327742u
const val WGPUFeatureName_RenderPassRenderArea : WGPUFeatureName = 327743u
const val WGPUFeatureName_AdapterPropertiesDrm : WGPUFeatureName = 327744u
const val WGPUFeatureName_DawnAllowUndefinedLoadStoreOp : WGPUFeatureName = 327745u
const val WGPUFeatureName_BufferMapWriteExtendedUsages : WGPUFeatureName = 327746u
const val WGPUFeatureName_SharedBufferMemoryHostPointer : WGPUFeatureName = 327747u
const val WGPUFeatureName_Force32 : WGPUFeatureName = 2147483647u

typealias WGPUFilterMode = UInt
const val WGPUFilterMode_Undefined : WGPUFilterMode = 0u
const val WGPUFilterMode_Nearest : WGPUFilterMode = 1u
const val WGPUFilterMode_Linear : WGPUFilterMode = 2u
const val WGPUFilterMode_Force32 : WGPUFilterMode = 2147483647u

typealias WGPUFrontFace = UInt
const val WGPUFrontFace_Undefined : WGPUFrontFace = 0u
const val WGPUFrontFace_CCW : WGPUFrontFace = 1u
const val WGPUFrontFace_CW : WGPUFrontFace = 2u
const val WGPUFrontFace_Force32 : WGPUFrontFace = 2147483647u

typealias WGPUIndexFormat = UInt
const val WGPUIndexFormat_Undefined : WGPUIndexFormat = 0u
const val WGPUIndexFormat_Uint16 : WGPUIndexFormat = 1u
const val WGPUIndexFormat_Uint32 : WGPUIndexFormat = 2u
const val WGPUIndexFormat_Force32 : WGPUIndexFormat = 2147483647u

typealias WGPUInstanceFeatureName = UInt
const val WGPUInstanceFeatureName_TimedWaitAny : WGPUInstanceFeatureName = 1u
const val WGPUInstanceFeatureName_ShaderSourceSPIRV : WGPUInstanceFeatureName = 2u
const val WGPUInstanceFeatureName_MultipleDevicesPerAdapter : WGPUInstanceFeatureName = 3u
const val WGPUInstanceFeatureName_Force32 : WGPUInstanceFeatureName = 2147483647u

typealias WGPULoadOp = UInt
const val WGPULoadOp_Undefined : WGPULoadOp = 0u
const val WGPULoadOp_Load : WGPULoadOp = 1u
const val WGPULoadOp_Clear : WGPULoadOp = 2u
const val WGPULoadOp_ExpandResolveTexture : WGPULoadOp = 327683u
const val WGPULoadOp_Force32 : WGPULoadOp = 2147483647u

typealias WGPULoggingType = UInt
const val WGPULoggingType_Verbose : WGPULoggingType = 1u
const val WGPULoggingType_Info : WGPULoggingType = 2u
const val WGPULoggingType_Warning : WGPULoggingType = 3u
const val WGPULoggingType_Error : WGPULoggingType = 4u
const val WGPULoggingType_Force32 : WGPULoggingType = 2147483647u

typealias WGPUMapAsyncStatus = UInt
const val WGPUMapAsyncStatus_Success : WGPUMapAsyncStatus = 1u
const val WGPUMapAsyncStatus_CallbackCancelled : WGPUMapAsyncStatus = 2u
const val WGPUMapAsyncStatus_Error : WGPUMapAsyncStatus = 3u
const val WGPUMapAsyncStatus_Aborted : WGPUMapAsyncStatus = 4u
const val WGPUMapAsyncStatus_Force32 : WGPUMapAsyncStatus = 2147483647u

typealias WGPUMipmapFilterMode = UInt
const val WGPUMipmapFilterMode_Undefined : WGPUMipmapFilterMode = 0u
const val WGPUMipmapFilterMode_Nearest : WGPUMipmapFilterMode = 1u
const val WGPUMipmapFilterMode_Linear : WGPUMipmapFilterMode = 2u
const val WGPUMipmapFilterMode_Force32 : WGPUMipmapFilterMode = 2147483647u

typealias WGPUOptionalBool = UInt
const val WGPUOptionalBool_False : WGPUOptionalBool = 0u
const val WGPUOptionalBool_True : WGPUOptionalBool = 1u
const val WGPUOptionalBool_Undefined : WGPUOptionalBool = 2u
const val WGPUOptionalBool_Force32 : WGPUOptionalBool = 2147483647u

typealias WGPUPopErrorScopeStatus = UInt
const val WGPUPopErrorScopeStatus_Success : WGPUPopErrorScopeStatus = 1u
const val WGPUPopErrorScopeStatus_CallbackCancelled : WGPUPopErrorScopeStatus = 2u
const val WGPUPopErrorScopeStatus_Error : WGPUPopErrorScopeStatus = 3u
const val WGPUPopErrorScopeStatus_Force32 : WGPUPopErrorScopeStatus = 2147483647u

typealias WGPUPowerPreference = UInt
const val WGPUPowerPreference_Undefined : WGPUPowerPreference = 0u
const val WGPUPowerPreference_LowPower : WGPUPowerPreference = 1u
const val WGPUPowerPreference_HighPerformance : WGPUPowerPreference = 2u
const val WGPUPowerPreference_Force32 : WGPUPowerPreference = 2147483647u

typealias WGPUPredefinedColorSpace = UInt
const val WGPUPredefinedColorSpace_SRGB : WGPUPredefinedColorSpace = 1u
const val WGPUPredefinedColorSpace_DisplayP3 : WGPUPredefinedColorSpace = 2u
const val WGPUPredefinedColorSpace_SRGBLinear : WGPUPredefinedColorSpace = 327683u
const val WGPUPredefinedColorSpace_DisplayP3Linear : WGPUPredefinedColorSpace = 327684u
const val WGPUPredefinedColorSpace_Rec2020Linear : WGPUPredefinedColorSpace = 327685u
const val WGPUPredefinedColorSpace_Force32 : WGPUPredefinedColorSpace = 2147483647u

typealias WGPUPresentMode = UInt
const val WGPUPresentMode_Undefined : WGPUPresentMode = 0u
const val WGPUPresentMode_Fifo : WGPUPresentMode = 1u
const val WGPUPresentMode_FifoRelaxed : WGPUPresentMode = 2u
const val WGPUPresentMode_Immediate : WGPUPresentMode = 3u
const val WGPUPresentMode_Mailbox : WGPUPresentMode = 4u
const val WGPUPresentMode_Force32 : WGPUPresentMode = 2147483647u

typealias WGPUPrimitiveTopology = UInt
const val WGPUPrimitiveTopology_Undefined : WGPUPrimitiveTopology = 0u
const val WGPUPrimitiveTopology_PointList : WGPUPrimitiveTopology = 1u
const val WGPUPrimitiveTopology_LineList : WGPUPrimitiveTopology = 2u
const val WGPUPrimitiveTopology_LineStrip : WGPUPrimitiveTopology = 3u
const val WGPUPrimitiveTopology_TriangleList : WGPUPrimitiveTopology = 4u
const val WGPUPrimitiveTopology_TriangleStrip : WGPUPrimitiveTopology = 5u
const val WGPUPrimitiveTopology_Force32 : WGPUPrimitiveTopology = 2147483647u

typealias WGPUQueryType = UInt
const val WGPUQueryType_Occlusion : WGPUQueryType = 1u
const val WGPUQueryType_Timestamp : WGPUQueryType = 2u
const val WGPUQueryType_Force32 : WGPUQueryType = 2147483647u

typealias WGPUQueueWorkDoneStatus = UInt
const val WGPUQueueWorkDoneStatus_Success : WGPUQueueWorkDoneStatus = 1u
const val WGPUQueueWorkDoneStatus_CallbackCancelled : WGPUQueueWorkDoneStatus = 2u
const val WGPUQueueWorkDoneStatus_Error : WGPUQueueWorkDoneStatus = 3u
const val WGPUQueueWorkDoneStatus_Force32 : WGPUQueueWorkDoneStatus = 2147483647u

typealias WGPURequestAdapterStatus = UInt
const val WGPURequestAdapterStatus_Success : WGPURequestAdapterStatus = 1u
const val WGPURequestAdapterStatus_CallbackCancelled : WGPURequestAdapterStatus = 2u
const val WGPURequestAdapterStatus_Unavailable : WGPURequestAdapterStatus = 3u
const val WGPURequestAdapterStatus_Error : WGPURequestAdapterStatus = 4u
const val WGPURequestAdapterStatus_Force32 : WGPURequestAdapterStatus = 2147483647u

typealias WGPURequestDeviceStatus = UInt
const val WGPURequestDeviceStatus_Success : WGPURequestDeviceStatus = 1u
const val WGPURequestDeviceStatus_CallbackCancelled : WGPURequestDeviceStatus = 2u
const val WGPURequestDeviceStatus_Error : WGPURequestDeviceStatus = 3u
const val WGPURequestDeviceStatus_Force32 : WGPURequestDeviceStatus = 2147483647u

typealias WGPUSamplerBindingType = UInt
const val WGPUSamplerBindingType_BindingNotUsed : WGPUSamplerBindingType = 0u
const val WGPUSamplerBindingType_Undefined : WGPUSamplerBindingType = 1u
const val WGPUSamplerBindingType_Filtering : WGPUSamplerBindingType = 2u
const val WGPUSamplerBindingType_NonFiltering : WGPUSamplerBindingType = 3u
const val WGPUSamplerBindingType_Comparison : WGPUSamplerBindingType = 4u
const val WGPUSamplerBindingType_Force32 : WGPUSamplerBindingType = 2147483647u

typealias WGPUSharedFenceType = UInt
const val WGPUSharedFenceType_VkSemaphoreOpaqueFD : WGPUSharedFenceType = 1u
const val WGPUSharedFenceType_SyncFD : WGPUSharedFenceType = 2u
const val WGPUSharedFenceType_VkSemaphoreZirconHandle : WGPUSharedFenceType = 3u
const val WGPUSharedFenceType_DXGISharedHandle : WGPUSharedFenceType = 4u
const val WGPUSharedFenceType_MTLSharedEvent : WGPUSharedFenceType = 5u
const val WGPUSharedFenceType_EGLSync : WGPUSharedFenceType = 6u
const val WGPUSharedFenceType_Force32 : WGPUSharedFenceType = 2147483647u

typealias WGPUStatus = UInt
const val WGPUStatus_Success : WGPUStatus = 1u
const val WGPUStatus_Error : WGPUStatus = 2u
const val WGPUStatus_Force32 : WGPUStatus = 2147483647u

typealias WGPUStencilOperation = UInt
const val WGPUStencilOperation_Undefined : WGPUStencilOperation = 0u
const val WGPUStencilOperation_Keep : WGPUStencilOperation = 1u
const val WGPUStencilOperation_Zero : WGPUStencilOperation = 2u
const val WGPUStencilOperation_Replace : WGPUStencilOperation = 3u
const val WGPUStencilOperation_Invert : WGPUStencilOperation = 4u
const val WGPUStencilOperation_IncrementClamp : WGPUStencilOperation = 5u
const val WGPUStencilOperation_DecrementClamp : WGPUStencilOperation = 6u
const val WGPUStencilOperation_IncrementWrap : WGPUStencilOperation = 7u
const val WGPUStencilOperation_DecrementWrap : WGPUStencilOperation = 8u
const val WGPUStencilOperation_Force32 : WGPUStencilOperation = 2147483647u

typealias WGPUStorageTextureAccess = UInt
const val WGPUStorageTextureAccess_BindingNotUsed : WGPUStorageTextureAccess = 0u
const val WGPUStorageTextureAccess_Undefined : WGPUStorageTextureAccess = 1u
const val WGPUStorageTextureAccess_WriteOnly : WGPUStorageTextureAccess = 2u
const val WGPUStorageTextureAccess_ReadOnly : WGPUStorageTextureAccess = 3u
const val WGPUStorageTextureAccess_ReadWrite : WGPUStorageTextureAccess = 4u
const val WGPUStorageTextureAccess_Force32 : WGPUStorageTextureAccess = 2147483647u

typealias WGPUStoreOp = UInt
const val WGPUStoreOp_Undefined : WGPUStoreOp = 0u
const val WGPUStoreOp_Store : WGPUStoreOp = 1u
const val WGPUStoreOp_Discard : WGPUStoreOp = 2u
const val WGPUStoreOp_Force32 : WGPUStoreOp = 2147483647u

typealias WGPUSType = UInt
const val WGPUSType_ShaderSourceSPIRV : WGPUSType = 1u
const val WGPUSType_ShaderSourceWGSL : WGPUSType = 2u
const val WGPUSType_RenderPassMaxDrawCount : WGPUSType = 3u
const val WGPUSType_SurfaceSourceMetalLayer : WGPUSType = 4u
const val WGPUSType_SurfaceSourceWindowsHWND : WGPUSType = 5u
const val WGPUSType_SurfaceSourceXlibWindow : WGPUSType = 6u
const val WGPUSType_SurfaceSourceWaylandSurface : WGPUSType = 7u
const val WGPUSType_SurfaceSourceAndroidNativeWindow : WGPUSType = 8u
const val WGPUSType_SurfaceSourceXCBWindow : WGPUSType = 9u
const val WGPUSType_SurfaceColorManagement : WGPUSType = 10u
const val WGPUSType_RequestAdapterWebXROptions : WGPUSType = 11u
const val WGPUSType_TextureComponentSwizzleDescriptor : WGPUSType = 12u
const val WGPUSType_ExternalTextureBindingLayout : WGPUSType = 13u
const val WGPUSType_ExternalTextureBindingEntry : WGPUSType = 14u
const val WGPUSType_CompatibilityModeLimits : WGPUSType = 15u
const val WGPUSType_TextureBindingViewDimension : WGPUSType = 16u
const val WGPUSType_EmscriptenSurfaceSourceCanvasHTMLSelector : WGPUSType = 262144u
const val WGPUSType_SurfaceDescriptorFromWindowsCoreWindow : WGPUSType = 327680u
const val WGPUSType_SurfaceDescriptorFromWindowsUWPSwapChainPanel : WGPUSType = 327683u
const val WGPUSType_DawnTextureInternalUsageDescriptor : WGPUSType = 327684u
const val WGPUSType_DawnEncoderInternalUsageDescriptor : WGPUSType = 327685u
const val WGPUSType_DawnInstanceDescriptor : WGPUSType = 327686u
const val WGPUSType_DawnCacheDeviceDescriptor : WGPUSType = 327687u
const val WGPUSType_DawnAdapterPropertiesPowerPreference : WGPUSType = 327688u
const val WGPUSType_DawnBufferDescriptorErrorInfoFromWireClient : WGPUSType = 327689u
const val WGPUSType_DawnTogglesDescriptor : WGPUSType = 327690u
const val WGPUSType_DawnShaderModuleSPIRVOptionsDescriptor : WGPUSType = 327691u
const val WGPUSType_RequestAdapterOptionsLUID : WGPUSType = 327692u
const val WGPUSType_RequestAdapterOptionsGetGLProc : WGPUSType = 327693u
const val WGPUSType_RequestAdapterOptionsD3D11Device : WGPUSType = 327694u
const val WGPUSType_DawnRenderPassSampleCount : WGPUSType = 327695u
const val WGPUSType_RenderPassPixelLocalStorage : WGPUSType = 327696u
const val WGPUSType_PipelineLayoutPixelLocalStorage : WGPUSType = 327697u
const val WGPUSType_BufferHostMappedPointer : WGPUSType = 327698u
const val WGPUSType_AdapterPropertiesMemoryHeaps : WGPUSType = 327699u
const val WGPUSType_AdapterPropertiesD3D : WGPUSType = 327700u
const val WGPUSType_AdapterPropertiesVk : WGPUSType = 327701u
const val WGPUSType_DawnWireWGSLControl : WGPUSType = 327702u
const val WGPUSType_DawnWGSLBlocklist : WGPUSType = 327703u
const val WGPUSType_DawnDrmFormatCapabilities : WGPUSType = 327704u
const val WGPUSType_ShaderModuleCompilationOptions : WGPUSType = 327705u
const val WGPUSType_ColorTargetStateExpandResolveTextureDawn : WGPUSType = 327706u
const val WGPUSType_RenderPassRenderAreaRect : WGPUSType = 327707u
const val WGPUSType_SharedTextureMemoryVkDedicatedAllocationDescriptor : WGPUSType = 327708u
const val WGPUSType_SharedTextureMemoryAHardwareBufferDescriptor : WGPUSType = 327709u
const val WGPUSType_SharedTextureMemoryDmaBufDescriptor : WGPUSType = 327710u
const val WGPUSType_SharedTextureMemoryOpaqueFDDescriptor : WGPUSType = 327711u
const val WGPUSType_SharedTextureMemoryZirconHandleDescriptor : WGPUSType = 327712u
const val WGPUSType_SharedTextureMemoryDXGISharedHandleDescriptor : WGPUSType = 327713u
const val WGPUSType_SharedTextureMemoryD3D11Texture2DDescriptor : WGPUSType = 327714u
const val WGPUSType_SharedTextureMemoryIOSurfaceDescriptor : WGPUSType = 327715u
const val WGPUSType_SharedTextureMemoryEGLImageDescriptor : WGPUSType = 327716u
const val WGPUSType_SharedTextureMemoryInitializedBeginState : WGPUSType = 327717u
const val WGPUSType_SharedTextureMemoryInitializedEndState : WGPUSType = 327718u
const val WGPUSType_SharedTextureMemoryVkImageLayoutBeginState : WGPUSType = 327719u
const val WGPUSType_SharedTextureMemoryVkImageLayoutEndState : WGPUSType = 327720u
const val WGPUSType_SharedTextureMemoryD3DSwapchainBeginState : WGPUSType = 327721u
const val WGPUSType_SharedFenceVkSemaphoreOpaqueFDDescriptor : WGPUSType = 327722u
const val WGPUSType_SharedFenceVkSemaphoreOpaqueFDExportInfo : WGPUSType = 327723u
const val WGPUSType_SharedFenceSyncFDDescriptor : WGPUSType = 327724u
const val WGPUSType_SharedFenceSyncFDExportInfo : WGPUSType = 327725u
const val WGPUSType_SharedFenceVkSemaphoreZirconHandleDescriptor : WGPUSType = 327726u
const val WGPUSType_SharedFenceVkSemaphoreZirconHandleExportInfo : WGPUSType = 327727u
const val WGPUSType_SharedFenceDXGISharedHandleDescriptor : WGPUSType = 327728u
const val WGPUSType_SharedFenceDXGISharedHandleExportInfo : WGPUSType = 327729u
const val WGPUSType_SharedFenceMTLSharedEventDescriptor : WGPUSType = 327730u
const val WGPUSType_SharedFenceMTLSharedEventExportInfo : WGPUSType = 327731u
const val WGPUSType_SharedBufferMemoryD3D12ResourceDescriptor : WGPUSType = 327732u
const val WGPUSType_StaticSamplerBindingLayout : WGPUSType = 327733u
const val WGPUSType_YCbCrVkDescriptor : WGPUSType = 327734u
const val WGPUSType_SharedTextureMemoryAHardwareBufferProperties : WGPUSType = 327735u
const val WGPUSType_AHardwareBufferProperties : WGPUSType = 327736u
const val WGPUSType_DawnTexelCopyBufferRowAlignmentLimits : WGPUSType = 327738u
const val WGPUSType_AdapterPropertiesSubgroupMatrixConfigs : WGPUSType = 327739u
const val WGPUSType_SharedFenceEGLSyncDescriptor : WGPUSType = 327740u
const val WGPUSType_SharedFenceEGLSyncExportInfo : WGPUSType = 327741u
const val WGPUSType_DawnInjectedInvalidSType : WGPUSType = 327742u
const val WGPUSType_DawnCompilationMessageUtf16 : WGPUSType = 327743u
const val WGPUSType_DawnFakeBufferOOMForTesting : WGPUSType = 327744u
const val WGPUSType_SurfaceDescriptorFromWindowsWinUISwapChainPanel : WGPUSType = 327745u
const val WGPUSType_DawnDeviceAllocatorControl : WGPUSType = 327746u
const val WGPUSType_DawnHostMappedPointerLimits : WGPUSType = 327747u
const val WGPUSType_RenderPassDescriptorResolveRect : WGPUSType = 327748u
const val WGPUSType_RequestAdapterWebGPUBackendOptions : WGPUSType = 327749u
const val WGPUSType_DawnFakeDeviceInitializeErrorForTesting : WGPUSType = 327750u
const val WGPUSType_SharedTextureMemoryD3D11BeginState : WGPUSType = 327751u
const val WGPUSType_DawnConsumeAdapterDescriptor : WGPUSType = 327752u
const val WGPUSType_TexelBufferBindingEntry : WGPUSType = 327753u
const val WGPUSType_TexelBufferBindingLayout : WGPUSType = 327754u
const val WGPUSType_SharedTextureMemoryMetalEndAccessState : WGPUSType = 327755u
const val WGPUSType_AdapterPropertiesWGPU : WGPUSType = 327756u
const val WGPUSType_SharedBufferMemoryFromWindowsHandleDescriptor : WGPUSType = 327757u
const val WGPUSType_SharedTextureMemoryD3D12ResourceDescriptor : WGPUSType = 327758u
const val WGPUSType_RequestAdapterOptionsAngleVirtualizationGroup : WGPUSType = 327759u
const val WGPUSType_PipelineLayoutResourceTable : WGPUSType = 327760u
const val WGPUSType_AdapterPropertiesDrm : WGPUSType = 327761u
const val WGPUSType_RenderBundleEncoderResourceTable : WGPUSType = 327762u
const val WGPUSType_DawnShaderSourceSPIRV : WGPUSType = 327763u
const val WGPUSType_SharedBufferMemoryHostPointerDescriptor : WGPUSType = 327764u
const val WGPUSType_Force32 : WGPUSType = 2147483647u

typealias WGPUSubgroupMatrixComponentType = UInt
const val WGPUSubgroupMatrixComponentType_F32 : WGPUSubgroupMatrixComponentType = 1u
const val WGPUSubgroupMatrixComponentType_F16 : WGPUSubgroupMatrixComponentType = 2u
const val WGPUSubgroupMatrixComponentType_U32 : WGPUSubgroupMatrixComponentType = 3u
const val WGPUSubgroupMatrixComponentType_I32 : WGPUSubgroupMatrixComponentType = 4u
const val WGPUSubgroupMatrixComponentType_U8 : WGPUSubgroupMatrixComponentType = 5u
const val WGPUSubgroupMatrixComponentType_I8 : WGPUSubgroupMatrixComponentType = 6u
const val WGPUSubgroupMatrixComponentType_Force32 : WGPUSubgroupMatrixComponentType = 2147483647u

typealias WGPUSurfaceGetCurrentTextureStatus = UInt
const val WGPUSurfaceGetCurrentTextureStatus_SuccessOptimal : WGPUSurfaceGetCurrentTextureStatus = 1u
const val WGPUSurfaceGetCurrentTextureStatus_SuccessSuboptimal : WGPUSurfaceGetCurrentTextureStatus = 2u
const val WGPUSurfaceGetCurrentTextureStatus_Timeout : WGPUSurfaceGetCurrentTextureStatus = 3u
const val WGPUSurfaceGetCurrentTextureStatus_Outdated : WGPUSurfaceGetCurrentTextureStatus = 4u
const val WGPUSurfaceGetCurrentTextureStatus_Lost : WGPUSurfaceGetCurrentTextureStatus = 5u
const val WGPUSurfaceGetCurrentTextureStatus_Error : WGPUSurfaceGetCurrentTextureStatus = 6u
const val WGPUSurfaceGetCurrentTextureStatus_Force32 : WGPUSurfaceGetCurrentTextureStatus = 2147483647u

typealias WGPUTexelBufferAccess = UInt
const val WGPUTexelBufferAccess_Undefined : WGPUTexelBufferAccess = 0u
const val WGPUTexelBufferAccess_ReadOnly : WGPUTexelBufferAccess = 1u
const val WGPUTexelBufferAccess_ReadWrite : WGPUTexelBufferAccess = 2u
const val WGPUTexelBufferAccess_Force32 : WGPUTexelBufferAccess = 2147483647u

typealias WGPUTextureAspect = UInt
const val WGPUTextureAspect_Undefined : WGPUTextureAspect = 0u
const val WGPUTextureAspect_All : WGPUTextureAspect = 1u
const val WGPUTextureAspect_StencilOnly : WGPUTextureAspect = 2u
const val WGPUTextureAspect_DepthOnly : WGPUTextureAspect = 3u
const val WGPUTextureAspect_Plane0Only : WGPUTextureAspect = 327680u
const val WGPUTextureAspect_Plane1Only : WGPUTextureAspect = 327681u
const val WGPUTextureAspect_Plane2Only : WGPUTextureAspect = 327682u
const val WGPUTextureAspect_Force32 : WGPUTextureAspect = 2147483647u

typealias WGPUTextureDimension = UInt
const val WGPUTextureDimension_Undefined : WGPUTextureDimension = 0u
const val WGPUTextureDimension_1D : WGPUTextureDimension = 1u
const val WGPUTextureDimension_2D : WGPUTextureDimension = 2u
const val WGPUTextureDimension_3D : WGPUTextureDimension = 3u
const val WGPUTextureDimension_Force32 : WGPUTextureDimension = 2147483647u

typealias WGPUTextureFormat = UInt
const val WGPUTextureFormat_Undefined : WGPUTextureFormat = 0u
const val WGPUTextureFormat_R8Unorm : WGPUTextureFormat = 1u
const val WGPUTextureFormat_R8Snorm : WGPUTextureFormat = 2u
const val WGPUTextureFormat_R8Uint : WGPUTextureFormat = 3u
const val WGPUTextureFormat_R8Sint : WGPUTextureFormat = 4u
const val WGPUTextureFormat_R16Unorm : WGPUTextureFormat = 5u
const val WGPUTextureFormat_R16Snorm : WGPUTextureFormat = 6u
const val WGPUTextureFormat_R16Uint : WGPUTextureFormat = 7u
const val WGPUTextureFormat_R16Sint : WGPUTextureFormat = 8u
const val WGPUTextureFormat_R16Float : WGPUTextureFormat = 9u
const val WGPUTextureFormat_RG8Unorm : WGPUTextureFormat = 10u
const val WGPUTextureFormat_RG8Snorm : WGPUTextureFormat = 11u
const val WGPUTextureFormat_RG8Uint : WGPUTextureFormat = 12u
const val WGPUTextureFormat_RG8Sint : WGPUTextureFormat = 13u
const val WGPUTextureFormat_R32Float : WGPUTextureFormat = 14u
const val WGPUTextureFormat_R32Uint : WGPUTextureFormat = 15u
const val WGPUTextureFormat_R32Sint : WGPUTextureFormat = 16u
const val WGPUTextureFormat_RG16Unorm : WGPUTextureFormat = 17u
const val WGPUTextureFormat_RG16Snorm : WGPUTextureFormat = 18u
const val WGPUTextureFormat_RG16Uint : WGPUTextureFormat = 19u
const val WGPUTextureFormat_RG16Sint : WGPUTextureFormat = 20u
const val WGPUTextureFormat_RG16Float : WGPUTextureFormat = 21u
const val WGPUTextureFormat_RGBA8Unorm : WGPUTextureFormat = 22u
const val WGPUTextureFormat_RGBA8UnormSrgb : WGPUTextureFormat = 23u
const val WGPUTextureFormat_RGBA8Snorm : WGPUTextureFormat = 24u
const val WGPUTextureFormat_RGBA8Uint : WGPUTextureFormat = 25u
const val WGPUTextureFormat_RGBA8Sint : WGPUTextureFormat = 26u
const val WGPUTextureFormat_BGRA8Unorm : WGPUTextureFormat = 27u
const val WGPUTextureFormat_BGRA8UnormSrgb : WGPUTextureFormat = 28u
const val WGPUTextureFormat_RGB10A2Uint : WGPUTextureFormat = 29u
const val WGPUTextureFormat_RGB10A2Unorm : WGPUTextureFormat = 30u
const val WGPUTextureFormat_RG11B10Ufloat : WGPUTextureFormat = 31u
const val WGPUTextureFormat_RGB9E5Ufloat : WGPUTextureFormat = 32u
const val WGPUTextureFormat_RG32Float : WGPUTextureFormat = 33u
const val WGPUTextureFormat_RG32Uint : WGPUTextureFormat = 34u
const val WGPUTextureFormat_RG32Sint : WGPUTextureFormat = 35u
const val WGPUTextureFormat_RGBA16Unorm : WGPUTextureFormat = 36u
const val WGPUTextureFormat_RGBA16Snorm : WGPUTextureFormat = 37u
const val WGPUTextureFormat_RGBA16Uint : WGPUTextureFormat = 38u
const val WGPUTextureFormat_RGBA16Sint : WGPUTextureFormat = 39u
const val WGPUTextureFormat_RGBA16Float : WGPUTextureFormat = 40u
const val WGPUTextureFormat_RGBA32Float : WGPUTextureFormat = 41u
const val WGPUTextureFormat_RGBA32Uint : WGPUTextureFormat = 42u
const val WGPUTextureFormat_RGBA32Sint : WGPUTextureFormat = 43u
const val WGPUTextureFormat_Stencil8 : WGPUTextureFormat = 44u
const val WGPUTextureFormat_Depth16Unorm : WGPUTextureFormat = 45u
const val WGPUTextureFormat_Depth24Plus : WGPUTextureFormat = 46u
const val WGPUTextureFormat_Depth24PlusStencil8 : WGPUTextureFormat = 47u
const val WGPUTextureFormat_Depth32Float : WGPUTextureFormat = 48u
const val WGPUTextureFormat_Depth32FloatStencil8 : WGPUTextureFormat = 49u
const val WGPUTextureFormat_BC1RGBAUnorm : WGPUTextureFormat = 50u
const val WGPUTextureFormat_BC1RGBAUnormSrgb : WGPUTextureFormat = 51u
const val WGPUTextureFormat_BC2RGBAUnorm : WGPUTextureFormat = 52u
const val WGPUTextureFormat_BC2RGBAUnormSrgb : WGPUTextureFormat = 53u
const val WGPUTextureFormat_BC3RGBAUnorm : WGPUTextureFormat = 54u
const val WGPUTextureFormat_BC3RGBAUnormSrgb : WGPUTextureFormat = 55u
const val WGPUTextureFormat_BC4RUnorm : WGPUTextureFormat = 56u
const val WGPUTextureFormat_BC4RSnorm : WGPUTextureFormat = 57u
const val WGPUTextureFormat_BC5RGUnorm : WGPUTextureFormat = 58u
const val WGPUTextureFormat_BC5RGSnorm : WGPUTextureFormat = 59u
const val WGPUTextureFormat_BC6HRGBUfloat : WGPUTextureFormat = 60u
const val WGPUTextureFormat_BC6HRGBFloat : WGPUTextureFormat = 61u
const val WGPUTextureFormat_BC7RGBAUnorm : WGPUTextureFormat = 62u
const val WGPUTextureFormat_BC7RGBAUnormSrgb : WGPUTextureFormat = 63u
const val WGPUTextureFormat_ETC2RGB8Unorm : WGPUTextureFormat = 64u
const val WGPUTextureFormat_ETC2RGB8UnormSrgb : WGPUTextureFormat = 65u
const val WGPUTextureFormat_ETC2RGB8A1Unorm : WGPUTextureFormat = 66u
const val WGPUTextureFormat_ETC2RGB8A1UnormSrgb : WGPUTextureFormat = 67u
const val WGPUTextureFormat_ETC2RGBA8Unorm : WGPUTextureFormat = 68u
const val WGPUTextureFormat_ETC2RGBA8UnormSrgb : WGPUTextureFormat = 69u
const val WGPUTextureFormat_EACR11Unorm : WGPUTextureFormat = 70u
const val WGPUTextureFormat_EACR11Snorm : WGPUTextureFormat = 71u
const val WGPUTextureFormat_EACRG11Unorm : WGPUTextureFormat = 72u
const val WGPUTextureFormat_EACRG11Snorm : WGPUTextureFormat = 73u
const val WGPUTextureFormat_ASTC4x4Unorm : WGPUTextureFormat = 74u
const val WGPUTextureFormat_ASTC4x4UnormSrgb : WGPUTextureFormat = 75u
const val WGPUTextureFormat_ASTC5x4Unorm : WGPUTextureFormat = 76u
const val WGPUTextureFormat_ASTC5x4UnormSrgb : WGPUTextureFormat = 77u
const val WGPUTextureFormat_ASTC5x5Unorm : WGPUTextureFormat = 78u
const val WGPUTextureFormat_ASTC5x5UnormSrgb : WGPUTextureFormat = 79u
const val WGPUTextureFormat_ASTC6x5Unorm : WGPUTextureFormat = 80u
const val WGPUTextureFormat_ASTC6x5UnormSrgb : WGPUTextureFormat = 81u
const val WGPUTextureFormat_ASTC6x6Unorm : WGPUTextureFormat = 82u
const val WGPUTextureFormat_ASTC6x6UnormSrgb : WGPUTextureFormat = 83u
const val WGPUTextureFormat_ASTC8x5Unorm : WGPUTextureFormat = 84u
const val WGPUTextureFormat_ASTC8x5UnormSrgb : WGPUTextureFormat = 85u
const val WGPUTextureFormat_ASTC8x6Unorm : WGPUTextureFormat = 86u
const val WGPUTextureFormat_ASTC8x6UnormSrgb : WGPUTextureFormat = 87u
const val WGPUTextureFormat_ASTC8x8Unorm : WGPUTextureFormat = 88u
const val WGPUTextureFormat_ASTC8x8UnormSrgb : WGPUTextureFormat = 89u
const val WGPUTextureFormat_ASTC10x5Unorm : WGPUTextureFormat = 90u
const val WGPUTextureFormat_ASTC10x5UnormSrgb : WGPUTextureFormat = 91u
const val WGPUTextureFormat_ASTC10x6Unorm : WGPUTextureFormat = 92u
const val WGPUTextureFormat_ASTC10x6UnormSrgb : WGPUTextureFormat = 93u
const val WGPUTextureFormat_ASTC10x8Unorm : WGPUTextureFormat = 94u
const val WGPUTextureFormat_ASTC10x8UnormSrgb : WGPUTextureFormat = 95u
const val WGPUTextureFormat_ASTC10x10Unorm : WGPUTextureFormat = 96u
const val WGPUTextureFormat_ASTC10x10UnormSrgb : WGPUTextureFormat = 97u
const val WGPUTextureFormat_ASTC12x10Unorm : WGPUTextureFormat = 98u
const val WGPUTextureFormat_ASTC12x10UnormSrgb : WGPUTextureFormat = 99u
const val WGPUTextureFormat_ASTC12x12Unorm : WGPUTextureFormat = 100u
const val WGPUTextureFormat_ASTC12x12UnormSrgb : WGPUTextureFormat = 101u
const val WGPUTextureFormat_R8BG8Biplanar420Unorm : WGPUTextureFormat = 327680u
const val WGPUTextureFormat_R10X6BG10X6Biplanar420Unorm : WGPUTextureFormat = 327681u
const val WGPUTextureFormat_R8BG8A8Triplanar420Unorm : WGPUTextureFormat = 327682u
const val WGPUTextureFormat_R8BG8Biplanar422Unorm : WGPUTextureFormat = 327683u
const val WGPUTextureFormat_R8BG8Biplanar444Unorm : WGPUTextureFormat = 327684u
const val WGPUTextureFormat_R10X6BG10X6Biplanar422Unorm : WGPUTextureFormat = 327685u
const val WGPUTextureFormat_R10X6BG10X6Biplanar444Unorm : WGPUTextureFormat = 327686u
const val WGPUTextureFormat_OpaqueYCbCrAndroid : WGPUTextureFormat = 327687u
const val WGPUTextureFormat_Force32 : WGPUTextureFormat = 2147483647u

typealias WGPUTextureSampleType = UInt
const val WGPUTextureSampleType_BindingNotUsed : WGPUTextureSampleType = 0u
const val WGPUTextureSampleType_Undefined : WGPUTextureSampleType = 1u
const val WGPUTextureSampleType_Float : WGPUTextureSampleType = 2u
const val WGPUTextureSampleType_UnfilterableFloat : WGPUTextureSampleType = 3u
const val WGPUTextureSampleType_Depth : WGPUTextureSampleType = 4u
const val WGPUTextureSampleType_Sint : WGPUTextureSampleType = 5u
const val WGPUTextureSampleType_Uint : WGPUTextureSampleType = 6u
const val WGPUTextureSampleType_Force32 : WGPUTextureSampleType = 2147483647u

typealias WGPUTextureViewDimension = UInt
const val WGPUTextureViewDimension_Undefined : WGPUTextureViewDimension = 0u
const val WGPUTextureViewDimension_1D : WGPUTextureViewDimension = 1u
const val WGPUTextureViewDimension_2D : WGPUTextureViewDimension = 2u
const val WGPUTextureViewDimension_2DArray : WGPUTextureViewDimension = 3u
const val WGPUTextureViewDimension_Cube : WGPUTextureViewDimension = 4u
const val WGPUTextureViewDimension_CubeArray : WGPUTextureViewDimension = 5u
const val WGPUTextureViewDimension_3D : WGPUTextureViewDimension = 6u
const val WGPUTextureViewDimension_Force32 : WGPUTextureViewDimension = 2147483647u

typealias WGPUToneMappingMode = UInt
const val WGPUToneMappingMode_Standard : WGPUToneMappingMode = 1u
const val WGPUToneMappingMode_Extended : WGPUToneMappingMode = 2u
const val WGPUToneMappingMode_Force32 : WGPUToneMappingMode = 2147483647u

typealias WGPUVertexFormat = UInt
const val WGPUVertexFormat_Uint8 : WGPUVertexFormat = 1u
const val WGPUVertexFormat_Uint8x2 : WGPUVertexFormat = 2u
const val WGPUVertexFormat_Uint8x4 : WGPUVertexFormat = 3u
const val WGPUVertexFormat_Sint8 : WGPUVertexFormat = 4u
const val WGPUVertexFormat_Sint8x2 : WGPUVertexFormat = 5u
const val WGPUVertexFormat_Sint8x4 : WGPUVertexFormat = 6u
const val WGPUVertexFormat_Unorm8 : WGPUVertexFormat = 7u
const val WGPUVertexFormat_Unorm8x2 : WGPUVertexFormat = 8u
const val WGPUVertexFormat_Unorm8x4 : WGPUVertexFormat = 9u
const val WGPUVertexFormat_Snorm8 : WGPUVertexFormat = 10u
const val WGPUVertexFormat_Snorm8x2 : WGPUVertexFormat = 11u
const val WGPUVertexFormat_Snorm8x4 : WGPUVertexFormat = 12u
const val WGPUVertexFormat_Uint16 : WGPUVertexFormat = 13u
const val WGPUVertexFormat_Uint16x2 : WGPUVertexFormat = 14u
const val WGPUVertexFormat_Uint16x4 : WGPUVertexFormat = 15u
const val WGPUVertexFormat_Sint16 : WGPUVertexFormat = 16u
const val WGPUVertexFormat_Sint16x2 : WGPUVertexFormat = 17u
const val WGPUVertexFormat_Sint16x4 : WGPUVertexFormat = 18u
const val WGPUVertexFormat_Unorm16 : WGPUVertexFormat = 19u
const val WGPUVertexFormat_Unorm16x2 : WGPUVertexFormat = 20u
const val WGPUVertexFormat_Unorm16x4 : WGPUVertexFormat = 21u
const val WGPUVertexFormat_Snorm16 : WGPUVertexFormat = 22u
const val WGPUVertexFormat_Snorm16x2 : WGPUVertexFormat = 23u
const val WGPUVertexFormat_Snorm16x4 : WGPUVertexFormat = 24u
const val WGPUVertexFormat_Float16 : WGPUVertexFormat = 25u
const val WGPUVertexFormat_Float16x2 : WGPUVertexFormat = 26u
const val WGPUVertexFormat_Float16x4 : WGPUVertexFormat = 27u
const val WGPUVertexFormat_Float32 : WGPUVertexFormat = 28u
const val WGPUVertexFormat_Float32x2 : WGPUVertexFormat = 29u
const val WGPUVertexFormat_Float32x3 : WGPUVertexFormat = 30u
const val WGPUVertexFormat_Float32x4 : WGPUVertexFormat = 31u
const val WGPUVertexFormat_Uint32 : WGPUVertexFormat = 32u
const val WGPUVertexFormat_Uint32x2 : WGPUVertexFormat = 33u
const val WGPUVertexFormat_Uint32x3 : WGPUVertexFormat = 34u
const val WGPUVertexFormat_Uint32x4 : WGPUVertexFormat = 35u
const val WGPUVertexFormat_Sint32 : WGPUVertexFormat = 36u
const val WGPUVertexFormat_Sint32x2 : WGPUVertexFormat = 37u
const val WGPUVertexFormat_Sint32x3 : WGPUVertexFormat = 38u
const val WGPUVertexFormat_Sint32x4 : WGPUVertexFormat = 39u
const val WGPUVertexFormat_Unorm10_10_10_2 : WGPUVertexFormat = 40u
const val WGPUVertexFormat_Unorm8x4BGRA : WGPUVertexFormat = 41u
const val WGPUVertexFormat_Snorm10_10_10_2 : WGPUVertexFormat = 42u
const val WGPUVertexFormat_Force32 : WGPUVertexFormat = 2147483647u

typealias WGPUVertexStepMode = UInt
const val WGPUVertexStepMode_Undefined : WGPUVertexStepMode = 0u
const val WGPUVertexStepMode_Vertex : WGPUVertexStepMode = 1u
const val WGPUVertexStepMode_Instance : WGPUVertexStepMode = 2u
const val WGPUVertexStepMode_Force32 : WGPUVertexStepMode = 2147483647u

typealias WGPUWaitStatus = UInt
const val WGPUWaitStatus_Success : WGPUWaitStatus = 1u
const val WGPUWaitStatus_TimedOut : WGPUWaitStatus = 2u
const val WGPUWaitStatus_Error : WGPUWaitStatus = 3u
const val WGPUWaitStatus_Force32 : WGPUWaitStatus = 2147483647u

typealias WGPUWGSLLanguageFeatureName = UInt
const val WGPUWGSLLanguageFeatureName_ReadonlyAndReadwriteStorageTextures : WGPUWGSLLanguageFeatureName = 1u
const val WGPUWGSLLanguageFeatureName_Packed4x8IntegerDotProduct : WGPUWGSLLanguageFeatureName = 2u
const val WGPUWGSLLanguageFeatureName_UnrestrictedPointerParameters : WGPUWGSLLanguageFeatureName = 3u
const val WGPUWGSLLanguageFeatureName_PointerCompositeAccess : WGPUWGSLLanguageFeatureName = 4u
const val WGPUWGSLLanguageFeatureName_UniformBufferStandardLayout : WGPUWGSLLanguageFeatureName = 5u
const val WGPUWGSLLanguageFeatureName_SubgroupId : WGPUWGSLLanguageFeatureName = 6u
const val WGPUWGSLLanguageFeatureName_TextureAndSamplerLet : WGPUWGSLLanguageFeatureName = 7u
const val WGPUWGSLLanguageFeatureName_SubgroupUniformity : WGPUWGSLLanguageFeatureName = 8u
const val WGPUWGSLLanguageFeatureName_TextureFormatsTier1 : WGPUWGSLLanguageFeatureName = 9u
const val WGPUWGSLLanguageFeatureName_LinearIndexing : WGPUWGSLLanguageFeatureName = 10u
const val WGPUWGSLLanguageFeatureName_ImmediateAddressSpace : WGPUWGSLLanguageFeatureName = 11u
const val WGPUWGSLLanguageFeatureName_BufferView : WGPUWGSLLanguageFeatureName = 12u
const val WGPUWGSLLanguageFeatureName_SwizzleAssignment : WGPUWGSLLanguageFeatureName = 13u
const val WGPUWGSLLanguageFeatureName_FragmentDepth : WGPUWGSLLanguageFeatureName = 14u
const val WGPUWGSLLanguageFeatureName_ChromiumTestingUnimplemented : WGPUWGSLLanguageFeatureName = 327680u
const val WGPUWGSLLanguageFeatureName_ChromiumTestingUnsafeExperimental : WGPUWGSLLanguageFeatureName = 327681u
const val WGPUWGSLLanguageFeatureName_ChromiumTestingExperimental : WGPUWGSLLanguageFeatureName = 327682u
const val WGPUWGSLLanguageFeatureName_ChromiumTestingShippedWithKillswitch : WGPUWGSLLanguageFeatureName = 327683u
const val WGPUWGSLLanguageFeatureName_ChromiumTestingShipped : WGPUWGSLLanguageFeatureName = 327684u
const val WGPUWGSLLanguageFeatureName_SizedBindingArray : WGPUWGSLLanguageFeatureName = 327685u
const val WGPUWGSLLanguageFeatureName_TexelBuffers : WGPUWGSLLanguageFeatureName = 327686u
const val WGPUWGSLLanguageFeatureName_ChromiumPrint : WGPUWGSLLanguageFeatureName = 327687u
const val WGPUWGSLLanguageFeatureName_Force32 : WGPUWGSLLanguageFeatureName = 2147483647u

expect interface WGPUChainedStruct {
    var next: WGPUChainedStruct?
    var sType: WGPUSType
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUChainedStruct
        fun allocate(allocator: MemoryAllocator): WGPUChainedStruct
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUChainedStruct) -> Unit): ArrayHolder<WGPUChainedStruct>
    }
}

expect interface WGPUBufferMapCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBufferMapCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUBufferMapCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferMapCallbackInfo) -> Unit): ArrayHolder<WGPUBufferMapCallbackInfo>
    }
}

expect interface WGPUCompilationInfoCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCompilationInfoCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUCompilationInfoCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfoCallbackInfo) -> Unit): ArrayHolder<WGPUCompilationInfoCallbackInfo>
    }
}

expect interface WGPUCreateComputePipelineAsyncCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCreateComputePipelineAsyncCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUCreateComputePipelineAsyncCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateComputePipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateComputePipelineAsyncCallbackInfo>
    }
}

expect interface WGPUCreateRenderPipelineAsyncCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCreateRenderPipelineAsyncCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUCreateRenderPipelineAsyncCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateRenderPipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateRenderPipelineAsyncCallbackInfo>
    }
}

expect interface WGPUDawnLoadCacheDataCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnLoadCacheDataCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUDawnLoadCacheDataCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnLoadCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnLoadCacheDataCallbackInfo>
    }
}

expect interface WGPUDawnStoreCacheDataCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnStoreCacheDataCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUDawnStoreCacheDataCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnStoreCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnStoreCacheDataCallbackInfo>
    }
}

expect interface WGPUDeviceLostCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDeviceLostCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUDeviceLostCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceLostCallbackInfo) -> Unit): ArrayHolder<WGPUDeviceLostCallbackInfo>
    }
}

expect interface WGPUDisposeCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDisposeCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUDisposeCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDisposeCallbackInfo) -> Unit): ArrayHolder<WGPUDisposeCallbackInfo>
    }
}

expect interface WGPULoggingCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPULoggingCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPULoggingCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULoggingCallbackInfo) -> Unit): ArrayHolder<WGPULoggingCallbackInfo>
    }
}

expect interface WGPUPopErrorScopeCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPopErrorScopeCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUPopErrorScopeCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPopErrorScopeCallbackInfo) -> Unit): ArrayHolder<WGPUPopErrorScopeCallbackInfo>
    }
}

expect interface WGPUQueueWorkDoneCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUQueueWorkDoneCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUQueueWorkDoneCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueWorkDoneCallbackInfo) -> Unit): ArrayHolder<WGPUQueueWorkDoneCallbackInfo>
    }
}

expect interface WGPURequestAdapterCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURequestAdapterCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPURequestAdapterCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterCallbackInfo) -> Unit): ArrayHolder<WGPURequestAdapterCallbackInfo>
    }
}

expect interface WGPURequestDeviceCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var mode: WGPUCallbackMode
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURequestDeviceCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPURequestDeviceCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestDeviceCallbackInfo) -> Unit): ArrayHolder<WGPURequestDeviceCallbackInfo>
    }
}

expect interface WGPUUncapturedErrorCallbackInfo {
    var nextInChain: WGPUChainedStruct?
    var callback: NativeAddress?
    var userdata1: NativeAddress?
    var userdata2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUUncapturedErrorCallbackInfo
        fun allocate(allocator: MemoryAllocator): WGPUUncapturedErrorCallbackInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUUncapturedErrorCallbackInfo) -> Unit): ArrayHolder<WGPUUncapturedErrorCallbackInfo>
    }
}

expect interface WGPUAdapterPropertiesD3D {
    var chain: WGPUChainedStruct
    var shaderModel: UInt
    var adapterLUIDLowPart: UInt
    var adapterLUIDHighPart: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesD3D
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesD3D
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesD3D) -> Unit): ArrayHolder<WGPUAdapterPropertiesD3D>
    }
}

expect interface WGPUAdapterPropertiesDrm {
    var chain: WGPUChainedStruct
    var hasPrimary: UInt
    var hasRender: UInt
    var primaryMajor: ULong
    var primaryMinor: ULong
    var renderMajor: ULong
    var renderMinor: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesDrm
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesDrm
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesDrm) -> Unit): ArrayHolder<WGPUAdapterPropertiesDrm>
    }
}

expect interface WGPUAdapterPropertiesVk {
    var chain: WGPUChainedStruct
    var driverVersion: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesVk
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesVk
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesVk) -> Unit): ArrayHolder<WGPUAdapterPropertiesVk>
    }
}

expect interface WGPUAdapterPropertiesWGPU {
    var chain: WGPUChainedStruct
    var backendType: WGPUBackendType
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesWGPU
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesWGPU
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesWGPU) -> Unit): ArrayHolder<WGPUAdapterPropertiesWGPU>
    }
}

expect interface WGPUBindingResource {
    var nextInChain: WGPUChainedStruct?
    var buffer: WGPUBuffer?
    var offset: ULong
    var size: ULong
    var sampler: WGPUSampler?
    var textureView: WGPUTextureView?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBindingResource
        fun allocate(allocator: MemoryAllocator): WGPUBindingResource
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindingResource) -> Unit): ArrayHolder<WGPUBindingResource>
    }
}

expect interface WGPUBlendComponent {
    var operation: WGPUBlendOperation
    var srcFactor: WGPUBlendFactor
    var dstFactor: WGPUBlendFactor
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBlendComponent
        fun allocate(allocator: MemoryAllocator): WGPUBlendComponent
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendComponent) -> Unit): ArrayHolder<WGPUBlendComponent>
    }
}

expect interface WGPUBufferBindingLayout {
    var nextInChain: WGPUChainedStruct?
    var type: WGPUBufferBindingType
    var hasDynamicOffset: UInt
    var minBindingSize: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBufferBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUBufferBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferBindingLayout) -> Unit): ArrayHolder<WGPUBufferBindingLayout>
    }
}

expect interface WGPUBufferHostMappedPointer {
    var chain: WGPUChainedStruct
    var pointer: NativeAddress?
    var disposeCallback: NativeAddress?
    var userdata: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBufferHostMappedPointer
        fun allocate(allocator: MemoryAllocator): WGPUBufferHostMappedPointer
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferHostMappedPointer) -> Unit): ArrayHolder<WGPUBufferHostMappedPointer>
    }
}

expect interface WGPUColor {
    var r: Double
    var g: Double
    var b: Double
    var a: Double
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUColor
        fun allocate(allocator: MemoryAllocator): WGPUColor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColor) -> Unit): ArrayHolder<WGPUColor>
    }
}

expect interface WGPUColorSpaceDawn {
    var nextInChain: WGPUChainedStruct?
    var primaries: WGPUColorSpacePrimariesDawn
    var transfer: WGPUColorSpaceTransferDawn
    var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
    var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
    var hdrReferenceWhiteLuminance: Float
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUColorSpaceDawn
        fun allocate(allocator: MemoryAllocator): WGPUColorSpaceDawn
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorSpaceDawn) -> Unit): ArrayHolder<WGPUColorSpaceDawn>
    }
}

expect interface WGPUColorTargetStateExpandResolveTextureDawn {
    var chain: WGPUChainedStruct
    var enabled: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUColorTargetStateExpandResolveTextureDawn
        fun allocate(allocator: MemoryAllocator): WGPUColorTargetStateExpandResolveTextureDawn
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetStateExpandResolveTextureDawn) -> Unit): ArrayHolder<WGPUColorTargetStateExpandResolveTextureDawn>
    }
}

expect interface WGPUCommandBufferDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCommandBufferDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUCommandBufferDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandBufferDescriptor) -> Unit): ArrayHolder<WGPUCommandBufferDescriptor>
    }
}

expect interface WGPUCompatibilityModeLimits {
    var chain: WGPUChainedStruct
    var maxStorageBuffersInVertexStage: UInt
    var maxStorageTexturesInVertexStage: UInt
    var maxStorageBuffersInFragmentStage: UInt
    var maxStorageTexturesInFragmentStage: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCompatibilityModeLimits
        fun allocate(allocator: MemoryAllocator): WGPUCompatibilityModeLimits
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompatibilityModeLimits) -> Unit): ArrayHolder<WGPUCompatibilityModeLimits>
    }
}

expect interface WGPUConstantEntry {
    var nextInChain: WGPUChainedStruct?
    var key: WGPUStringView
    var value: Double
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUConstantEntry
        fun allocate(allocator: MemoryAllocator): WGPUConstantEntry
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUConstantEntry) -> Unit): ArrayHolder<WGPUConstantEntry>
    }
}

expect interface WGPUCopyTextureForBrowserOptions {
    var nextInChain: WGPUChainedStruct?
    var flipY: UInt
    var needsColorSpaceConversion: UInt
    var srcAlphaMode: WGPUAlphaMode
    var srcTransferFunctionParameters: NativeAddress?
    var conversionMatrix: NativeAddress?
    var dstTransferFunctionParameters: NativeAddress?
    var dstAlphaMode: WGPUAlphaMode
    var internalUsage: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCopyTextureForBrowserOptions
        fun allocate(allocator: MemoryAllocator): WGPUCopyTextureForBrowserOptions
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCopyTextureForBrowserOptions) -> Unit): ArrayHolder<WGPUCopyTextureForBrowserOptions>
    }
}

expect interface WGPUDawnAdapterPropertiesPowerPreference {
    var chain: WGPUChainedStruct
    var powerPreference: WGPUPowerPreference
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnAdapterPropertiesPowerPreference
        fun allocate(allocator: MemoryAllocator): WGPUDawnAdapterPropertiesPowerPreference
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnAdapterPropertiesPowerPreference) -> Unit): ArrayHolder<WGPUDawnAdapterPropertiesPowerPreference>
    }
}

expect interface WGPUDawnBufferDescriptorErrorInfoFromWireClient {
    var chain: WGPUChainedStruct
    var outOfMemory: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnBufferDescriptorErrorInfoFromWireClient
        fun allocate(allocator: MemoryAllocator): WGPUDawnBufferDescriptorErrorInfoFromWireClient
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnBufferDescriptorErrorInfoFromWireClient) -> Unit): ArrayHolder<WGPUDawnBufferDescriptorErrorInfoFromWireClient>
    }
}

expect interface WGPUDawnCacheDeviceDescriptor {
    var chain: WGPUChainedStruct
    var isolationKey: WGPUStringView
    var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
    var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnCacheDeviceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnCacheDeviceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCacheDeviceDescriptor) -> Unit): ArrayHolder<WGPUDawnCacheDeviceDescriptor>
    }
}

expect interface WGPUDawnCompilationMessageUtf16 {
    var chain: WGPUChainedStruct
    var linePos: ULong
    var offset: ULong
    var length: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnCompilationMessageUtf16
        fun allocate(allocator: MemoryAllocator): WGPUDawnCompilationMessageUtf16
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCompilationMessageUtf16) -> Unit): ArrayHolder<WGPUDawnCompilationMessageUtf16>
    }
}

expect interface WGPUDawnConsumeAdapterDescriptor {
    var chain: WGPUChainedStruct
    var consumeAdapter: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnConsumeAdapterDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnConsumeAdapterDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnConsumeAdapterDescriptor) -> Unit): ArrayHolder<WGPUDawnConsumeAdapterDescriptor>
    }
}

expect interface WGPUDawnDeviceAllocatorControl {
    var chain: WGPUChainedStruct
    var allocatorHeapBlockSize: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnDeviceAllocatorControl
        fun allocate(allocator: MemoryAllocator): WGPUDawnDeviceAllocatorControl
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDeviceAllocatorControl) -> Unit): ArrayHolder<WGPUDawnDeviceAllocatorControl>
    }
}

expect interface WGPUDawnDrmFormatProperties {
    var modifier: ULong
    var modifierPlaneCount: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatProperties
        fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatProperties
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatProperties) -> Unit): ArrayHolder<WGPUDawnDrmFormatProperties>
    }
}

expect interface WGPUDawnEncoderInternalUsageDescriptor {
    var chain: WGPUChainedStruct
    var useInternalUsages: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnEncoderInternalUsageDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnEncoderInternalUsageDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnEncoderInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnEncoderInternalUsageDescriptor>
    }
}

expect interface WGPUDawnFakeBufferOOMForTesting {
    var chain: WGPUChainedStruct
    var fakeOOMAtWireClientMap: UInt
    var fakeOOMAtNativeMap: UInt
    var fakeOOMAtDevice: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnFakeBufferOOMForTesting
        fun allocate(allocator: MemoryAllocator): WGPUDawnFakeBufferOOMForTesting
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeBufferOOMForTesting) -> Unit): ArrayHolder<WGPUDawnFakeBufferOOMForTesting>
    }
}

expect interface WGPUDawnFakeDeviceInitializeErrorForTesting {
    var chain: WGPUChainedStruct
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnFakeDeviceInitializeErrorForTesting
        fun allocate(allocator: MemoryAllocator): WGPUDawnFakeDeviceInitializeErrorForTesting
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeDeviceInitializeErrorForTesting) -> Unit): ArrayHolder<WGPUDawnFakeDeviceInitializeErrorForTesting>
    }
}

expect interface WGPUDawnHostMappedPointerLimits {
    var chain: WGPUChainedStruct
    var hostMappedPointerAlignment: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnHostMappedPointerLimits
        fun allocate(allocator: MemoryAllocator): WGPUDawnHostMappedPointerLimits
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnHostMappedPointerLimits) -> Unit): ArrayHolder<WGPUDawnHostMappedPointerLimits>
    }
}

expect interface WGPUDawnInjectedInvalidSType {
    var chain: WGPUChainedStruct
    var invalidSType: WGPUSType
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnInjectedInvalidSType
        fun allocate(allocator: MemoryAllocator): WGPUDawnInjectedInvalidSType
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnInjectedInvalidSType) -> Unit): ArrayHolder<WGPUDawnInjectedInvalidSType>
    }
}

expect interface WGPUDawnRenderPassSampleCount {
    var chain: WGPUChainedStruct
    var sampleCount: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnRenderPassSampleCount
        fun allocate(allocator: MemoryAllocator): WGPUDawnRenderPassSampleCount
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnRenderPassSampleCount) -> Unit): ArrayHolder<WGPUDawnRenderPassSampleCount>
    }
}

expect interface WGPUDawnShaderModuleSPIRVOptionsDescriptor {
    var chain: WGPUChainedStruct
    var allowNonUniformDerivatives: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnShaderModuleSPIRVOptionsDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnShaderModuleSPIRVOptionsDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderModuleSPIRVOptionsDescriptor) -> Unit): ArrayHolder<WGPUDawnShaderModuleSPIRVOptionsDescriptor>
    }
}

expect interface WGPUDawnShaderSourceSPIRV {
    var chain: WGPUChainedStruct
    var codeSize: ULong
    var code: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnShaderSourceSPIRV
        fun allocate(allocator: MemoryAllocator): WGPUDawnShaderSourceSPIRV
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUDawnShaderSourceSPIRV>
    }
}

expect interface WGPUDawnTexelCopyBufferRowAlignmentLimits {
    var chain: WGPUChainedStruct
    var minTexelCopyBufferRowAlignment: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnTexelCopyBufferRowAlignmentLimits
        fun allocate(allocator: MemoryAllocator): WGPUDawnTexelCopyBufferRowAlignmentLimits
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTexelCopyBufferRowAlignmentLimits) -> Unit): ArrayHolder<WGPUDawnTexelCopyBufferRowAlignmentLimits>
    }
}

expect interface WGPUDawnTextureInternalUsageDescriptor {
    var chain: WGPUChainedStruct
    var internalUsage: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnTextureInternalUsageDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnTextureInternalUsageDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTextureInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnTextureInternalUsageDescriptor>
    }
}

expect interface WGPUDawnTogglesDescriptor {
    var chain: WGPUChainedStruct
    var enabledToggleCount: ULong
    var enabledToggles: NativeAddress?
    var disabledToggleCount: ULong
    var disabledToggles: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnTogglesDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDawnTogglesDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTogglesDescriptor) -> Unit): ArrayHolder<WGPUDawnTogglesDescriptor>
    }
}

expect interface WGPUDawnWGSLBlocklist {
    var chain: WGPUChainedStruct
    var blocklistedFeatureCount: ULong
    var blocklistedFeatures: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnWGSLBlocklist
        fun allocate(allocator: MemoryAllocator): WGPUDawnWGSLBlocklist
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWGSLBlocklist) -> Unit): ArrayHolder<WGPUDawnWGSLBlocklist>
    }
}

expect interface WGPUDawnWireWGSLControl {
    var chain: WGPUChainedStruct
    var enableExperimental: UInt
    var enableUnsafe: UInt
    var enableTesting: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnWireWGSLControl
        fun allocate(allocator: MemoryAllocator): WGPUDawnWireWGSLControl
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWireWGSLControl) -> Unit): ArrayHolder<WGPUDawnWireWGSLControl>
    }
}

expect interface WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
    var chain: WGPUChainedStruct
    var selector: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector
        fun allocate(allocator: MemoryAllocator): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUEmscriptenSurfaceSourceCanvasHTMLSelector) -> Unit): ArrayHolder<WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>
    }
}

expect interface WGPUExtent2D {
    var width: UInt
    var height: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUExtent2D
        fun allocate(allocator: MemoryAllocator): WGPUExtent2D
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent2D) -> Unit): ArrayHolder<WGPUExtent2D>
    }
}

expect interface WGPUExtent3D {
    var width: UInt
    var height: UInt
    var depthOrArrayLayers: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUExtent3D
        fun allocate(allocator: MemoryAllocator): WGPUExtent3D
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent3D) -> Unit): ArrayHolder<WGPUExtent3D>
    }
}

expect interface WGPUExternalTextureBindingEntry {
    var chain: WGPUChainedStruct
    var externalTexture: WGPUExternalTexture?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingEntry
        fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingEntry
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingEntry) -> Unit): ArrayHolder<WGPUExternalTextureBindingEntry>
    }
}

expect interface WGPUExternalTextureBindingLayout {
    var chain: WGPUChainedStruct
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingLayout) -> Unit): ArrayHolder<WGPUExternalTextureBindingLayout>
    }
}

expect interface WGPUFuture {
    var id: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUFuture
        fun allocate(allocator: MemoryAllocator): WGPUFuture
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFuture) -> Unit): ArrayHolder<WGPUFuture>
    }
}

expect interface WGPUInstanceLimits {
    var nextInChain: WGPUChainedStruct?
    var timedWaitAnyMaxCount: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUInstanceLimits
        fun allocate(allocator: MemoryAllocator): WGPUInstanceLimits
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceLimits) -> Unit): ArrayHolder<WGPUInstanceLimits>
    }
}

expect interface WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
    var unused: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER
        fun allocate(allocator: MemoryAllocator): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER) -> Unit): ArrayHolder<WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>
    }
}

expect interface WGPUMemoryHeapInfo {
    var properties: ULong
    var size: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUMemoryHeapInfo
        fun allocate(allocator: MemoryAllocator): WGPUMemoryHeapInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMemoryHeapInfo) -> Unit): ArrayHolder<WGPUMemoryHeapInfo>
    }
}

expect interface WGPUMultisampleState {
    var nextInChain: WGPUChainedStruct?
    var count: UInt
    var mask: UInt
    var alphaToCoverageEnabled: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUMultisampleState
        fun allocate(allocator: MemoryAllocator): WGPUMultisampleState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMultisampleState) -> Unit): ArrayHolder<WGPUMultisampleState>
    }
}

expect interface WGPUOrigin2D {
    var x: UInt
    var y: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUOrigin2D
        fun allocate(allocator: MemoryAllocator): WGPUOrigin2D
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin2D) -> Unit): ArrayHolder<WGPUOrigin2D>
    }
}

expect interface WGPUOrigin3D {
    var x: UInt
    var y: UInt
    var z: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUOrigin3D
        fun allocate(allocator: MemoryAllocator): WGPUOrigin3D
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin3D) -> Unit): ArrayHolder<WGPUOrigin3D>
    }
}

expect interface WGPUPassTimestampWrites {
    var nextInChain: WGPUChainedStruct?
    var querySet: WGPUQuerySet?
    var beginningOfPassWriteIndex: UInt
    var endOfPassWriteIndex: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPassTimestampWrites
        fun allocate(allocator: MemoryAllocator): WGPUPassTimestampWrites
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPassTimestampWrites) -> Unit): ArrayHolder<WGPUPassTimestampWrites>
    }
}

expect interface WGPUPipelineLayoutResourceTable {
    var chain: WGPUChainedStruct
    var usesResourceTable: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPipelineLayoutResourceTable
        fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutResourceTable
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutResourceTable) -> Unit): ArrayHolder<WGPUPipelineLayoutResourceTable>
    }
}

expect interface WGPUPipelineLayoutStorageAttachment {
    var nextInChain: WGPUChainedStruct?
    var offset: ULong
    var format: WGPUTextureFormat
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPipelineLayoutStorageAttachment
        fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutStorageAttachment
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutStorageAttachment) -> Unit): ArrayHolder<WGPUPipelineLayoutStorageAttachment>
    }
}

expect interface WGPUPrimitiveState {
    var nextInChain: WGPUChainedStruct?
    var topology: WGPUPrimitiveTopology
    var stripIndexFormat: WGPUIndexFormat
    var frontFace: WGPUFrontFace
    var cullMode: WGPUCullMode
    var unclippedDepth: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPrimitiveState
        fun allocate(allocator: MemoryAllocator): WGPUPrimitiveState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPrimitiveState) -> Unit): ArrayHolder<WGPUPrimitiveState>
    }
}

expect interface WGPUQuerySetDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var type: WGPUQueryType
    var count: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUQuerySetDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUQuerySetDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQuerySetDescriptor) -> Unit): ArrayHolder<WGPUQuerySetDescriptor>
    }
}

expect interface WGPUQueueDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUQueueDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUQueueDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueDescriptor) -> Unit): ArrayHolder<WGPUQueueDescriptor>
    }
}

expect interface WGPURenderBundleDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderBundleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPURenderBundleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleDescriptor) -> Unit): ArrayHolder<WGPURenderBundleDescriptor>
    }
}

expect interface WGPURenderBundleEncoderResourceTable {
    var chain: WGPUChainedStruct
    var usesResourceTable: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderBundleEncoderResourceTable
        fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderResourceTable
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderResourceTable) -> Unit): ArrayHolder<WGPURenderBundleEncoderResourceTable>
    }
}

expect interface WGPURenderPassDepthStencilAttachment {
    var nextInChain: WGPUChainedStruct?
    var view: WGPUTextureView?
    var depthLoadOp: WGPULoadOp
    var depthStoreOp: WGPUStoreOp
    var depthClearValue: Float
    var depthReadOnly: UInt
    var stencilLoadOp: WGPULoadOp
    var stencilStoreOp: WGPUStoreOp
    var stencilClearValue: UInt
    var stencilReadOnly: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassDepthStencilAttachment
        fun allocate(allocator: MemoryAllocator): WGPURenderPassDepthStencilAttachment
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDepthStencilAttachment) -> Unit): ArrayHolder<WGPURenderPassDepthStencilAttachment>
    }
}

expect interface WGPURenderPassDescriptorResolveRect {
    var chain: WGPUChainedStruct
    var colorOffsetX: UInt
    var colorOffsetY: UInt
    var resolveOffsetX: UInt
    var resolveOffsetY: UInt
    var width: UInt
    var height: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassDescriptorResolveRect
        fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptorResolveRect
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptorResolveRect) -> Unit): ArrayHolder<WGPURenderPassDescriptorResolveRect>
    }
}

expect interface WGPURenderPassMaxDrawCount {
    var chain: WGPUChainedStruct
    var maxDrawCount: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassMaxDrawCount
        fun allocate(allocator: MemoryAllocator): WGPURenderPassMaxDrawCount
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassMaxDrawCount) -> Unit): ArrayHolder<WGPURenderPassMaxDrawCount>
    }
}

expect interface WGPURequestAdapterWebGPUBackendOptions {
    var chain: WGPUChainedStruct
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURequestAdapterWebGPUBackendOptions
        fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebGPUBackendOptions
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebGPUBackendOptions) -> Unit): ArrayHolder<WGPURequestAdapterWebGPUBackendOptions>
    }
}

expect interface WGPURequestAdapterWebXROptions {
    var chain: WGPUChainedStruct
    var xrCompatible: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURequestAdapterWebXROptions
        fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebXROptions
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebXROptions) -> Unit): ArrayHolder<WGPURequestAdapterWebXROptions>
    }
}

expect interface WGPUResourceTableDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var size: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUResourceTableDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUResourceTableDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUResourceTableDescriptor) -> Unit): ArrayHolder<WGPUResourceTableDescriptor>
    }
}

expect interface WGPUSamplerBindingLayout {
    var nextInChain: WGPUChainedStruct?
    var type: WGPUSamplerBindingType
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSamplerBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUSamplerBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerBindingLayout) -> Unit): ArrayHolder<WGPUSamplerBindingLayout>
    }
}

expect interface WGPUShaderModuleCompilationOptions {
    var chain: WGPUChainedStruct
    var strictMath: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUShaderModuleCompilationOptions
        fun allocate(allocator: MemoryAllocator): WGPUShaderModuleCompilationOptions
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleCompilationOptions) -> Unit): ArrayHolder<WGPUShaderModuleCompilationOptions>
    }
}

expect interface WGPUShaderSourceSPIRV {
    var chain: WGPUChainedStruct
    var codeSize: UInt
    var code: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUShaderSourceSPIRV
        fun allocate(allocator: MemoryAllocator): WGPUShaderSourceSPIRV
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUShaderSourceSPIRV>
    }
}

expect interface WGPUShaderSourceWGSL {
    var chain: WGPUChainedStruct
    var code: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUShaderSourceWGSL
        fun allocate(allocator: MemoryAllocator): WGPUShaderSourceWGSL
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceWGSL) -> Unit): ArrayHolder<WGPUShaderSourceWGSL>
    }
}

expect interface WGPUSharedBufferMemoryBeginAccessDescriptor {
    var nextInChain: WGPUChainedStruct?
    var initialized: UInt
    var fenceCount: ULong
    var fences: NativeAddress?
    var signaledValueCount: ULong
    var signaledValues: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryBeginAccessDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryBeginAccessDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryBeginAccessDescriptor>
    }
}

expect interface WGPUSharedBufferMemoryEndAccessState {
    var nextInChain: WGPUChainedStruct?
    var initialized: UInt
    var fenceCount: ULong
    var fences: NativeAddress?
    var signaledValueCount: ULong
    var signaledValues: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryEndAccessState
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryEndAccessState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedBufferMemoryEndAccessState>
    }
}

expect interface WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: NativeAddress?
    var size: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryFromWindowsHandleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryFromWindowsHandleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryFromWindowsHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryFromWindowsHandleDescriptor>
    }
}

expect interface WGPUSharedBufferMemoryHostPointerDescriptor {
    var chain: WGPUChainedStruct
    var pointer: NativeAddress?
    var size: ULong
    var disposeCallbackInfo: WGPUDisposeCallbackInfo
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryHostPointerDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryHostPointerDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryHostPointerDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryHostPointerDescriptor>
    }
}

expect interface WGPUSharedBufferMemoryProperties {
    var nextInChain: WGPUChainedStruct?
    var usage: ULong
    var size: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryProperties
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryProperties
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryProperties) -> Unit): ArrayHolder<WGPUSharedBufferMemoryProperties>
    }
}

expect interface WGPUSharedFenceDXGISharedHandleDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleDescriptor>
    }
}

expect interface WGPUSharedFenceDXGISharedHandleExportInfo {
    var chain: WGPUChainedStruct
    var handle_2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleExportInfo>
    }
}

expect interface WGPUSharedFenceEGLSyncDescriptor {
    var chain: WGPUChainedStruct
    var sync: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncDescriptor>
    }
}

expect interface WGPUSharedFenceEGLSyncExportInfo {
    var chain: WGPUChainedStruct
    var sync: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncExportInfo>
    }
}

expect interface WGPUSharedFenceMTLSharedEventDescriptor {
    var chain: WGPUChainedStruct
    var sharedEvent: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventDescriptor>
    }
}

expect interface WGPUSharedFenceMTLSharedEventExportInfo {
    var chain: WGPUChainedStruct
    var sharedEvent: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventExportInfo>
    }
}

expect interface WGPUSharedFenceSyncFDDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDDescriptor>
    }
}

expect interface WGPUSharedFenceSyncFDExportInfo {
    var chain: WGPUChainedStruct
    var handle_2: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDExportInfo>
    }
}

expect interface WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>
    }
}

expect interface WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
    var chain: WGPUChainedStruct
    var handle_2: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>
    }
}

expect interface WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>
    }
}

expect interface WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
    var chain: WGPUChainedStruct
    var handle_2: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>
    }
}

expect interface WGPUSharedTextureMemoryAHardwareBufferDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryD3D11BeginState {
    var chain: WGPUChainedStruct
    var requiresEndAccessFence: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3D11BeginState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3D11BeginState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3D11BeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3D11BeginState>
    }
}

expect interface WGPUSharedTextureMemoryD3DSwapchainBeginState {
    var chain: WGPUChainedStruct
    var isSwapchain: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3DSwapchainBeginState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3DSwapchainBeginState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3DSwapchainBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3DSwapchainBeginState>
    }
}

expect interface WGPUSharedTextureMemoryDmaBufPlane {
    var fd: Int
    var offset: ULong
    var stride: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDmaBufPlane
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufPlane
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufPlane) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufPlane>
    }
}

expect interface WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
    var chain: WGPUChainedStruct
    var handle_2: NativeAddress?
    var useKeyedMutex: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDXGISharedHandleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDXGISharedHandleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDXGISharedHandleDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryEGLImageDescriptor {
    var chain: WGPUChainedStruct
    var image: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryEGLImageDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEGLImageDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEGLImageDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEGLImageDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryIOSurfaceDescriptor {
    var chain: WGPUChainedStruct
    var ioSurface: NativeAddress?
    var allowStorageBinding: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryIOSurfaceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryIOSurfaceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryIOSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryIOSurfaceDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryOpaqueFDDescriptor {
    var chain: WGPUChainedStruct
    var vkImageCreateInfo: NativeAddress?
    var memoryFD: Int
    var memoryTypeIndex: UInt
    var allocationSize: ULong
    var dedicatedAllocation: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryOpaqueFDDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryOpaqueFDDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryOpaqueFDDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
    var chain: WGPUChainedStruct
    var dedicatedAllocation: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryVkImageLayoutBeginState {
    var chain: WGPUChainedStruct
    var oldLayout: Int
    var newLayout: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutBeginState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutBeginState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutBeginState>
    }
}

expect interface WGPUSharedTextureMemoryVkImageLayoutEndState {
    var chain: WGPUChainedStruct
    var oldLayout: Int
    var newLayout: Int
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutEndState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutEndState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutEndState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutEndState>
    }
}

expect interface WGPUSharedTextureMemoryZirconHandleDescriptor {
    var chain: WGPUChainedStruct
    var memoryFD: UInt
    var allocationSize: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryZirconHandleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryZirconHandleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryZirconHandleDescriptor>
    }
}

expect interface WGPUStaticSamplerBindingLayout {
    var chain: WGPUChainedStruct
    var sampler: WGPUSampler?
    var sampledTextureBinding: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUStaticSamplerBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUStaticSamplerBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStaticSamplerBindingLayout) -> Unit): ArrayHolder<WGPUStaticSamplerBindingLayout>
    }
}

expect interface WGPUStencilFaceState {
    var compare: WGPUCompareFunction
    var failOp: WGPUStencilOperation
    var depthFailOp: WGPUStencilOperation
    var passOp: WGPUStencilOperation
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUStencilFaceState
        fun allocate(allocator: MemoryAllocator): WGPUStencilFaceState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStencilFaceState) -> Unit): ArrayHolder<WGPUStencilFaceState>
    }
}

expect interface WGPUStorageTextureBindingLayout {
    var nextInChain: WGPUChainedStruct?
    var access: WGPUStorageTextureAccess
    var format: WGPUTextureFormat
    var viewDimension: WGPUTextureViewDimension
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUStorageTextureBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUStorageTextureBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStorageTextureBindingLayout) -> Unit): ArrayHolder<WGPUStorageTextureBindingLayout>
    }
}

expect interface WGPUSubgroupMatrixConfig {
    var componentType: WGPUSubgroupMatrixComponentType
    var resultComponentType: WGPUSubgroupMatrixComponentType
    var M: UInt
    var N: UInt
    var K: UInt
    var minSubgroupSize: UInt
    var maxSubgroupSize: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSubgroupMatrixConfig
        fun allocate(allocator: MemoryAllocator): WGPUSubgroupMatrixConfig
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSubgroupMatrixConfig) -> Unit): ArrayHolder<WGPUSubgroupMatrixConfig>
    }
}

expect interface WGPUSupportedFeatures {
    var featureCount: ULong
    var features: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSupportedFeatures
        fun allocate(allocator: MemoryAllocator): WGPUSupportedFeatures
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedFeatures) -> Unit): ArrayHolder<WGPUSupportedFeatures>
    }
}

expect interface WGPUSupportedInstanceFeatures {
    var featureCount: ULong
    var features: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSupportedInstanceFeatures
        fun allocate(allocator: MemoryAllocator): WGPUSupportedInstanceFeatures
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedInstanceFeatures) -> Unit): ArrayHolder<WGPUSupportedInstanceFeatures>
    }
}

expect interface WGPUSupportedWGSLLanguageFeatures {
    var featureCount: ULong
    var features: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSupportedWGSLLanguageFeatures
        fun allocate(allocator: MemoryAllocator): WGPUSupportedWGSLLanguageFeatures
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedWGSLLanguageFeatures) -> Unit): ArrayHolder<WGPUSupportedWGSLLanguageFeatures>
    }
}

expect interface WGPUSurfaceCapabilities {
    var nextInChain: WGPUChainedStruct?
    var usages: ULong
    var formatCount: ULong
    var formats: NativeAddress?
    var presentModeCount: ULong
    var presentModes: NativeAddress?
    var alphaModeCount: ULong
    var alphaModes: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceCapabilities
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceCapabilities
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceCapabilities) -> Unit): ArrayHolder<WGPUSurfaceCapabilities>
    }
}

expect interface WGPUSurfaceColorManagement {
    var chain: WGPUChainedStruct
    var colorSpace: WGPUPredefinedColorSpace
    var toneMappingMode: WGPUToneMappingMode
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceColorManagement
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceColorManagement
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceColorManagement) -> Unit): ArrayHolder<WGPUSurfaceColorManagement>
    }
}

expect interface WGPUSurfaceConfiguration {
    var nextInChain: WGPUChainedStruct?
    var device: WGPUDevice?
    var format: WGPUTextureFormat
    var usage: ULong
    var width: UInt
    var height: UInt
    var viewFormatCount: ULong
    var viewFormats: NativeAddress?
    var alphaMode: WGPUCompositeAlphaMode
    var presentMode: WGPUPresentMode
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceConfiguration
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceConfiguration
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceConfiguration) -> Unit): ArrayHolder<WGPUSurfaceConfiguration>
    }
}

expect interface WGPUSurfaceDescriptorFromWindowsCoreWindow {
    var chain: WGPUChainedStruct
    var coreWindow: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsCoreWindow
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsCoreWindow
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsCoreWindow) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsCoreWindow>
    }
}

expect interface WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
    var chain: WGPUChainedStruct
    var swapChainPanel: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>
    }
}

expect interface WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
    var chain: WGPUChainedStruct
    var swapChainPanel: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>
    }
}

expect interface WGPUSurfaceSourceAndroidNativeWindow {
    var chain: WGPUChainedStruct
    var window: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceAndroidNativeWindow
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceAndroidNativeWindow
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceAndroidNativeWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceAndroidNativeWindow>
    }
}

expect interface WGPUSurfaceSourceMetalLayer {
    var chain: WGPUChainedStruct
    var layer: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceMetalLayer
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceMetalLayer
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceMetalLayer) -> Unit): ArrayHolder<WGPUSurfaceSourceMetalLayer>
    }
}

expect interface WGPUSurfaceSourceWaylandSurface {
    var chain: WGPUChainedStruct
    var display: NativeAddress?
    var surface: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWaylandSurface
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWaylandSurface
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWaylandSurface) -> Unit): ArrayHolder<WGPUSurfaceSourceWaylandSurface>
    }
}

expect interface WGPUSurfaceSourceWindowsHWND {
    var chain: WGPUChainedStruct
    var hinstance: NativeAddress?
    var hwnd: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWindowsHWND
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWindowsHWND
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWindowsHWND) -> Unit): ArrayHolder<WGPUSurfaceSourceWindowsHWND>
    }
}

expect interface WGPUSurfaceSourceXCBWindow {
    var chain: WGPUChainedStruct
    var connection: NativeAddress?
    var window: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXCBWindow
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXCBWindow
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXCBWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXCBWindow>
    }
}

expect interface WGPUSurfaceSourceXlibWindow {
    var chain: WGPUChainedStruct
    var display: NativeAddress?
    var window: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXlibWindow
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXlibWindow
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXlibWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXlibWindow>
    }
}

expect interface WGPUSurfaceTexture {
    var nextInChain: WGPUChainedStruct?
    var texture: WGPUTexture?
    var status: WGPUSurfaceGetCurrentTextureStatus
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceTexture
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceTexture
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceTexture) -> Unit): ArrayHolder<WGPUSurfaceTexture>
    }
}

expect interface WGPUTexelBufferBindingEntry {
    var chain: WGPUChainedStruct
    var texelBufferView: WGPUTexelBufferView?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingEntry
        fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingEntry
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingEntry) -> Unit): ArrayHolder<WGPUTexelBufferBindingEntry>
    }
}

expect interface WGPUTexelBufferBindingLayout {
    var chain: WGPUChainedStruct
    var access: WGPUTexelBufferAccess
    var format: WGPUTextureFormat
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingLayout) -> Unit): ArrayHolder<WGPUTexelBufferBindingLayout>
    }
}

expect interface WGPUTexelBufferViewDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var format: WGPUTextureFormat
    var offset: ULong
    var size: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelBufferViewDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUTexelBufferViewDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferViewDescriptor) -> Unit): ArrayHolder<WGPUTexelBufferViewDescriptor>
    }
}

expect interface WGPUTexelCopyBufferLayout {
    var offset: ULong
    var bytesPerRow: UInt
    var rowsPerImage: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferLayout
        fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferLayout) -> Unit): ArrayHolder<WGPUTexelCopyBufferLayout>
    }
}

expect interface WGPUTextureBindingLayout {
    var nextInChain: WGPUChainedStruct?
    var sampleType: WGPUTextureSampleType
    var viewDimension: WGPUTextureViewDimension
    var multisampled: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureBindingLayout
        fun allocate(allocator: MemoryAllocator): WGPUTextureBindingLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingLayout) -> Unit): ArrayHolder<WGPUTextureBindingLayout>
    }
}

expect interface WGPUTextureBindingViewDimension {
    var chain: WGPUChainedStruct
    var textureBindingViewDimension: WGPUTextureViewDimension
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureBindingViewDimension
        fun allocate(allocator: MemoryAllocator): WGPUTextureBindingViewDimension
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingViewDimension) -> Unit): ArrayHolder<WGPUTextureBindingViewDimension>
    }
}

expect interface WGPUTextureComponentSwizzle {
    var r: WGPUComponentSwizzle
    var g: WGPUComponentSwizzle
    var b: WGPUComponentSwizzle
    var a: WGPUComponentSwizzle
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzle
        fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzle
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzle) -> Unit): ArrayHolder<WGPUTextureComponentSwizzle>
    }
}

expect interface WGPUVertexAttribute {
    var nextInChain: WGPUChainedStruct?
    var format: WGPUVertexFormat
    var offset: ULong
    var shaderLocation: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUVertexAttribute
        fun allocate(allocator: MemoryAllocator): WGPUVertexAttribute
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexAttribute) -> Unit): ArrayHolder<WGPUVertexAttribute>
    }
}

expect interface WGPUYCbCrVkDescriptor {
    var chain: WGPUChainedStruct
    var vkFormat: UInt
    var vkYCbCrModel: UInt
    var vkYCbCrRange: UInt
    var vkComponentSwizzleRed: UInt
    var vkComponentSwizzleGreen: UInt
    var vkComponentSwizzleBlue: UInt
    var vkComponentSwizzleAlpha: UInt
    var vkXChromaOffset: UInt
    var vkYChromaOffset: UInt
    var vkChromaFilter: WGPUFilterMode
    var forceExplicitReconstruction: UInt
    var externalFormat: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUYCbCrVkDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUYCbCrVkDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUYCbCrVkDescriptor) -> Unit): ArrayHolder<WGPUYCbCrVkDescriptor>
    }
}

expect interface WGPUAdapterPropertiesMemoryHeaps {
    var chain: WGPUChainedStruct
    var heapCount: ULong
    var heapInfo: WGPUMemoryHeapInfo?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesMemoryHeaps
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesMemoryHeaps
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesMemoryHeaps) -> Unit): ArrayHolder<WGPUAdapterPropertiesMemoryHeaps>
    }
}

expect interface WGPUAdapterPropertiesSubgroupMatrixConfigs {
    var chain: WGPUChainedStruct
    var configCount: ULong
    var configs: WGPUSubgroupMatrixConfig?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesSubgroupMatrixConfigs
        fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesSubgroupMatrixConfigs
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesSubgroupMatrixConfigs) -> Unit): ArrayHolder<WGPUAdapterPropertiesSubgroupMatrixConfigs>
    }
}

expect interface WGPUAHardwareBufferProperties {
    var yCbCrInfo: WGPUYCbCrVkDescriptor
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAHardwareBufferProperties
        fun allocate(allocator: MemoryAllocator): WGPUAHardwareBufferProperties
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUAHardwareBufferProperties>
    }
}

expect interface WGPUBindGroupEntry {
    var nextInChain: WGPUChainedStruct?
    var binding: UInt
    var buffer: WGPUBuffer?
    var offset: ULong
    var size: ULong
    var sampler: WGPUSampler?
    var textureView: WGPUTextureView?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBindGroupEntry
        fun allocate(allocator: MemoryAllocator): WGPUBindGroupEntry
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupEntry) -> Unit): ArrayHolder<WGPUBindGroupEntry>
    }
}

expect interface WGPUBindGroupLayoutEntry {
    var nextInChain: WGPUChainedStruct?
    var binding: UInt
    var visibility: ULong
    var bindingArraySize: UInt
    var buffer: WGPUBufferBindingLayout
    var sampler: WGPUSamplerBindingLayout
    var texture: WGPUTextureBindingLayout
    var storageTexture: WGPUStorageTextureBindingLayout
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBindGroupLayoutEntry
        fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutEntry
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutEntry) -> Unit): ArrayHolder<WGPUBindGroupLayoutEntry>
    }
}

expect interface WGPUBlendState {
    var color: WGPUBlendComponent
    var alpha: WGPUBlendComponent
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBlendState
        fun allocate(allocator: MemoryAllocator): WGPUBlendState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendState) -> Unit): ArrayHolder<WGPUBlendState>
    }
}

expect interface WGPUBufferDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var usage: ULong
    var size: ULong
    var mappedAtCreation: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBufferDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUBufferDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferDescriptor) -> Unit): ArrayHolder<WGPUBufferDescriptor>
    }
}

expect interface WGPUCommandEncoderDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCommandEncoderDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUCommandEncoderDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandEncoderDescriptor) -> Unit): ArrayHolder<WGPUCommandEncoderDescriptor>
    }
}

expect interface WGPUCompilationMessage {
    var nextInChain: WGPUChainedStruct?
    var message: WGPUStringView
    var type: WGPUCompilationMessageType
    var lineNum: ULong
    var linePos: ULong
    var offset: ULong
    var length: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCompilationMessage
        fun allocate(allocator: MemoryAllocator): WGPUCompilationMessage
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationMessage) -> Unit): ArrayHolder<WGPUCompilationMessage>
    }
}

expect interface WGPUComputePassDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var timestampWrites: WGPUPassTimestampWrites?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUComputePassDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUComputePassDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePassDescriptor) -> Unit): ArrayHolder<WGPUComputePassDescriptor>
    }
}

expect interface WGPUComputeState {
    var nextInChain: WGPUChainedStruct?
    var module: WGPUShaderModule?
    var entryPoint: WGPUStringView
    var constantCount: ULong
    var constants: WGPUConstantEntry?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUComputeState
        fun allocate(allocator: MemoryAllocator): WGPUComputeState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputeState) -> Unit): ArrayHolder<WGPUComputeState>
    }
}

expect interface WGPUDawnDrmFormatCapabilities {
    var chain: WGPUChainedStruct
    var propertiesCount: ULong
    var properties: WGPUDawnDrmFormatProperties?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatCapabilities
        fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatCapabilities
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnDrmFormatCapabilities>
    }
}

expect interface WGPUDepthStencilState {
    var nextInChain: WGPUChainedStruct?
    var format: WGPUTextureFormat
    var depthWriteEnabled: WGPUOptionalBool
    var depthCompare: WGPUCompareFunction
    var stencilFront: WGPUStencilFaceState
    var stencilBack: WGPUStencilFaceState
    var stencilReadMask: UInt
    var stencilWriteMask: UInt
    var depthBias: Int
    var depthBiasSlopeScale: Float
    var depthBiasClamp: Float
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDepthStencilState
        fun allocate(allocator: MemoryAllocator): WGPUDepthStencilState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDepthStencilState) -> Unit): ArrayHolder<WGPUDepthStencilState>
    }
}

expect interface WGPUExternalTextureDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var plane0: WGPUTextureView?
    var plane1: WGPUTextureView?
    var cropOrigin: WGPUOrigin2D
    var cropSize: WGPUExtent2D
    var apparentSize: WGPUExtent2D
    var doYuvToRgbConversionOnly: UInt
    var yuvToRgbConversionMatrix: NativeAddress?
    var srcTransferFunctionParameters: NativeAddress?
    var dstTransferFunctionParameters: NativeAddress?
    var gamutConversionMatrix: NativeAddress?
    var mirrored: UInt
    var rotation: WGPUExternalTextureRotation
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUExternalTextureDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUExternalTextureDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureDescriptor) -> Unit): ArrayHolder<WGPUExternalTextureDescriptor>
    }
}

expect interface WGPUFutureWaitInfo {
    var future: WGPUFuture
    var completed: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUFutureWaitInfo
        fun allocate(allocator: MemoryAllocator): WGPUFutureWaitInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFutureWaitInfo) -> Unit): ArrayHolder<WGPUFutureWaitInfo>
    }
}

expect interface WGPUImageCopyExternalTexture {
    var nextInChain: WGPUChainedStruct?
    var externalTexture: WGPUExternalTexture?
    var origin: WGPUOrigin3D
    var naturalSize: WGPUExtent2D
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUImageCopyExternalTexture
        fun allocate(allocator: MemoryAllocator): WGPUImageCopyExternalTexture
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUImageCopyExternalTexture) -> Unit): ArrayHolder<WGPUImageCopyExternalTexture>
    }
}

expect interface WGPUInstanceDescriptor {
    var nextInChain: WGPUChainedStruct?
    var requiredFeatureCount: ULong
    var requiredFeatures: NativeAddress?
    var requiredLimits: WGPUInstanceLimits?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUInstanceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUInstanceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceDescriptor) -> Unit): ArrayHolder<WGPUInstanceDescriptor>
    }
}

expect interface WGPULimits {
    var nextInChain: WGPUChainedStruct?
    var maxTextureDimension1D: UInt
    var maxTextureDimension2D: UInt
    var maxTextureDimension3D: UInt
    var maxTextureArrayLayers: UInt
    var maxBindGroups: UInt
    var maxBindGroupsPlusVertexBuffers: UInt
    var maxBindingsPerBindGroup: UInt
    var maxDynamicUniformBuffersPerPipelineLayout: UInt
    var maxDynamicStorageBuffersPerPipelineLayout: UInt
    var maxSampledTexturesPerShaderStage: UInt
    var maxSamplersPerShaderStage: UInt
    var maxStorageBuffersPerShaderStage: UInt
    var maxStorageTexturesPerShaderStage: UInt
    var maxUniformBuffersPerShaderStage: UInt
    var maxUniformBufferBindingSize: ULong
    var maxStorageBufferBindingSize: ULong
    var minUniformBufferOffsetAlignment: UInt
    var minStorageBufferOffsetAlignment: UInt
    var maxVertexBuffers: UInt
    var maxBufferSize: ULong
    var maxVertexAttributes: UInt
    var maxVertexBufferArrayStride: UInt
    var maxInterStageShaderVariables: UInt
    var maxColorAttachments: UInt
    var maxColorAttachmentBytesPerSample: UInt
    var maxComputeWorkgroupStorageSize: UInt
    var maxComputeInvocationsPerWorkgroup: UInt
    var maxComputeWorkgroupSizeX: UInt
    var maxComputeWorkgroupSizeY: UInt
    var maxComputeWorkgroupSizeZ: UInt
    var maxComputeWorkgroupsPerDimension: UInt
    var maxImmediateSize: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPULimits
        fun allocate(allocator: MemoryAllocator): WGPULimits
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULimits) -> Unit): ArrayHolder<WGPULimits>
    }
}

expect interface WGPUPipelineLayoutPixelLocalStorage {
    var chain: WGPUChainedStruct
    var totalPixelLocalStorageSize: ULong
    var storageAttachmentCount: ULong
    var storageAttachments: WGPUPipelineLayoutStorageAttachment?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPipelineLayoutPixelLocalStorage
        fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutPixelLocalStorage
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutPixelLocalStorage) -> Unit): ArrayHolder<WGPUPipelineLayoutPixelLocalStorage>
    }
}

expect interface WGPURenderBundleEncoderDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var colorFormatCount: ULong
    var colorFormats: NativeAddress?
    var depthStencilFormat: WGPUTextureFormat
    var sampleCount: UInt
    var depthReadOnly: UInt
    var stencilReadOnly: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderBundleEncoderDescriptor
        fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderDescriptor) -> Unit): ArrayHolder<WGPURenderBundleEncoderDescriptor>
    }
}

expect interface WGPURenderPassColorAttachment {
    var nextInChain: WGPUChainedStruct?
    var view: WGPUTextureView?
    var depthSlice: UInt
    var resolveTarget: WGPUTextureView?
    var loadOp: WGPULoadOp
    var storeOp: WGPUStoreOp
    var clearValue: WGPUColor
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassColorAttachment
        fun allocate(allocator: MemoryAllocator): WGPURenderPassColorAttachment
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassColorAttachment) -> Unit): ArrayHolder<WGPURenderPassColorAttachment>
    }
}

expect interface WGPURenderPassRenderAreaRect {
    var chain: WGPUChainedStruct
    var origin: WGPUOrigin2D
    var size: WGPUExtent2D
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassRenderAreaRect
        fun allocate(allocator: MemoryAllocator): WGPURenderPassRenderAreaRect
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassRenderAreaRect) -> Unit): ArrayHolder<WGPURenderPassRenderAreaRect>
    }
}

expect interface WGPURenderPassStorageAttachment {
    var nextInChain: WGPUChainedStruct?
    var offset: ULong
    var storage: WGPUTextureView?
    var loadOp: WGPULoadOp
    var storeOp: WGPUStoreOp
    var clearValue: WGPUColor
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassStorageAttachment
        fun allocate(allocator: MemoryAllocator): WGPURenderPassStorageAttachment
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassStorageAttachment) -> Unit): ArrayHolder<WGPURenderPassStorageAttachment>
    }
}

expect interface WGPURequestAdapterOptions {
    var nextInChain: WGPUChainedStruct?
    var featureLevel: WGPUFeatureLevel
    var powerPreference: WGPUPowerPreference
    var forceFallbackAdapter: UInt
    var backendType: WGPUBackendType
    var compatibleSurface: WGPUSurface?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURequestAdapterOptions
        fun allocate(allocator: MemoryAllocator): WGPURequestAdapterOptions
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterOptions) -> Unit): ArrayHolder<WGPURequestAdapterOptions>
    }
}

expect interface WGPUSamplerDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var addressModeU: WGPUAddressMode
    var addressModeV: WGPUAddressMode
    var addressModeW: WGPUAddressMode
    var magFilter: WGPUFilterMode
    var minFilter: WGPUFilterMode
    var mipmapFilter: WGPUMipmapFilterMode
    var lodMinClamp: Float
    var lodMaxClamp: Float
    var compare: WGPUCompareFunction
    var maxAnisotropy: UShort
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSamplerDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSamplerDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerDescriptor) -> Unit): ArrayHolder<WGPUSamplerDescriptor>
    }
}

expect interface WGPUShaderModuleDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUShaderModuleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUShaderModuleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleDescriptor) -> Unit): ArrayHolder<WGPUShaderModuleDescriptor>
    }
}

expect interface WGPUSharedBufferMemoryDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryDescriptor>
    }
}

expect interface WGPUSharedFenceDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDescriptor>
    }
}

expect interface WGPUSharedFenceExportInfo {
    var nextInChain: WGPUChainedStruct?
    var type: WGPUSharedFenceType
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedFenceExportInfo
        fun allocate(allocator: MemoryAllocator): WGPUSharedFenceExportInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceExportInfo>
    }
}

expect interface WGPUSharedTextureMemoryAHardwareBufferProperties {
    var chain: WGPUChainedStruct
    var yCbCrInfo: WGPUYCbCrVkDescriptor
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferProperties
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferProperties
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferProperties>
    }
}

expect interface WGPUSharedTextureMemoryBeginAccessDescriptor {
    var nextInChain: WGPUChainedStruct?
    var concurrentRead: UInt
    var initialized: UInt
    var fenceCount: ULong
    var fences: NativeAddress?
    var signaledValueCount: ULong
    var signaledValues: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryBeginAccessDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryBeginAccessDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryBeginAccessDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryDmaBufDescriptor {
    var chain: WGPUChainedStruct
    var size: WGPUExtent3D
    var drmFormat: UInt
    var drmModifier: ULong
    var planeCount: ULong
    var planes: WGPUSharedTextureMemoryDmaBufPlane?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDmaBufDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryMetalEndAccessState {
    var chain: WGPUChainedStruct
    var commandsScheduledFuture: WGPUFuture
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryMetalEndAccessState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryMetalEndAccessState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryMetalEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryMetalEndAccessState>
    }
}

expect interface WGPUSurfaceDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSurfaceDescriptor>
    }
}

expect interface WGPUTexelCopyBufferInfo {
    var layout: WGPUTexelCopyBufferLayout
    var buffer: WGPUBuffer?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferInfo
        fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferInfo) -> Unit): ArrayHolder<WGPUTexelCopyBufferInfo>
    }
}

expect interface WGPUTexelCopyTextureInfo {
    var texture: WGPUTexture?
    var mipLevel: UInt
    var origin: WGPUOrigin3D
    var aspect: WGPUTextureAspect
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTexelCopyTextureInfo
        fun allocate(allocator: MemoryAllocator): WGPUTexelCopyTextureInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyTextureInfo) -> Unit): ArrayHolder<WGPUTexelCopyTextureInfo>
    }
}

expect interface WGPUTextureComponentSwizzleDescriptor {
    var chain: WGPUChainedStruct
    var swizzle: WGPUTextureComponentSwizzle
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzleDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzleDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzleDescriptor) -> Unit): ArrayHolder<WGPUTextureComponentSwizzleDescriptor>
    }
}

expect interface WGPUTextureDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var usage: ULong
    var dimension: WGPUTextureDimension
    var size: WGPUExtent3D
    var format: WGPUTextureFormat
    var mipLevelCount: UInt
    var sampleCount: UInt
    var viewFormatCount: ULong
    var viewFormats: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUTextureDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureDescriptor) -> Unit): ArrayHolder<WGPUTextureDescriptor>
    }
}

expect interface WGPUVertexBufferLayout {
    var nextInChain: WGPUChainedStruct?
    var stepMode: WGPUVertexStepMode
    var arrayStride: ULong
    var attributeCount: ULong
    var attributes: WGPUVertexAttribute?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUVertexBufferLayout
        fun allocate(allocator: MemoryAllocator): WGPUVertexBufferLayout
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexBufferLayout) -> Unit): ArrayHolder<WGPUVertexBufferLayout>
    }
}

expect interface WGPUAdapterInfo {
    var nextInChain: WGPUChainedStruct?
    var vendor: WGPUStringView
    var architecture: WGPUStringView
    var device: WGPUStringView
    var description: WGPUStringView
    var backendType: WGPUBackendType
    var adapterType: WGPUAdapterType
    var vendorID: UInt
    var deviceID: UInt
    var subgroupMinSize: UInt
    var subgroupMaxSize: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUAdapterInfo
        fun allocate(allocator: MemoryAllocator): WGPUAdapterInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterInfo) -> Unit): ArrayHolder<WGPUAdapterInfo>
    }
}

expect interface WGPUBindGroupDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var layout: WGPUBindGroupLayout?
    var entryCount: ULong
    var entries: WGPUBindGroupEntry?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBindGroupDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUBindGroupDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupDescriptor) -> Unit): ArrayHolder<WGPUBindGroupDescriptor>
    }
}

expect interface WGPUBindGroupLayoutDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var entryCount: ULong
    var entries: WGPUBindGroupLayoutEntry?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUBindGroupLayoutDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutDescriptor) -> Unit): ArrayHolder<WGPUBindGroupLayoutDescriptor>
    }
}

expect interface WGPUColorTargetState {
    var nextInChain: WGPUChainedStruct?
    var format: WGPUTextureFormat
    var blend: WGPUBlendState?
    var writeMask: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUColorTargetState
        fun allocate(allocator: MemoryAllocator): WGPUColorTargetState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetState) -> Unit): ArrayHolder<WGPUColorTargetState>
    }
}

expect interface WGPUCompilationInfo {
    var nextInChain: WGPUChainedStruct?
    var messageCount: ULong
    var messages: WGPUCompilationMessage?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUCompilationInfo
        fun allocate(allocator: MemoryAllocator): WGPUCompilationInfo
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfo) -> Unit): ArrayHolder<WGPUCompilationInfo>
    }
}

expect interface WGPUComputePipelineDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var layout: WGPUPipelineLayout?
    var compute: WGPUComputeState
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUComputePipelineDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUComputePipelineDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePipelineDescriptor) -> Unit): ArrayHolder<WGPUComputePipelineDescriptor>
    }
}

expect interface WGPUDawnFormatCapabilities {
    var nextInChain: WGPUChainedStruct?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDawnFormatCapabilities
        fun allocate(allocator: MemoryAllocator): WGPUDawnFormatCapabilities
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnFormatCapabilities>
    }
}

expect interface WGPUDeviceDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var requiredFeatureCount: ULong
    var requiredFeatures: NativeAddress?
    var requiredLimits: WGPULimits?
    var defaultQueue: WGPUQueueDescriptor
    var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
    var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUDeviceDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUDeviceDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceDescriptor) -> Unit): ArrayHolder<WGPUDeviceDescriptor>
    }
}

expect interface WGPUPipelineLayoutDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var bindGroupLayoutCount: ULong
    var bindGroupLayouts: NativeAddress?
    var immediateSize: UInt
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUPipelineLayoutDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutDescriptor) -> Unit): ArrayHolder<WGPUPipelineLayoutDescriptor>
    }
}

expect interface WGPURenderPassPixelLocalStorage {
    var chain: WGPUChainedStruct
    var totalPixelLocalStorageSize: ULong
    var storageAttachmentCount: ULong
    var storageAttachments: WGPURenderPassStorageAttachment?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassPixelLocalStorage
        fun allocate(allocator: MemoryAllocator): WGPURenderPassPixelLocalStorage
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassPixelLocalStorage) -> Unit): ArrayHolder<WGPURenderPassPixelLocalStorage>
    }
}

expect interface WGPUSharedTextureMemoryDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDescriptor>
    }
}

expect interface WGPUSharedTextureMemoryEndAccessState {
    var nextInChain: WGPUChainedStruct?
    var initialized: UInt
    var fenceCount: ULong
    var fences: NativeAddress?
    var signaledValueCount: ULong
    var signaledValues: NativeAddress?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryEndAccessState
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEndAccessState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEndAccessState>
    }
}

expect interface WGPUSharedTextureMemoryProperties {
    var nextInChain: WGPUChainedStruct?
    var usage: ULong
    var size: WGPUExtent3D
    var format: WGPUTextureFormat
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryProperties
        fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryProperties
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryProperties>
    }
}

expect interface WGPUTextureViewDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var format: WGPUTextureFormat
    var dimension: WGPUTextureViewDimension
    var baseMipLevel: UInt
    var mipLevelCount: UInt
    var baseArrayLayer: UInt
    var arrayLayerCount: UInt
    var aspect: WGPUTextureAspect
    var usage: ULong
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUTextureViewDescriptor
        fun allocate(allocator: MemoryAllocator): WGPUTextureViewDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureViewDescriptor) -> Unit): ArrayHolder<WGPUTextureViewDescriptor>
    }
}

expect interface WGPUVertexState {
    var nextInChain: WGPUChainedStruct?
    var module: WGPUShaderModule?
    var entryPoint: WGPUStringView
    var constantCount: ULong
    var constants: WGPUConstantEntry?
    var bufferCount: ULong
    var buffers: WGPUVertexBufferLayout?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUVertexState
        fun allocate(allocator: MemoryAllocator): WGPUVertexState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexState) -> Unit): ArrayHolder<WGPUVertexState>
    }
}

expect interface WGPUFragmentState {
    var nextInChain: WGPUChainedStruct?
    var module: WGPUShaderModule?
    var entryPoint: WGPUStringView
    var constantCount: ULong
    var constants: WGPUConstantEntry?
    var targetCount: ULong
    var targets: WGPUColorTargetState?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPUFragmentState
        fun allocate(allocator: MemoryAllocator): WGPUFragmentState
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFragmentState) -> Unit): ArrayHolder<WGPUFragmentState>
    }
}

expect interface WGPURenderPassDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var colorAttachmentCount: ULong
    var colorAttachments: WGPURenderPassColorAttachment?
    var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
    var occlusionQuerySet: WGPUQuerySet?
    var timestampWrites: WGPUPassTimestampWrites?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPassDescriptor
        fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptor) -> Unit): ArrayHolder<WGPURenderPassDescriptor>
    }
}

expect interface WGPURenderPipelineDescriptor {
    var nextInChain: WGPUChainedStruct?
    var label: WGPUStringView
    var layout: WGPUPipelineLayout?
    var vertex: WGPUVertexState
    var primitive: WGPUPrimitiveState
    var depthStencil: WGPUDepthStencilState?
    var multisample: WGPUMultisampleState
    var fragment: WGPUFragmentState?
    val handler: NativeAddress
    companion object {
        operator fun invoke(address: NativeAddress): WGPURenderPipelineDescriptor
        fun allocate(allocator: MemoryAllocator): WGPURenderPipelineDescriptor
        fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPipelineDescriptor) -> Unit): ArrayHolder<WGPURenderPipelineDescriptor>
    }
}

expect fun wgpuCreateInstance(descriptor: WGPUInstanceDescriptor?): WGPUInstance?

expect fun wgpuGetInstanceFeatures(features: WGPUSupportedInstanceFeatures?): Unit

expect fun wgpuGetInstanceLimits(limits: WGPUInstanceLimits?): WGPUStatus

expect fun wgpuHasInstanceFeature(feature: WGPUInstanceFeatureName): UInt

expect fun wgpuGetProcAddress(procName: WGPUStringView): NativeAddress?

expect fun wgpuAdapterCreateDevice(adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?): WGPUDevice?

expect fun wgpuAdapterGetFeatures(adapter: WGPUAdapter?, features: WGPUSupportedFeatures?): Unit

expect fun wgpuAdapterGetFormatCapabilities(adapter: WGPUAdapter?, format: WGPUTextureFormat, capabilities: WGPUDawnFormatCapabilities?): WGPUStatus

expect fun wgpuAdapterGetInfo(adapter: WGPUAdapter?, info: WGPUAdapterInfo?): WGPUStatus

expect fun wgpuAdapterGetInstance(adapter: WGPUAdapter?): WGPUInstance?

expect fun wgpuAdapterGetLimits(adapter: WGPUAdapter?, limits: WGPULimits?): WGPUStatus

expect fun wgpuAdapterHasFeature(adapter: WGPUAdapter?, feature: WGPUFeatureName): UInt

expect fun wgpuAdapterRequestDevice(allocator: MemoryAllocator, adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?, callbackInfo: WGPURequestDeviceCallbackInfo): WGPUFuture

expect fun wgpuAdapterAddRef(adapter: WGPUAdapter?): Unit

expect fun wgpuAdapterRelease(adapter: WGPUAdapter?): Unit

expect fun wgpuAdapterInfoFreeMembers(adapterInfo: WGPUAdapterInfo): Unit

expect fun wgpuAdapterPropertiesMemoryHeapsFreeMembers(adapterPropertiesMemoryHeaps: WGPUAdapterPropertiesMemoryHeaps): Unit

expect fun wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers(adapterPropertiesSubgroupMatrixConfigs: WGPUAdapterPropertiesSubgroupMatrixConfigs): Unit

expect fun wgpuBindGroupSetLabel(bindGroup: WGPUBindGroup?, label: WGPUStringView): Unit

expect fun wgpuBindGroupAddRef(bindGroup: WGPUBindGroup?): Unit

expect fun wgpuBindGroupRelease(bindGroup: WGPUBindGroup?): Unit

expect fun wgpuBindGroupLayoutSetLabel(bindGroupLayout: WGPUBindGroupLayout?, label: WGPUStringView): Unit

expect fun wgpuBindGroupLayoutAddRef(bindGroupLayout: WGPUBindGroupLayout?): Unit

expect fun wgpuBindGroupLayoutRelease(bindGroupLayout: WGPUBindGroupLayout?): Unit

expect fun wgpuBufferCreateTexelView(buffer: WGPUBuffer?, descriptor: WGPUTexelBufferViewDescriptor?): WGPUTexelBufferView?

expect fun wgpuBufferDestroy(buffer: WGPUBuffer?): Unit

expect fun wgpuBufferGetConstMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress?

expect fun wgpuBufferGetMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress?

expect fun wgpuBufferGetMapState(buffer: WGPUBuffer?): WGPUBufferMapState

expect fun wgpuBufferGetSize(buffer: WGPUBuffer?): ULong

expect fun wgpuBufferGetUsage(buffer: WGPUBuffer?): ULong

expect fun wgpuBufferMapAsync(allocator: MemoryAllocator, buffer: WGPUBuffer?, mode: ULong, offset: ULong, size: ULong, callbackInfo: WGPUBufferMapCallbackInfo): WGPUFuture

expect fun wgpuBufferReadMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus

expect fun wgpuBufferSetLabel(buffer: WGPUBuffer?, label: WGPUStringView): Unit

expect fun wgpuBufferUnmap(buffer: WGPUBuffer?): Unit

expect fun wgpuBufferWriteMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus

expect fun wgpuBufferAddRef(buffer: WGPUBuffer?): Unit

expect fun wgpuBufferRelease(buffer: WGPUBuffer?): Unit

expect fun wgpuCommandBufferSetLabel(commandBuffer: WGPUCommandBuffer?, label: WGPUStringView): Unit

expect fun wgpuCommandBufferAddRef(commandBuffer: WGPUCommandBuffer?): Unit

expect fun wgpuCommandBufferRelease(commandBuffer: WGPUCommandBuffer?): Unit

expect fun wgpuCommandEncoderBeginComputePass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUComputePassDescriptor?): WGPUComputePassEncoder?

expect fun wgpuCommandEncoderBeginRenderPass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPURenderPassDescriptor?): WGPURenderPassEncoder?

expect fun wgpuCommandEncoderClearBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit

expect fun wgpuCommandEncoderCopyBufferToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUBuffer?, sourceOffset: ULong, destination: WGPUBuffer?, destinationOffset: ULong, size: ULong): Unit

expect fun wgpuCommandEncoderCopyBufferToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyBufferInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit

expect fun wgpuCommandEncoderCopyTextureToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyBufferInfo?, copySize: WGPUExtent3D?): Unit

expect fun wgpuCommandEncoderCopyTextureToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit

expect fun wgpuCommandEncoderFinish(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUCommandBufferDescriptor?): WGPUCommandBuffer?

expect fun wgpuCommandEncoderInjectValidationError(commandEncoder: WGPUCommandEncoder?, message: WGPUStringView): Unit

expect fun wgpuCommandEncoderInsertDebugMarker(commandEncoder: WGPUCommandEncoder?, markerLabel: WGPUStringView): Unit

expect fun wgpuCommandEncoderPopDebugGroup(commandEncoder: WGPUCommandEncoder?): Unit

expect fun wgpuCommandEncoderPushDebugGroup(commandEncoder: WGPUCommandEncoder?, groupLabel: WGPUStringView): Unit

expect fun wgpuCommandEncoderResolveQuerySet(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, firstQuery: UInt, queryCount: UInt, destination: WGPUBuffer?, destinationOffset: ULong): Unit

expect fun wgpuCommandEncoderSetLabel(commandEncoder: WGPUCommandEncoder?, label: WGPUStringView): Unit

expect fun wgpuCommandEncoderWriteBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit

expect fun wgpuCommandEncoderWriteTimestamp(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit

expect fun wgpuCommandEncoderAddRef(commandEncoder: WGPUCommandEncoder?): Unit

expect fun wgpuCommandEncoderRelease(commandEncoder: WGPUCommandEncoder?): Unit

expect fun wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder: WGPUComputePassEncoder?, workgroupCountX: UInt, workgroupCountY: UInt, workgroupCountZ: UInt): Unit

expect fun wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder: WGPUComputePassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit

expect fun wgpuComputePassEncoderEnd(computePassEncoder: WGPUComputePassEncoder?): Unit

expect fun wgpuComputePassEncoderInsertDebugMarker(computePassEncoder: WGPUComputePassEncoder?, markerLabel: WGPUStringView): Unit

expect fun wgpuComputePassEncoderPopDebugGroup(computePassEncoder: WGPUComputePassEncoder?): Unit

expect fun wgpuComputePassEncoderPushDebugGroup(computePassEncoder: WGPUComputePassEncoder?, groupLabel: WGPUStringView): Unit

expect fun wgpuComputePassEncoderSetBindGroup(computePassEncoder: WGPUComputePassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit

expect fun wgpuComputePassEncoderSetImmediates(computePassEncoder: WGPUComputePassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit

expect fun wgpuComputePassEncoderSetLabel(computePassEncoder: WGPUComputePassEncoder?, label: WGPUStringView): Unit

expect fun wgpuComputePassEncoderSetPipeline(computePassEncoder: WGPUComputePassEncoder?, pipeline: WGPUComputePipeline?): Unit

expect fun wgpuComputePassEncoderSetResourceTable(computePassEncoder: WGPUComputePassEncoder?, table: WGPUResourceTable?): Unit

expect fun wgpuComputePassEncoderWriteTimestamp(computePassEncoder: WGPUComputePassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit

expect fun wgpuComputePassEncoderAddRef(computePassEncoder: WGPUComputePassEncoder?): Unit

expect fun wgpuComputePassEncoderRelease(computePassEncoder: WGPUComputePassEncoder?): Unit

expect fun wgpuComputePipelineGetBindGroupLayout(computePipeline: WGPUComputePipeline?, groupIndex: UInt): WGPUBindGroupLayout?

expect fun wgpuComputePipelineSetLabel(computePipeline: WGPUComputePipeline?, label: WGPUStringView): Unit

expect fun wgpuComputePipelineAddRef(computePipeline: WGPUComputePipeline?): Unit

expect fun wgpuComputePipelineRelease(computePipeline: WGPUComputePipeline?): Unit

expect fun wgpuDawnDrmFormatCapabilitiesFreeMembers(dawnDrmFormatCapabilities: WGPUDawnDrmFormatCapabilities): Unit

expect fun wgpuDeviceCreateBindGroup(device: WGPUDevice?, descriptor: WGPUBindGroupDescriptor?): WGPUBindGroup?

expect fun wgpuDeviceCreateBindGroupLayout(device: WGPUDevice?, descriptor: WGPUBindGroupLayoutDescriptor?): WGPUBindGroupLayout?

expect fun wgpuDeviceCreateBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer?

expect fun wgpuDeviceCreateCommandEncoder(device: WGPUDevice?, descriptor: WGPUCommandEncoderDescriptor?): WGPUCommandEncoder?

expect fun wgpuDeviceCreateComputePipeline(device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?): WGPUComputePipeline?

expect fun wgpuDeviceCreateComputePipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?, callbackInfo: WGPUCreateComputePipelineAsyncCallbackInfo): WGPUFuture

expect fun wgpuDeviceCreateErrorBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer?

expect fun wgpuDeviceCreateErrorComputePipeline(device: WGPUDevice?, label: WGPUStringView): WGPUComputePipeline?

expect fun wgpuDeviceCreateErrorExternalTexture(device: WGPUDevice?): WGPUExternalTexture?

expect fun wgpuDeviceCreateErrorRenderPipeline(device: WGPUDevice?, label: WGPUStringView): WGPURenderPipeline?

expect fun wgpuDeviceCreateErrorShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?, errorMessage: WGPUStringView): WGPUShaderModule?

expect fun wgpuDeviceCreateErrorTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture?

expect fun wgpuDeviceCreateExternalTexture(device: WGPUDevice?, externalTextureDescriptor: WGPUExternalTextureDescriptor?): WGPUExternalTexture?

expect fun wgpuDeviceCreatePipelineLayout(device: WGPUDevice?, descriptor: WGPUPipelineLayoutDescriptor?): WGPUPipelineLayout?

expect fun wgpuDeviceCreateQuerySet(device: WGPUDevice?, descriptor: WGPUQuerySetDescriptor?): WGPUQuerySet?

expect fun wgpuDeviceCreateRenderBundleEncoder(device: WGPUDevice?, descriptor: WGPURenderBundleEncoderDescriptor?): WGPURenderBundleEncoder?

expect fun wgpuDeviceCreateRenderPipeline(device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?): WGPURenderPipeline?

expect fun wgpuDeviceCreateRenderPipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?, callbackInfo: WGPUCreateRenderPipelineAsyncCallbackInfo): WGPUFuture

expect fun wgpuDeviceCreateResourceTable(device: WGPUDevice?, descriptor: WGPUResourceTableDescriptor?): WGPUResourceTable?

expect fun wgpuDeviceCreateSampler(device: WGPUDevice?, descriptor: WGPUSamplerDescriptor?): WGPUSampler?

expect fun wgpuDeviceCreateShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?): WGPUShaderModule?

expect fun wgpuDeviceCreateTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture?

expect fun wgpuDeviceDestroy(device: WGPUDevice?): Unit

expect fun wgpuDeviceForceLoss(device: WGPUDevice?, type: WGPUDeviceLostReason, message: WGPUStringView): Unit

expect fun wgpuDeviceGetAdapter(device: WGPUDevice?): WGPUAdapter?

expect fun wgpuDeviceGetAdapterInfo(device: WGPUDevice?, adapterInfo: WGPUAdapterInfo?): WGPUStatus

expect fun wgpuDeviceGetAHardwareBufferProperties(device: WGPUDevice?, handle: NativeAddress?, properties: WGPUAHardwareBufferProperties?): WGPUStatus

expect fun wgpuDeviceGetFeatures(device: WGPUDevice?, features: WGPUSupportedFeatures?): Unit

expect fun wgpuDeviceGetLimits(device: WGPUDevice?, limits: WGPULimits?): WGPUStatus

expect fun wgpuDeviceGetLostFuture(allocator: MemoryAllocator, device: WGPUDevice?): WGPUFuture

expect fun wgpuDeviceGetQueue(device: WGPUDevice?): WGPUQueue?

expect fun wgpuDeviceHasFeature(device: WGPUDevice?, feature: WGPUFeatureName): UInt

expect fun wgpuDeviceImportSharedBufferMemory(device: WGPUDevice?, descriptor: WGPUSharedBufferMemoryDescriptor?): WGPUSharedBufferMemory?

expect fun wgpuDeviceImportSharedFence(device: WGPUDevice?, descriptor: WGPUSharedFenceDescriptor?): WGPUSharedFence?

expect fun wgpuDeviceImportSharedTextureMemory(device: WGPUDevice?, descriptor: WGPUSharedTextureMemoryDescriptor?): WGPUSharedTextureMemory?

expect fun wgpuDeviceInjectError(device: WGPUDevice?, type: WGPUErrorType, message: WGPUStringView): Unit

expect fun wgpuDevicePopErrorScope(allocator: MemoryAllocator, device: WGPUDevice?, callbackInfo: WGPUPopErrorScopeCallbackInfo): WGPUFuture

expect fun wgpuDevicePushErrorScope(device: WGPUDevice?, filter: WGPUErrorFilter): Unit

expect fun wgpuDeviceSetLabel(device: WGPUDevice?, label: WGPUStringView): Unit

expect fun wgpuDeviceSetLoggingCallback(device: WGPUDevice?, callbackInfo: WGPULoggingCallbackInfo): Unit

expect fun wgpuDeviceTick(device: WGPUDevice?): Unit

expect fun wgpuDeviceValidateTextureDescriptor(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): Unit

expect fun wgpuDeviceAddRef(device: WGPUDevice?): Unit

expect fun wgpuDeviceRelease(device: WGPUDevice?): Unit

expect fun wgpuExternalTextureDestroy(externalTexture: WGPUExternalTexture?): Unit

expect fun wgpuExternalTextureExpire(externalTexture: WGPUExternalTexture?): Unit

expect fun wgpuExternalTextureRefresh(externalTexture: WGPUExternalTexture?): Unit

expect fun wgpuExternalTextureSetLabel(externalTexture: WGPUExternalTexture?, label: WGPUStringView): Unit

expect fun wgpuExternalTextureAddRef(externalTexture: WGPUExternalTexture?): Unit

expect fun wgpuExternalTextureRelease(externalTexture: WGPUExternalTexture?): Unit

expect fun wgpuInstanceCreateSurface(instance: WGPUInstance?, descriptor: WGPUSurfaceDescriptor?): WGPUSurface?

expect fun wgpuInstanceGetWGSLLanguageFeatures(instance: WGPUInstance?, features: WGPUSupportedWGSLLanguageFeatures?): Unit

expect fun wgpuInstanceHasWGSLLanguageFeature(instance: WGPUInstance?, feature: WGPUWGSLLanguageFeatureName): UInt

expect fun wgpuInstanceProcessEvents(instance: WGPUInstance?): Unit

expect fun wgpuInstanceRequestAdapter(allocator: MemoryAllocator, instance: WGPUInstance?, options: WGPURequestAdapterOptions?, callbackInfo: WGPURequestAdapterCallbackInfo): WGPUFuture

expect fun wgpuInstanceWaitAny(instance: WGPUInstance?, futureCount: ULong, futures: WGPUFutureWaitInfo?, timeoutNS: ULong): WGPUWaitStatus

expect fun wgpuInstanceAddRef(instance: WGPUInstance?): Unit

expect fun wgpuInstanceRelease(instance: WGPUInstance?): Unit

expect fun wgpuPipelineLayoutSetLabel(pipelineLayout: WGPUPipelineLayout?, label: WGPUStringView): Unit

expect fun wgpuPipelineLayoutAddRef(pipelineLayout: WGPUPipelineLayout?): Unit

expect fun wgpuPipelineLayoutRelease(pipelineLayout: WGPUPipelineLayout?): Unit

expect fun wgpuQuerySetDestroy(querySet: WGPUQuerySet?): Unit

expect fun wgpuQuerySetGetCount(querySet: WGPUQuerySet?): UInt

expect fun wgpuQuerySetGetType(querySet: WGPUQuerySet?): WGPUQueryType

expect fun wgpuQuerySetSetLabel(querySet: WGPUQuerySet?, label: WGPUStringView): Unit

expect fun wgpuQuerySetAddRef(querySet: WGPUQuerySet?): Unit

expect fun wgpuQuerySetRelease(querySet: WGPUQuerySet?): Unit

expect fun wgpuQueueCopyExternalTextureForBrowser(queue: WGPUQueue?, source: WGPUImageCopyExternalTexture?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit

expect fun wgpuQueueCopyTextureForBrowser(queue: WGPUQueue?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit

expect fun wgpuQueueOnSubmittedWorkDone(allocator: MemoryAllocator, queue: WGPUQueue?, callbackInfo: WGPUQueueWorkDoneCallbackInfo): WGPUFuture

expect fun wgpuQueueSetLabel(queue: WGPUQueue?, label: WGPUStringView): Unit

expect fun wgpuQueueSubmit(queue: WGPUQueue?, commandCount: ULong, commands: NativeAddress?): Unit

expect fun wgpuQueueWriteBuffer(queue: WGPUQueue?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit

expect fun wgpuQueueWriteTexture(queue: WGPUQueue?, destination: WGPUTexelCopyTextureInfo?, data: NativeAddress?, dataSize: ULong, dataLayout: WGPUTexelCopyBufferLayout?, writeSize: WGPUExtent3D?): Unit

expect fun wgpuQueueAddRef(queue: WGPUQueue?): Unit

expect fun wgpuQueueRelease(queue: WGPUQueue?): Unit

expect fun wgpuRenderBundleSetLabel(renderBundle: WGPURenderBundle?, label: WGPUStringView): Unit

expect fun wgpuRenderBundleAddRef(renderBundle: WGPURenderBundle?): Unit

expect fun wgpuRenderBundleRelease(renderBundle: WGPURenderBundle?): Unit

expect fun wgpuRenderBundleEncoderDraw(renderBundleEncoder: WGPURenderBundleEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit

expect fun wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder: WGPURenderBundleEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit

expect fun wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit

expect fun wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit

expect fun wgpuRenderBundleEncoderFinish(renderBundleEncoder: WGPURenderBundleEncoder?, descriptor: WGPURenderBundleDescriptor?): WGPURenderBundle?

expect fun wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder: WGPURenderBundleEncoder?, markerLabel: WGPUStringView): Unit

expect fun wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?): Unit

expect fun wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupLabel: WGPUStringView): Unit

expect fun wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit

expect fun wgpuRenderBundleEncoderSetImmediates(renderBundleEncoder: WGPURenderBundleEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit

expect fun wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit

expect fun wgpuRenderBundleEncoderSetLabel(renderBundleEncoder: WGPURenderBundleEncoder?, label: WGPUStringView): Unit

expect fun wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder: WGPURenderBundleEncoder?, pipeline: WGPURenderPipeline?): Unit

expect fun wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit

expect fun wgpuRenderBundleEncoderAddRef(renderBundleEncoder: WGPURenderBundleEncoder?): Unit

expect fun wgpuRenderBundleEncoderRelease(renderBundleEncoder: WGPURenderBundleEncoder?): Unit

expect fun wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?, queryIndex: UInt): Unit

expect fun wgpuRenderPassEncoderDraw(renderPassEncoder: WGPURenderPassEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit

expect fun wgpuRenderPassEncoderDrawIndexed(renderPassEncoder: WGPURenderPassEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit

expect fun wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit

expect fun wgpuRenderPassEncoderDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit

expect fun wgpuRenderPassEncoderEnd(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPassEncoderExecuteBundles(renderPassEncoder: WGPURenderPassEncoder?, bundleCount: ULong, bundles: NativeAddress?): Unit

expect fun wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder: WGPURenderPassEncoder?, markerLabel: WGPUStringView): Unit

expect fun wgpuRenderPassEncoderMultiDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit

expect fun wgpuRenderPassEncoderMultiDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit

expect fun wgpuRenderPassEncoderPixelLocalStorageBarrier(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder: WGPURenderPassEncoder?, groupLabel: WGPUStringView): Unit

expect fun wgpuRenderPassEncoderSetBindGroup(renderPassEncoder: WGPURenderPassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit

expect fun wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder: WGPURenderPassEncoder?, color: WGPUColor?): Unit

expect fun wgpuRenderPassEncoderSetImmediates(renderPassEncoder: WGPURenderPassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit

expect fun wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder: WGPURenderPassEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit

expect fun wgpuRenderPassEncoderSetLabel(renderPassEncoder: WGPURenderPassEncoder?, label: WGPUStringView): Unit

expect fun wgpuRenderPassEncoderSetPipeline(renderPassEncoder: WGPURenderPassEncoder?, pipeline: WGPURenderPipeline?): Unit

expect fun wgpuRenderPassEncoderSetResourceTable(renderPassEncoder: WGPURenderPassEncoder?, table: WGPUResourceTable?): Unit

expect fun wgpuRenderPassEncoderSetScissorRect(renderPassEncoder: WGPURenderPassEncoder?, x: UInt, y: UInt, width: UInt, height: UInt): Unit

expect fun wgpuRenderPassEncoderSetStencilReference(renderPassEncoder: WGPURenderPassEncoder?, reference: UInt): Unit

expect fun wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder: WGPURenderPassEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit

expect fun wgpuRenderPassEncoderSetViewport(renderPassEncoder: WGPURenderPassEncoder?, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float): Unit

expect fun wgpuRenderPassEncoderWriteTimestamp(renderPassEncoder: WGPURenderPassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit

expect fun wgpuRenderPassEncoderAddRef(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPassEncoderRelease(renderPassEncoder: WGPURenderPassEncoder?): Unit

expect fun wgpuRenderPipelineGetBindGroupLayout(renderPipeline: WGPURenderPipeline?, groupIndex: UInt): WGPUBindGroupLayout?

expect fun wgpuRenderPipelineSetLabel(renderPipeline: WGPURenderPipeline?, label: WGPUStringView): Unit

expect fun wgpuRenderPipelineAddRef(renderPipeline: WGPURenderPipeline?): Unit

expect fun wgpuRenderPipelineRelease(renderPipeline: WGPURenderPipeline?): Unit

expect fun wgpuResourceTableDestroy(resourceTable: WGPUResourceTable?): Unit

expect fun wgpuResourceTableGetSize(resourceTable: WGPUResourceTable?): UInt

expect fun wgpuResourceTableInsert(resourceTable: WGPUResourceTable?, resource: WGPUBindingResource?): UInt

expect fun wgpuResourceTableRemove(resourceTable: WGPUResourceTable?, slot: UInt): WGPUStatus

expect fun wgpuResourceTableSetLabel(resourceTable: WGPUResourceTable?, label: WGPUStringView): Unit

expect fun wgpuResourceTableUpdate(resourceTable: WGPUResourceTable?, slot: UInt, resource: WGPUBindingResource?): WGPUStatus

expect fun wgpuResourceTableAddRef(resourceTable: WGPUResourceTable?): Unit

expect fun wgpuResourceTableRelease(resourceTable: WGPUResourceTable?): Unit

expect fun wgpuSamplerSetLabel(sampler: WGPUSampler?, label: WGPUStringView): Unit

expect fun wgpuSamplerAddRef(sampler: WGPUSampler?): Unit

expect fun wgpuSamplerRelease(sampler: WGPUSampler?): Unit

expect fun wgpuShaderModuleGetCompilationInfo(allocator: MemoryAllocator, shaderModule: WGPUShaderModule?, callbackInfo: WGPUCompilationInfoCallbackInfo): WGPUFuture

expect fun wgpuShaderModuleSetLabel(shaderModule: WGPUShaderModule?, label: WGPUStringView): Unit

expect fun wgpuShaderModuleAddRef(shaderModule: WGPUShaderModule?): Unit

expect fun wgpuShaderModuleRelease(shaderModule: WGPUShaderModule?): Unit

expect fun wgpuSharedBufferMemoryBeginAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryBeginAccessDescriptor?): WGPUStatus

expect fun wgpuSharedBufferMemoryCreateBuffer(sharedBufferMemory: WGPUSharedBufferMemory?, descriptor: WGPUBufferDescriptor?): WGPUBuffer?

expect fun wgpuSharedBufferMemoryEndAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryEndAccessState?): WGPUStatus

expect fun wgpuSharedBufferMemoryGetProperties(sharedBufferMemory: WGPUSharedBufferMemory?, properties: WGPUSharedBufferMemoryProperties?): WGPUStatus

expect fun wgpuSharedBufferMemoryIsDeviceLost(sharedBufferMemory: WGPUSharedBufferMemory?): UInt

expect fun wgpuSharedBufferMemorySetLabel(sharedBufferMemory: WGPUSharedBufferMemory?, label: WGPUStringView): Unit

expect fun wgpuSharedBufferMemoryAddRef(sharedBufferMemory: WGPUSharedBufferMemory?): Unit

expect fun wgpuSharedBufferMemoryRelease(sharedBufferMemory: WGPUSharedBufferMemory?): Unit

expect fun wgpuSharedBufferMemoryEndAccessStateFreeMembers(sharedBufferMemoryEndAccessState: WGPUSharedBufferMemoryEndAccessState): Unit

expect fun wgpuSharedFenceExportInfo(sharedFence: WGPUSharedFence?, info: WGPUSharedFenceExportInfo?): Unit

expect fun wgpuSharedFenceSetLabel(sharedFence: WGPUSharedFence?, label: WGPUStringView): Unit

expect fun wgpuSharedFenceAddRef(sharedFence: WGPUSharedFence?): Unit

expect fun wgpuSharedFenceRelease(sharedFence: WGPUSharedFence?): Unit

expect fun wgpuSharedTextureMemoryBeginAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryBeginAccessDescriptor?): WGPUStatus

expect fun wgpuSharedTextureMemoryCreateTexture(sharedTextureMemory: WGPUSharedTextureMemory?, descriptor: WGPUTextureDescriptor?): WGPUTexture?

expect fun wgpuSharedTextureMemoryEndAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryEndAccessState?): WGPUStatus

expect fun wgpuSharedTextureMemoryGetProperties(sharedTextureMemory: WGPUSharedTextureMemory?, properties: WGPUSharedTextureMemoryProperties?): WGPUStatus

expect fun wgpuSharedTextureMemoryIsDeviceLost(sharedTextureMemory: WGPUSharedTextureMemory?): UInt

expect fun wgpuSharedTextureMemorySetLabel(sharedTextureMemory: WGPUSharedTextureMemory?, label: WGPUStringView): Unit

expect fun wgpuSharedTextureMemoryAddRef(sharedTextureMemory: WGPUSharedTextureMemory?): Unit

expect fun wgpuSharedTextureMemoryRelease(sharedTextureMemory: WGPUSharedTextureMemory?): Unit

expect fun wgpuSharedTextureMemoryEndAccessStateFreeMembers(sharedTextureMemoryEndAccessState: WGPUSharedTextureMemoryEndAccessState): Unit

expect fun wgpuSupportedFeaturesFreeMembers(supportedFeatures: WGPUSupportedFeatures): Unit

expect fun wgpuSupportedInstanceFeaturesFreeMembers(supportedInstanceFeatures: WGPUSupportedInstanceFeatures): Unit

expect fun wgpuSupportedWGSLLanguageFeaturesFreeMembers(supportedWGSLLanguageFeatures: WGPUSupportedWGSLLanguageFeatures): Unit

expect fun wgpuSurfaceConfigure(surface: WGPUSurface?, config: WGPUSurfaceConfiguration?): Unit

expect fun wgpuSurfaceGetCapabilities(surface: WGPUSurface?, adapter: WGPUAdapter?, capabilities: WGPUSurfaceCapabilities?): WGPUStatus

expect fun wgpuSurfaceGetCurrentTexture(surface: WGPUSurface?, surfaceTexture: WGPUSurfaceTexture?): Unit

expect fun wgpuSurfacePresent(surface: WGPUSurface?): WGPUStatus

expect fun wgpuSurfaceSetLabel(surface: WGPUSurface?, label: WGPUStringView): Unit

expect fun wgpuSurfaceUnconfigure(surface: WGPUSurface?): Unit

expect fun wgpuSurfaceAddRef(surface: WGPUSurface?): Unit

expect fun wgpuSurfaceRelease(surface: WGPUSurface?): Unit

expect fun wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities: WGPUSurfaceCapabilities): Unit

expect fun wgpuTexelBufferViewSetLabel(texelBufferView: WGPUTexelBufferView?, label: WGPUStringView): Unit

expect fun wgpuTexelBufferViewAddRef(texelBufferView: WGPUTexelBufferView?): Unit

expect fun wgpuTexelBufferViewRelease(texelBufferView: WGPUTexelBufferView?): Unit

expect fun wgpuTextureCreateErrorView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView?

expect fun wgpuTextureCreateView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView?

expect fun wgpuTextureDestroy(texture: WGPUTexture?): Unit

expect fun wgpuTextureGetDepthOrArrayLayers(texture: WGPUTexture?): UInt

expect fun wgpuTextureGetDimension(texture: WGPUTexture?): WGPUTextureDimension

expect fun wgpuTextureGetFormat(texture: WGPUTexture?): WGPUTextureFormat

expect fun wgpuTextureGetHeight(texture: WGPUTexture?): UInt

expect fun wgpuTextureGetMipLevelCount(texture: WGPUTexture?): UInt

expect fun wgpuTextureGetSampleCount(texture: WGPUTexture?): UInt

expect fun wgpuTextureGetTextureBindingViewDimension(texture: WGPUTexture?): WGPUTextureViewDimension

expect fun wgpuTextureGetUsage(texture: WGPUTexture?): ULong

expect fun wgpuTextureGetWidth(texture: WGPUTexture?): UInt

expect fun wgpuTextureSetLabel(texture: WGPUTexture?, label: WGPUStringView): Unit

expect fun wgpuTextureSetOwnershipForMemoryDump(texture: WGPUTexture?, ownerGuid: ULong): Unit

expect fun wgpuTextureAddRef(texture: WGPUTexture?): Unit

expect fun wgpuTextureRelease(texture: WGPUTexture?): Unit

expect fun wgpuTextureViewSetLabel(textureView: WGPUTextureView?, label: WGPUStringView): Unit

expect fun wgpuTextureViewAddRef(textureView: WGPUTextureView?): Unit

expect fun wgpuTextureViewRelease(textureView: WGPUTextureView?): Unit

fun interface WGPUCallback : Callback {
    fun invoke()
    companion object
}

@CallbackRuntimeApi
internal val WGPUCallbackType: CallbackType<WGPUCallback> = CallbackType(
    canonicalId = "typedef:WGPUCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCallback,
): CallbackRegistration<WGPUCallback>

@CallbackRuntimeApi
internal expect fun WGPUCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCallback,
): PreparedCallbackRegistration<WGPUCallback>

fun interface WGPUDawnStoreCacheDataFunction : Callback {
    fun invoke(
        key: NativeAddress?,
        keySize: ULong,
        value: NativeAddress?,
        valueSize: ULong,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUDawnStoreCacheDataFunctionType: CallbackType<WGPUDawnStoreCacheDataFunction> = CallbackType(
    canonicalId = "typedef:WGPUDawnStoreCacheDataFunction",
    hasRoutingUserdata = true,
)

expect fun WGPUDawnStoreCacheDataFunction.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDawnStoreCacheDataFunction,
): CallbackRegistration<WGPUDawnStoreCacheDataFunction>

@CallbackRuntimeApi
internal expect fun WGPUDawnStoreCacheDataFunction.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDawnStoreCacheDataFunction,
): PreparedCallbackRegistration<WGPUDawnStoreCacheDataFunction>

fun interface WGPUProc : Callback {
    fun invoke()
    companion object
}

@CallbackRuntimeApi
internal val WGPUProcType: CallbackType<WGPUProc> = CallbackType(
    canonicalId = "typedef:WGPUProc",
    hasRoutingUserdata = false,
)

expect fun WGPUProc.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUProc,
): CallbackRegistration<WGPUProc>

@CallbackRuntimeApi
internal expect fun WGPUProc.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUProc,
): PreparedCallbackRegistration<WGPUProc>

@UnsafeCallbackRearmApi
expect fun WGPUProc.Companion.rearmAfterNativeQuiescence(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUProc,
): CallbackRegistration<WGPUProc>

fun interface WGPUBufferMapCallback : Callback {
    fun invoke(
        status: WGPUMapAsyncStatus,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUBufferMapCallbackType: CallbackType<WGPUBufferMapCallback> = CallbackType(
    canonicalId = "typedef:WGPUBufferMapCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUBufferMapCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUBufferMapCallback,
): CallbackRegistration<WGPUBufferMapCallback>

@CallbackRuntimeApi
internal expect fun WGPUBufferMapCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUBufferMapCallback,
): PreparedCallbackRegistration<WGPUBufferMapCallback>

fun interface WGPUCompilationInfoCallback : Callback {
    fun invoke(
        status: WGPUCompilationInfoRequestStatus,
        compilationInfo: NativeAddress?,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUCompilationInfoCallbackType: CallbackType<WGPUCompilationInfoCallback> = CallbackType(
    canonicalId = "typedef:WGPUCompilationInfoCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUCompilationInfoCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCompilationInfoCallback,
): CallbackRegistration<WGPUCompilationInfoCallback>

@CallbackRuntimeApi
internal expect fun WGPUCompilationInfoCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCompilationInfoCallback,
): PreparedCallbackRegistration<WGPUCompilationInfoCallback>

fun interface WGPUCreateComputePipelineAsyncCallback : Callback {
    fun invoke(
        status: WGPUCreatePipelineAsyncStatus,
        pipeline: WGPUComputePipeline?,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUCreateComputePipelineAsyncCallbackType: CallbackType<WGPUCreateComputePipelineAsyncCallback> = CallbackType(
    canonicalId = "typedef:WGPUCreateComputePipelineAsyncCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUCreateComputePipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCreateComputePipelineAsyncCallback,
): CallbackRegistration<WGPUCreateComputePipelineAsyncCallback>

@CallbackRuntimeApi
internal expect fun WGPUCreateComputePipelineAsyncCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCreateComputePipelineAsyncCallback,
): PreparedCallbackRegistration<WGPUCreateComputePipelineAsyncCallback>

fun interface WGPUCreateRenderPipelineAsyncCallback : Callback {
    fun invoke(
        status: WGPUCreatePipelineAsyncStatus,
        pipeline: WGPURenderPipeline?,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUCreateRenderPipelineAsyncCallbackType: CallbackType<WGPUCreateRenderPipelineAsyncCallback> = CallbackType(
    canonicalId = "typedef:WGPUCreateRenderPipelineAsyncCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUCreateRenderPipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCreateRenderPipelineAsyncCallback,
): CallbackRegistration<WGPUCreateRenderPipelineAsyncCallback>

@CallbackRuntimeApi
internal expect fun WGPUCreateRenderPipelineAsyncCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUCreateRenderPipelineAsyncCallback,
): PreparedCallbackRegistration<WGPUCreateRenderPipelineAsyncCallback>

fun interface WGPUDawnStoreCacheDataCallback : Callback {
    fun invoke(
        keySize: ULong,
        key: NativeAddress?,
        valueSize: ULong,
        value: NativeAddress?,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUDawnStoreCacheDataCallbackType: CallbackType<WGPUDawnStoreCacheDataCallback> = CallbackType(
    canonicalId = "typedef:WGPUDawnStoreCacheDataCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUDawnStoreCacheDataCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDawnStoreCacheDataCallback,
): CallbackRegistration<WGPUDawnStoreCacheDataCallback>

@CallbackRuntimeApi
internal expect fun WGPUDawnStoreCacheDataCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDawnStoreCacheDataCallback,
): PreparedCallbackRegistration<WGPUDawnStoreCacheDataCallback>

fun interface WGPUDeviceLostCallback : Callback {
    fun invoke(
        device: NativeAddress?,
        reason: WGPUDeviceLostReason,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUDeviceLostCallbackType: CallbackType<WGPUDeviceLostCallback> = CallbackType(
    canonicalId = "typedef:WGPUDeviceLostCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUDeviceLostCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDeviceLostCallback,
): CallbackRegistration<WGPUDeviceLostCallback>

@CallbackRuntimeApi
internal expect fun WGPUDeviceLostCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDeviceLostCallback,
): PreparedCallbackRegistration<WGPUDeviceLostCallback>

fun interface WGPUDisposeCallback : Callback {
    fun invoke(status: WGPUCallbackStatus, userdata1: NativeAddress?)
    companion object
}

@CallbackRuntimeApi
internal val WGPUDisposeCallbackType: CallbackType<WGPUDisposeCallback> = CallbackType(
    canonicalId = "typedef:WGPUDisposeCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUDisposeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDisposeCallback,
): CallbackRegistration<WGPUDisposeCallback>

@CallbackRuntimeApi
internal expect fun WGPUDisposeCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUDisposeCallback,
): PreparedCallbackRegistration<WGPUDisposeCallback>

fun interface WGPULoggingCallback : Callback {
    fun invoke(
        type: WGPULoggingType,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPULoggingCallbackType: CallbackType<WGPULoggingCallback> = CallbackType(
    canonicalId = "typedef:WGPULoggingCallback",
    hasRoutingUserdata = true,
)

expect fun WGPULoggingCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPULoggingCallback,
): CallbackRegistration<WGPULoggingCallback>

@CallbackRuntimeApi
internal expect fun WGPULoggingCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPULoggingCallback,
): PreparedCallbackRegistration<WGPULoggingCallback>

fun interface WGPUPopErrorScopeCallback : Callback {
    fun invoke(
        status: WGPUPopErrorScopeStatus,
        type: WGPUErrorType,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUPopErrorScopeCallbackType: CallbackType<WGPUPopErrorScopeCallback> = CallbackType(
    canonicalId = "typedef:WGPUPopErrorScopeCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUPopErrorScopeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUPopErrorScopeCallback,
): CallbackRegistration<WGPUPopErrorScopeCallback>

@CallbackRuntimeApi
internal expect fun WGPUPopErrorScopeCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUPopErrorScopeCallback,
): PreparedCallbackRegistration<WGPUPopErrorScopeCallback>

fun interface WGPUQueueWorkDoneCallback : Callback {
    fun invoke(
        status: WGPUQueueWorkDoneStatus,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUQueueWorkDoneCallbackType: CallbackType<WGPUQueueWorkDoneCallback> = CallbackType(
    canonicalId = "typedef:WGPUQueueWorkDoneCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUQueueWorkDoneCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUQueueWorkDoneCallback,
): CallbackRegistration<WGPUQueueWorkDoneCallback>

@CallbackRuntimeApi
internal expect fun WGPUQueueWorkDoneCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUQueueWorkDoneCallback,
): PreparedCallbackRegistration<WGPUQueueWorkDoneCallback>

fun interface WGPURequestAdapterCallback : Callback {
    fun invoke(
        status: WGPURequestAdapterStatus,
        adapter: WGPUAdapter?,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPURequestAdapterCallbackType: CallbackType<WGPURequestAdapterCallback> = CallbackType(
    canonicalId = "typedef:WGPURequestAdapterCallback",
    hasRoutingUserdata = true,
)

expect fun WGPURequestAdapterCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPURequestAdapterCallback,
): CallbackRegistration<WGPURequestAdapterCallback>

@CallbackRuntimeApi
internal expect fun WGPURequestAdapterCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPURequestAdapterCallback,
): PreparedCallbackRegistration<WGPURequestAdapterCallback>

fun interface WGPURequestDeviceCallback : Callback {
    fun invoke(
        status: WGPURequestDeviceStatus,
        device: WGPUDevice?,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPURequestDeviceCallbackType: CallbackType<WGPURequestDeviceCallback> = CallbackType(
    canonicalId = "typedef:WGPURequestDeviceCallback",
    hasRoutingUserdata = true,
)

expect fun WGPURequestDeviceCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPURequestDeviceCallback,
): CallbackRegistration<WGPURequestDeviceCallback>

@CallbackRuntimeApi
internal expect fun WGPURequestDeviceCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPURequestDeviceCallback,
): PreparedCallbackRegistration<WGPURequestDeviceCallback>

fun interface WGPUUncapturedErrorCallback : Callback {
    fun invoke(
        device: NativeAddress?,
        type: WGPUErrorType,
        message: WGPUStringView,
        userdata1: NativeAddress?,
    )

    companion object
}

@CallbackRuntimeApi
internal val WGPUUncapturedErrorCallbackType: CallbackType<WGPUUncapturedErrorCallback> = CallbackType(
    canonicalId = "typedef:WGPUUncapturedErrorCallback",
    hasRoutingUserdata = true,
)

expect fun WGPUUncapturedErrorCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUUncapturedErrorCallback,
): CallbackRegistration<WGPUUncapturedErrorCallback>

@CallbackRuntimeApi
internal expect fun WGPUUncapturedErrorCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler = CallbackExceptionHandler.Default,
    callback: WGPUUncapturedErrorCallback,
): PreparedCallbackRegistration<WGPUUncapturedErrorCallback>

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUBufferMapCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUBufferMapCallback>,
    userdata1: NativeAddress? = null,
): WGPUBufferMapCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPURequestAdapterCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPURequestAdapterCallback>,
    userdata1: NativeAddress? = null,
): WGPURequestAdapterCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPURequestDeviceCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPURequestDeviceCallback>,
    userdata1: NativeAddress? = null,
): WGPURequestDeviceCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUCompilationInfoCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUCompilationInfoCallback>,
    userdata1: NativeAddress? = null,
): WGPUCompilationInfoCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUCreateComputePipelineAsyncCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUCreateComputePipelineAsyncCallback>,
    userdata1: NativeAddress? = null,
): WGPUCreateComputePipelineAsyncCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUCreateRenderPipelineAsyncCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUCreateRenderPipelineAsyncCallback>,
    userdata1: NativeAddress? = null,
): WGPUCreateRenderPipelineAsyncCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUQueueWorkDoneCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUQueueWorkDoneCallback>,
    userdata1: NativeAddress? = null,
): WGPUQueueWorkDoneCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUPopErrorScopeCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUPopErrorScopeCallback>,
    userdata1: NativeAddress? = null,
): WGPUPopErrorScopeCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUDeviceLostCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    mode: WGPUCallbackMode,
    registration: CallbackRegistration<WGPUDeviceLostCallback>,
    userdata1: NativeAddress? = null,
): WGPUDeviceLostCallbackInfo {
    require(
        mode == WGPUCallbackMode_WaitAnyOnly ||
            mode == WGPUCallbackMode_AllowProcessEvents ||
            mode == WGPUCallbackMode_AllowSpontaneous,
    )
    val info = allocate(allocator)
    info.mode = mode
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPUUncapturedErrorCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    registration: CallbackRegistration<WGPUUncapturedErrorCallback>,
    userdata1: NativeAddress? = null,
): WGPUUncapturedErrorCallbackInfo {
    val info = allocate(allocator)
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

/**
 * CONSUMED_DURING_CALL: the owning native call copies the callback-info value or containing descriptor, so the allocator scope may close after the call while the registration remains live.
 *
 * This factory does not own [registration].
 */
fun WGPULoggingCallbackInfo.Companion.allocate(
    allocator: MemoryAllocator,
    registration: CallbackRegistration<WGPULoggingCallback>,
    userdata1: NativeAddress? = null,
): WGPULoggingCallbackInfo {
    val info = allocate(allocator)
    info.callback = registration.callback
    info.userdata2 = registration.userdata
    info.userdata1 = userdata1
    return info
}

