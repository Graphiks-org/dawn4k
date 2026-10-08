package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import javax.swing.SwingUtilities
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DemoSurfaceReplacementTest {
    @Test fun lifecycleResumeAfterLongGapDoesNotAdvanceRealParticleBuffer() = runBlocking {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return@runBlocking
        val now = MutableStateFlow(1_000_000_000L)
        lateinit var window: ComposeWindow
        val controller = DemoSessionController(this, nowNanos = { now.value })
        controller.evidence.captureFingerprints(true)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow()
                window.setSize(420, 320)
                window.setContent { }
                window.isVisible = true
            }
            controller.attach(DesktopSurfaceLease(1, awaitMetalLayerHost(window)))
            await { controller.evidence.snapshot().frameCount > 0 }
            controller.lifecycle(false)
            await { !controller.state.value.lifecycleActive }
            // Complete the outstanding frame before reading the paused checkpoint.
            controller.withSurface { }
            val before = controller.evidence.snapshot()
            now.value = 101_000_000_000L
            controller.lifecycle(true)
            await { controller.evidence.snapshot().frameCount > before.frameCount }
            assertEquals(before.simulationFingerprint, controller.evidence.snapshot().simulationFingerprint)
            assertFalse(controller.controls.state.value.paused)
            assertEquals(1L, controller.evidence.snapshot().deviceCreations)
        } finally {
            controller.close()
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }

    @Test fun pausedGpuBufferSurvivesReplacementAndStaleDetachWithoutNewDevice() = runBlocking {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return@runBlocking
        val windows = mutableListOf<ComposeWindow>()
        val controller = DemoSessionController(this)
        controller.evidence.captureFingerprints(true)
        try {
            SwingUtilities.invokeAndWait {
                repeat(2) { windows.add(ComposeWindow()) }
                windows.forEach { window ->
                    window.setSize(420, 320)
                    window.setContent { }
                    window.isVisible = true
                }
            }
            val first = DesktopSurfaceLease(1, awaitMetalLayerHost(windows[0]))
            controller.attach(first)
            await { controller.evidence.snapshot().frameCount > 0 }
            controller.controls.togglePause()
            controller.controls.selectCount(256)
            controller.controls.reset()
            await {
                val s = controller.evidence.snapshot()
                s.paused && s.count == 256 && s.resetGeneration == 1L && s.simulationFingerprint != null
            }
            val before = controller.evidence.snapshot()
            assertNotNull(before.simulationFingerprint)
            controller.detach(1)
            assertFalse(first.valid)
            val second = DesktopSurfaceLease(2, awaitMetalLayerHost(windows[1]))
            controller.attach(second)
            await { controller.evidence.snapshot().surfaceGeneration == 2L }
            controller.detach(1)
            val after = controller.evidence.snapshot()
            assertTrue(second.valid)
            assertEquals(before.simulationFingerprint, after.simulationFingerprint)
            assertEquals(before.count, after.count)
            assertEquals(before.resetGeneration, after.resetGeneration)
            assertEquals(1L, after.deviceCreations)
            assertEquals(2L, after.surfaceCreations)
            assertEquals(before.sessionId, after.sessionId)
            assertEquals(null, after.firstError)
            controller.stop()
            assertFalse(second.valid)
            assertEquals(DemoPhase.Stopped, controller.state.value.phase)
        } finally {
            controller.close()
            SwingUtilities.invokeAndWait { windows.forEach { it.dispose() } }
        }
    }

    private suspend fun await(predicate: () -> Boolean) = withTimeout(10_000) {
        while (!predicate()) delay(10)
    }
}
