package org.graphiks.dawn4k.demo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertFailsWith

class WaylandDemoCancellationTest {
    @Test fun onlyAnObservedWindowCloseIsASuccessfulCancellation() {
        finishWaylandCancellation(CancellationException("closed"), true, null)
        assertFailsWith<CancellationException> {
            finishWaylandCancellation(CancellationException("external cancellation"), false, null)
        }
        assertFailsWith<IllegalStateException> {
            finishWaylandCancellation(CancellationException("disconnected"), true, "Wayland connection failed")
        }
    }

    @Test fun configureTimeoutIsAnErrorEvenIfCloseArrivesConcurrently() {
        val timeout = runBlocking {
            assertFailsWith<TimeoutCancellationException> { withTimeout(1) { delay(100) } }
        }
        assertFailsWith<TimeoutCancellationException> { finishWaylandCancellation(timeout, false, null) }
        assertFailsWith<TimeoutCancellationException> { finishWaylandCancellation(timeout, true, null) }
    }
}
