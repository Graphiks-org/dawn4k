package org.graphiks.dawn4k

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

class MappedRangeContractGpuTest {
    @Test
    fun partialMappingUsesAbsoluteOffsets() = runBlocking {
        if (!gpuTestEnvironment("MappedRangeContractGpuTest.partialMappingUsesAbsoluteOffsets")) return@runBlocking
        NativeFixture.open().use { fixture ->
            fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite)).use { buffer ->
                buffer.mapAsync(GPUMapMode.Write, 8uL, 8uL).getOrThrow()
                assertFailsWith<IllegalArgumentException> { buffer.getMappedRange(0uL, 8uL) }
                assertFailsWith<IllegalArgumentException> { buffer.getMappedRange() }
                assertEquals(8uL, buffer.getMappedRange(8uL).size)
                buffer.unmap()
            }
        }
    }

    @Test
    fun omittedSizeExtendsToBufferEndNotMappingEnd() = runBlocking {
        if (!gpuTestEnvironment("MappedRangeContractGpuTest.omittedSizeExtendsToBufferEndNotMappingEnd")) return@runBlocking
        NativeFixture.open().use { fixture ->
            fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite)).use { buffer ->
                buffer.mapAsync(GPUMapMode.Write, 8uL, 4uL).getOrThrow()
                assertFailsWith<IllegalArgumentException> { buffer.getMappedRange(8uL) }
                assertFailsWith<IllegalArgumentException> { buffer.getMappedRange(8uL, 8uL) }
                assertEquals(4uL, buffer.getMappedRange(8uL, 4uL).size)
                buffer.unmap()
            }
        }
    }

    @Test
    fun overlappingViewsThrowWithoutScopeErrorAndUnmapResetsViews() = runBlocking {
        if (!gpuTestEnvironment("MappedRangeContractGpuTest.overlappingViewsThrowWithoutScopeErrorAndUnmapResetsViews")) return@runBlocking
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            fixture.createBuffer(BufferDescriptor(32uL, GPUBufferUsage.MapWrite, mappedAtCreation = true)).use { buffer ->
                device.pushErrorScope(GPUErrorFilter.Validation)
                try {
                    assertEquals(16uL, buffer.getMappedRange(0uL, 16uL).size)
                    assertFailsWith<IllegalArgumentException> { buffer.getMappedRange(8uL, 16uL) }
                    assertFailsWith<IllegalArgumentException> { buffer.getMappedRange(0uL, 16uL) }
                    assertEquals(16uL, buffer.getMappedRange(16uL, 16uL).size)
                } finally {
                    assertNull(device.popErrorScope().getOrThrow())
                }
                buffer.unmap()
                buffer.mapAsync(GPUMapMode.Write).getOrThrow()
                assertEquals(32uL, buffer.getMappedRange().size)
                buffer.unmap()
            }
        }
    }
}
