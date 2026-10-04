package org.graphiks.dawn4k.internal

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.createComputePipelineAsync
import org.graphiks.dawn4k.createRenderPipelineAsync
import org.graphiks.dawn4k.createShaderModule
import org.graphiks.dawn4k.testing.NativeFixture
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
 * hanging — the async pipeline creations abandoned by that close fold into
 * their [Result] contract instead of throwing out of it. Runs only through the
 * gpuTest* tasks; a host without an adapter fails these tests (no silent skip).
 */
class DawnRuntimeGpuTest {

    @Test
    fun twentySessionsOpenAndCloseLeavingNoRegistrationsOrRemainingRefs() = runBlocking {
        // The fixture raw-opens once first: it is the entry point of the later
        // tasks and delegates to exactly the machinery stressed below.
        NativeFixture.open().use { fixture ->
            assertEquals(0, fixture.runtime.debugOpenCallbacks())
        }

        val runtime = DawnRuntime(DawnConfig())
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
        val runtime = DawnRuntime(DawnConfig())
        try {
            // Prove the GPU first: a host without an adapter fails here.
            runtime.openSession().close()

            val interrupted = async(Dispatchers.Default) {
                runCatching { runtime.openSession() }
            }
            // Deterministic hand-off: wait until the second openSession has an
            // adapter request in flight — its callback registration is open.
            withTimeout(ADAPTER_HANDOFF_TIMEOUT_MS) {
                while (runtime.debugOpenCallbacks() == 0) yield()
            }
            runtime.close()

            val outcome = interrupted.await()
            assertTrue(outcome.isFailure)
            assertIs<DawnRuntimeClosedException>(outcome.exceptionOrNull())
        } finally {
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    @Test
    fun asyncPipelineCreationsAbandonedByRuntimeCloseFoldIntoFailures() = runBlocking {
        val runtime = DawnRuntime(DawnConfig())
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
            // Deterministic hand-off: both creations have their callback
            // registrations open before the close abandons them.
            withTimeout(CREATION_HANDOFF_TIMEOUT_MS) {
                while (runtime.debugOpenCallbacks() < 2) yield()
            }
            runtime.close()

            val computeOutcome = compute.await()
            assertTrue(computeOutcome.isFailure)
            assertIs<DawnRuntimeClosedException>(computeOutcome.exceptionOrNull())
            val renderOutcome = render.await()
            assertTrue(renderOutcome.isFailure)
            assertIs<DawnRuntimeClosedException>(renderOutcome.exceptionOrNull())

            // A creation issued after the close folds the dead dispatcher's
            // refusal into the Result the same way, never out of the contract.
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
        const val CREATION_HANDOFF_TIMEOUT_MS = 20_000L
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
