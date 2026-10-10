package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.TimeUnit

/** Real Linux presentation and cancellation; opt in only inside the desktop. */
class LinuxDemoWindowTest {
    @Test fun presentsParticlesAndKeepsControlsWorkingAfterResize() {
        if (!enabled()) return
        initializeLinuxXlibThreading()
        val controls = ParticleControls()
        val originalOutput = System.out
        val trace = ByteArrayOutputStream()
        val capture = PrintStream(trace, true)
        lateinit var window: ComposeWindow
        var created = false
        System.setOut(capture)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                created = true
                window.title = "dawn4k Linux integration test"
                window.setSize(900, 600)
                window.setContent { DemoApp(window, controls) }
                window.isVisible = true
            }
            await("GPU initialization", controls, 60) { controls.state.value.ready }
            await("first presented frame", controls) { trace.toString().contains("frame 1 rendered") }
            assertTrue(trace.toString().contains("llvmpipe"), "must identify the software Vulkan adapter: $trace")
            controls.togglePause()
            controls.selectCount(256)
            await("count change while paused", controls) {
                trace.toString().contains("particles=256, paused=true, delta=0")
            }
            controls.reset()
            await("reset", controls) { trace.toString().contains("scene reset (256 particles)") }
            val configurations = trace.toString().lineSequence().count { it.contains("configuring surface") }
            SwingUtilities.invokeAndWait { window.setSize(1100, 700) }
            await("resize", controls) {
                trace.toString().lineSequence().count { it.contains("configuring surface") } > configurations
            }
            val beforeNativeResize = trace.toString().lineSequence().count { it.contains("configuring surface") }
            var nativeWindow = 0L
            SwingUtilities.invokeAndWait { nativeWindow = window.windowHandle }
            val resize = ProcessBuilder("xdotool", "windowsize", nativeWindow.toString(), "950", "650").start()
            assertTrue(resize.waitFor(5, TimeUnit.SECONDS), "native resize command timed out")
            assertEquals(0, resize.exitValue())
            await("window-manager resize, not only Java setSize", controls) {
                trace.toString().lineSequence().count { it.contains("configuring surface") } > beforeNativeResize
            }
            controls.togglePause()
            await("resume", controls) { trace.toString().contains("particles=256, paused=false") }
            fun nativeCommand(vararg arguments: String) {
                val command = ProcessBuilder(listOf("xdotool") + arguments).start()
                assertTrue(command.waitFor(5, TimeUnit.SECONDS), "X11 command timed out")
                assertEquals(0, command.exitValue())
            }
            fun lastFrame(): Long = Regex("frame (\\d+) rendered").findAll(trace.toString())
                .lastOrNull()?.groupValues?.get(1)?.toLong() ?: 0L
            nativeCommand("windowunmap", nativeWindow.toString())
            try {
                // Allow an already acquired frame to finish, then observe the live
                // render loop rather than treating unchanged geometry as hidden.
                Thread.sleep(300)
                val hiddenFrame = lastFrame()
                Thread.sleep(2000)
                assertEquals(hiddenFrame, lastFrame(), "hidden windows must stop presenting")
                nativeCommand("windowmap", nativeWindow.toString())
                await("presentation after native map restoration", controls) { lastFrame() > hiddenFrame }
            } finally {
                nativeCommand("windowmap", nativeWindow.toString())
            }
            assertEquals(null, controls.state.value.error)
        } finally {
            SwingUtilities.invokeAndWait { if (created && window.isDisplayable) window.dispose() }
            try { awaitNoHosts() } finally {
                System.setOut(originalOutput)
                originalOutput.print(trace.toString())
                capture.close()
            }
        }
        assertEquals(null, controls.state.value.error)
    }

    @Test fun disposalDuringStartupDoesNotLeakNativeOwners() {
        if (!enabled()) return
        initializeLinuxXlibThreading()
        repeat(3) {
            val controls = ParticleControls()
            lateinit var window: ComposeWindow
            var created = false
            try {
                SwingUtilities.invokeAndWait {
                    window = ComposeWindow()
                    created = true
                    window.title = "dawn4k startup cancellation test"
                    window.setSize(900, 600)
                    window.setContent { DemoApp(window, controls) }
                    window.isVisible = true
                }
            } finally {
                SwingUtilities.invokeAndWait { if (created) window.dispose() }
            }
            // Let composition cancellation reach the host-attachment boundary too.
            Thread.sleep(100)
            awaitNoHosts()
            assertEquals(null, controls.state.value.error)
        }
    }

    private fun enabled(): Boolean {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_DESKTOP_TESTS") != "1") return false
        assertTrue(!System.getenv("DISPLAY").isNullOrEmpty(), "desktop tests require DISPLAY")
        return true
    }

    private fun await(description: String, controls: ParticleControls, seconds: Int = 20, condition: () -> Boolean) {
        val deadline = System.nanoTime() + seconds * 1_000_000_000L
        while (System.nanoTime() < deadline && controls.state.value.error == null) {
            if (condition()) return
            Thread.sleep(25)
        }
        assertTrue(condition(), "Waiting for $description: ${controls.state.value.error}")
    }

    private fun awaitNoHosts() {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (System.nanoTime() < deadline) {
            if (Thread.getAllStackTraces().keys.none { it.name.startsWith("dawn-x11-owner") && it.isAlive }) return
            Thread.sleep(25)
        }
        assertTrue(false, "native X11 owner threads leaked after composition cancellation")
    }
}
