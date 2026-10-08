package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.awt.MouseInfo
import java.awt.Robot
import java.awt.event.InputEvent
import java.io.File
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicReference
import javax.imageio.ImageIO
import javax.swing.SwingUtilities
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/** Opt-in native pointer proof: only the temporary qualification window is clicked. */
class MacDemoPointerTest {
    @Test fun realPointerClicksPauseCountAndResetBeforeNativeViewportResize() {
        if (!System.getProperty("os.name").lowercase().contains("mac") ||
            System.getenv("DAWN_DESKTOP_TESTS") != "1") return
        val controls = ParticleControls()
        val bounds = ConcurrentHashMap<String, LogicalViewport>()
        val viewport = AtomicReference<LogicalViewport?>(null)
        lateinit var window: ComposeWindow
        val robot = Robot()
        val originalPointer = MouseInfo.getPointerInfo().location
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                window.isUndecorated = true
                window.isTransparent = true
                window.setSize(960, 600)
                window.setLocation(20, 40)
                window.setContent { DemoApp(window, controls, onViewportBounds = { viewport.set(it) },
                    onControlBounds = { label, area -> bounds[label] = area }) }
                window.isVisible = true
                window.toFront()
            }
            await("GPU readiness and actual button bounds") { controls.state.value.ready && bounds.containsKey("256") }
            fun click(label: String) {
                SwingUtilities.invokeAndWait {
                    val area = bounds.getValue(label)
                    val origin = window.contentPane.locationOnScreen
                    println("[pointer] $label area=$area contentOrigin=$origin frameOrigin=${window.locationOnScreen} insets=${window.insets}")
                    robot.mouseMove(origin.x + (area.x + area.width / 2).toInt(),
                        origin.y + (area.y + area.height / 2).toInt())
                }
                robot.mousePress(InputEvent.BUTTON1_DOWN_MASK)
                robot.mouseRelease(InputEvent.BUTTON1_DOWN_MASK)
            }
            click("Pause")
            await("native Pause click") { controls.state.value.paused }
            click("256")
            await("native count click") { controls.state.value.count == 256 }
            click("Reset")
            await("native Reset click") { controls.state.value.resetGeneration == 1L }
            assertEquals(null, controls.state.value.error)
            SwingUtilities.invokeAndWait { window.setSize(500, 700) }
            await("compact viewport after resize") {
                val view = viewport.get()
                view != null && view.x == 0f && bounds.getValue("Reset").y >= view.y + view.height
            }
            var rectangle = java.awt.Rectangle()
            SwingUtilities.invokeAndWait {
                val position = window.locationOnScreen
                rectangle = java.awt.Rectangle(position.x, position.y, window.width, window.height)
            }
            val file = File("build/demo-kmp/mac-pointer.png")
            file.parentFile.mkdirs()
            var image = robot.createScreenCapture(rectangle)
            // Native resize/present may lag the Compose layout callback. Await
            // the clear colour at both viewport edges rather than screenshot
            // an old wide drawable clipped into the newly compact window.
            await("visible resized native viewport") {
                image = robot.createScreenCapture(rectangle)
                image.getRGB(8, 8) and 0xFFFFFF == 0 &&
                    image.getRGB(image.width - 9, 8) and 0xFFFFFF == 0
            }
            ImageIO.write(image, "png", file)
            val area = checkNotNull(viewport.get())
            val sampleY = (area.y + area.height / 2).toInt()
            val colors = (0 until image.width).map { image.getRGB(it, sampleY) }.toSet()
            assertTrue(colors.size > 2, "presented particles must be visibly composited, not hidden behind Compose")
        } finally {
            SwingUtilities.invokeAndWait { window.dispose() }
            robot.mouseMove(originalPointer.x, originalPointer.y)
        }
    }

    private fun await(label: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(20)
        }
        assertTrue(condition(), "deadline: $label")
    }
}
