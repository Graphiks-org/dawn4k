package org.graphiks.dawn4k.compose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.webgpu.toolkit.surface.PixelExtent

class DawnViewportContractTest {

    /** A target whose state the test drives directly; retention is never exercised here. */
    private class FakeTarget(initial: DawnTargetState) : DawnSurfaceTarget {
        val states = MutableStateFlow(initial)
        override val state = states
        override suspend fun retain(): DawnTargetLease = error("retention is not part of the viewport contract")
    }

    @Test
    fun hostExposesTheTargetState() = kotlinx.coroutines.test.runTest {
        val target = FakeTarget(DawnTargetState(7L, PixelExtent(64, 48), true))
        val host = DawnViewportHost(target, DawnBackend.Metal)
        try {
            assertEquals(7L, host.state.value.generation)
            assertEquals(PixelExtent(64, 48), host.state.value.extent)
            assertTrue(host.state.value.available)
            target.states.value = DawnTargetState(8L, PixelExtent(128, 96), false)
            assertEquals(8L, host.state.value.generation)
            assertEquals(PixelExtent(128, 96), host.state.value.extent)
            assertFalse(host.state.value.available)
        } finally {
            host.close()
        }
    }

    @Test
    fun hostIsClosedExactlyOnce() = kotlinx.coroutines.test.runTest {
        val target = FakeTarget(DawnTargetState(1L, PixelExtent(1, 1), true))
        var closed = 0
        val host = DawnViewportHost(target, DawnBackend.Metal) { closed++ }
        host.close()
        host.close()
        assertEquals(1, closed)
        // A second close must not run the hook again: the once() wrapper claims it.
        host.close()
        assertEquals(1, closed)
    }
}
