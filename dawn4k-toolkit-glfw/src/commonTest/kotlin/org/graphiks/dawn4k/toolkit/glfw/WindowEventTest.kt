package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

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
}
