package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import org.graphiks.dawn4k.native.*
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.kffi.NativeAddress
import java.lang.foreign.Arena
import java.lang.foreign.ValueLayout.JAVA_INT

class SurfaceConfigurationTest {
    @Test fun replacementPreservesRequiredRgbaEvenWhenBgraIsPreferred() {
        val selected = selectSurfaceConfiguration(
            listOf(WGPUTextureFormat_BGRA8Unorm, WGPUTextureFormat_RGBA8Unorm),
            listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Fifo),
            requiredFormat = GPUTextureFormat.RGBA8Unorm,
        )
        assertEquals(GPUTextureFormat.RGBA8Unorm, selected.textureFormat)
    }

    @Test fun replacementRejectsUnsupportedRequiredFormatInsteadOfResettingScene() {
        assertFailsWith<IllegalStateException> {
            selectSurfaceConfiguration(listOf(WGPUTextureFormat_BGRA8Unorm),
                listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Fifo),
                requiredFormat = GPUTextureFormat.RGBA8Unorm)
        }
    }
    @Test fun capabilityMembersAreCopiedBeforeBeingFreed() {
        Arena.ofConfined().use { arena ->
            val values = arena.allocate(8, 4)
            values.set(JAVA_INT, 0, 23)
            values.set(JAVA_INT, 4, 19)
            val snapshot = readSurfaceEnums(NativeAddress(values.address()), 2uL)
            values.set(JAVA_INT, 0, 0)
            assertEquals(listOf(23u, 19u), snapshot)
        }
    }

    @Test fun rejectsInvalidCapabilityPointersAndUnboundedCounts() {
        assertEquals(emptyList(), readSurfaceEnums(null, 0uL))
        assertFailsWith<IllegalStateException> { readSurfaceEnums(null, 1uL) }
        assertFailsWith<IllegalStateException> { readSurfaceEnums(NativeAddress(1L), ULong.MAX_VALUE) }
    }

    @Test fun prefersBgraWithoutDependingOnAdvertisementOrder() {
        val config = selectSurfaceConfiguration(
            listOf(WGPUTextureFormat_RGBA8Unorm, WGPUTextureFormat_BGRA8Unorm),
            listOf(WGPUCompositeAlphaMode_Opaque, WGPUCompositeAlphaMode_Auto),
            listOf(WGPUPresentMode_Fifo))
        assertEquals(GPUTextureFormat.BGRA8Unorm, config.textureFormat)
        assertEquals(WGPUCompositeAlphaMode_Auto, config.alphaMode)
    }

    @Test fun acceptsRgbaOnlySurface() {
        val config = selectSurfaceConfiguration(listOf(WGPUTextureFormat_RGBA8Unorm),
            listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Fifo))
        assertEquals(GPUTextureFormat.RGBA8Unorm, config.textureFormat)
        assertEquals(WGPUTextureFormat_RGBA8Unorm, config.nativeFormat)
        assertEquals(WGPUCompositeAlphaMode_Opaque, config.alphaMode)
    }

    @Test fun acceptsAdvertisedPremultipliedAlphaWhenOpaqueIsUnavailable() {
        val config = selectSurfaceConfiguration(listOf(WGPUTextureFormat_BGRA8Unorm),
            listOf(WGPUCompositeAlphaMode_Premultiplied), listOf(WGPUPresentMode_Fifo))
        assertEquals(WGPUCompositeAlphaMode_Premultiplied, config.alphaMode)
    }

    @Test fun rejectsUnsupportedAndEmptyCapabilities() {
        for (formats in listOf(emptyList(), listOf(WGPUTextureFormat_Depth32Float))) {
            assertFailsWith<IllegalStateException> {
                selectSurfaceConfiguration(formats, listOf(WGPUCompositeAlphaMode_Opaque),
                    listOf(WGPUPresentMode_Fifo))
            }
        }
        assertFailsWith<IllegalStateException> {
            selectSurfaceConfiguration(listOf(WGPUTextureFormat_BGRA8Unorm),
                emptyList(), listOf(WGPUPresentMode_Fifo))
        }
        assertFailsWith<IllegalStateException> {
            selectSurfaceConfiguration(listOf(WGPUTextureFormat_BGRA8Unorm),
                listOf(WGPUCompositeAlphaMode_Opaque), listOf(WGPUPresentMode_Immediate))
        }
    }
}
