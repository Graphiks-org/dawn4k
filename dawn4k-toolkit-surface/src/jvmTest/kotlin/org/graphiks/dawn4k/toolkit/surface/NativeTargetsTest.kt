package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.memoryScope
import kotlin.test.*

@Suppress("DEPRECATION")
class NativeTargetsTest {
    @Test fun sourceDescriptorsPreserveTypedHandles() = memoryScope { allocator ->
        val metal = NativeSurfaceSource.Metal(11).chain(allocator)
        assertEquals(WGPUSType_SurfaceSourceMetalLayer, metal.sType)
        assertEquals(11L, WGPUSurfaceSourceMetalLayer(metal.handler).layer?.rawValue)
        val win = NativeSurfaceSource.Win32(12, 13).chain(allocator)
        assertEquals(WGPUSType_SurfaceSourceWindowsHWND, win.sType)
        assertEquals(12L, WGPUSurfaceSourceWindowsHWND(win.handler).hwnd?.rawValue)
        assertEquals(13L, WGPUSurfaceSourceWindowsHWND(win.handler).hinstance?.rawValue)
        val x11 = NativeSurfaceSource.X11(14, 15u).chain(allocator)
        assertEquals(WGPUSType_SurfaceSourceXlibWindow, x11.sType)
        assertEquals(15uL, WGPUSurfaceSourceXlibWindow(x11.handler).window)
        val wayland = NativeSurfaceSource.Wayland(16, 17).chain(allocator)
        assertEquals(WGPUSType_SurfaceSourceWaylandSurface, wayland.sType)
        assertEquals(17L, WGPUSurfaceSourceWaylandSurface(wayland.handler).surface?.rawValue)
    }

    private val states = MutableStateFlow(DawnTargetState(1, PixelExtent(64, 32), true))
    private val noRetention = object : DawnTargetRetention {
        override suspend fun retain() {}
        override suspend fun retire() {}
    }
    private fun owner(worker: DawnExecutionOwner) = DawnPlatformOwner.fromExecutionOwner(worker)
    @Test fun rejectsNullPlatformHandlesBeforeNativeCalls(): Unit = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        try {
            val platform = owner(worker)
            assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.metalLayer(0, platform, states, noRetention) }
            assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.win32(0, 0, platform, states, noRetention) }
            assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.x11(0, 0u, platform, states, noRetention) }
            assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.wayland(0, 0, platform, states, noRetention) }
        } finally { worker.close() }
    }
    @Test fun targetLeaseRetiresOnItsPlatformOwner() = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        val observed = mutableListOf<Long>()
        val expected = worker.run { Thread.currentThread().id }
        val retention = object : DawnTargetRetention {
            override suspend fun retain() { observed += Thread.currentThread().id; delay(1); observed += Thread.currentThread().id }
            override suspend fun retire() { delay(1); observed += Thread.currentThread().id }
        }
        try {
            val target = hostTarget(owner(worker), retention)
            val lease = target.retain()
            lease.retire()
            lease.retire()
            assertEquals(listOf(expected, expected, expected), observed)
        } finally { worker.close() }
    }
    @Test fun unavailableTargetCannotCreateSurface(): Unit = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        try {
            val target = hostTarget(owner(worker), noRetention)
            val lease = target.retain()
            states.value = states.value.copy(available = false)
            val context = DawnContext.create()
            try { assertFailsWith<IllegalStateException> { lease.createSurface(context) } }
            finally { lease.retire(); context.close() }
        } finally { worker.close() }
    }
    @Test fun incompatibleHostFailsBeforeRetaining(): Unit = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        try {
            val platform = owner(worker)
            when {
                System.getProperty("os.name").startsWith("Mac") ->
                    assertFailsWith<UnsupportedOperationException> { DawnSurfaceTargets.win32(1, 1, platform, states, noRetention) }
                else -> assertFailsWith<UnsupportedOperationException> { DawnSurfaceTargets.metalLayer(1, platform, states, noRetention) }
            }
        } finally { worker.close() }
    }
    @Test fun cancelledRetentionReturnReleasesTheAcquiredReference() = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        var retains = 0
        var retires = 0
        try {
            val opening = launch {
                val caller = currentCoroutineContext()[Job]!!
                val retention = object : DawnTargetRetention {
                    override suspend fun retain() { retains++; caller.cancel() }
                    override suspend fun retire() { retires++ }
                }
                hostTarget(owner(worker), retention).retain()
            }
            opening.join()
            assertTrue(opening.isCancelled)
            assertEquals(1, retains)
            assertEquals(1, retires)
        } finally { worker.close() }
    }
    @Test fun retirementWaitsForFrameAcknowledgement() = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        val order = mutableListOf<String>()
        try {
            val retention = object : DawnTargetRetention {
                override suspend fun retain() {}
                override suspend fun retire() { order += "target-retire" }
            }
            val target = hostTarget(owner(worker), retention)
            val native = CountingSurface().apply { onClose = { order += "surface-release" } }
            val attachment = org.graphiks.dawn4k.toolkit.surface.internal.SurfaceAttachment(
                target.retain(), null, { native }, native::close, native,
            )
            assertTrue(order.isEmpty())
            attachment.retire()
            assertEquals(listOf("surface-release", "target-retire"), order)
        } finally { worker.close() }
    }
    @Test fun retirementTimeoutKeepsOneAcknowledgementAttempt() = runBlocking {
        val worker = DawnExecutionOwner.dedicated()
        val gate = CompletableDeferred<Unit>()
        var retirementCalls = 0
        var waiting: Deferred<Unit>? = null
        try {
            val retention = object : DawnTargetRetention {
                override suspend fun retain() {}
                override suspend fun retire() { retirementCalls++; gate.await() }
            }
            val lease = hostTarget(owner(worker), retention).retain()
            waiting = async { withTimeout(20) { lease.retire() } }
            withTimeout(5_000) { waiting.join() }
            assertFailsWith<TimeoutCancellationException> { waiting.await() }
            gate.complete(Unit)
            lease.retire()
            assertEquals(1, retirementCalls)
        } finally {
            gate.complete(Unit)
            waiting?.join()
            worker.close()
        }
    }
    private fun hostTarget(owner: DawnPlatformOwner, retention: DawnTargetRetention): DawnSurfaceTarget =
        when {
            System.getProperty("os.name").startsWith("Mac") -> DawnSurfaceTargets.metalLayer(1, owner, states, retention)
            System.getProperty("os.name").startsWith("Windows") -> DawnSurfaceTargets.win32(1, 1, owner, states, retention)
            else -> DawnSurfaceTargets.x11(1, 1u, owner, states, retention)
        }
}
