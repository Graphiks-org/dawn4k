@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.cinterop.toKString
import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.glfw.internal.NativeGlfwBackend
import platform.posix.getenv
import kotlin.test.*

class DawnNativeWindowTest {
    @Test fun nativeWindowUsesSameLifecycleContract() {
        val activation = getenv("DAWN_DESKTOP_TESTS")?.toKString()
        println("Native GLFW acceptance flag in executable: $activation")
        if (activation != "1") return
        DawnMainThreadLoop.run { main ->
            val backend = main.backend as NativeGlfwBackend
            val first = openDawnWindow(DawnWindowOptions(width = 64, height = 64), mainThread = main)
            val second = openDawnWindow(DawnWindowOptions(width = 64, height = 64), mainThread = main)
            try {
                assertEquals(2, backend.liveWindowCount)
                assertTrue(first.extent.value.isPositive)
                first.shutdown()
                assertEquals(1, backend.liveWindowCount)
                assertTrue(second.presentation.withFrame { it.present() })
                second.requestClose()
            } finally { first.shutdown(); second.shutdown() }
            assertEquals(0, backend.liveWindowCount)
            assertFalse(main.runtime.isActive)
            println("Native GLFW acceptance completed: two windows retired after GPU presentation")
        }
    }
}
