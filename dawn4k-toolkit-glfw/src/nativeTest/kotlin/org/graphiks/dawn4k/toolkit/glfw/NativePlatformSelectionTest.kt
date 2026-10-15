@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.cinterop.toKString
import org.graphiks.dawn4k.toolkit.glfw.internal.*
import org.graphiks.dawn4k.toolkit.glfw.c.*
import platform.posix.getenv
import kotlin.test.*

class NativePlatformSelectionTest {
    @Test fun activeWaylandCreatesWaylandTarget() {
        if (getenv("DAWN_WAYLAND_TESTS")?.toKString() != "1") return
        assertNotNull(getenv("DISPLAY"), "exercise selection even when X11 is also advertised")
        assertNotNull(getenv("WAYLAND_DISPLAY"))
        glfwInitHint(GLFW_PLATFORM, GLFW_PLATFORM_WAYLAND)
        try { DawnMainThreadLoop.run { main ->
            val window = openDawnWindow(DawnWindowOptions(width = 64, height = 64), mainThread = main)
            try {
                assertEquals(GlfwPlatform.Wayland, (main.backend as NativeGlfwBackend).targetPlatform(window.nativeHandle))
                assertTrue(window.presentation.withFrame { it.present() })
            } finally { window.shutdown() }
        } } finally { glfwInitHint(GLFW_PLATFORM, GLFW_ANY_PLATFORM) }
    }
}
