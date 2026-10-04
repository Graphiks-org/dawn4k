package org.graphiks.dawn4k

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUTextureAspect
import org.graphiks.webgpu.GPUTextureDimension
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.SamplerDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.descriptors.TextureViewDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Real-GPU render resources: textures, texture views, samplers, render pipelines
 * and query sets, with a real offscreen render color oracle. Runs only through
 * the gpuTest* tasks; a host without an adapter fails these tests (no silent skip).
 */
class RenderResourcesGpuTest {

    @Test
    fun offscreenRenderHasAColorOracle() = runTest {
        val fixture = NativeFixture.open()
        try {
            assertContentEquals(byteArrayOf(-1, 0, 0, -1), fixture.renderPixel(GPUTextureFormat.RGBA8Unorm))
        } finally { fixture.close() }
    }

    @Test
    fun textureMetadataAndNullViewDescriptorAreCorrect() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createTexture(
                TextureDescriptor(
                    size = Extent3D(width = 8u, height = 4u),
                    format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.TextureBinding,
                    mipLevelCount = 2u,
                ),
            ).use { texture ->
                assertEquals(8u, texture.width)
                assertEquals(4u, texture.height)
                assertEquals(1u, texture.depthOrArrayLayers)
                assertEquals(2u, texture.mipLevelCount)
                assertEquals(1u, texture.sampleCount)
                assertEquals(GPUTextureDimension.TwoD, texture.dimension)
                assertEquals(GPUTextureFormat.RGBA8Unorm, texture.format)
                assertTrue(GPUTextureUsage.TextureBinding in texture.usage)

                // createView(null) adopts the C defaults and owns a reference that
                // close() releases back to the session — no leak.
                val before = fixture.session.resources.debugRemainingRefs()
                texture.createView(null).use {
                    assertEquals(before + 1, fixture.session.resources.debugRemainingRefs())
                }
                assertEquals(before, fixture.session.resources.debugRemainingRefs())
            }
        } finally { fixture.close() }
    }

    @Test
    fun depthStencilTextureAndAspectViewsAreCreated() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createTexture(
                TextureDescriptor(
                    size = Extent3D(width = 4u, height = 4u),
                    format = GPUTextureFormat.Depth24PlusStencil8,
                    usage = GPUTextureUsage.RenderAttachment,
                ),
            ).use { texture ->
                texture.createView(TextureViewDescriptor(aspect = GPUTextureAspect.DepthOnly)).use { }
                texture.createView(TextureViewDescriptor(aspect = GPUTextureAspect.StencilOnly)).use { }
            }
        } finally { fixture.close() }
    }

    @Test
    fun mipmappedAndLayeredTextureViewsAreCreated() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createTexture(
                TextureDescriptor(
                    size = Extent3D(width = 8u, height = 8u, depthOrArrayLayers = 4u),
                    format = GPUTextureFormat.RGBA8Unorm,
                    usage = GPUTextureUsage.TextureBinding,
                    mipLevelCount = 3u,
                ),
            ).use { texture ->
                assertEquals(4u, texture.depthOrArrayLayers)
                assertEquals(3u, texture.mipLevelCount)
                texture.createView(
                    TextureViewDescriptor(
                        baseMipLevel = 1u,
                        mipLevelCount = 2u,
                        baseArrayLayer = 1u,
                        arrayLayerCount = 2u,
                    ),
                ).use { }
            }
        } finally { fixture.close() }
    }

    @Test
    fun samplerCompareNullAndMaxAnisotropyAreCreated() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createSampler(SamplerDescriptor(compare = null, maxAnisotropy = 8u)).use { }
            fixture.createSampler(
                SamplerDescriptor(compare = GPUCompareFunction.Less, magFilter = GPUFilterMode.Linear),
            ).use { }
        } finally { fixture.close() }
    }

    @Test
    fun renderPipelineWithDepthStencilAndNullableDepthWriteIsCreated() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createShaderModule(ShaderModuleDescriptor(VERTEX_SHADER)).use { shader ->
                // depthWriteEnabled == null -> native Undefined (resolved by Dawn).
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        depthStencil = DepthStencilState(
                            format = GPUTextureFormat.Depth32Float,
                            depthWriteEnabled = null,
                            depthCompare = GPUCompareFunction.LessEqual,
                        ),
                    ),
                ).use { }
                // depthWriteEnabled == true -> native True.
                fixture.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        depthStencil = DepthStencilState(
                            format = GPUTextureFormat.Depth32Float,
                            depthWriteEnabled = true,
                            depthCompare = GPUCompareFunction.LessEqual,
                        ),
                    ),
                ).use { }
            }
        } finally { fixture.close() }
    }

    @Test
    fun occlusionQuerySetReportsTypeAndCount() = runTest {
        val fixture = NativeFixture.open()
        try {
            fixture.createQuerySet(QuerySetDescriptor(type = GPUQueryType.Occlusion, count = 2u)).use { querySet ->
                assertEquals(GPUQueryType.Occlusion, querySet.type)
                assertEquals(2u, querySet.count)
            }
        } finally { fixture.close() }
    }

    private companion object {
        val VERTEX_SHADER = """
            @vertex
            fn vs_main(@builtin(vertex_index) vertexIndex: u32) -> @builtin(position) vec4f {
                var positions = array<vec2f, 3>(
                    vec2f(-1.0, -1.0),
                    vec2f( 3.0, -1.0),
                    vec2f(-1.0,  3.0),
                );
                return vec4f(positions[vertexIndex], 0.0, 1.0);
            }
        """.trimIndent()
    }
}
