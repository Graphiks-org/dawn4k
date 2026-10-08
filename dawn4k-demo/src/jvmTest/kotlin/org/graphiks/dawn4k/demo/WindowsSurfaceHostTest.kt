package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

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
            Arena.ofConfined().use { arena ->
                val user32 = SymbolLookup.libraryLookup("user32.dll", arena)
                val linker = Linker.nativeLinker()
                val findChild = linker.downcallHandle(user32.find("FindWindowExW").orElseThrow(),
                    FunctionDescriptor.of(ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS))
                val className = arena.allocate(14L, 2L)
                "STATIC\u0000".forEachIndexed { i, c -> className.set(JAVA_CHAR, i * 2L, c) }
                val child = findChild.invokeWithArguments(MemorySegment.ofAddress(parent),
                    MemorySegment.NULL, className, MemorySegment.NULL) as MemorySegment
                assertTrue(child != MemorySegment.NULL, "the native viewport must exist")
                val post = linker.downcallHandle(user32.find("PostMessageW").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT, JAVA_LONG, JAVA_LONG))
                val visible = linker.downcallHandle(user32.find("IsWindowVisible").orElseThrow(),
                    FunctionDescriptor.of(JAVA_INT, ADDRESS))
                assertTrue(visible.invokeWithArguments(child) as Int != 0)
                // DefWindowProc removes WS_VISIBLE when it processes WM_SETREDRAW(false).
                // A posted message must be consumed even without a GPU render loop.
                assertTrue(post.invokeWithArguments(child, 0x000B, 0L, 0L) as Int != 0)
                val deadline = System.nanoTime() + 2_000_000_000L
                while (visible.invokeWithArguments(child) as Int != 0 && System.nanoTime() < deadline) {
                    Thread.sleep(20)
                }
                assertEquals(0, visible.invokeWithArguments(child) as Int,
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
