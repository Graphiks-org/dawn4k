package org.graphiks.dawn4k.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.PixelExtent as ToolkitPixelExtent
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/** The view's target state is the single source of truth; the adapter owns nothing. */
class ToolkitViewLeaseTest {
    private class ViewTarget(override val state: MutableStateFlow<DawnTargetState>) : DawnSurfaceTarget {
        override suspend fun retain(): DawnTargetLease = error("retention belongs to the toolkit view")
    }

    @Test fun availabilityAndExtentDeriveFromTheViewTargetState() {
        val states = MutableStateFlow(DawnTargetState(7, ToolkitPixelExtent(320, 240), true))
        var retirements = 0
        val lease = ToolkitViewLease(ViewTarget(states), DawnBackend.Metal) { retirements++ }
        assertEquals(7L, lease.generation)
        assertEquals(DawnBackend.Metal, lease.backend)
        assertTrue(lease.valid)
        assertEquals(PixelExtent(320, 240), lease.pixelExtent())
        states.value = states.value.copy(extent = ToolkitPixelExtent(640, 480))
        assertEquals(PixelExtent(640, 480), lease.pixelExtent(), "resize must flow through the target state")
        states.value = states.value.copy(available = false)
        assertFalse(lease.valid, "invalidation must invalidate the lease")
        assertEquals(7L, lease.generation, "generation must stay the lease identity")
        runBlocking {
            lease.awaitRetirement()
            assertEquals(1, retirements)
        }
        lease.close()
    }

    @Test fun detachFailureStaysConfinedToTheSessionState(): Unit = runBlocking {
        val states = MutableStateFlow(DawnTargetState(3, ToolkitPixelExtent(320, 240), true))
        var retirements = 0
        val lease = ToolkitViewLease(ViewTarget(states), DawnBackend.Metal) { retirements++ }
        // The controller rethrows the failure that already failed the session; a detached
        // fire-and-forget retirement must swallow it instead of crashing an unhandled scope.
        retireDetachedLease(lease) { throw IllegalStateException("detach already failed the session") }
        assertEquals(1, retirements, "the view's own retirement must still be awaited")
    }

    @Test fun conflatedRemoveAndReAddStillDetachesTheSupersededLease(): Unit = runBlocking {
        val states = MutableStateFlow(DawnTargetState(5, ToolkitPixelExtent(320, 240), true))
        val attached = MutableStateFlow<List<DemoSurfaceLease>>(emptyList())
        val detached = MutableStateFlow<List<Long>>(emptyList())
        val collector = launch {
            bridgeTargetInto({ attached.value = attached.value + it }, { detached.value = detached.value + it },
                DawnBackend.Metal, ViewTarget(states)) { }
        }
        try {
            withTimeout(5_000) { attached.first { it.isNotEmpty() } }
            assertEquals(5L, attached.value.single().generation)
            // Both assignments land in one dispatch window, so the collector never sees
            // the (gen5, false) invalidation — only the re-added generation 6.
            states.value = states.value.copy(available = false)
            states.value = DawnTargetState(6, ToolkitPixelExtent(640, 480), true)
            withTimeout(5_000) { attached.first { it.size == 2 } }
            assertEquals(listOf(5L, 6L), attached.value.map { it.generation }, "the superseded lease must be replaced")
            withTimeout(5_000) { detached.first { it.count { generation -> generation == 5L } == 1 } }
        } finally {
            // Cancelling the collector must retire whatever lease is still outstanding.
            collector.cancel()
        }
        withTimeout(5_000) { detached.first { it.count { generation -> generation == 6L } == 1 } }
        assertEquals(setOf(5L, 6L), detached.value.toSet())
    }
}
