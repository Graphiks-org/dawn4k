package org.graphiks.dawn4k.demo

import androidx.compose.ui.awt.ComposeWindow
import javax.swing.SwingUtilities
import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import kotlin.test.*

class DemoToolkitLifecycleTest {
    @Test fun failedTargetRetirementStillAllowsApplicationCleanup(): Unit = fixture { window, controller ->
        val original = DesktopSurfaceLease(1, awaitMetalLayerHost(window))
        var retained: DawnTargetLease? = null
        var failed = false
        val target = object : DawnSurfaceTarget by original.target {
            override suspend fun retain(): DawnTargetLease {
                val lease = original.target.retain().also { retained = it }
                return object : DawnTargetLease by lease {
                    override suspend fun retire() {
                        if (!failed) { failed = true; error("transient target retirement failure") }
                        lease.retire()
                    }
                }
            }
        }
        controller.attach(object : DemoSurfaceLease by original { override val target = target })
        await(controller) { controller.evidence.snapshot().frameCount > 0 }
        coroutineScope {
            val detached = async { runCatching { controller.detach(1) } }
            try {
                val result = withTimeout(1_500) { detached.await() }
                assertEquals("transient target retirement failure", result.exceptionOrNull()?.message)
                assertFalse(original.valid)
            } finally {
                // Release the test's retained reference even if the old implementation deadlocks.
                retained?.retire()
                detached.await()
            }
        }
    }

    @Test fun demoRestartUsesNewSession(): Unit = fixture { window, controller ->
        controller.attach(DesktopSurfaceLease(1, awaitMetalLayerHost(window)))
        await(controller) { controller.evidence.snapshot().frameCount > 0 }
        val first = assertNotNull(controller.gpuSession.value)
        controller.stop()
        assertEquals(DawnSessionState.Closed, first.state.value)
        controller.restart()
        controller.attach(DesktopSurfaceLease(2, awaitMetalLayerHost(window)))
        await(controller) { controller.evidence.snapshot().sessionId == 2L && controller.evidence.snapshot().frameCount > 0 }
        assertNotSame(first, assertNotNull(controller.gpuSession.value))
        assertEquals(DemoPhase.Running, controller.state.value.phase)
    }

    @Test fun demoStopWaitsForToolkitRetirement(): Unit = fixture { window, controller ->
        val original = DesktopSurfaceLease(1, awaitMetalLayerHost(window))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        var nativeSurface: DawnSurfaceReference? = null
        val target = object : DawnSurfaceTarget by original.target {
            override suspend fun retain(): DawnTargetLease {
                val retained = original.target.retain()
                return object : DawnTargetLease by retained {
                    override fun createSurface(context: DawnContext): DawnSurfaceReference =
                        retained.createSurface(context).also { nativeSurface = it }
                    override suspend fun retire() {
                        assertFailsWith<IllegalStateException> { assertNotNull(nativeSurface).nativeHandle() }
                        entered.complete(Unit)
                        release.await()
                        retained.retire()
                    }
                }
            }
        }
        controller.attach(object : DemoSurfaceLease by original { override val target = target })
        await(controller) { controller.evidence.snapshot().frameCount > 0 }
        val gpu = assertNotNull(controller.gpuSession.value)
        coroutineScope {
            val stopped = async { controller.stop() }
            try {
                withTimeout(5_000) { entered.await() }
                assertFalse(stopped.isCompleted)
                assertTrue(original.valid)
                assertNotEquals(DawnSessionState.Closed, gpu.state.value)
            } finally { release.complete(Unit) }
            stopped.await()
        }
        assertFalse(original.valid)
        assertEquals(DawnSessionState.Closed, gpu.state.value)
        assertEquals(DemoPhase.Stopped, controller.state.value.phase)
    }

    @Test fun failedSurfaceAttachmentPreservesPrimaryError(): Unit = fixture { window, controller ->
        val original = DesktopSurfaceLease(1, awaitMetalLayerHost(window))
        var cleanupFailed = false
        val target = object : DawnSurfaceTarget by original.target {
            override suspend fun retain(): DawnTargetLease {
                val retained = original.target.retain()
                return object : DawnTargetLease by retained {
                    override fun createSurface(context: DawnContext): DawnSurfaceReference =
                        throw IllegalArgumentException("primary attachment failure")
                    override suspend fun retire() {
                        retained.retire()
                        if (!cleanupFailed) { cleanupFailed = true; error("secondary cleanup failure") }
                    }
                }
            }
        }
        controller.attach(object : DemoSurfaceLease by original { override val target = target })
        withTimeout(10_000) { while (controller.state.value.phase != DemoPhase.Failed) delay(10) }
        assertEquals("primary attachment failure", controller.state.value.error)
        assertTrue(cleanupFailed)
        controller.stop()
        assertFalse(original.valid)
    }

    private fun fixture(block: suspend (ComposeWindow, DemoSessionController) -> Unit): Unit = runBlocking {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return@runBlocking
        lateinit var window: ComposeWindow
        val controller = DemoSessionController(this)
        try {
            SwingUtilities.invokeAndWait {
                window = ComposeWindow().apply { setSize(420, 320); setContent { }; isVisible = true }
            }
            block(window, controller)
        } finally {
            controller.close()
            SwingUtilities.invokeAndWait { window.dispose() }
        }
    }

    private suspend fun await(controller: DemoSessionController, predicate: () -> Boolean) = withTimeout(10_000) {
        while (!predicate()) {
            check(controller.state.value.error == null) { controller.state.value.error ?: "demo failed" }
            delay(10)
        }
    }
}
