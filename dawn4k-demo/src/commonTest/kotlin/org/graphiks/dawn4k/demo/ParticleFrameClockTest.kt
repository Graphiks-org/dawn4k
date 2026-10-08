package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class ParticleFrameClockTest {
    @Test
    fun pauseAndResumeDoNotAdvanceOrCatchUpSimulationTime() {
        val clock = ParticleFrameClock()
        val playing = ParticleControlState(ready = true)
        assertEquals(0f, clock.advance(playing, 0L))
        assertEquals(0.016f, clock.advance(playing, 16_000_000L))
        assertEquals(0f, clock.advance(playing.copy(paused = true), 32_000_000L))
        assertEquals(0f, clock.advance(playing.copy(paused = true), 5_000_000_000L))
        assertEquals(0f, clock.advance(playing, 6_000_000_000L))
        assertEquals(0.016f, clock.advance(playing, 6_016_000_000L))
    }

    @Test
    fun resetAndCountChangesRenderTheInitialStateBeforeSimulating() {
        val clock = ParticleFrameClock()
        val playing = ParticleControlState(ready = true)
        clock.advance(playing, 0L)
        val reset = playing.copy(resetGeneration = 1)
        assertEquals(0f, clock.advance(reset, 16_000_000L))
        assertEquals(0.016f, clock.advance(reset, 32_000_000L))
        val changed = reset.copy(count = 256)
        assertEquals(0f, clock.advance(changed, 48_000_000L))
        assertEquals(0.016f, clock.advance(changed, 64_000_000L))
    }

    @Test
    fun longFramesStayWithinTheSceneDeltaLimit() {
        val clock = ParticleFrameClock()
        val state = ParticleControlState(ready = true)
        clock.advance(state, 0L)
        assertEquals(0.05f, clock.advance(state, 1_000_000_000L))
    }
}
