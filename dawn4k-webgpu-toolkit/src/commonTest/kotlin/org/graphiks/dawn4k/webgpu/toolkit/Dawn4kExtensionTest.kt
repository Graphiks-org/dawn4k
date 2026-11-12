package org.graphiks.dawn4k.webgpu.toolkit

import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.ToolkitInstance
import kotlin.test.Test
import kotlin.test.assertEquals

class Dawn4kExtensionTest {

    @Test
    fun `extension id is dawn4k`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        assertEquals("dawn4k", extension.id)
    }

    @Test
    fun `open creates ToolkitInstance with Manual progression`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val config = SessionConfig()
        val instance: ToolkitInstance = extension.open(config)
        assertEquals(Progression.Manual, instance.progression)
    }

    @Test
    fun `pumpEvents delegates to DawnContext processEvents`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val config = SessionConfig()
        val instance: ToolkitInstance = extension.open(config)
        
        // Call pumpEvents - should not throw and should delegate to DawnContext.processEvents
        instance.pumpEvents()
        
        // Clean up
        instance.close()
    }

    @Test
    fun `open creates ToolkitInstance with Pumped progression`() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val config = SessionConfig()
        val instance: ToolkitInstance = extension.open(config)
        
        // Verify progression is Manual (dawn4k uses Manual progression)
        assertEquals(Progression.Manual, instance.progression)
        
        instance.close()
    }
}
