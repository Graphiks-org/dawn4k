package org.graphiks.dawn4k.toolkit.surface

import java.lang.foreign.MemorySegment
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import kotlin.test.*

/**
 * Spec F10: the raw-handle integration surface for non-GLFW windowing. Each `MemorySegment`
 * overload must behave exactly like its `Long` counterpart for the same address, and every
 * handle must be rejected while zero before any platform access. No test reaches a real
 * display server: zero handles fail before the platform guard, and the guard fails before
 * the first native call, so only this host's own target family ever constructs a target.
 */
class JvmHandleOverloadsTest {
    private val worker = DawnExecutionOwner.dedicated()
    private val owner get() = DawnPlatformOwner.fromExecutionOwner(worker)
    private val states = MutableStateFlow(DawnTargetState(1, PixelExtent(64, 32), true))
    private val noRetention = object : DawnTargetRetention {
        override suspend fun retain() {}
        override suspend fun retire() {}
    }

    @AfterTest fun closeWorker() = worker.close()

    private fun handle(address: Long): MemorySegment = MemorySegment.ofAddress(address)

    /** The `Long` overload's require message must come out of the shared path unchanged. */
    @Test fun nullSegmentHandlesFailWithTheZeroLongMessages() {
        assertEquals("Metal layer is null", message { DawnSurfaceTargets.metalLayer(0, owner, states, noRetention) })
        assertEquals("Metal layer is null", message { DawnSurfaceTargets.metalLayer(MemorySegment.NULL, owner, states) })
        assertEquals("X11 display and window must be nonzero", message { DawnSurfaceTargets.x11(0, 0x1BuL, owner, states, noRetention) })
        assertEquals("X11 display and window must be nonzero", message { DawnSurfaceTargets.x11(handle(0x10), 0uL, owner, states, noRetention) })
        assertEquals("X11 display and window must be nonzero", message { DawnSurfaceTargets.x11(MemorySegment.NULL, 0x1BuL, owner, states, noRetention) })
        assertEquals("Wayland display and surface must be nonzero", message { DawnSurfaceTargets.wayland(0, 0x20, owner, states, noRetention) })
        assertEquals("Wayland display and surface must be nonzero", message { DawnSurfaceTargets.wayland(MemorySegment.NULL, handle(0x20), owner, states, noRetention) })
        assertEquals("Wayland display and surface must be nonzero", message { DawnSurfaceTargets.wayland(handle(0x10), MemorySegment.NULL, owner, states, noRetention) })
        assertEquals("Win32 window and instance must be nonzero", message { DawnSurfaceTargets.win32(0, 0x20, owner, states, noRetention) })
        assertEquals("Win32 window and instance must be nonzero", message { DawnSurfaceTargets.win32(MemorySegment.NULL, handle(0x20), owner, states, noRetention) })
        assertEquals("Win32 window and instance must be nonzero", message { DawnSurfaceTargets.win32(handle(0x10), MemorySegment.NULL, owner, states, noRetention) })
    }

    @Test fun x11OverloadsBehaveIdenticallyForTheSameAddress() {
        val byLong = outcome { DawnSurfaceTargets.x11(0x10, 0x1BuL, owner, states, noRetention) }
        val bySegment = outcome { DawnSurfaceTargets.x11(handle(0x10), 0x1BuL, owner, states, noRetention) }
        assertSameOutcome(byLong, bySegment)
        if (bySegment.isSuccess) {
            assertSame(states, byLong.getOrThrow().state)
            assertSame(states, bySegment.getOrThrow().state)
        }
    }

    @Test fun waylandOverloadsBehaveIdenticallyForTheSameAddress() {
        val byLong = outcome { DawnSurfaceTargets.wayland(0x10, 0x20, owner, states, noRetention) }
        val bySegment = outcome { DawnSurfaceTargets.wayland(handle(0x10), handle(0x20), owner, states, noRetention) }
        assertSameOutcome(byLong, bySegment)
        if (bySegment.isSuccess) {
            assertSame(states, byLong.getOrThrow().state)
            assertSame(states, bySegment.getOrThrow().state)
        }
    }

    @Test fun win32OverloadsBehaveIdenticallyForTheSameAddress() {
        val byLong = outcome { DawnSurfaceTargets.win32(0x10, 0x20, owner, states, noRetention) }
        val bySegment = outcome { DawnSurfaceTargets.win32(handle(0x10), handle(0x20), owner, states, noRetention) }
        assertSameOutcome(byLong, bySegment)
        if (bySegment.isSuccess) {
            assertSame(states, byLong.getOrThrow().state)
            assertSame(states, bySegment.getOrThrow().state)
        }
    }

    @Test fun metalLayerOverloadsBehaveIdenticallyForTheSameAddress() {
        val byLong = outcome { DawnSurfaceTargets.metalLayer(0x10, owner, states, noRetention) }
        val bySegment = outcome { DawnSurfaceTargets.metalLayer(handle(0x10), owner, states) }
        assertSameOutcome(byLong, bySegment)
        if (bySegment.isSuccess) {
            assertSame(states, byLong.getOrThrow().state)
            assertSame(states, bySegment.getOrThrow().state)
        }
    }

    /** Apple host only: the ObjC retain stays lazy, so an unavailable target fails first. */
    @Test fun segmentMetalLayerRejectsUnavailableTargetsBeforeObjcRetain(): Unit = runBlocking {
        if (surfacePlatform != SurfacePlatform.Apple) return@runBlocking
        states.value = states.value.copy(available = false)
        val target = DawnSurfaceTargets.metalLayer(handle(0x10), owner, states)
        assertFailsWith<IllegalStateException> { target.retain() }
    }

    private inline fun message(block: () -> Unit): String =
        assertFailsWith<IllegalArgumentException>(block = block).message!!

    private inline fun outcome(block: () -> DawnSurfaceTarget): Result<DawnSurfaceTarget> = runCatching(block)

    private fun assertSameOutcome(byLong: Result<DawnSurfaceTarget>, bySegment: Result<DawnSurfaceTarget>) {
        assertEquals(byLong.isSuccess, bySegment.isSuccess)
        assertEquals(byLong.exceptionOrNull()?.javaClass, bySegment.exceptionOrNull()?.javaClass)
        assertEquals(byLong.exceptionOrNull()?.message, bySegment.exceptionOrNull()?.message)
    }
}
