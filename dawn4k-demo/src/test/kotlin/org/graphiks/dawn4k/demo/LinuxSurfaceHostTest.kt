package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import kotlinx.coroutines.CancellationException

class LinuxSurfaceHostTest {
    @Test fun missingDisplayReportsTheDesktopLauncher() {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_MISSING_DISPLAY_TEST") != "1") return
        assertTrue(System.getenv("DISPLAY").isNullOrEmpty())
        val failure = assertFailsWith<IllegalStateException> {
            LinuxSurfaceHost.attach(1L, AtomicReference(Rectangle(0, 0, 100, 100)))
        }
        assertTrue(failure.message.orEmpty().contains("/opt/demo/demo.sh"))
    }

    @Test fun rejectsNullParentWithoutLoadingLinuxLibraries() {
        assertFailsWith<IllegalArgumentException> {
            LinuxSurfaceHost.attach(0L, AtomicReference(Rectangle()))
        }
    }

    @Test fun childTracksRealGeometryVisibilityAndCleansUp() {
        if (!desktopTests()) return
        initializeLinuxXlibThreading()
        withWindow { window ->
            val parent = edt { window.windowHandle }
            val bounds = AtomicReference(Rectangle(20, 30, 200, 150))
            val host = LinuxSurfaceHost.attach(parent, bounds)
            try {
                assertEquals(200 to 150, host.pixelSize())
                val tree = information(parent, "-tree")
                val child = Regex("(0x[0-9a-f]+).*200x150\\+20\\+30").find(tree)?.groupValues?.get(1)
                assertTrue(child != null, tree)
                bounds.set(Rectangle(20, 30, 320, 240))
                assertEquals(320 to 240, host.pixelSize())
                assertTrue(information(child).contains("Width: 320"))
                bounds.set(Rectangle())
                assertEquals(0 to 0, host.pixelSize())
                assertTrue(information(child).contains("Map State: IsUnMapped"))
                bounds.set(Rectangle(20, 30, 200, 150))
                assertEquals(200 to 150, host.pixelSize())
                assertTrue(information(child).contains("Map State: IsViewable"))
            } finally {
                host.close()
                host.close()
            }
            assertTrue(host.isClosed)
        }
        assertTrue(Thread.getAllStackTraces().keys.none { it.name.startsWith("dawn-x11-owner") && it.isAlive })
    }

    @Test fun parentDisposalCancelsAndAnAlreadyDestroyedParentDoesNotCrash() {
        if (!desktopTests()) return
        initializeLinuxXlibThreading()
        withWindow { window ->
            val parent = edt { window.windowHandle }
            val bounds = AtomicReference(Rectangle(0, 0, 100, 100))
            val host = LinuxSurfaceHost.attach(parent, bounds)
            try {
                edt { window.dispose() }
                // AWT's native toolkit disposes asynchronously after the EDT call.
                val deadline = System.nanoTime() + 2_000_000_000L
                var cancelled = false
                while (!cancelled && System.nanoTime() < deadline) {
                    try { host.pixelSize() } catch (_: CancellationException) { cancelled = true }
                    if (!cancelled) Thread.sleep(10)
                }
                assertTrue(cancelled, "the destroyed parent must cancel the viewport")
            } finally {
                host.close()
            }
            assertFailsWith<CancellationException> { LinuxSurfaceHost.attach(parent, bounds) }
        }
        assertTrue(Thread.getAllStackTraces().keys.none { it.name.startsWith("dawn-x11-owner") && it.isAlive })
    }

    private fun desktopTests(): Boolean {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_DESKTOP_TESTS") != "1") return false
        assertTrue(!System.getenv("DISPLAY").isNullOrEmpty(), "desktop tests require DISPLAY")
        return true
    }

    private fun withWindow(operation: (ComposeWindow) -> Unit) {
        val window = edt {
            ComposeWindow().apply {
                title = "dawn4k Linux viewport test"
                setSize(500, 400)
                setContent {}
                isVisible = true
            }
        }
        try { operation(window) } finally { edt { window.dispose() } }
    }

    private fun <T> edt(operation: () -> T): T {
        var result: Result<T>? = null
        SwingUtilities.invokeAndWait { result = runCatching(operation) }
        return requireNotNull(result).getOrThrow()
    }

    private fun information(window: Long, vararg options: String) = information("0x${window.toString(16)}", *options)
    private fun information(window: String, vararg options: String): String {
        val process = ProcessBuilder(listOf("xwininfo", "-id", window) + options)
            .redirectErrorStream(true).start()
        val output = process.inputStream.bufferedReader().readText()
        assertEquals(0, process.waitFor(), output)
        return output
    }
}
