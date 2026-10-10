package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoSessionStateTest {
    @Test fun lateReadinessAndFailureCannotChangeRestartedControls() {
        val controls = ParticleControls()
        controls.beginSession(2)
        controls.initialize(65536, 1)
        controls.fail("old GPU error", 1)
        assertFalse(controls.state.value.ready)
        assertEquals(null, controls.state.value.error)
        controls.initialize(1024, 2)
        assertTrue(controls.state.value.ready)
        controls.fail("first", 2)
        controls.fail("second", 2)
        assertEquals("first", controls.state.value.error)
    }

    @Test fun stoppingControlsDisablesRequestsAndLateInitializationUntilNewSession() {
        val controls = ParticleControls()
        controls.initialize(65536)
        controls.togglePause()
        controls.reset()
        controls.deactivate()
        val stopped = controls.state.value
        controls.initialize(65536, 1)
        controls.reset()
        controls.selectCount(256)
        assertEquals(stopped, controls.state.value)
        controls.beginSession(2)
        assertEquals(0, controls.state.value.resetGeneration)
        assertFalse(controls.state.value.paused)
    }

    @Test fun lifecycleSuspensionDoesNotChangeUserPauseAndResetsTimeBaseline() {
        val clock = ParticleFrameClock()
        val running = ParticleControlState(ready = true)
        assertEquals(0f, clock.advance(running, 1_000_000_000L))
        assertEquals(0.016f, clock.advance(running, 1_016_000_000L), 0.00001f)
        clock.suspend()
        assertEquals(0f, clock.advance(running, 101_000_000_000L))
        assertEquals(0f, clock.advance(running.copy(paused = true), 102_000_000_000L))
    }
}
