package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.JAVA_INT
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Characterize Skiko's supported public handle before creating native children. */
class LinuxWindowHandleTest {
    @Test fun composeNativeHandleIdentifiesAnX11Drawable() {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_DESKTOP_TESTS") != "1") return
        assertTrue(!System.getenv("DISPLAY").isNullOrEmpty())
        Arena.ofConfined().use { arena ->
            val init = Linker.nativeLinker().downcallHandle(
                SymbolLookup.libraryLookup("libX11.so.6", arena).find("XInitThreads").orElseThrow(),
                FunctionDescriptor.of(JAVA_INT))
            assertTrue(init.invokeWithArguments() as Int != 0)
        }
        lateinit var window: ComposeWindow
        var created = false
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                created = true
                window.title = "dawn4k Linux handle probe"
                window.setSize(500, 400)
                window.setContent {}
                window.isVisible = true
            }
            var handle = 0L
            SwingUtilities.invokeAndWait { handle = window.windowHandle }
            assertTrue(handle != 0L)
            val process = ProcessBuilder("xwininfo", "-id", "0x${handle.toString(16)}")
                .redirectErrorStream(true).start()
            val information = process.inputStream.bufferedReader().readText()
            assertEquals(0, process.waitFor(), information)
            assertTrue(information.contains("Class: InputOutput"), information)
            println("[test] Compose native handle $handle: $information")
        } finally {
            SwingUtilities.invokeAndWait { if (created) window.dispose() }
        }
    }
}
