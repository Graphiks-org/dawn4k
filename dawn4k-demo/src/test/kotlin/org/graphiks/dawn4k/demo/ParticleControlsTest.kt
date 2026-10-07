package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class ParticleControlsTest {
    @Test fun steppingSelectsAdjacentCountsAndClampsAtDeviceLimits() {
        val controls = ParticleControls()
        controls.stepCount(1)
        assertEquals(4096, controls.state.value.count)
        controls.initialize(1024)
        controls.stepCount(1)
        assertEquals(1024, controls.state.value.count)
        controls.stepCount(-1)
        assertEquals(256, controls.state.value.count)
        controls.stepCount(-100)
        assertEquals(256, controls.state.value.count)
        controls.stepCount(100)
        assertEquals(1024, controls.state.value.count)
        controls.stepCount(0)
        assertEquals(1024, controls.state.value.count)
        controls.fail("closed")
        controls.stepCount(-1)
        assertEquals(1024, controls.state.value.count)
    }

    @Test fun steppingWorksWhenDeviceSupportsOnlyOneChoice() {
        val controls = ParticleControls()
        controls.initialize(128)
        controls.stepCount(1)
        controls.stepCount(-1)
        assertEquals(128, controls.state.value.count)
    }

    @Test
    fun controlsCannotIssueRequestsBeforeGpuInitialization() {
        val controls = ParticleControls()
        controls.togglePause()
        controls.reset()
        controls.selectCount(256)
        assertFalse(controls.state.value.ready)
        assertFalse(controls.state.value.paused)
        assertEquals(0, controls.state.value.resetGeneration)
        assertEquals(4096, controls.state.value.count)
    }

    @Test
    fun countChoicesRespectDeviceLimitsAndDefaultToAnAvailableCount() {
        val controls = ParticleControls()
        controls.initialize(2000)
        assertTrue(controls.state.value.ready)
        assertEquals(listOf(256, 1024), controls.state.value.availableCounts)
        assertEquals(1024, controls.state.value.count)
        controls.selectCount(65536)
        assertEquals(1024, controls.state.value.count)
        controls.selectCount(256)
        assertEquals(256, controls.state.value.count)
    }

    @Test
    fun lowCapacityDeviceStillHasAUsableCount() {
        val controls = ParticleControls()
        controls.initialize(128)
        assertEquals(listOf(128), controls.state.value.availableCounts)
        assertEquals(128, controls.state.value.count)
    }

    @Test
    fun resettingWhilePausedPreservesPauseAndPublishesEachReset() {
        val controls = ParticleControls()
        controls.initialize(65536)
        controls.togglePause()
        controls.reset()
        controls.reset()
        assertTrue(controls.state.value.paused)
        assertEquals(2, controls.state.value.resetGeneration)
        controls.togglePause()
        assertFalse(controls.state.value.paused)
    }

    @Test
    fun fatalFailureDisablesFurtherCommandsAndKeepsTheDiagnostic() {
        val controls = ParticleControls()
        controls.initialize(65536)
        controls.fail("GPU lost")
        val failed = controls.state.value
        assertFalse(failed.ready)
        assertEquals("GPU lost", failed.error)
        controls.togglePause()
        controls.reset()
        controls.selectCount(256)
        assertEquals(failed, controls.state.value)
    }
}
