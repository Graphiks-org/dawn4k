package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.gpuTestConfig
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.GPUTextureViewDimension
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import org.graphiks.webgpu.descriptors.TextureDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

class TextureBindingViewDimensionGpuTest {
    @Test
    fun compatibilityDeviceRejectsCubeBindingDimensionWithOnlyTwoLayers() = runTest {
        val options = RequestAdapterOptions(featureLevel = "compatibility")
        if (!gpuTestEnvironment("TextureBindingViewDimensionGpuTest.compatibilityDeviceRejectsCubeBindingDimensionWithOnlyTwoLayers", options)) return@runTest
        assertIs<GPUValidationError>(textureCreationError(options, layers = 2u))
    }

    @Test
    fun compatibilityDeviceAcceptsCubeBindingDimensionWithSixLayers() = runTest {
        val options = RequestAdapterOptions(featureLevel = "compatibility")
        if (!gpuTestEnvironment("TextureBindingViewDimensionGpuTest.compatibilityDeviceAcceptsCubeBindingDimensionWithSixLayers", options)) return@runTest
        assertNull(textureCreationError(options, layers = 6u))
    }

    @Test
    fun coreDeviceIgnoresTheCompatibilityBindingDimensionRestriction() = runTest {
        val options = RequestAdapterOptions(featureLevel = "core")
        if (!gpuTestEnvironment("TextureBindingViewDimensionGpuTest.coreDeviceIgnoresTheCompatibilityBindingDimensionRestriction", options)) return@runTest
        assertNull(textureCreationError(options, layers = 2u))
    }

    private suspend fun textureCreationError(options: RequestAdapterOptions, layers: UInt): GPUError? =
        DawnContext.create(gpuTestConfig()).useWithProgress { context ->
            context.requestAdapter(options).getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().use { device ->
                    assertEquals(options.featureLevel == "core", GPUFeatureName.CoreFeaturesAndLimits in device.features)
                    device.pushErrorScope(GPUErrorFilter.Validation)
                    device.createTexture(
                        TextureDescriptor(
                            size = Extent3D(width = 4u, height = 4u, depthOrArrayLayers = layers),
                            format = GPUTextureFormat.RGBA8Unorm,
                            usage = GPUTextureUsage.TextureBinding,
                            textureBindingViewDimension = GPUTextureViewDimension.Cube,
                        ),
                    ).use { }
                    device.popErrorScope().getOrThrow()
                }
            }
        }
}
