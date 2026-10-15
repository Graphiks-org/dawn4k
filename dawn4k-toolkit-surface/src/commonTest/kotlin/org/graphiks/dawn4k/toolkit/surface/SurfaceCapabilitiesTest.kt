package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.dawn4k.toolkit.surface.internal.*
import org.graphiks.webgpu.GPUTextureFormat
import kotlin.test.*

class SurfaceCapabilitiesTest {
    private val advertised = SurfaceCapabilities(
        listOf(GPUTextureFormat.RGBA8Unorm), listOf(DawnAlphaMode.Opaque), listOf(DawnPresentMode.Immediate),
    )
    @Test fun choosesOnlyAdvertisedCapabilities() {
        val chosen = advertised.select(DawnPresentationOptions(), PixelExtent(64, 32))
        assertEquals(GPUTextureFormat.RGBA8Unorm, chosen.format)
        assertEquals(DawnAlphaMode.Opaque, chosen.alphaMode)
        assertEquals(DawnPresentMode.Immediate, chosen.presentMode)
    }
    @Test fun requiredFormatCannotSilentlyFallback() {
        assertFailsWith<IllegalArgumentException> {
            advertised.select(DawnPresentationOptions(requiredFormat = GPUTextureFormat.BGRA8Unorm), PixelExtent(1, 1))
        }
    }
    @Test fun allOptionalPreferencesCanFallback() {
        val chosen = advertised.select(DawnPresentationOptions(
            preferredFormats = emptyList(), preferredAlphaModes = emptyList(), preferredPresentModes = emptyList(),
        ), PixelExtent(0, 0))
        assertEquals(GPUTextureFormat.RGBA8Unorm, chosen.format)
        assertEquals(PixelExtent(0, 0), chosen.extent)
    }
    @Test fun requiredModesCannotSilentlyFallback() {
        assertFailsWith<IllegalArgumentException> {
            advertised.select(DawnPresentationOptions(requiredPresentMode = DawnPresentMode.Fifo), PixelExtent(1, 1))
        }
        assertFailsWith<IllegalArgumentException> {
            advertised.select(DawnPresentationOptions(requiredAlphaMode = DawnAlphaMode.Premultiplied), PixelExtent(1, 1))
        }
    }
}
