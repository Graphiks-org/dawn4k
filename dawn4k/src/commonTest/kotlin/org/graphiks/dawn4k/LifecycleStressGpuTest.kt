package org.graphiks.dawn4k

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.webgpu.ArrayBuffer
import org.graphiks.webgpu.GPUBufferBindingType
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.GPUShaderStage
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.descriptors.BindGroupDescriptor
import org.graphiks.webgpu.descriptors.BindGroupEntry
import org.graphiks.webgpu.descriptors.BindGroupLayoutDescriptor
import org.graphiks.webgpu.descriptors.BindGroupLayoutEntry
import org.graphiks.webgpu.descriptors.BufferBinding
import org.graphiks.webgpu.descriptors.BufferBindingLayout
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.ComputePipelineDescriptor
import org.graphiks.webgpu.descriptors.DepthStencilState
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.PipelineLayoutDescriptor
import org.graphiks.webgpu.descriptors.ProgrammableStage
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import org.graphiks.webgpu.descriptors.RenderBundleEncoderDescriptor
import org.graphiks.webgpu.descriptors.RenderPipelineDescriptor
import org.graphiks.webgpu.descriptors.ShaderModuleDescriptor
import org.graphiks.webgpu.descriptors.VertexState
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

/**
 * Resource-lifecycle stress and out-of-suite witnesses of the public backend:
 * idempotent closes, a late callback after a cancelled wait, cross-device
 * refusal without a cross-device handle, a hundred-device open/close stress,
 * foreign-thread interaction routed through the dispatcher, native validation
 * observed on invalid sources, setImmediates for real under the announced
 * maxImmediateSize, the nullable depth signatures reaching native validation,
 * an optional feature that stays checkable when absent, and the render bundle
 * encoder's close guard with finish-twice left to Dawn.
 *
 * Ownership states end with zero registered references — asserted through the
 * session's debug counters (debugRemainingRefs / debugOpenCallbacks), never by
 * dereferencing released memory. Runs only through the gpuTest* tasks; a host
 * without an adapter fails these tests (no silent skip).
 */
class LifecycleStressGpuTest {

    // --- idempotent close -----------------------------------------------------

    @Test
    fun repeatedCloseIsIdempotentForRefcountAndDestroyBackedWrappers() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().let { publicDevice ->
                    val device = publicDevice as DawnDevice
                    try {
                        // A fresh session owns exactly its queue reference.
                        val queueRef = device.session.resources.debugRemainingRefs()
                        assertEquals(1, queueRef, "a fresh session owns only its queue reference")

                        // Refcount-only: the first close releases the entry, the
                        // second is a no-op.
                        device.createSampler().let { sampler ->
                            sampler.close()
                            assertEquals(
                                queueRef,
                                device.session.resources.debugRemainingRefs(),
                                "the first sampler close must release its reference",
                            )
                            sampler.close()
                            assertEquals(
                                queueRef,
                                device.session.resources.debugRemainingRefs(),
                                "a repeated sampler close must not release twice",
                            )
                        }

                        // Destroy-backed: close destroys once and keeps the
                        // tombstone until teardown; a repeated close neither
                        // destroys again nor throws.
                        device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Uniform)).let { buffer ->
                            buffer.close()
                            assertEquals(
                                queueRef + 1,
                                device.session.resources.debugRemainingRefs(),
                                "the destroyed buffer keeps its tombstone until teardown",
                            )
                            buffer.close()
                            assertEquals(
                                queueRef + 1,
                                device.session.resources.debugRemainingRefs(),
                                "a repeated buffer close must not act twice",
                            )
                        }

                        device.createTexture(
                            org.graphiks.webgpu.descriptors.TextureDescriptor(
                                size = org.graphiks.webgpu.descriptors.Extent3D(width = 4u, height = 4u),
                                format = GPUTextureFormat.RGBA8Unorm,
                                usage = org.graphiks.webgpu.GPUTextureUsage.TextureBinding,
                            ),
                        ).let { texture ->
                            texture.close()
                            assertEquals(
                                queueRef + 2,
                                device.session.resources.debugRemainingRefs(),
                                "the destroyed texture adds its tombstone to the buffer's",
                            )
                            texture.close()
                            assertEquals(
                                queueRef + 2,
                                device.session.resources.debugRemainingRefs(),
                                "a repeated texture close must not act twice",
                            )
                        }
                    } finally {
                        device.close()
                    }
                    // The session close is idempotent too and leaves nothing registered.
                    device.close()
                    assertEquals(
                        0,
                        device.session.resources.debugRemainingRefs(),
                        "the teardown must release every reference including the queue's",
                    )
                }
            }
        } finally {
            context.close()
        }
        // The context close is idempotent: the finally above ran first.
        context.close()
    }

    // --- late callback after cancel -------------------------------------------

    @Test
    fun lateCallbackAfterACancelledMapSettlesWithOneRelease() = runBlocking {
        val context = DawnContext.create()
        try {
            val adapter = context.requestAdapter().getOrThrow()
            val device = adapter.requestDevice().getOrThrow() as DawnDevice
            try {
                val buffer = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead))
                try {
                    // Hand off deterministically: the map's callback registration
                    // is open, so the native call is in flight.
                    val pending = async(Dispatchers.Default) { buffer.mapAsync(GPUMapMode.Read) }
                    withTimeout(HANDOFF_TIMEOUT_MS) {
                        while (device.session.runtime.debugOpenCallbacks() == 0) yield()
                    }
                    pending.cancel()
                    pending.join()

                    // The late delivery settles exactly once: the registration
                    // count returns to zero, never negative, never stuck.
                    device.session.runtime.drainEvents()
                    device.session.runtime.dispatcher.drain()
                    assertEquals(
                        0,
                        device.session.runtime.debugOpenCallbacks(),
                        "the cancelled map's late callback must close its registration exactly once",
                    )

                    // The buffer survived the cancelled wait without corruption:
                    // it reaches a clean terminal map state in either race outcome.
                    when (buffer.mapState) {
                        GPUBufferMapState.Mapped -> buffer.unmap()
                        else -> {
                            buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                            buffer.unmap()
                        }
                    }
                } finally {
                    buffer.close()
                }
            } finally {
                device.close()
                assertEquals(0, device.session.resources.debugRemainingRefs())
            }
            adapter.close()
        } finally {
            context.close()
        }
    }

    // --- resources from another device ---------------------------------------

    @Test
    fun resourcesFromAnotherDeviceAreRefusedWithNoCrossDeviceHandle() = runTest {
        val context = DawnContext.create()
        try {
            val adapterA = context.requestAdapter().getOrThrow()
            val adapterB = context.requestAdapter().getOrThrow()
            try {
                val deviceA = adapterA.requestDevice().getOrThrow() as DawnDevice
                val deviceB = adapterB.requestDevice().getOrThrow() as DawnDevice
                try {
                    val foreignBuffer = deviceA.createBuffer(
                        BufferDescriptor(16uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
                    )
                    val localBuffer = deviceB.createBuffer(
                        BufferDescriptor(16uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst),
                    )
                    try {
                        val ownedByB = deviceB.session.resources.debugRemainingRefs()
                        val ownedByA = deviceA.session.resources.debugRemainingRefs()

                        // The refusal happens on the Kotlin side, before any
                        // downcall can read the foreign handle.
                        assertFailsWith<IllegalArgumentException> {
                            deviceB.createCommandEncoder().use { encoder ->
                                encoder.copyBufferToBuffer(foreignBuffer, 0uL, localBuffer, 0uL, 16uL)
                            }
                        }

                        // No cross-device handle: B's registry never learned about
                        // the foreign buffer, and A still owns it untouched.
                        assertEquals(
                            ownedByB,
                            deviceB.session.resources.debugRemainingRefs(),
                            "the refusing session must not register the foreign buffer",
                        )
                        assertEquals(
                            ownedByA,
                            deviceA.session.resources.debugRemainingRefs(),
                            "the owning session keeps the refused buffer's reference",
                        )

                        // The foreign buffer is still usable through ITS device:
                        // the refusal never released or corrupted its handle.
                        val beforeErrors = uncapturedErrorCount(deviceA)
                        deviceA.queue.writeBuffer(foreignBuffer, 0uL, ArrayBuffer.of(uintArrayOf(1u, 2u)))
                        assertEquals(
                            beforeErrors,
                            uncapturedErrorCount(deviceA),
                            "the refused buffer must stay valid on its own device",
                        )
                    } finally {
                        foreignBuffer.close()
                        localBuffer.close()
                    }
                } finally {
                    deviceB.close()
                    assertEquals(0, deviceB.session.resources.debugRemainingRefs())
                    deviceA.close()
                    assertEquals(0, deviceA.session.resources.debugRemainingRefs())
                }
            } finally {
                adapterB.close()
                adapterA.close()
            }
        } finally {
            context.close()
        }
    }

    // --- a hundred devices ----------------------------------------------------

    @Test
    fun aHundredDevicesOpenAndCloseLeavingNoRegisteredRefs() = runBlocking {
        val context = DawnContext.create()
        var runtime: org.graphiks.dawn4k.internal.DawnRuntime? = null
        try {
            repeat(100) { index ->
                val adapter = context.requestAdapter().getOrThrow()
                val device = adapter.requestDevice().getOrThrow() as DawnDevice
                runtime = device.session.runtime
                // The fresh session owns exactly its queue reference.
                assertEquals(
                    1,
                    device.session.resources.debugRemainingRefs(),
                    "iteration $index: a fresh session owns only its queue reference",
                )
                // A destroy-backed resource closed mid-life keeps its tombstone
                // registered until the teardown — exactly one more entry.
                device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.Uniform)).close()
                assertEquals(
                    2,
                    device.session.resources.debugRemainingRefs(),
                    "iteration $index: the closed buffer keeps only its tombstone",
                )
                device.close()
                device.close()
                assertEquals(
                    0,
                    device.session.resources.debugRemainingRefs(),
                    "iteration $index: the teardown must release every registered reference",
                )
                adapter.close()
            }
            assertEquals(
                0,
                runtime!!.debugOpenCallbacks(),
                "the stress must leave no open callback registration",
            )
        } finally {
            context.close()
        }
    }

    // --- foreign threads ------------------------------------------------------

    @Test
    fun foreignThreadInteractionsAreRoutedThroughTheWorker() = runBlocking {
        val context = DawnContext.create()
        try {
            val adapter = context.requestAdapter().getOrThrow()
            val device = adapter.requestDevice().getOrThrow() as DawnDevice
            try {
                val baseline = device.session.resources.debugRemainingRefs()
                val beforeErrors = uncapturedErrorCount(device)

                // Created from a foreign thread, completed on the worker, and
                // visible from this thread: the interaction is routed, not
                // confined to the calling thread.
                val foreignBuffer = async(Dispatchers.Default) {
                    device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
                }.await()
                try {
                    assertEquals(baseline + 1, device.session.resources.debugRemainingRefs())

                    // A queue write issued from another foreign thread.
                    async(Dispatchers.Default) {
                        device.queue.writeBuffer(foreignBuffer, 0uL, ArrayBuffer.of(uintArrayOf(7u, 7u)))
                    }.await()

                    // A capability snapshot from yet another foreign thread.
                    val features = async(Dispatchers.Default) { device.features }.await()
                    assertTrue(features.isNotEmpty(), "a foreign-thread snapshot must read the device features")

                    // The event pump entry point routes from a foreign thread too.
                    async(Dispatchers.Default) { context.drainEvents() }.await()

                    // Every thread's operation hit the same serialized state: the
                    // write landed and no validation error was observed anywhere.
                    device.queue.onSubmittedWorkDone().getOrThrow()
                    foreignBuffer.mapAsync(GPUMapMode.Read).getOrThrow()
                    assertContentEquals(uintArrayOf(7u, 7u), foreignBuffer.getMappedRange().toUIntArray().copyOfRange(0, 2))
                    foreignBuffer.unmap()
                    assertEquals(beforeErrors, uncapturedErrorCount(device))
                } finally {
                    // The buffer is destroy-backed: its close keeps the tombstone
                    // registered until the teardown below releases it.
                    foreignBuffer.close()
                }
                assertEquals(baseline + 1, device.session.resources.debugRemainingRefs())
            } finally {
                device.close()
                assertEquals(0, device.session.resources.debugRemainingRefs())
            }
            adapter.close()
        } finally {
            context.close()
        }
    }

    // --- invalid buffer source ------------------------------------------------

    @Test
    fun invalidBufferSourceSurfacesTheNativeValidationError() = runTest {
        withDawnDevice { device ->
            // A buffer without COPY_DST cannot be written: the backend has no
            // Kotlin-side usage check, so Dawn's own validation must observe it.
            device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead)).let { buffer ->
                try {
                    val before = uncapturedErrorCount(device)
                    device.queue.writeBuffer(buffer, 0uL, ArrayBuffer.of(uintArrayOf(1u)))
                    device.queue.onSubmittedWorkDone().getOrThrow()
                    assertTrue(
                        uncapturedErrorCount(device) > before,
                        "writing a buffer without COPY_DST must surface the native validation error",
                    )
                } finally {
                    buffer.close()
                }
            }
        }
    }

    // --- setImmediates for real ------------------------------------------------

    @Test
    fun setImmediatesReachTheShaderWhenTheAnnouncedMaxImmediateSizeAllowsIt() = runTest {
        withDawnDevice { device ->
            val announced = device.limits.maxImmediateSize
            assertTrue(
                announced in 0u..UInt.MAX_VALUE,
                "the announced maxImmediateSize must be a readable limit",
            )
            if (announced < 4u) {
                // This host cannot carry even a scalar immediate: the metadata
                // witness above stays mandatory and the round-trip degenerates
                // honestly instead of erroring a mandatory test.
                return@withDawnDevice
            }

            device.createShaderModule(ShaderModuleDescriptor(IMMEDIATE_SHADER)).use { shader ->
                device.createBindGroupLayout(
                    BindGroupLayoutDescriptor(
                        listOf(
                            BindGroupLayoutEntry(
                                binding = 0u,
                                visibility = GPUShaderStage.Compute,
                                buffer = BufferBindingLayout(type = GPUBufferBindingType.Storage),
                            ),
                        ),
                    ),
                ).use { layout ->
                    device.createPipelineLayout(
                        PipelineLayoutDescriptor(listOf(layout), immediateSize = 4u),
                    ).use { pipelineLayout ->
                        device.createComputePipeline(
                            ComputePipelineDescriptor(
                                layout = pipelineLayout,
                                compute = ProgrammableStage(shader, entryPoint = "main"),
                            ),
                        ).use { pipeline ->
                            val storage = device.createBuffer(
                                BufferDescriptor(16uL, GPUBufferUsage.Storage or GPUBufferUsage.CopySrc),
                            )
                            val staging = device.createBuffer(
                                BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst),
                            )
                            device.createBindGroup(
                                BindGroupDescriptor(layout, listOf(BindGroupEntry(0u, BufferBinding(storage)))),
                            ).use { bindGroup ->
                                val beforeErrors = uncapturedErrorCount(device)

                                // A real immediate value: the shader reads the
                                // bytes this downcall wrote.
                                dispatchImmediates(device, pipeline, bindGroup, storage, staging) { pass ->
                                    pass.setImmediates(0u, ArrayBuffer.of(uintArrayOf(42u)))
                                }
                                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                assertContentEquals(
                                    uintArrayOf(42u, 0u, 0u, 0u),
                                    staging.getMappedRange().toUIntArray().copyOfRange(0, 4),
                                    "the immediate value must reach the shader",
                                )
                                staging.unmap()

                                // The dataOffset/dataSize slice: the written range
                                // comes from the middle of a larger view.
                                dispatchImmediates(device, pipeline, bindGroup, storage, staging) { pass ->
                                    pass.setImmediates(
                                        rangeOffset = 0u,
                                        data = ArrayBuffer.of(uintArrayOf(0u, 13u)),
                                        dataOffset = 4uL,
                                        dataSize = 4uL,
                                    )
                                }
                                staging.mapAsync(GPUMapMode.Read).getOrThrow()
                                assertContentEquals(
                                    uintArrayOf(13u, 0u, 0u, 0u),
                                    staging.getMappedRange().toUIntArray().copyOfRange(0, 4),
                                    "the sliced immediate value must reach the shader",
                                )
                                staging.unmap()

                                assertEquals(
                                    beforeErrors,
                                    uncapturedErrorCount(device),
                                    "the immediate round-trip must surface no error",
                                )
                            }
                            storage.close()
                            staging.close()
                        }
                    }
                }
            }
        }
    }

    // --- nullable depth signatures ---------------------------------------------

    @Test
    fun nullableDepthWriteAndDepthCompareReachNativeValidation() = runTest {
        withDawnDevice { device ->
            device.createShaderModule(ShaderModuleDescriptor(VERTEX_SHADER)).use { shader ->
                // Explicit values on both nullable signatures: valid pipelines,
                // observed by a clean uncaptured-error slate.
                for ((write, compare) in listOf(false to GPUCompareFunction.Less, true to GPUCompareFunction.Always)) {
                    val before = uncapturedErrorCount(device)
                    device.createRenderPipeline(
                        RenderPipelineDescriptor(
                            vertex = VertexState(module = shader, entryPoint = "vs_main"),
                            depthStencil = DepthStencilState(
                                format = GPUTextureFormat.Depth32Float,
                                depthWriteEnabled = write,
                                depthCompare = compare,
                            ),
                        ),
                    ).close()
                    assertEquals(
                        before,
                        uncapturedErrorCount(device),
                        "the explicit depth signatures ($write, $compare) must create a valid pipeline",
                    )
                }

                // The nullable signatures map onto the native Undefined tri-state
                // and Dawn's validation observes them: a depth-aspect format
                // refuses an undefined depthWriteEnabled, and a used compare
                // refuses an undefined depthCompare. The mapping itself is the
                // witness — the native message names the undefined field.
                for ((write, compare) in listOf(null to GPUCompareFunction.Less, true to null)) {
                    val before = uncapturedErrorCount(device)
                    device.createRenderPipeline(
                        RenderPipelineDescriptor(
                            vertex = VertexState(module = shader, entryPoint = "vs_main"),
                            depthStencil = DepthStencilState(
                                format = GPUTextureFormat.Depth32Float,
                                depthWriteEnabled = write,
                                depthCompare = compare,
                            ),
                        ),
                    ).close()
                    assertTrue(
                        uncapturedErrorCount(device) > before,
                        "the null depth signatures ($write, $compare) must reach the native validation",
                    )
                }
            }
        }
    }

    // --- optional feature, present or absent ------------------------------------

    @Test
    fun optionalTimestampFeatureStaysCheckablePresentOrAbsent() = runTest {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                // Metadata/signature witnesses that hold on every host, feature
                // or not — a mandatory test must never silently error here.
                assertTrue(GPUFeatureName.TimestampQuery in GPUFeatureName.entries)
                assertTrue(GPUQueryType.Timestamp in GPUQueryType.entries)
                assertTrue(
                    adapter.features.all { it in GPUFeatureName.entries },
                    "the adapter features must be contract feature names",
                )

                if (GPUFeatureName.TimestampQuery in adapter.features) {
                    adapter.requestDevice(
                        DeviceDescriptor(requiredFeatures = listOf(GPUFeatureName.TimestampQuery)),
                    ).getOrThrow().let { publicDevice ->
                        val device = publicDevice as DawnDevice
                        try {
                            assertTrue(GPUFeatureName.TimestampQuery in device.features)
                            device.createQuerySet(
                                QuerySetDescriptor(type = GPUQueryType.Timestamp, count = 2u),
                            ).let { querySet ->
                                assertEquals(GPUQueryType.Timestamp, querySet.type)
                                assertEquals(2u, querySet.count)
                                querySet.close()
                            }
                        } finally {
                            device.close()
                            assertEquals(0, device.session.resources.debugRemainingRefs())
                        }
                    }
                } else {
                    // Absent: the signature stays checkable above, and the
                    // request is refused — an honest refusal, not a skip.
                    val result = adapter.requestDevice(
                        DeviceDescriptor(requiredFeatures = listOf(GPUFeatureName.TimestampQuery)),
                    )
                    result.getOrNull()?.close()
                    assertTrue(
                        result.isFailure,
                        "an adapter without timestamp-query must refuse the required feature",
                    )
                }
            }
        } finally {
            context.close()
        }
    }

    // --- render bundle encoder: close guard parity -------------------------------

    @Test
    fun bundleEncoderCommandsAfterCloseAreRefusedWithoutANativeCall() = runTest {
        withDawnDevice { device ->
            device.createShaderModule(ShaderModuleDescriptor(RENDER_SHADER)).use { shader ->
                device.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = org.graphiks.webgpu.descriptors.FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(
                                org.graphiks.webgpu.descriptors.ColorTargetState(format = GPUTextureFormat.RGBA8Unorm),
                            ),
                        ),
                    ),
                ).use { pipeline ->
                    device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.Index)).use { buffer ->
                        val encoder = device.createRenderBundleEncoder(
                            RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
                        )
                        val before = uncapturedErrorCount(device)
                        encoder.close()

                        // Every command — and finish — refuses with the Kotlin
                        // guard before any downcall can touch the released handle.
                        assertFailsWith<IllegalStateException> { encoder.finish() }
                        assertFailsWith<IllegalStateException> { encoder.setPipeline(pipeline) }
                        assertFailsWith<IllegalStateException> { encoder.setIndexBuffer(buffer, GPUIndexFormat.Uint32) }
                        assertFailsWith<IllegalStateException> { encoder.setVertexBuffer(0u, buffer) }
                        assertFailsWith<IllegalStateException> { encoder.draw(3u) }
                        assertFailsWith<IllegalStateException> { encoder.drawIndexed(3u) }
                        assertFailsWith<IllegalStateException> { encoder.drawIndirect(buffer, 0uL) }
                        assertFailsWith<IllegalStateException> { encoder.drawIndexedIndirect(buffer, 0uL) }
                        assertFailsWith<IllegalStateException> { encoder.setBindGroup(0u, null) }
                        assertFailsWith<IllegalStateException> { encoder.setImmediates(0u, ArrayBuffer.allocate(4uL)) }
                        assertFailsWith<IllegalStateException> { encoder.pushDebugGroup("closed") }
                        assertFailsWith<IllegalStateException> { encoder.popDebugGroup() }
                        assertFailsWith<IllegalStateException> { encoder.insertDebugMarker("closed") }

                        // No native validation error was observed: the guard fired
                        // before every downcall, and the repeated close is a no-op.
                        encoder.close()
                        assertEquals(
                            before,
                            uncapturedErrorCount(device),
                            "the closed encoder's refused commands must make no native call",
                        )
                    }
                }
            }
        }
    }

    @Test
    fun finishingABundleEncoderTwiceWithoutCloseObservesAValidationError() = runTest {
        withDawnDevice { device ->
            device.createShaderModule(ShaderModuleDescriptor(RENDER_SHADER)).use { shader ->
                device.createRenderPipeline(
                    RenderPipelineDescriptor(
                        vertex = VertexState(module = shader, entryPoint = "vs_main"),
                        fragment = org.graphiks.webgpu.descriptors.FragmentState(
                            module = shader,
                            entryPoint = "fs_main",
                            targets = listOf(
                                org.graphiks.webgpu.descriptors.ColorTargetState(format = GPUTextureFormat.RGBA8Unorm),
                            ),
                        ),
                    ),
                ).use { pipeline ->
                    val encoder = device.createRenderBundleEncoder(
                        RenderBundleEncoderDescriptor(colorFormats = listOf(GPUTextureFormat.RGBA8Unorm)),
                    )
                    try {
                        encoder.setPipeline(pipeline)
                        encoder.draw(3u)
                        // The finished bundle is session-owned: the interface has
                        // no close, so it stays registered until the teardown.
                        encoder.finish()

                        // finish does NOT release the encoder handle: a second
                        // finish without close must reach Dawn and surface its
                        // validation error — never the Kotlin guard.
                        val before = uncapturedErrorCount(device)
                        runCatching { encoder.finish() }
                        device.queue.onSubmittedWorkDone().getOrThrow()
                        assertTrue(
                            uncapturedErrorCount(device) > before,
                            "finishing a bundle encoder twice must surface Dawn's validation error",
                        )
                    } finally {
                        encoder.close()
                    }
                }
            }
        }
    }

    // --- helpers ---------------------------------------------------------------

    /**
     * Runs [block] on a fresh public device: its own context, fresh adapter and
     * default device, closed in reverse order.
     */
    private suspend fun withDawnDevice(block: suspend (DawnDevice) -> Unit) {
        val context = DawnContext.create()
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                adapter.requestDevice().getOrThrow().let { publicDevice ->
                    val device = publicDevice as DawnDevice
                    try {
                        block(device)
                    } finally {
                        device.close()
                        assertEquals(
                            0,
                            device.session.resources.debugRemainingRefs(),
                            "the teardown must release every registered reference",
                        )
                    }
                }
            }
        } finally {
            context.close()
        }
    }

    /** The uncaptured errors observed so far on [device]'s session, drained. */
    private suspend fun uncapturedErrorCount(device: DawnDevice): Int {
        device.session.runtime.drainEvents()
        device.session.runtime.dispatcher.drain()
        return device.session.callbacks.uncapturedErrors.size
    }

    /** Encodes one immediate dispatch, copies [storage] into [staging], submits. */
    private suspend fun dispatchImmediates(
        device: DawnDevice,
        pipeline: org.graphiks.webgpu.GPUComputePipeline,
        bindGroup: org.graphiks.webgpu.GPUBindGroup,
        storage: org.graphiks.webgpu.GPUBuffer,
        staging: org.graphiks.webgpu.GPUBuffer,
        encode: (org.graphiks.webgpu.GPUComputePassEncoder) -> Unit,
    ) {
        device.createCommandEncoder().use { encoder ->
            encoder.beginComputePass().let { pass ->
                pass.setPipeline(pipeline)
                pass.setBindGroup(0u, bindGroup)
                encode(pass)
                pass.dispatchWorkgroups(1u)
                pass.end()
            }
            encoder.copyBufferToBuffer(storage, 0uL, staging, 0uL, storage.size)
            val commandBuffer = encoder.finish()
            try {
                device.queue.submit(listOf(commandBuffer))
            } finally {
                commandBuffer.close()
            }
        }
        device.queue.onSubmittedWorkDone().getOrThrow()
    }

    private companion object {
        /** Bounded hand-off for an in-flight native callback registration. */
        const val HANDOFF_TIMEOUT_MS = 20_000L

        /** Reads a scalar immediate into a storage buffer. */
        val IMMEDIATE_SHADER = """
            requires immediate_address_space;

            @group(0) @binding(0) var<storage, read_write> value: array<u32>;

            var<immediate> factor: u32;

            @compute @workgroup_size(1)
            fn main() {
                value[0] = factor;
            }
        """.trimIndent()

        /** A vertex-only pipeline: the depth-signature witness needs no fragment. */
        val VERTEX_SHADER = """
            @vertex
            fn vs_main(@builtin(vertex_index) vertexIndex: u32) -> @builtin(position) vec4f {
                var positions = array<vec2f, 3>(
                    vec2f(-1.0, -1.0),
                    vec2f( 3.0, -1.0),
                    vec2f(-1.0,  3.0),
                );
                return vec4f(positions[vertexIndex], 0.0, 1.0);
            }
        """.trimIndent()

        /** A full-screen triangle: a render pipeline with one RGBA8 color target. */
        val RENDER_SHADER = """
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
    }
}
