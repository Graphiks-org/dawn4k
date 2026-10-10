package org.graphiks.dawn4k.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.DawnComputePipeline
import org.graphiks.dawn4k.DawnRenderPipeline
import org.graphiks.dawn4k.createComputePipelineAsync
import org.graphiks.dawn4k.createRenderPipelineAsync
import org.graphiks.dawn4k.createShaderModule
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.dawn4k.testing.gpuTestConfig
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertTrue

/**
 * Real-GPU lifecycle of [DawnRuntime]: twenty device sessions opened and closed
 * leave no open callback registration and no owned reference behind, and a wait
 * in flight when the runtime closes fails with the close diagnostic instead of
 * hanging — the async pipeline creations racing that close fold into their
 * [Result] contract instead of throwing out of it. Standard tasks run these
 * tests, with named warnings on adapter absence (or failure in strict mode).
 */
class DawnRuntimeGpuTest {

    @Test
    fun twentySessionsOpenAndCloseLeavingNoRegistrationsOrRemainingRefs() = runBlocking {
        if (!gpuTestEnvironment("DawnRuntimeGpuTest.twentySessionsOpenAndCloseLeavingNoRegistrationsOrRemainingRefs")) return@runBlocking
        // The fixture raw-opens once first: it is the entry point of the later
        // tasks and delegates to exactly the machinery stressed below.
        NativeFixture.open().use { fixture ->
            assertEquals(0, fixture.runtime.debugOpenCallbacks())
        }

        val runtime = DawnRuntime(gpuTestConfig())
        try {
            repeat(20) {
                val session = runtime.openSession()
                // Only the queue ref is owned so far; later tasks add more.
                assertEquals(1, session.resources.debugRemainingRefs())
                session.close()
                assertEquals(0, session.resources.debugRemainingRefs())
                session.close()
                runtime.drainEvents()
            }
            assertEquals(0, runtime.debugOpenCallbacks())
        } finally {
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    @Test
    fun waitInterruptedByRuntimeCloseFailsWithTheCloseDiagnostic() = runBlocking {
        if (!gpuTestEnvironment("DawnRuntimeGpuTest.waitInterruptedByRuntimeCloseFailsWithTheCloseDiagnostic")) return@runBlocking
        val runtime = DawnRuntime(gpuTestConfig())
        try {
            // Device creation must succeed after the availability probe.
            runtime.openSession().close()

            val interrupted = runtime.dispatcher.call {
                // Start the request and close in one worker operation. Merely
                // observing an open callback from another thread races its
                // completion on fast adapters, notably Windows D3D12.
                val opening = async(start = CoroutineStart.UNDISPATCHED) {
                    runCatching { runtime.openSession() }
                }
                assertTrue(runtime.debugOpenCallbacks() > 0)
                runtime.close()
                opening
            }

            val outcome = interrupted.await()
            assertTrue(outcome.isFailure)
            assertIs<DawnRuntimeClosedException>(outcome.exceptionOrNull())
        } finally {
            runtime.close()
        }
        // A worker-reentrant close returns before the late callback is drained.
        withTimeout(ADAPTER_HANDOFF_TIMEOUT_MS) {
            while (runtime.debugOpenCallbacks() != 0) yield()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    /**
     * The invariant is the fold, never the throw: an async pipeline creation
     * that is in flight when the runtime closes lands inside its [Result]
     * contract either way. The close abandons a creation still in flight into
     * `Result.failure(DawnRuntimeClosedException)`; a creation whose WGSL
     * compilation settles before the close enqueue completes successfully.
     * Both shapes are the contract, so both are accepted: the ≥2-callbacks
     * hand-off below maximizes the abandoned shape but cannot guarantee it
     * under load, and the invariant under test holds either way.
     */
    @Test
    fun asyncPipelineCreationsFoldAcrossRuntimeClose() = runBlocking {
        if (!gpuTestEnvironment("DawnRuntimeGpuTest.asyncPipelineCreationsFoldAcrossRuntimeClose")) return@runBlocking
        val runtime = DawnRuntime(gpuTestConfig())
        try {
            val session = runtime.openSession()
            // The close below deliberately breaks the ownership order — sessions
            // close before their runtime — because only a runtime close abandons
            // an in-flight creation. The session therefore outlives the
            // dispatcher it routes through: its close would throw through the
            // dead dispatcher, so it is not attempted and the session's own
            // references are orphaned with the runtime instead.
            val computeShader = session.createShaderModule(ShaderModuleDescriptor(COMPUTE_SHADER))
            val renderShader = session.createShaderModule(ShaderModuleDescriptor(RENDER_SHADER))
            val compute = async(Dispatchers.Default) {
                session.createComputePipelineAsync(ComputePipelineDescriptor(ProgrammableStage(computeShader, "main")))
            }
            val render = async(Dispatchers.Default) {
                session.createRenderPipelineAsync(renderPipelineDescriptor(renderShader))
            }
            // Best-effort hand-off: both creations have their callback
            // registrations open before the close, which maximizes the
            // abandoned shape — but a creation may legitimately settle first,
            // so this must not be load-bearing.
            withTimeout(CREATION_HANDOFF_TIMEOUT_MS) {
                while (runtime.debugOpenCallbacks() < 2 && !compute.isCompleted && !render.isCompleted) yield()
            }
            runtime.close()

            // The fold: each in-flight creation lands inside its Result — the
            // abandoned shape fails with the close diagnostic, the
            // settled-before-close shape delivers a live pipeline. Neither
            // ever throws out of the contract.
            val computeOutcome = compute.await()
            if (computeOutcome.isFailure) {
                assertIs<DawnRuntimeClosedException>(computeOutcome.exceptionOrNull())
            } else {
                assertIs<DawnComputePipeline>(computeOutcome.getOrThrow())
            }
            val renderOutcome = render.await()
            if (renderOutcome.isFailure) {
                assertIs<DawnRuntimeClosedException>(renderOutcome.exceptionOrNull())
            } else {
                assertIs<DawnRenderPipeline>(renderOutcome.getOrThrow())
            }

            // A creation issued after the close folds the dead dispatcher's
            // refusal into the Result the same way, never out of the contract —
            // and here nothing can settle anymore, so failure is strict.
            val late = session.createComputePipelineAsync(
                ComputePipelineDescriptor(ProgrammableStage(computeShader, "main")),
            )
            assertTrue(late.isFailure)
        } finally {
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    /** A valid async render target: the full-screen triangle on an RGBA8 view. */
    private fun renderPipelineDescriptor(shader: GPUShaderModule): RenderPipelineDescriptor = RenderPipelineDescriptor(
        vertex = VertexState(module = shader, entryPoint = "vs_main"),
        fragment = FragmentState(
            module = shader,
            entryPoint = "fs_main",
            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
        ),
    )

    private companion object {
        const val ADAPTER_HANDOFF_TIMEOUT_MS = 20_000L
        // Generous on purpose: under a heavily loaded build (a forced rerun of the
        // whole battery, a concurrent Kotlin/Native link), the two in-flight
        // creations can take tens of seconds just to be scheduled.
        const val CREATION_HANDOFF_TIMEOUT_MS = 60_000L
    }
}

/** A one-entry-point compute shader: the async creation's valid target. */
private val COMPUTE_SHADER = """
    @group(0) @binding(0) var<storage, read_write> value: array<u32>;

    @compute @workgroup_size(1)
    fn main() {
        value[0] = 1u;
    }
""".trimIndent()

/** A full-screen triangle: the async render creation's valid target. */
private val RENDER_SHADER = """
    @vertex
    fn vs_main(@builtin(vertex_index) vertexIndex: u32) -> @builtin(position) vec4f {
        var positions = array<vec2f, 3>(
            vec2f(-1.0, -1.0),
            vec2f( 3.0, -1.0),
            vec2f(-1.0,  3.0),
        );
        return vec4f(positions[vertexIndex], 0.0, 1.0);
    }

    @fragment
    fn fs_main() -> @location(0) vec4f {
        return vec4f(1.0, 0.0, 0.0, 1.0);
    }
""".trimIndent()
