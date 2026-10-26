package org.graphiks.dawn4k.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
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
}
