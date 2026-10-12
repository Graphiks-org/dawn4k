package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Real Windows presentation, controls, resize and composition cancellation. */
class WindowsDemoWindowTest {
    @Test
    fun windowsPresentsParticlesAndKeepsControlsWorkingAfterResize() {
        if (!System.getProperty("os.name").startsWith("Windows")) return
        val controls = ParticleControls()
        val originalOutput = System.out
        val trace = ByteArrayOutputStream()
        val capture = PrintStream(trace, true)
        lateinit var window: ComposeWindow
        var windowCreated = false
        System.setOut(capture)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                windowCreated = true
                window.setSize(900, 600)
                window.setContent { DemoApp(window, controls) }
                window.isVisible = true
            }
            await("GPU initialization", controls) { controls.state.value.ready }
            await("first presented frame", controls) { trace.toString().contains("frame 1 rendered") }
            controls.togglePause()
            controls.selectCount(256)
            await("particle count change while paused", controls) {
                trace.toString().contains("particles=256, paused=true, delta=0")
            }
            controls.reset()
            await("particle reset", controls) { trace.toString().contains("scene reset (256 particles)") }
            val configurations = trace.toString().lineSequence().count { it.contains("configuring surface") }
            SwingUtilities.invokeAndWait { window.setSize(1100, 700) }
            await("resize", controls) {
                trace.toString().lineSequence().count { it.contains("configuring surface") } > configurations
            }
            controls.togglePause()
            await("simulation resume", controls) { trace.toString().contains("particles=256, paused=false") }
            assertEquals(null, controls.state.value.error)
        } finally {
            SwingUtilities.invokeAndWait { if (windowCreated && window.isDisplayable) window.dispose() }
            System.setOut(originalOutput)
            originalOutput.print(trace.toString())
            capture.close()
        }
    }

    private fun await(description: String, controls: ParticleControls, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 20_000_000_000L
        while (System.nanoTime() < deadline && controls.state.value.error == null) {
            if (condition()) return
            Thread.sleep(25)
        }
        assertTrue(condition(), "Waiting for $description: ${controls.state.value.error}")
    }
}
