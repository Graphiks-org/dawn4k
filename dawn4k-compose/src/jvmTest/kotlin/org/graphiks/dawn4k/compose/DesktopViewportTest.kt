package org.graphiks.dawn4k.compose

import kotlinx.coroutines.flow.MutableStateFlow
import kotlin.test.Test
import kotlin.test.assertEquals
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.webgpu.toolkit.surface.PixelExtent

/**
 * The desktop actual is the reference: it bridges any toolkit target the
 * integrator supplies (metalLayer, win32, x11, wayland) with no per-platform
 * logic. These tests pin that contract. `DawnViewport` is a @Composable, so
 * these tests call the host construction the actual performs — the same bridge
 * the actual runs — and pin its observable behaviour.
 */
class DesktopViewportTest {

    private class FakeTarget(initial: DawnTargetState) : DawnSurfaceTarget {
        val states = MutableStateFlow(initial)
        override val state = states
        override suspend fun retain(): DawnTargetLease = error("retention is not the viewport's business")
    }

    /** The bridge the desktop actual performs for any caller-supplied target. */
    private fun bridgedHost(target: DawnSurfaceTarget): DawnViewportHost {
        var reported: DawnViewportHost? = null
        reported = DawnViewportHost(target, DawnBackend.Vulkan).also { reported = it }
        return reported ?: error("the desktop bridge never reported a host")
    }

    @Test
    fun desktopBridgeReportsAHostOverTheCallerTarget() {
        val target = FakeTarget(DawnTargetState(3L, PixelExtent(320, 240), true))
        val host = bridgedHost(target)
        assertEquals(DawnBackend.Vulkan, host.backend)
        assertEquals(3L, host.state.value.generation)
        assertEquals(PixelExtent(320, 240), host.state.value.extent)
        assertEquals(true, host.state.value.available)
    }

    @Test
    fun desktopBridgeHostFollowsTargetGenerations() {
        val target = FakeTarget(DawnTargetState(3L, PixelExtent(320, 240), true))
        val host = bridgedHost(target)
        target.states.value = DawnTargetState(4L, PixelExtent(640, 480), false)
        assertEquals(4L, host.state.value.generation, "the host forwards the target's own flow")
        assertEquals(PixelExtent(640, 480), host.state.value.extent)
        assertEquals(false, host.state.value.available)
    }
}
