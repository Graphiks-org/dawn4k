package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.glfw.internal.GlfwWindowTarget
import org.graphiks.dawn4k.toolkit.surface.DawnPresentation
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
class WindowEventTest {
    @Test fun closeRequestSurvivesSaturatedInputAndIsReplayed() = runTest {
        val state = WindowSurfaceState(RecordingBackend(), 1L)
        val release = CompletableDeferred<Unit>()
        val received = mutableListOf<DawnWindowEvent>()
        backgroundScope.launch {
            state.events.collect {
                received += it
                if (received.size == 1) release.await()
            }
        }
        runCurrent()
        state.event(DawnWindowEvent.PointerMoved(0.0, 0.0))
        runCurrent()
        repeat(100) { state.event(DawnWindowEvent.PointerMoved(it.toDouble(), 1.0)) }
        state.event(DawnWindowEvent.CloseRequested)
        state.event(DawnWindowEvent.PointerMoved(500.0, 500.0))
        release.complete(Unit)
        runCurrent()
        assertEquals(DawnWindowEvent.CloseRequested, received.last())
        assertEquals(listOf(DawnWindowEvent.CloseRequested), state.events.replayCache)
    }

    /** DawnWindow.requestClose is the sole programmatic emitter; repeated requests stay single-event. */
    @Test fun dawnWindowRequestCloseEmitsExactlyOneTerminalCloseRequest() {
        val backend = RecordingBackend()
        DawnMainThreadLoop.run(backend) { main ->
            val (native, window) = main.owner.onOwner {
                val runtime = main.runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
                val lease = NativeWindowLease(backend, backend.createWindow(DawnWindowOptions()), runtime)
                lease to DawnWindow(main, lease, object : WindowSession {
                    override val gpu: DawnSession get() = error("close-contract test does not access a GPU")
                    override val presentation: DawnPresentation get() = error("close-contract test does not access a surface")
                    override suspend fun shutdown() {}
                }).also(main::register)
            }
            try {
                window.requestClose()
                window.requestClose()
                assertEquals(2, backend.closeRequests)
                assertEquals(listOf(DawnWindowEvent.CloseRequested), window.events.replayCache)
                native.state.event(DawnWindowEvent.PointerMoved(9.0, 9.0))
                assertEquals(listOf(DawnWindowEvent.CloseRequested), window.events.replayCache)
            } finally { window.shutdown() }
        }
    }

    /** Backends never emit on the programmatic close path — recording fake matches the real contract. */
    @Test fun backendRequestCloseAloneEmitsNothing() = runTest {
        val backend = RecordingBackend()
        val state = WindowSurfaceState(backend, 1L)
        backend.requestClose(1L)
        runCurrent()
        assertTrue(state.events.replayCache.isEmpty())
    }

    /** The shared close-button callback path still delivers exactly one terminal event. */
    @Test fun sharedTargetCloseButtonCallbackDeliversOneTerminalCloseRequest() = runTest {
        val target = GlfwWindowTarget(PixelExtent(300, 150))
        val state = WindowSurfaceState(RecordingBackend(), 1L)
        target.sink = state::event
        val received = mutableListOf<DawnWindowEvent>()
        backgroundScope.launch { state.events.collect { received += it } }
        runCurrent()
        target.onResized(320, 240)
        target.onKey(65, 30, 1, 0)
        target.onPointerMoved(4.0, 5.0)
        target.onPointerButton(0, 1, 0)
        target.onClose()
        target.onScroll(1.0, 1.0)
        runCurrent()
        assertEquals(
            listOf(
                DawnWindowEvent.Resized(PixelExtent(320, 240)),
                DawnWindowEvent.Key(65, 30, 1, 0),
                DawnWindowEvent.PointerMoved(4.0, 5.0),
                DawnWindowEvent.PointerButton(0, 1, 0),
                DawnWindowEvent.CloseRequested,
            ),
            received,
        )
        assertEquals(listOf(DawnWindowEvent.CloseRequested), state.events.replayCache)
        assertFalse(target.states.value.available)
        assertEquals(PixelExtent(320, 240), target.states.value.extent)
    }

    /** Retention counts references, invokes the hooks, and gates awaitRetirement. */
    @Test fun sharedTargetRetentionCountsReferencesAndRunsHooks() = runTest {
        val hookOrder = mutableListOf<String>()
        val target = GlfwWindowTarget(
            PixelExtent(10, 10),
            onRetain = { hookOrder += "retain" },
            onRetire = { hookOrder += "retire" },
        )
        target.retention.retain()
        target.retention.retain()
        val retirement = async { target.awaitRetirement() }
        runCurrent()
        assertFalse(retirement.isCompleted)
        target.retention.retire()
        runCurrent()
        assertFalse(retirement.isCompleted)
        target.retention.retire()
        runCurrent()
        assertTrue(retirement.isCompleted)
        assertEquals(listOf("retain", "retain", "retire", "retire"), hookOrder)
    }
}
