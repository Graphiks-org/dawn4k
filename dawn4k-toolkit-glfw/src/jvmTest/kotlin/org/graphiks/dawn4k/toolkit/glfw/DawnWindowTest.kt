package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.lwjgl.glfw.GLFW
import org.junit.Assume.assumeTrue
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class DawnWindowTest {
    @Test fun retryAfterPartialAllocationDoesNotDestroyWindowTwice() {
        var attempts = 0
        val backend = RecordingBackend().apply {
            extentFailure = IllegalArgumentException("extent query failed")
            beforeTermination = { if (++attempts == 1) throw IllegalStateException("termination interrupted") }
        }
        assertFailsWith<IllegalArgumentException> {
            DawnMainThreadLoop.run(backend) { main ->
                openDawnWindow(DawnWindowOptions(), DawnSessionOptions(), DawnPresentationOptions(), main) { _, _, _ ->
                    error("must not reach GPU bootstrap")
                }
            }
        }
        assertEquals(1, backend.destroys)
        assertEquals(1, backend.terminations)
    }

    @Test fun interruptedPartialRetirementKeepsTheWindowUntilRetry() {
        val gate = CompletableDeferred<Unit>()
        val retry = CompletableDeferred<Unit>()
        var attempts = 0
        val backend = RecordingBackend().apply {
            beforeRetirement = {
                if (++attempts == 1) throw CancellationException("platform retirement interrupted")
                retry.complete(Unit)
                gate.await()
            }
        }
        DawnMainThreadLoop.run(backend) { main ->
            assertFailsWith<IllegalArgumentException> {
                openDawnWindow(DawnWindowOptions(), DawnSessionOptions(), DawnPresentationOptions(), main) { _, _, _ ->
                    throw IllegalArgumentException("startup failed")
                }
            }
            try {
                withTimeout(2_000) { retry.await() }
                assertEquals(0, backend.destroys)
                assertTrue(backend.initialized)
            } finally { gate.complete(Unit) }
        }
        assertEquals(1, backend.destroys)
        assertEquals(1, backend.terminations)
    }

    @Test fun failedStartupReturnsWhileNativeRetirementIsStillPending() {
        val gate = CompletableDeferred<Unit>()
        val entered = CompletableDeferred<Unit>()
        val backend = RecordingBackend().apply {
            beforeRetirement = { entered.complete(Unit); gate.await() }
        }
        DawnMainThreadLoop.run(backend) { main ->
            val opening = async {
                runCatching {
                    openDawnWindow(DawnWindowOptions(), DawnSessionOptions(), DawnPresentationOptions(), main) { _, _, _ ->
                        throw IllegalArgumentException("startup failed")
                    }
                }
            }
            try {
                entered.await()
                delay(50)
                assertTrue(opening.isCompleted, "startup error must not wait for native retirement")
                assertEquals(0, backend.destroys)
                assertTrue(backend.initialized)
                assertIs<IllegalArgumentException>(opening.await().exceptionOrNull())
            } finally { gate.complete(Unit); opening.await() }
        }
        assertEquals(1, backend.destroys)
        assertEquals(1, backend.terminations)
    }

    @Test fun liveShutdownTimeoutPreservesTheRunningMainOwner() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        DawnMainThreadLoop.run { main ->
            val window = openDawnWindow(DawnWindowOptions(width = 32, height = 32),
                DawnSessionOptions(shutdownTimeout = 100.milliseconds), mainThread = main)
            val entered = CompletableDeferred<Unit>()
            val finish = CompletableDeferred<Unit>()
            val application = launch(Dispatchers.Default) { window.gpu.run { entered.complete(Unit); finish.await() } }
            try {
                entered.await()
                assertFailsWith<DawnShutdownTimeoutException> { window.shutdown() }
                assertTrue(main.runtime.isActive)
                main.owner.onOwner { assertEquals(GLFW.GLFW_TRUE, GLFW.glfwGetWindowAttrib(window.nativeHandle, GLFW.GLFW_VISIBLE)) }
            } finally {
                finish.complete(Unit)
                application.join()
                window.shutdown()
            }
        }
    }

    @Test fun liveSecondWindowSurvivesFirstCloseAndReportsResize() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        DawnMainThreadLoop.run { main ->
            val first = openDawnWindow(DawnWindowOptions(width = 64, height = 64), mainThread = main)
            val second = openDawnWindow(DawnWindowOptions(width = 64, height = 64), mainThread = main)
            try {
                first.shutdown()
                val original = second.extent.value
                assertFailsWith<IllegalArgumentException> { second.resize(0, 80) }
                second.resize(96, 80)
                withTimeout(3_000) { second.extent.first { it != original && it.isPositive } }
                assertTrue(second.presentation.withFrame { it.present() })
                second.requestClose()
                withTimeout(3_000) { second.events.first { it == DawnWindowEvent.CloseRequested } }
            } finally { first.shutdown(); second.shutdown() }
            assertFailsWith<IllegalStateException> { second.resize(32, 32) }
        }
    }

    @Test fun liveBorrowedRuntimeRemainsInitialized() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        DawnMainThreadLoop.run { main ->
            assertTrue(GLFW.glfwInit())
            try {
                val window = openDawnWindow(DawnWindowOptions(width = 32, height = 32,
                    runtimeOwnership = DawnGlfwRuntimeOwnership.Borrowed), mainThread = main)
                window.shutdown()
                assertNotEquals(0, GLFW.glfwGetPlatform())
            } finally { GLFW.glfwTerminate() }
        }
    }

    @Test fun pixelExtentFailureDestroysTheNewWindow() {
        val backend = RecordingBackend().apply { extentFailure = IllegalArgumentException("extent query failed") }
        assertFailsWith<IllegalArgumentException> {
            DawnMainThreadLoop.run(backend) { main ->
                openDawnWindow(DawnWindowOptions(), DawnSessionOptions(), DawnPresentationOptions(), main) { _, _, _ ->
                    error("surface opener must not be reached")
                }
            }
        }
        assertEquals(1, backend.destroys)
    }
    @Test fun partialWindowStartupDestroysWindow() {
        val backend = RecordingBackend()
        assertFailsWith<IllegalArgumentException> {
            DawnMainThreadLoop.run(backend) { main ->
                openDawnWindow(DawnWindowOptions(), DawnSessionOptions(), DawnPresentationOptions(), main) { _, _, _ ->
                    throw IllegalArgumentException("surface startup failed")
                }
            }
        }
        assertEquals(1, backend.creates)
        assertEquals(1, backend.destroys)
        assertEquals(1, backend.terminations)
    }
    @Test fun nonMainCallerFailsBeforeAllocation() = runBlocking {
        val backend = RecordingBackend()
        withContext(Dispatchers.Default) {
            assertFailsWith<IllegalStateException> { DawnMainThreadLoop.run(backend) { } }
        }
        assertEquals(0, backend.creates)
        assertEquals(0, backend.initializations)
    }
    @Test fun liveWindowRendersAndCloses() {
        assumeTrue(System.getenv("DAWN_DESKTOP_TESTS") == "1")
        var captured: DawnWindow? = null
        runBlocking {
            withDawnWindow(DawnWindowOptions(width = 64, height = 64)) { gpu, window, presentation ->
                captured = window
                assertTrue(window.extent.value.isPositive)
                assertTrue(presentation.withFrame { frame ->
                    (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                        encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
                            RenderPassColorAttachment(frame.view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                                clearValue = Color(0.0, 0.0, 1.0, 1.0)),
                        ))).use { it.end() }
                        encoder.finish().use { gpu.queue.submit(listOf(it)) }
                    }
                    gpu.queue.onSubmittedWorkDone().getOrThrow()
                    frame.present()
                })
                assertFailsWith<IllegalStateException> { window.shutdown() }
                window.requestClose()
            }
            val closed = assertNotNull(captured)
            closed.shutdown()
            closed.shutdown()
        }
    }
}
