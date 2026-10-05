package org.graphiks.dawn4k

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ColorTargetState
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.FragmentState
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.QueueDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The public Graphiks WebGPU contract of this backend, validated against the
 * PUBLISHED acid suite: a context requests a fresh adapter, the adapter a
 * device, and the borrowed device runs the published buffer cases verbatim.
 * The companion cases pin the contract's own promises — fresh adapters,
 * required features and limits, error scopes, the uncaptured-error callback,
 * labels, and async pipeline creation.
 *
 * Runs only through the gpuTest* tasks; a host without an adapter fails these
 * tests (no silent skip).
 */
class PublicContractGpuTest {

    @Test
    fun publicBuffersMatchTheSharedContract() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().use { device ->
                    org.graphiks.webgpu.suite.acid.buffers.mapWriteRoundTrip(device)
                    org.graphiks.webgpu.suite.acid.buffers.mappedAtCreation(device)
                }
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun eachAdapterRequestYieldsAFreshAdapter() = runTest {
        val context = DawnContext.create()
        try {
            val first = context.requestAdapter().getOrThrow()
            try {
                val second = context.requestAdapter().getOrThrow()
                try {
                    assertTrue(first !== second, "two adapter requests must not share one adapter object")
                    // Both fresh references stay independently usable.
                    assertEquals(first.info.vendor, second.info.vendor)
                    assertEquals(first.limits.maxTextureDimension2D, second.limits.maxTextureDimension2D)
                } finally {
                    second.close()
                }
            } finally {
                first.close()
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun requiredFeaturesAreHonoured() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                val supported = adapter.features
                assertTrue(
                    supported.isNotEmpty(),
                    "the adapter reports no feature at all; the required-features case cannot run",
                )
                val present = supported.first()
                adapter.requestDevice(
                    DeviceDescriptor(requiredFeatures = listOf(present)),
                ).getOrThrow().use { device ->
                    assertTrue(
                        present in device.features,
                        "the required feature $present must be among the device's features ${device.features}",
                    )
                }
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun unsupportedRequiredFeatureIsRefused() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                // The refusal's target: a feature the adapter lacks when one
                // exists. The validation machine reports every contract feature
                // (Apple M2 Max / Metal), so the case degenerates to proving the
                // same required-capabilities plumbing with a request no adapter
                // can satisfy — a supported feature plus an impossible limit;
                // the limit's isolation is proven by its own case below.
                val unsupported = GPUFeatureName.entries.firstOrNull { it !in adapter.features }
                val result = when (unsupported) {
                    null -> adapter.requestDevice(
                        DeviceDescriptor(
                            requiredFeatures = listOf(adapter.features.first()),
                            requiredLimits = impossibleLimits(adapter),
                        ),
                    )
                    else -> adapter.requestDevice(DeviceDescriptor(requiredFeatures = listOf(unsupported)))
                }
                result.getOrNull()?.close()
                assertTrue(
                    result.isFailure,
                    "an unsatisfiable required-capabilities request must fail the device request",
                )
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun requiredLimitsAtAdapterValuesAreAccepted() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                val required = object : GPUSupportedLimits by adapter.limits {}
                adapter.requestDevice(DeviceDescriptor(requiredLimits = required)).getOrThrow().use { device ->
                    assertTrue(
                        device.limits.maxComputeWorkgroupSizeX >= required.maxComputeWorkgroupSizeX,
                        "the device maxComputeWorkgroupSizeX ${device.limits.maxComputeWorkgroupSizeX} " +
                            "is below the required ${required.maxComputeWorkgroupSizeX}",
                    )
                }
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun excessiveRequiredLimitsAreRefused() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                val base = adapter.limits.maxComputeWorkgroupSizeX
                val result = adapter.requestDevice(DeviceDescriptor(requiredLimits = impossibleLimits(adapter)))
                result.getOrNull()?.close()
                assertTrue(
                    result.isFailure,
                    "requiring maxComputeWorkgroupSizeX=${base + 1u} above the adapter's $base must fail",
                )
            }
        } finally {
            context.close()
        }
    }

    @Test
    fun emptyErrorScopesPopWithoutError() = runTest {
        withPublicDevice { device ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            assertNull(
                device.popErrorScope().getOrThrow(),
                "an empty validation scope must pop without an error",
            )
        }
    }

    @Test
    fun nestedErrorScopesPopInReverseOrder() = runTest {
        withPublicDevice { device ->
            device.pushErrorScope(GPUErrorFilter.Validation)
            device.pushErrorScope(GPUErrorFilter.Validation)
            // Usage None is invalid in a conforming backend (the published
            // suite's own uncaptured oracle): Dawn generates the validation
            // error of the failed creation synchronously — captured here by
            // the INNER scope — and hands back the invalid object, which the
            // published case closes like this one does.
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
            val inner = device.popErrorScope().getOrThrow()
            assertIs<GPUValidationError>(
                inner,
                "the inner scope must capture the validation error (observed: $inner)",
            )
            val outer = device.popErrorScope().getOrThrow()
            assertNull(outer, "the outer scope observed no error of its own")
        }
    }

    @Test
    fun uncapturedErrorReachesTheDescriptorCallback() = runTest {
        val received = CompletableDeferred<GPUError>()
        withPublicDevice(
            DeviceDescriptor(onUncapturedError = GPUUncapturedErrorCallback { received.complete(it) }),
        ) { device ->
            // The invalid buffer sits OUTSIDE any scope, so its validation
            // error cannot be captured and must reach the descriptor's
            // callback; the invalid object is closed like the published case.
            device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
            // Chromium-style late delivery is bounded by real work, never a sleep.
            device.queue.onSubmittedWorkDone().getOrThrow()
            val error = withTimeout(5.seconds) { received.await() }
            assertIs<GPUValidationError>(
                error,
                "the invalid buffer outside any scope must reach the uncaptured-error callback (message: ${error.message})",
            )
        }
    }

    @Test
    fun deviceAndQueueKeepTheirDescriptorLabels() = runTest {
        withPublicDevice(
            DeviceDescriptor(
                label = "public-device-λ",
                defaultQueue = QueueDescriptor(label = "public-queue-λ"),
            ),
        ) { device ->
            assertEquals("public-device-λ", device.label, "the device keeps its descriptor label")
            assertEquals("public-queue-λ", device.queue.label, "the default queue keeps its descriptor label")
            device.label = "renamed-device-λ"
            assertEquals("renamed-device-λ", device.label)
            val saved = device.queue.label
            device.queue.label = "renamed-queue-λ"
            assertEquals(
                "renamed-queue-λ",
                device.queue.label,
                "the queue label must be stable across queue accesses",
            )
            device.queue.label = saved
        }
    }

    @Test
    fun asyncComputePipelineCreationResolvesAndRejects() = runTest {
        withPublicDevice { device ->
            device.createShaderModule(ShaderModuleDescriptor(COMPUTE_SHADER)).use { shader ->
                device.createComputePipelineAsync(
                    ComputePipelineDescriptor(
                        compute = ProgrammableStage(shader, entryPoint = "main"),
                        label = "async-compute-λ",
                    ),
                ).getOrThrow().use { pipeline ->
                    assertEquals("async-compute-λ", pipeline.label, "the async pipeline keeps its descriptor label")
                }
                val rejected = device.createComputePipelineAsync(
                    ComputePipelineDescriptor(
                        compute = ProgrammableStage(shader, entryPoint = "no_such_entry"),
                        label = "rejected-compute-λ",
                    ),
                )
                rejected.getOrNull()?.close()
                assertTrue(
                    rejected.isFailure,
                    "an unknown compute entry point must reject the async creation",
                )
            }
        }
    }

    @Test
    fun asyncRenderPipelineCreationResolvesAndRejects() = runTest {
        withPublicDevice { device ->
            device.createShaderModule(ShaderModuleDescriptor(RENDER_SHADER)).use { shader ->
                device.createRenderPipelineAsync(renderDescriptor(shader, "vs_main")).getOrThrow().use { pipeline ->
                    assertEquals("async-render-λ", pipeline.label, "the async pipeline keeps its descriptor label")
                }
                val rejected = device.createRenderPipelineAsync(
                    renderDescriptor(shader, "no_such_entry", label = "rejected-render-λ"),
                )
                rejected.getOrNull()?.close()
                assertTrue(
                    rejected.isFailure,
                    "an unknown vertex entry point must reject the async creation",
                )
            }
        }
    }

    // --- Helpers ------------------------------------------------------------

    /**
     * Runs [block] on a fresh public device: its own context, fresh adapter,
     * device from [descriptor] (default when null), closed in reverse order.
     */
    private suspend fun withPublicDevice(
        descriptor: GPUDeviceDescriptor? = null,
        block: suspend (GPUDevice) -> Unit,
    ) {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter: GPUAdapter ->
                adapter.requestDevice(descriptor).getOrThrow().use { device ->
                    block(device)
                }
            }
        } finally {
            context.close()
        }
    }

    /** One above the adapter's own workgroup-size bound: no adapter accepts it. */
    private fun impossibleLimits(adapter: GPUAdapter): GPUSupportedLimits {
        val base = adapter.limits.maxComputeWorkgroupSizeX
        assertTrue(base < UInt.MAX_VALUE, "maxComputeWorkgroupSizeX is the maximum UInt; no excess can exist")
        return object : GPUSupportedLimits by adapter.limits {
            override val maxComputeWorkgroupSizeX = base + 1u
        }
    }

    private fun renderDescriptor(
        shader: GPUShaderModule,
        entryPoint: String,
        label: String = "async-render-λ",
    ): RenderPipelineDescriptor = RenderPipelineDescriptor(
        vertex = VertexState(module = shader, entryPoint = entryPoint),
        fragment = FragmentState(
            module = shader,
            entryPoint = "fs_main",
            targets = listOf(ColorTargetState(format = GPUTextureFormat.RGBA8Unorm)),
        ),
        label = label,
    )
}

/** A one-entry-point compute shader: the async factory's valid target. */
private val COMPUTE_SHADER = """
    @group(0) @binding(0) var<storage, read_write> value: array<u32>;

    @compute @workgroup_size(1)
    fn main() {
        value[0] = 1u;
    }
""".trimIndent()

/** A full-screen triangle: valid render targets for the async factory. */
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
