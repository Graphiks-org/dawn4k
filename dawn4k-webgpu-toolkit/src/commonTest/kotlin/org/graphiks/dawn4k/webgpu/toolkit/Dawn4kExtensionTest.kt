package org.graphiks.dawn4k.webgpu.toolkit

import org.graphiks.webgpu.toolkit.Progression
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Contract tests for [Dawn4kWebGpuToolkitExtension] that do not need a GPU.
 *
 * Opening an instance creates a real Dawn context, which loads the native runtime and
 * therefore belongs to the GPU-backed suite, not to these portable checks.
 */
class Dawn4kExtensionTest {

    @Test
    fun `extension id is dawn4k`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        assertEquals("dawn4k", extension.id)
    }

    @Test
    fun `extension is the WebGPU toolkit SPI entry point`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        assertEquals("dawn4k", extension.id)
        assertEquals(Progression.Pumped, Progression.valueOf("Pumped"))
    }
}
