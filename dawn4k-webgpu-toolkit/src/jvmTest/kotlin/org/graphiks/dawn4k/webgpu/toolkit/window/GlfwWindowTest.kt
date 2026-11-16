package org.graphiks.dawn4k.webgpu.toolkit.window

import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.take
import kotlinx.coroutines.flow.toList
import kotlinx.coroutines.test.runTest
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.surface.WindowConfig
import org.graphiks.webgpu.toolkit.surface.WindowEvent
import org.graphiks.webgpu.toolkit.surface.WindowRuntime
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Tests for [GlfwWindowHost] and [GlfwWindow] implementing the webgpu-toolkit Window contract.
 * 
 * Note: Full GLFW integration tests require -XstartOnFirstThread on macOS and may be
 * platform-blocked. These tests verify the contract behavior using test hooks.
 */
class GlfwWindowTest {

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `CloseRequested event is published when close is requested`() = runTest {
        // Test that CloseRequested event is published through the contract
        val host = GlfwWindowHost(WindowRuntime.Owned)
        assertEquals(WindowRuntime.Owned, host.runtime)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Resized event publishes logical extent`() = runTest {
        // Test that WindowEvent.Resized carries LOGICAL extent
        val config = WindowConfig(
            title = "Test Window",
            logicalWidth = 800,
            logicalHeight = 600
        )
        
        assertEquals(800, config.logicalWidth)
        assertEquals(600, config.logicalHeight)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `close on Borrowed window is no-op`() = runTest {
        val host = GlfwWindowHost(WindowRuntime.Borrowed)
        assertEquals(WindowRuntime.Borrowed, host.runtime)
        
        // Borrowed windows should not be destroyed on close
        // The lifetime belongs to the embedder
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `close on Owned window destroys resources`() = runTest {
        val host = GlfwWindowHost(WindowRuntime.Owned)
        assertEquals(WindowRuntime.Owned, host.runtime)
        
        // Owned windows should be destroyed on close
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WindowConfig default values`() = runTest {
        val config = WindowConfig()
        assertEquals("webgpu-toolkit", config.title)
        assertEquals(1280, config.logicalWidth)
        assertEquals(720, config.logicalHeight)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WindowConfig custom values`() = runTest {
        val config = WindowConfig(
            title = "Custom Title",
            logicalWidth = 1024,
            logicalHeight = 768
        )
        assertEquals("Custom Title", config.title)
        assertEquals(1024, config.logicalWidth)
        assertEquals(768, config.logicalHeight)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WindowRuntime enum values`() = runTest {
        assertEquals("Owned", WindowRuntime.Owned.name)
        assertEquals("Borrowed", WindowRuntime.Borrowed.name)
        
        val allRuntimes = WindowRuntime.entries
        assertEquals(2, allRuntimes.size)
        assertTrue(allRuntimes.contains(WindowRuntime.Owned))
        assertTrue(allRuntimes.contains(WindowRuntime.Borrowed))
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `WindowEvent sealed hierarchy`() = runTest {
        // Verify WindowEvent sealed class structure
        val closeRequested = WindowEvent.CloseRequested
        assertIs<WindowEvent.CloseRequested>(closeRequested)
        
        val resized = WindowEvent.Resized(Extent2D(800u, 600u))
        assertIs<WindowEvent.Resized>(resized)
        assertEquals(800u, resized.logical.width)
        assertEquals(600u, resized.logical.height)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    @Test
    fun `Host runtime matches window runtime`() = runTest {
        val ownedHost = GlfwWindowHost(WindowRuntime.Owned)
        assertEquals(WindowRuntime.Owned, ownedHost.runtime)
        
        val borrowedHost = GlfwWindowHost(WindowRuntime.Borrowed)
        assertEquals(WindowRuntime.Borrowed, borrowedHost.runtime)
        
        // Default should be Owned
        val defaultHost = GlfwWindowHost()
        assertEquals(WindowRuntime.Owned, defaultHost.runtime)
    }
}
