@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.cinterop.toKString
import kotlinx.coroutines.*
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.glfw.internal.NativeGlfwBackend
import org.graphiks.dawn4k.toolkit.glfw.internal.GlfwPlatform
import platform.posix.getenv
import kotlin.test.*

class DawnNativeWindowTest {
    @Test fun nativeWindowUsesSameLifecycleContract() {
        val activation = getenv("DAWN_DESKTOP_TESTS")?.toKString()
        println("Native GLFW acceptance flag in executable: $activation")
        if (activation != "1") return
        val backendName = getenv("DAWN_TEST_BACKEND")?.toKString()
            ?: if (kotlin.native.Platform.osFamily == kotlin.native.OsFamily.MACOSX) "Metal" else "Vulkan"
        val options = DawnSessionOptions(config = DawnConfig(DawnBackend.valueOf(backendName)))
        DawnMainThreadLoop.run { main ->
            val backend = main.backend as NativeGlfwBackend
            val first = openDawnWindow(DawnWindowOptions(width = 64, height = 64), options, mainThread = main)
            val second = openDawnWindow(DawnWindowOptions(width = 64, height = 64), options, mainThread = main)
            try {
                assertEquals(2, backend.liveWindowCount)
                assertTrue(first.extent.value.isPositive)
                first.shutdown()
                assertEquals(1, backend.liveWindowCount)
                assertTrue(second.presentation.withFrame { it.present() })
                second.gpu.run { gpu ->
                    println("TOOLKIT_GLFW backend=$backendName required=${getenv("DAWN_REQUIRE_ADAPTER")?.toKString() ?: "1"} device=${gpu.adapter.info.device}")
                }
                val kind = when (backend.targetPlatform(second.nativeHandle)) {
                    GlfwPlatform.Cocoa -> "metal"
                    GlfwPlatform.Win32 -> "win32"
                    GlfwPlatform.X11 -> "x11"
                    GlfwPlatform.Wayland -> "wayland"
                }
                println("TOOLKIT_PRESENTATION kind=$kind")
                second.requestClose()
            } finally { first.shutdown(); second.shutdown() }
            assertEquals(0, backend.liveWindowCount)
            assertFalse(main.runtime.isActive)
            println("Native GLFW acceptance completed: two windows retired after GPU presentation")
        }
    }
}
