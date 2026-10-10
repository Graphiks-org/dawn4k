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
import org.graphiks.dawn4k.startTestProgress
import org.graphiks.dawn4k.settleTestEvents
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
import kotlin.test.assertFailsWith

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
        val events = runtime.startTestProgress(this)
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
            events.cancel()
            runtime.settleTestEvents()
            runtime.close()
        }
        assertEquals(0, runtime.debugOpenCallbacks())
    }

    @Test
    fun closeRefusesAnOutstandingRequestAndCanBeRetried() = runBlocking {
        if (!gpuTestEnvironment("DawnRuntimeGpuTest.closeRefusesAnOutstandingRequestAndCanBeRetried")) return@runBlocking
        val runtime = DawnRuntime(gpuTestConfig())
        try {
            val opening = async(start = CoroutineStart.UNDISPATCHED) { runtime.openSession() }
            assertFailsWith<IllegalStateException> { runtime.close() }
            withTimeout(ADAPTER_HANDOFF_TIMEOUT_MS) {
                while (!opening.isCompleted) { runtime.processEvents(); yield() }
            }
            opening.await().close()
            runtime.settleTestEvents()
        } finally {
            runtime.close()
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
    fun asyncPipelinesMustSettleBeforeDeviceAndContextClose() = runBlocking {
        if (!gpuTestEnvironment("DawnRuntimeGpuTest.asyncPipelinesMustSettleBeforeDeviceAndContextClose")) return@runBlocking
        val runtime = DawnRuntime(gpuTestConfig())
        val events = runtime.startTestProgress(this)
        try {
            val session = runtime.openSession()
            val computeShader = session.createShaderModule(ShaderModuleDescriptor(COMPUTE_SHADER))
            val renderShader = session.createShaderModule(ShaderModuleDescriptor(RENDER_SHADER))
            events.cancel()
            val compute = async(start = CoroutineStart.UNDISPATCHED) {
                session.createComputePipelineAsync(ComputePipelineDescriptor(ProgrammableStage(computeShader, "main")))
            }
            val render = async(start = CoroutineStart.UNDISPATCHED) {
                session.createRenderPipelineAsync(renderPipelineDescriptor(renderShader))
            }
            assertFailsWith<IllegalStateException> { session.close() }
            assertFailsWith<IllegalStateException> { runtime.close() }
            withTimeout(CREATION_HANDOFF_TIMEOUT_MS) {
                while (!compute.isCompleted || !render.isCompleted) { runtime.processEvents(); yield() }
            }
            assertIs<DawnComputePipeline>(compute.await().getOrThrow()).close()
            assertIs<DawnRenderPipeline>(render.await().getOrThrow()).close()
            session.close()
            runtime.settleTestEvents()
        } finally {
            events.cancel()
            runtime.settleTestEvents()
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
