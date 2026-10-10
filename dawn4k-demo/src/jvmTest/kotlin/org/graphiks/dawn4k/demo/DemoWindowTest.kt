package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import javax.swing.SwingUtilities
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertFailsWith
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.CGSize
import org.graphiks.kffi.objc.NSArray
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.NSWindow
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability
import kotlin.math.roundToInt
import java.util.concurrent.atomic.AtomicReference

/** Exercises the real transparent Compose → AppKit particle layer → Dawn lifecycle. */
@OptIn(PlatformAvailability::class)
class DemoWindowTest {
    @Test
    fun controlsChangeTheRunningSceneAndResetItWhilePaused() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        lateinit var window: ComposeWindow
        val controls = ParticleControls()
        val originalOutput = System.out
        val trace = ByteArrayOutputStream()
        val capture = PrintStream(trace, true)
        System.setOut(capture)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                window.isUndecorated = true
                window.isTransparent = true
                window.setSize(420, 320)
                window.setContent { DemoApp(window, controls = controls) }
                window.isVisible = true
            }
            awaitCondition("GPU controls ready") { controls.state.value.ready }
            controls.togglePause()
            controls.selectCount(256)
            awaitCondition("paused frame with the new particle count") {
                trace.toString().contains("particles=256, paused=true, delta=0")
            }
            controls.reset()
            awaitCondition("reset applied to the paused scene") {
                trace.toString().contains("[demo] scene reset (256 particles)")
            }
            controls.togglePause()
            awaitCondition("resumed particle frame") {
                trace.toString().contains("particles=256, paused=false")
            }
            assertEquals(null, controls.state.value.error)
        } finally {
            try {
                SwingUtilities.invokeAndWait { if (window.isDisplayable) window.dispose() }
            } finally {
                System.setOut(originalOutput)
                originalOutput.print(trace.toString())
                capture.close()
            }
        }
    }

    @Test
    fun closingBeforeNativeAttachmentCancelsStartupWithoutTouchingADisposedWindow() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        lateinit var window: ComposeWindow
        SwingUtilities.invokeAndWait {
            window = ComposeWindow()
            window.setSize(420, 320)
            window.setContent {}
            window.isVisible = true
            window.dispose()
        }
        assertFailsWith<CancellationException> {
            runBlocking { awaitMetalLayerHost(window) }
        }
    }

    @Test
    fun demoConfiguresAParticleLayerBelowComposeAtWindowPixelSizeAndReleasesItOnClose() {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return
        lateinit var window: ComposeWindow
        var overlay = MemorySegment.NULL
        val viewport = AtomicReference<LogicalViewport?>(null)
        val originalOutput = System.out
        val trace = ByteArrayOutputStream()
        val capture = PrintStream(trace, true)
        System.setOut(capture)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                window.isUndecorated = true
                window.isTransparent = true
                window.setSize(420, 320)
                window.setContent { DemoApp(window, onViewportBounds = { viewport.set(it) }) }
                window.isVisible = true
            }
            var handle = 0L
            awaitCondition("Compose native window") {
                SwingUtilities.invokeAndWait { handle = window.windowHandle }
                handle != 0L
            }
            awaitCondition("Dawn overlay configuration") {
                onAppKitThread {
                    val view = NSView(NSWindow(MemorySegment.ofAddress(handle)).contentView())
                    val layers = NSArray(CALayer(view.layer()).sublayers())
                    val candidate = (0 until layers.count()).map { layers.objectAtIndex(it) }
                        .firstOrNull {
                            CALayer(it).zPosition() < 0.0 && ObjCRuntime.msgSend(
                                ValueLayout.JAVA_BOOLEAN, it, ObjCRuntime.sel("isKindOfClass:"),
                                ObjCRuntime.getClass("CAMetalLayer")
                            ) == true
                        }
                    if (candidate == null || drawableSize(candidate).first <= 0) false
                    else {
                        overlay = candidate
                        ObjCRuntime.msgSend(null, overlay, ObjCRuntime.sel("retain"))
                        assertEquals(view.layer(), CALayer(overlay).superlayer())
                        true
                    }
                }
            }
            // This trace is emitted only after acquisition, submission and successful present.
            awaitCondition("first successfully presented particle frame") {
                trace.toString().contains("[demo] frame 1 rendered")
            }
            val initialSize = onAppKitThread { drawableSize(overlay) }
            SwingUtilities.invokeAndWait { window.setSize(960, 600) }
            awaitCondition("Dawn resize in physical pixels") {
                onAppKitThread {
                    val nativeWindow = NSWindow(MemorySegment.ofAddress(handle))
                    val bounds = NSView(nativeWindow.contentView()).bounds()
                    val scale = nativeWindow.backingScaleFactor()
                    val area = viewport.get() ?: return@onAppKitThread false
                    val expected = (area.width * scale).roundToInt() to
                        (area.height * scale).roundToInt()
                    val actual = drawableSize(overlay)
                    actual != initialSize && actual == expected && area.x >= 300f &&
                        actual.first < (bounds.size.width * scale).roundToInt()
                }
            }
            SwingUtilities.invokeAndWait { window.dispose() }
            awaitCondition("overlay cleanup after composition cancellation") {
                onAppKitThread { CALayer(overlay).superlayer() == MemorySegment.NULL }
            }
        } finally {
            try {
                SwingUtilities.invokeAndWait { if (window.isDisplayable) window.dispose() }
                if (overlay != MemorySegment.NULL) onAppKitThread {
                    ObjCRuntime.msgSend(null, overlay, ObjCRuntime.sel("release"))
                }
            } finally {
                System.setOut(originalOutput)
                originalOutput.print(trace.toString())
                capture.close()
            }
        }
    }

    private fun drawableSize(layer: MemorySegment): Pair<Int, Int> {
        val size = ObjCRuntime.msgSendStruct(CGSize.layout, layer, ObjCRuntime.sel("drawableSize"))
        return size.get(ValueLayout.JAVA_DOUBLE, 0).roundToInt() to
            size.get(ValueLayout.JAVA_DOUBLE, 8).roundToInt()
    }

    private fun awaitCondition(description: String, condition: () -> Boolean) {
        val deadline = System.nanoTime() + 10_000_000_000L
        while (System.nanoTime() < deadline) {
            if (condition()) return
            Thread.sleep(25)
        }
        assertTrue(condition(), "timed out waiting for $description")
    }
}
