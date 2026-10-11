package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.graphiks.kffi.win32.generated.FindWindowExW
import org.graphiks.kffi.win32.generated.IsWindowVisible
import org.graphiks.kffi.win32.generated.PostMessageW
import org.graphiks.kffi.win32.generated.init

class WindowsSurfaceHostTest {
    @Test
    fun nativeViewportRespondsToWindowsMessagesWithoutTheGpuLoop() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        lateinit var window: ComposeWindow
        var host: WindowsSurfaceHost? = null
        var parent = 0L
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                window.setSize(500, 400)
                window.setContent {}
                window.isVisible = true
                parent = window.windowHandle
                host = WindowsSurfaceHost.attach(parent, AtomicReference(Rectangle(20, 30, 200, 150)))
                assertEquals(200 to 150, host.pixelSize())
            }
            init()
            Arena.ofConfined().use { arena ->
                val className = arena.allocate(14L, 2L)
                "STATIC\u0000".forEachIndexed { i, c -> className.set(JAVA_CHAR, i * 2L, c) }
                val child = FindWindowExW(MemorySegment.ofAddress(parent),
                    MemorySegment.NULL, className, MemorySegment.NULL)
                assertTrue(child != MemorySegment.NULL, "the native viewport must exist")
                assertTrue(IsWindowVisible(child) != 0)
                // DefWindowProc removes WS_VISIBLE when it processes WM_SETREDRAW(false).
                // A posted message must be consumed even without a GPU render loop.
                assertTrue(PostMessageW(child, 0x000B, 0L, 0L) != 0)
                val deadline = System.nanoTime() + 2_000_000_000L
                while (IsWindowVisible(child) != 0 && System.nanoTime() < deadline) {
                    Thread.sleep(20)
                }
                assertEquals(0, IsWindowVisible(child),
                    "the native viewport must process posted Windows messages")
            }
        } finally {
            SwingUtilities.invokeAndWait {
                host?.close()
                window.dispose()
            }
        }
    }
}
