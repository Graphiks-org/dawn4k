package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.*
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUMapMode
import kotlin.test.Test
import kotlin.test.assertEquals

/** Guards the numeric compatibility required by direct native flag forwarding. */
class BufferFlagsTest {
    @Test
    fun mapModeValuesAndCombinationsMatchDawnConstants() {
        assertEquals(WGPUMapMode_None, GPUMapMode.None.value)
        assertEquals(WGPUMapMode_Read, GPUMapMode.Read.value)
        assertEquals(WGPUMapMode_Write, GPUMapMode.Write.value)
        assertEquals(WGPUMapMode_Read or WGPUMapMode_Write, (GPUMapMode.Read or GPUMapMode.Write).value)
    }

    @Test
    fun bufferUsageValuesAndEveryCombinationMatchDawnConstants() {
        val flags = listOf(
            GPUBufferUsage.MapRead to WGPUBufferUsage_MapRead,
            GPUBufferUsage.MapWrite to WGPUBufferUsage_MapWrite,
            GPUBufferUsage.CopySrc to WGPUBufferUsage_CopySrc,
            GPUBufferUsage.CopyDst to WGPUBufferUsage_CopyDst,
            GPUBufferUsage.Index to WGPUBufferUsage_Index,
            GPUBufferUsage.Vertex to WGPUBufferUsage_Vertex,
            GPUBufferUsage.Uniform to WGPUBufferUsage_Uniform,
            GPUBufferUsage.Storage to WGPUBufferUsage_Storage,
            GPUBufferUsage.Indirect to WGPUBufferUsage_Indirect,
            GPUBufferUsage.QueryResolve to WGPUBufferUsage_QueryResolve,
        )
        assertEquals(WGPUBufferUsage_None, GPUBufferUsage.None.value)
        flags.forEach { (flag, expected) -> assertEquals(expected, flag.value) }
        for (bits in 0 until (1 shl flags.size)) {
            val selected = flags.filterIndexed { index, _ -> bits and (1 shl index) != 0 }
            val usage = selected.fold(GPUBufferUsage.None) { acc, flag -> acc or flag.first }
            val expected = selected.fold(WGPUBufferUsage_None) { acc, flag -> acc or flag.second }
            assertEquals(expected, usage.value, "combination $bits")
        }
    }
}
