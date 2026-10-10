package org.graphiks.dawn4k.demo

import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerEventType
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticleComposeSceneTest {
    @Test fun offOwnerCallsCannotRenderOrCloseTheNativeScene() = withRaster { scene, _, _ ->
        scene.render(0)
        Executors.newSingleThreadExecutor().use { executor ->
            executor.submit {
                assertFailsWith<IllegalStateException> { scene.render(16_000_000) }
                assertFailsWith<IllegalStateException> { scene.close() }
            }.get(5, TimeUnit.SECONDS)
        }
        assertEquals(800, scene.render(demoMonotonicNanos()).width)
    }

    @Test fun malformedPackedFramesCannotReachNativeShmUpload() {
        assertFailsWith<IllegalArgumentException> { UiPixelFrame(1, 1, 0, byteArrayOf()) }
        assertFailsWith<IllegalArgumentException> { UiPixelFrame(1, 1, 4, byteArrayOf(1, 2, 3)) }
        assertFailsWith<IllegalArgumentException> { UiPixelFrame(Int.MAX_VALUE, 1, 0, byteArrayOf()) }
    }

    @Test fun letterAndEscapeKeysNeverInvokeOldGlobalActions() {
        var closed = false
        withRaster(onClose = { closed = true }) { scene, controller, _ ->
            scene.render(0)
            for (key in listOf(Key.P, Key.R, Key.Plus, Key.Minus, Key.Escape)) key(scene, key)
            assertFalse(controller.controls.state.value.paused)
            assertEquals(0L, controller.controls.state.value.resetGeneration)
            assertEquals(4096, controller.controls.state.value.count)
            assertFalse(closed)
        }
    }

    @Test fun rasterControlsReceiveActualPointerPressAndRelease() = withRaster { scene, controller, bounds ->
        val first = scene.render(0)
        assertEquals(800, first.width)
        assertEquals(600, first.height)
        assertEquals(3200, first.stride)
        assertEquals(1_920_000, first.bgra.size)
        click(scene, bounds.getValue("Pause"))
        assertTrue(controller.controls.state.value.paused)
        click(scene, bounds.getValue("256"))
        assertEquals(256, controller.controls.state.value.count)
        click(scene, bounds.getValue("Reset"))
        assertEquals(1L, controller.controls.state.value.resetGeneration)
    }

    @Test fun focusLossCancelsPressedControlAndUnfocusedKeysCannotAct() = withRaster { scene, controller, bounds ->
        scene.render(0)
        val area = bounds.getValue("Pause")
        scene.pointer(PointerEventType.Press, area.x + area.width / 2, area.y + area.height / 2, true)
        scene.focus(false)
        scene.pointer(PointerEventType.Release, area.x + area.width / 2, area.y + area.height / 2, false)
        scene.key(Key.Enter, KeyEventType.KeyDown)
        scene.key(Key.Enter, KeyEventType.KeyUp)
        scene.render(16_000_000)
        assertFalse(controller.controls.state.value.paused)
        scene.focus(true)
        scene.render(32_000_000)
        assertFalse(controller.controls.state.value.paused)
    }

    @Test fun tabAndEnterSpaceActivateFocusedComposeControlsNotGlobalShortcuts() = withRaster { scene, controller, _ ->
        scene.render(0)
        key(scene, Key.Tab)
        key(scene, Key.Enter)
        assertTrue(controller.controls.state.value.paused)
        key(scene, Key.Spacebar)
        assertFalse(controller.controls.state.value.paused)
        key(scene, Key.Tab)
        key(scene, Key.Enter)
        assertEquals(1L, controller.controls.state.value.resetGeneration)
    }

    @Test fun scaleTwoPreservesLogicalBoundsAndConvertsPointerExactlyOnce() = withRaster { scene, controller, bounds ->
        scene.render(0)
        val pause = bounds.getValue("Pause")
        scene.resize(800, 600, 2)
        val scaled = scene.render(16_000_000)
        assertEquals(1600, scaled.width)
        assertEquals(1200, scaled.height)
        assertEquals(6400, scaled.stride)
        val actual = bounds.getValue("Pause")
        assertEquals(pause.x, actual.x, 0.5f)
        assertEquals(pause.y, actual.y, 0.5f)
        assertEquals(pause.width, actual.width, 0.5f)
        assertEquals(pause.height, actual.height, 0.5f)
        click(scene, pause)
        assertTrue(controller.controls.state.value.paused)
        scene.resize(320, 480, 2)
        scene.render(32_000_000)
        val viewport = checkNotNull(controller.evidence.snapshot().viewport)
        assertTrue(bounds.getValue("Reset").y >= viewport.y + viewport.height)
    }

    @Test fun closeControlInvokesLifetimeCallbackAndSceneCloseIsIdempotent() {
        var closed = false
        withRaster(onClose = { closed = true }) { scene, _, bounds ->
            scene.render(0)
            click(scene, bounds.getValue("Close"))
            assertTrue(closed)
            scene.close()
            scene.close()
            assertFailsWith<IllegalStateException> { scene.render(16_000_000) }
        }
    }

    @Test fun zeroSizeSuspendsRasterAndOverflowOrNonpositiveScaleFailsBeforeAllocation() = withRaster { scene, _, _ ->
        scene.resize(0, 600, 1)
        assertTrue(scene.render(0).bgra.isEmpty())
        assertFailsWith<IllegalArgumentException> { scene.resize(Int.MAX_VALUE, 600, 2) }
        assertFailsWith<IllegalArgumentException> { scene.resize(800, Int.MAX_VALUE, 1) }
        assertFailsWith<IllegalArgumentException> { scene.resize(800, 600, 0) }
        assertFailsWith<IllegalArgumentException> { scene.resize(-1, 600, 1) }
    }

    private fun withRaster(
        onClose: () -> Unit = {},
        block: (ParticleComposeScene, DemoSessionController, MutableMap<String, LogicalViewport>) -> Unit,
    ) {
        val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val bounds = mutableMapOf<String, LogicalViewport>()
        controller.controls.initialize(65536, 1)
        try {
            ParticleComposeScene(controller, onClose, {}, { label, area -> bounds[label] = area }).use { scene ->
                scene.resize(800, 600, 1)
                scene.focus(true)
                block(scene, controller, bounds)
            }
        } finally { runBlocking { controller.close() } }
    }

    private fun click(scene: ParticleComposeScene, area: LogicalViewport) {
        scene.pointer(PointerEventType.Press, area.x + area.width / 2, area.y + area.height / 2, true)
        scene.pointer(PointerEventType.Release, area.x + area.width / 2, area.y + area.height / 2, false)
        scene.render(demoMonotonicNanos())
    }

    private fun key(scene: ParticleComposeScene, key: Key) {
        scene.key(key, KeyEventType.KeyDown)
        scene.key(key, KeyEventType.KeyUp)
        scene.render(demoMonotonicNanos())
    }
}
