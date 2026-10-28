package org.graphiks.dawn4k.demo

import kotlin.time.Duration.Companion.seconds
import java.nio.file.Files
import java.nio.file.Path
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.isActive
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import org.graphiks.webgpu.suite.demos.particles.maxParticleCount
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

internal enum class WaylandCheckpoint { Capabilities, Presentation }

/** Decorates actual Dawn calls; only the scheduling gate is controlled by the test. */
internal class WaylandLifecycleFixture(private val checkpoint: WaylandCheckpoint) : AutoCloseable {
    val controls = ParticleControls()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val reached = CountDownLatch(1)
    private val release = CountDownLatch(1)
    private val closeReceived = CountDownLatch(1)
    private val created = AtomicReference<DawnSurfaceReference?>()
    private val renderFailure = AtomicReference<Throwable?>()
    private val host = WaylandSurfaceHost.open(controls) {
        scope.cancel()
        closeReceived.countDown()
    }
    private val lease = DesktopSurfaceLease(1, host)
    private val observedTarget = object : DawnSurfaceTarget by lease.target {
        override suspend fun retain(): DawnTargetLease {
            val retained = lease.target.retain()
            return object : DawnTargetLease by retained {
                override fun createSurface(context: DawnContext): DawnSurfaceReference =
                    retained.createSurface(context).also(created::set)
            }
        }
    }
    private fun awaitRelease() {
        reached.countDown()
        check(release.await(15, TimeUnit.SECONDS)) { "test did not release the real Dawn operation" }
    }
    private val rendering = scope.launch {
        try {
            withTimeout(10_000) { while (host.pixelSize().first <= 0) kotlinx.coroutines.delay(10) }
            lease.pixelExtent()
            withDawnSurface(observedTarget, DawnSessionOptions(config = DawnConfig(DawnBackend.Vulkan), startupTimeout = 60.seconds)) { gpu, presentation ->
                // Map the actual surface before the gate, leaving controls uninitialized
                // for the post-capabilities cancellation/connection-loss scenario.
                assertTrue(presentDemoTestFrame(gpu, presentation))
                if (checkpoint == WaylandCheckpoint.Capabilities) { awaitRelease(); currentCoroutineContext().ensureActive() }
                controls.initialize(maxParticleCount(gpu.device.limits))
                assertTrue(presentDemoTestFrame(gpu, presentation) {
                    if (checkpoint == WaylandCheckpoint.Presentation) awaitRelease()
                })
                while (currentCoroutineContext().isActive) kotlinx.coroutines.delay(10)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            renderFailure.set(failure)
            throw failure
        } finally { withContext(NonCancellable) { lease.awaitRetirement() } }
    }

    fun awaitCheckpoint() {
        assertTrue(reached.await(90, TimeUnit.SECONDS), "real Dawn $checkpoint checkpoint not reached: ${renderFailure.get()}")
        assertNotNull(created.get(), "must own an actual Dawn/Vulkan surface")
        if (checkpoint == WaylandCheckpoint.Capabilities) assertFalse(controls.state.value.ready)
        println("[test] Wayland $checkpoint checkpoint reached; real native surface, ready=${controls.state.value.ready}")
    }

    fun compositorClose() {
        swayCommand("[app_id=\"org.graphiks.dawn4k.demo\" pid=${ProcessHandle.current().pid()}] kill")
        assertTrue(closeReceived.await(5, TimeUnit.SECONDS), "the native close event was not dispatched")
    }

    fun awaitTerminalEvent() {
        assertTrue(closeReceived.await(5, TimeUnit.SECONDS), "connection failure was not dispatched while GPU setup was pending")
        assertNotNull(controls.state.value.error)
    }

    fun releaseAndVerifyCleanup() {
        release.countDown()
        runBlocking { withTimeout(10_000) { rendering.join() } }
        assertTrue(renderFailure.get() == null, "unexpected renderer failure: ${renderFailure.get()}")
        // This must reject before touching the now-dead native handles.
        assertFailsWith<IllegalStateException> { assertNotNull(created.get()).nativeHandle() }
    }

    override fun close() {
        release.countDown()
        scope.cancel()
        try { runBlocking { withTimeout(10_000) { rendering.join() } } }
        finally { runBlocking { lease.awaitRetirement() }; host.close() }
        assertTrue(Thread.getAllStackTraces().keys.none { it.isAlive && it.name.startsWith("dawn-wayland-owner-") })
    }
}

internal fun verifyActualCloseAt(checkpoint: WaylandCheckpoint) {
    WaylandLifecycleFixture(checkpoint).use { fixture ->
        fixture.awaitCheckpoint()
        fixture.compositorClose()
        fixture.releaseAndVerifyCleanup()
        assertTrue(fixture.controls.state.value.error == null)
    }
}

class WaylandLifecycleCheckpointTest {
    @Test fun closeWhilePresentationIsPendingReleasesTextureSurfaceAndOwner() {
        if (!waylandTestsEnabled()) return
        verifyActualCloseAt(WaylandCheckpoint.Presentation)
    }

    @Test fun disconnectAfterCapabilityNegotiationCannotBeErasedByInitialization() {
        if (!waylandTestsEnabled() || System.getenv("DAWN_WAYLAND_PENDING_TESTS") != "1") return
        check(Files.isRegularFile(Path.of("/opt/demo/disconnect-fixture"))) {
            "compositor-loss fixture must run only in its disposable desktop"
        }
        WaylandLifecycleFixture(WaylandCheckpoint.Capabilities).use { fixture ->
            fixture.awaitCheckpoint()
            nativeCommand("python3", "-c", """
                import os, pathlib, signal
                for path in pathlib.Path('/proc').iterdir():
                    if not path.name.isdigit(): continue
                    try:
                        args = (path / 'cmdline').read_bytes().split(b'\0')
                        if b'/opt/demo/start-desktop.py' in args: os.kill(int(path.name), signal.SIGSTOP)
                        if (path / 'comm').read_text().strip() == 'sway': os.kill(int(path.name), signal.SIGTERM)
                    except (FileNotFoundError, ProcessLookupError): pass
            """.trimIndent())
            fixture.awaitTerminalEvent()
            fixture.releaseAndVerifyCleanup()
            val failure = assertNotNull(fixture.controls.state.value.error)
            assertTrue(failure.contains("Wayland connection"))
            assertFalse(fixture.controls.state.value.ready)
            assertFailsWith<IllegalStateException> {
                finishWaylandCancellation(CancellationException("disconnected"), true, failure)
            }
        }
    }
}
