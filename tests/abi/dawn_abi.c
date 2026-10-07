/*
 * ABI oracle for the generated Dawn (WebGPU) bindings.
 *
 * Prints sizes, alignments and field offsets measured from the real Dawn header.
 * The build compiles this with `cc -std=c11 -I<dawn-include>` and runs it on the
 * host; the output is compared against the layouts baked into the generated
 * bindings. Values are never hardcoded here: everything comes from the header.
 *
 * Compile with -DDAWN_ABI_NO_MAIN to build it as a helper library instead.
 */
#include <stddef.h>
#include <stdio.h>
#include <dawn/webgpu.h>

#define TYPE(T) printf("%s.size=%zu\n%s.align=%zu\n", #T, sizeof(T), #T, _Alignof(T))
#define FIELD(T, F) printf("%s.%s.offset=%zu\n", #T, #F, offsetof(T, F))

#define CALLBACK_INFO_FIELDS(T) \
    FIELD(T, nextInChain);      \
    FIELD(T, mode);             \
    FIELD(T, callback);         \
    FIELD(T, userdata1);        \
    FIELD(T, userdata2)

#define CALLBACK_INFO_FIELDS_NO_MODE(T) \
    FIELD(T, nextInChain);              \
    FIELD(T, callback);                 \
    FIELD(T, userdata1);                \
    FIELD(T, userdata2)

#ifndef DAWN_ABI_NO_MAIN
int main(void) {
    printf("pointerSize=%zu\n", sizeof(void *));

    TYPE(WGPUStringView);
    FIELD(WGPUStringView, data);
    FIELD(WGPUStringView, length);

    TYPE(WGPUFuture);
    FIELD(WGPUFuture, id);

    TYPE(WGPUChainedStruct);
    FIELD(WGPUChainedStruct, next);
    FIELD(WGPUChainedStruct, sType);

    TYPE(WGPUSurfaceSourceXlibWindow);
    FIELD(WGPUSurfaceSourceXlibWindow, chain);
    FIELD(WGPUSurfaceSourceXlibWindow, display);
    FIELD(WGPUSurfaceSourceXlibWindow, window);

    TYPE(WGPUSurfaceCapabilities);
    FIELD(WGPUSurfaceCapabilities, nextInChain);
    FIELD(WGPUSurfaceCapabilities, usages);
    FIELD(WGPUSurfaceCapabilities, formatCount);
    FIELD(WGPUSurfaceCapabilities, formats);
    FIELD(WGPUSurfaceCapabilities, presentModeCount);
    FIELD(WGPUSurfaceCapabilities, presentModes);
    FIELD(WGPUSurfaceCapabilities, alphaModeCount);
    FIELD(WGPUSurfaceCapabilities, alphaModes);

    TYPE(WGPUSurfaceConfiguration);
    FIELD(WGPUSurfaceConfiguration, nextInChain);
    FIELD(WGPUSurfaceConfiguration, device);
    FIELD(WGPUSurfaceConfiguration, format);
    FIELD(WGPUSurfaceConfiguration, usage);
    FIELD(WGPUSurfaceConfiguration, width);
    FIELD(WGPUSurfaceConfiguration, height);
    FIELD(WGPUSurfaceConfiguration, viewFormatCount);
    FIELD(WGPUSurfaceConfiguration, viewFormats);
    FIELD(WGPUSurfaceConfiguration, alphaMode);
    FIELD(WGPUSurfaceConfiguration, presentMode);

    TYPE(WGPUBufferDescriptor);
    FIELD(WGPUBufferDescriptor, nextInChain);
    FIELD(WGPUBufferDescriptor, label);
    FIELD(WGPUBufferDescriptor, usage);
    FIELD(WGPUBufferDescriptor, size);
    FIELD(WGPUBufferDescriptor, mappedAtCreation);

    TYPE(WGPUTextureDescriptor);
    FIELD(WGPUTextureDescriptor, nextInChain);
    FIELD(WGPUTextureDescriptor, label);
    FIELD(WGPUTextureDescriptor, usage);
    FIELD(WGPUTextureDescriptor, dimension);
    FIELD(WGPUTextureDescriptor, size);
    FIELD(WGPUTextureDescriptor, format);
    FIELD(WGPUTextureDescriptor, mipLevelCount);
    FIELD(WGPUTextureDescriptor, sampleCount);
    FIELD(WGPUTextureDescriptor, viewFormatCount);
    FIELD(WGPUTextureDescriptor, viewFormats);

    TYPE(WGPURenderPipelineDescriptor);
    FIELD(WGPURenderPipelineDescriptor, nextInChain);
    FIELD(WGPURenderPipelineDescriptor, label);
    FIELD(WGPURenderPipelineDescriptor, layout);
    FIELD(WGPURenderPipelineDescriptor, vertex);
    FIELD(WGPURenderPipelineDescriptor, primitive);
    FIELD(WGPURenderPipelineDescriptor, depthStencil);
    FIELD(WGPURenderPipelineDescriptor, multisample);
    FIELD(WGPURenderPipelineDescriptor, fragment);

    TYPE(WGPUDeviceDescriptor);
    FIELD(WGPUDeviceDescriptor, nextInChain);
    FIELD(WGPUDeviceDescriptor, label);
    FIELD(WGPUDeviceDescriptor, requiredFeatureCount);
    FIELD(WGPUDeviceDescriptor, requiredFeatures);
    FIELD(WGPUDeviceDescriptor, requiredLimits);
    FIELD(WGPUDeviceDescriptor, defaultQueue);
    FIELD(WGPUDeviceDescriptor, deviceLostCallbackInfo);
    FIELD(WGPUDeviceDescriptor, uncapturedErrorCallbackInfo);

    TYPE(WGPUTextureComponentSwizzleDescriptor);
    FIELD(WGPUTextureComponentSwizzleDescriptor, chain);
    FIELD(WGPUTextureComponentSwizzleDescriptor, swizzle);

    TYPE(WGPUCompatibilityModeLimits);
    FIELD(WGPUCompatibilityModeLimits, chain);
    FIELD(WGPUCompatibilityModeLimits, maxStorageBuffersInVertexStage);
    FIELD(WGPUCompatibilityModeLimits, maxStorageTexturesInVertexStage);
    FIELD(WGPUCompatibilityModeLimits, maxStorageBuffersInFragmentStage);
    FIELD(WGPUCompatibilityModeLimits, maxStorageTexturesInFragmentStage);

    TYPE(WGPULimits);
    FIELD(WGPULimits, nextInChain);
    FIELD(WGPULimits, maxTextureDimension1D);
    FIELD(WGPULimits, maxTextureDimension2D);
    FIELD(WGPULimits, maxTextureDimension3D);
    FIELD(WGPULimits, maxTextureArrayLayers);
    FIELD(WGPULimits, maxBindGroups);
    FIELD(WGPULimits, maxBindGroupsPlusVertexBuffers);
    FIELD(WGPULimits, maxBindingsPerBindGroup);
    FIELD(WGPULimits, maxDynamicUniformBuffersPerPipelineLayout);
    FIELD(WGPULimits, maxDynamicStorageBuffersPerPipelineLayout);
    FIELD(WGPULimits, maxSampledTexturesPerShaderStage);
    FIELD(WGPULimits, maxSamplersPerShaderStage);
    FIELD(WGPULimits, maxStorageBuffersPerShaderStage);
    FIELD(WGPULimits, maxStorageTexturesPerShaderStage);
    FIELD(WGPULimits, maxUniformBuffersPerShaderStage);
    FIELD(WGPULimits, maxUniformBufferBindingSize);
    FIELD(WGPULimits, maxStorageBufferBindingSize);
    FIELD(WGPULimits, minUniformBufferOffsetAlignment);
    FIELD(WGPULimits, minStorageBufferOffsetAlignment);
    FIELD(WGPULimits, maxVertexBuffers);
    FIELD(WGPULimits, maxBufferSize);
    FIELD(WGPULimits, maxVertexAttributes);
    FIELD(WGPULimits, maxVertexBufferArrayStride);
    FIELD(WGPULimits, maxInterStageShaderVariables);
    FIELD(WGPULimits, maxColorAttachments);
    FIELD(WGPULimits, maxColorAttachmentBytesPerSample);
    FIELD(WGPULimits, maxComputeWorkgroupStorageSize);
    FIELD(WGPULimits, maxComputeInvocationsPerWorkgroup);
    FIELD(WGPULimits, maxComputeWorkgroupSizeX);
    FIELD(WGPULimits, maxComputeWorkgroupSizeY);
    FIELD(WGPULimits, maxComputeWorkgroupSizeZ);
    FIELD(WGPULimits, maxComputeWorkgroupsPerDimension);
    FIELD(WGPULimits, maxImmediateSize);

    /* Every CallbackInfo from the callback contract, plus the raw Dawn ones. */
    TYPE(WGPUBufferMapCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUBufferMapCallbackInfo);

    TYPE(WGPUCompilationInfoCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUCompilationInfoCallbackInfo);

    TYPE(WGPUCreateComputePipelineAsyncCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUCreateComputePipelineAsyncCallbackInfo);

    TYPE(WGPUCreateRenderPipelineAsyncCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUCreateRenderPipelineAsyncCallbackInfo);

    TYPE(WGPUPopErrorScopeCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUPopErrorScopeCallbackInfo);

    TYPE(WGPUQueueWorkDoneCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUQueueWorkDoneCallbackInfo);

    TYPE(WGPURequestAdapterCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPURequestAdapterCallbackInfo);

    TYPE(WGPURequestDeviceCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPURequestDeviceCallbackInfo);

    TYPE(WGPUDeviceLostCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUDeviceLostCallbackInfo);

    TYPE(WGPUUncapturedErrorCallbackInfo);
    CALLBACK_INFO_FIELDS_NO_MODE(WGPUUncapturedErrorCallbackInfo);

    TYPE(WGPULoggingCallbackInfo);
    CALLBACK_INFO_FIELDS_NO_MODE(WGPULoggingCallbackInfo);

    TYPE(WGPUDawnLoadCacheDataCallbackInfo);
    CALLBACK_INFO_FIELDS_NO_MODE(WGPUDawnLoadCacheDataCallbackInfo);

    TYPE(WGPUDawnStoreCacheDataCallbackInfo);
    CALLBACK_INFO_FIELDS_NO_MODE(WGPUDawnStoreCacheDataCallbackInfo);

    TYPE(WGPUDisposeCallbackInfo);
    CALLBACK_INFO_FIELDS(WGPUDisposeCallbackInfo);

    return 0;
}
#endif

#ifdef DAWN_ABI_NO_MAIN
/*
 * Test helper: receives a CallbackInfo BY VALUE and invokes its callback with a
 * UTF-8 WGPUStringView whose explicit length covers an embedded NUL. The callback
 * must use the explicit length, never a NUL scan. It does not simulate GPU validation.
 */
int dawn_abi_invoke_buffer_map(WGPUBufferMapCallbackInfo info, int status) {
    static const char message[] = {'a', 'b', '\0', 'c', 'd'};
    WGPUStringView view;
    view.data = message;
    view.length = 5;
    info.callback((WGPUMapAsyncStatus) status, view, info.userdata1, info.userdata2);
    return 1;
}
#endif
