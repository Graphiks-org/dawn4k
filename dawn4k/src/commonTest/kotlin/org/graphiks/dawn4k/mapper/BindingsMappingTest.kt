package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUBufferBindingType_BindingNotUsed
import org.graphiks.dawn4k.native.WGPUBufferBindingType_ReadOnlyStorage
import org.graphiks.dawn4k.native.WGPUBufferBindingType_Storage
import org.graphiks.dawn4k.native.WGPUBufferBindingType_Uniform
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Error
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Force32
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Info
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Warning
import org.graphiks.dawn4k.native.WGPUComputeState
import org.graphiks.dawn4k.native.WGPUShaderStage_Compute
import org.graphiks.dawn4k.native.WGPUShaderStage_Fragment
import org.graphiks.dawn4k.native.WGPUShaderStage_None
import org.graphiks.dawn4k.native.WGPUShaderStage_Vertex
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUCompilationMessageType
import org.graphiks.webgpu.GPUShaderStage
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Pure mapping tests: every conversion table maps its Kotlin value through its
 * named Dawn constant (never assuming the numeric values coincide), and the
 * compute-state builder keeps UTF-8 keys and nullable entry points inside the
 * consumed allocator scope with bit-exact f64 constant values.
 */
class BindingsMappingTest {

    @Test
    fun shaderStageTableMapsEveryFlagIndividually() {
        assertEquals(WGPUShaderStage_None, GPUShaderStage.None.toNativeShaderStage())
        assertEquals(WGPUShaderStage_Vertex, GPUShaderStage.Vertex.toNativeShaderStage())
        assertEquals(WGPUShaderStage_Fragment, GPUShaderStage.Fragment.toNativeShaderStage())
        assertEquals(WGPUShaderStage_Compute, GPUShaderStage.Compute.toNativeShaderStage())
    }

    @Test
    fun shaderStageTableCombinesFlags() {
        val combined = GPUShaderStage.Vertex or GPUShaderStage.Fragment
        assertEquals(
            WGPUShaderStage_Vertex or WGPUShaderStage_Fragment,
            combined.toNativeShaderStage(),
        )
    }

    @Test
    fun bufferBindingTypeTableIsExhaustive() {
        assertEquals(WGPUBufferBindingType_BindingNotUsed, GPUBufferBindingType.BindingNotUsed.toNativeBufferBindingType())
        assertEquals(WGPUBufferBindingType_Uniform, GPUBufferBindingType.Uniform.toNativeBufferBindingType())
        assertEquals(WGPUBufferBindingType_Storage, GPUBufferBindingType.Storage.toNativeBufferBindingType())
        assertEquals(WGPUBufferBindingType_ReadOnlyStorage, GPUBufferBindingType.ReadOnlyStorage.toNativeBufferBindingType())
    }

    @Test
    fun compilationMessageTypeTableIsExhaustive() {
        assertEquals(GPUCompilationMessageType.Error, WGPUCompilationMessageType_Error.toCompilationMessageType())
        assertEquals(GPUCompilationMessageType.Warning, WGPUCompilationMessageType_Warning.toCompilationMessageType())
        assertEquals(GPUCompilationMessageType.Info, WGPUCompilationMessageType_Info.toCompilationMessageType())
        assertFailsWith<IllegalStateException> { WGPUCompilationMessageType_Force32.toCompilationMessageType() }
    }

    @Test
    fun constantValuesAreBitExactDoubles() = memoryScope { allocator ->
        val state = WGPUComputeState.allocate(allocator)
        // 1.0 / 3.0 is not exactly representable in binary: a bit-exact passthrough
        // must round-trip the f64 bits, never go through Float or string formatting.
        val value = 1.0 / 3.0
        allocator.initComputeState(state, entryPoint = "main", constants = mapOf("scale" to value))
        assertEquals(1uL, state.constantCount)
        val entry = state.constants ?: error("the constant entry array was not allocated")
        assertEquals("scale", entry.key.data?.toKString())
        assertEquals(value, entry.value)
        assertEquals(value.toRawBits(), entry.value.toRawBits())
    }

    @Test
    fun nullEntryPointProducesAnEmptyStringView() = memoryScope { allocator ->
        val state = WGPUComputeState.allocate(allocator)
        allocator.initComputeState(state, entryPoint = null, constants = emptyMap())
        assertNull(state.entryPoint.data)
        assertEquals(WGPU_STRLEN, state.entryPoint.length)
        assertEquals(0uL, state.constantCount)
        assertNull(state.constants)
    }

    @Test
    fun utf8KeysAndEntryPointRoundTrip() = memoryScope { allocator ->
        val state = WGPUComputeState.allocate(allocator)
        allocator.initComputeState(state, entryPoint = "ma\u00EFn", constants = mapOf("cl\u00E9" to 1.5))
        assertEquals("ma\u00EFn", state.entryPoint.data?.toKString())
        val entry = state.constants ?: error("the constant entry array was not allocated")
        assertEquals("cl\u00E9", entry.key.data?.toKString())
    }
}
