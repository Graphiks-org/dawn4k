package org.graphiks.dawn4k

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.native.WGPUBufferMapCallback
import org.graphiks.dawn4k.native.register
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.webgpu.GPUBufferMapState
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

/**
 * Real-GPU buffer mapping tests: real mapped memory (no CPU copy), partial and
 * disjoint ranges, native validation errors, and cancellation/teardown hygiene.
 * Runs only through the gpuTest* tasks; a host without an adapter fails these
 * tests (no silent skip).
 */
class BufferGpuTest {

    @Test
    fun mappedWritesReachReadback() = runTest {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(5u, 7u, 11u, 13u))
                source.unmap()
                fixture.copyAndSubmit(source, target, 16uL)
                target.mapAsync(GPUMapMode.Read).getOrThrow()
                assertContentEquals(uintArrayOf(5u, 7u, 11u, 13u), target.getMappedRange().toUIntArray())
                target.unmap()
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun mappedAtCreationStartsMapped() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite, mappedAtCreation = true))
            try {
                assertEquals(GPUBufferMapState.Mapped, buffer.mapState)
                buffer.getMappedRange().setUInts(0uL, uintArrayOf(1u, 2u, 3u, 4u))
                buffer.unmap()
                assertEquals(GPUBufferMapState.Unmapped, buffer.mapState)
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun partialMapAtOffsetEightMapsOnlyThatRange() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write, offset = 8uL, size = 8uL).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(7u, 11u))
                source.unmap()
                fixture.copyAndSubmit(source, target, 16uL)
                target.mapAsync(GPUMapMode.Read, offset = 8uL, size = 8uL).getOrThrow()
                assertContentEquals(uintArrayOf(7u, 11u), target.getMappedRange().toUIntArray())
                target.unmap()
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun remapAfterUnmapWorks() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(3u, 3u, 3u, 3u))
                source.unmap()
                // A second map after unmap overwrites the same range.
                source.mapAsync(GPUMapMode.Write).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(9u, 9u, 9u, 9u))
                source.unmap()
                fixture.copyAndSubmit(source, target, 16uL)
                target.mapAsync(GPUMapMode.Read).getOrThrow()
                assertContentEquals(uintArrayOf(9u, 9u, 9u, 9u), target.getMappedRange().toUIntArray())
                target.unmap()
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun twoDisjointRangesMapIndependently() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val source = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite or GPUBufferUsage.CopySrc))
            val target = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst))
            try {
                source.mapAsync(GPUMapMode.Write, offset = 0uL, size = 8uL).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(1u, 2u))
                source.unmap()
                source.mapAsync(GPUMapMode.Write, offset = 8uL, size = 8uL).getOrThrow()
                source.getMappedRange().setUInts(0uL, uintArrayOf(3u, 4u))
                source.unmap()
                fixture.copyAndSubmit(source, target, 16uL)
                target.mapAsync(GPUMapMode.Read).getOrThrow()
                assertContentEquals(uintArrayOf(1u, 2u, 3u, 4u), target.getMappedRange().toUIntArray())
                target.unmap()
            } finally {
                source.close()
                target.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun overlappingMapFailsWithNativeError() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite))
            try {
                buffer.mapAsync(GPUMapMode.Write, offset = 0uL, size = 16uL).getOrThrow()
                // Mapping a range that overlaps the already-mapped range must fail
                // at the native layer, not with a Kotlin IllegalArgumentException.
                val overlap = buffer.mapAsync(GPUMapMode.Write, offset = 8uL, size = 8uL)
                assertTrue(overlap.isFailure)
                assertTrue(overlap.exceptionOrNull() is DawnBufferMapException)
                buffer.unmap()
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun unalignedOffsetFailsWithNativeError() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite))
            try {
                // offset must be a multiple of 8: a bounds-valid but misaligned
                // range must surface the native validation error.
                val result = buffer.mapAsync(GPUMapMode.Write, offset = 4uL, size = 8uL)
                assertTrue(result.isFailure)
                assertTrue(result.exceptionOrNull() is DawnBufferMapException)
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun mapAfterDestroyFailsWithNativeErrorAndHandleStaysValid() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead))
            buffer.close()
            val result = buffer.mapAsync(GPUMapMode.Read)
            assertTrue(result.isFailure)
            assertTrue(result.exceptionOrNull() is DawnBufferMapException)
            // The tombstone handle stays valid: an idempotent close must never
            // dereference a freed view.
            buffer.close()
        } finally {
            fixture.close()
        }
    }

    @Test
    fun readAndWriteModesAreDistinct() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val readBuffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead))
            val writeBuffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite))
            try {
                readBuffer.mapAsync(GPUMapMode.Read).getOrThrow()
                readBuffer.unmap()
                assertTrue(readBuffer.mapAsync(GPUMapMode.Write).exceptionOrNull() is DawnBufferMapException)

                writeBuffer.mapAsync(GPUMapMode.Write).getOrThrow()
                writeBuffer.unmap()
                assertTrue(writeBuffer.mapAsync(GPUMapMode.Read).exceptionOrNull() is DawnBufferMapException)
            } finally {
                readBuffer.close()
                writeBuffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun zeroSizeRangeReturnsEmptyView() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapWrite))
            try {
                buffer.mapAsync(GPUMapMode.Write).getOrThrow()
                val empty = buffer.getMappedRange(0uL, 0uL)
                assertEquals(0uL, empty.size)
                buffer.unmap()
            } finally {
                buffer.close()
            }
        } finally {
            fixture.close()
        }
    }

    @Test
    fun closeDuringPendingMapSettlesWithoutLeak() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val buffer = fixture.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead))
            // Start a map and destroy the buffer while it is in flight.
            val pending = async(Dispatchers.Default) { buffer.mapAsync(GPUMapMode.Read) }
            withTimeout(PENDING_HANDOFF_TIMEOUT_MS) {
                while (fixture.runtime.debugOpenCallbacks() == 0) yield()
            }
            buffer.close()
            // The pending map must settle (never hang), either completing first
            // or aborting on the destroy with a native error.
            pending.await()
            fixture.runtime.drainEvents()
            assertEquals(0, fixture.runtime.debugOpenCallbacks())
        } finally {
            fixture.close()
        }
    }

    @Test
    fun subdeviceOperationClosesRegistrationWhenIssueThrows() = runBlocking {
        val fixture = NativeFixture.open()
        try {
            val operation = PendingOperation<Unit> { }
            var registration: CallbackRegistration<WGPUBufferMapCallback>? = null
            val runtime = fixture.runtime
            // A native issue that throws after the callback was registered must
            // still close the registration: the registered callback never fires,
            // so nothing else would ever revoke it.
            assertFailsWith<IllegalStateException> {
                runtime.dispatcher.call {
                    runtime.beginSubdeviceOperation(
                        operation = operation,
                        issue = {
                            registration = WGPUBufferMapCallback.register(CallbackPolicy.ONCE) { _, _, _ -> }
                            throw IllegalStateException("injected failure")
                        },
                        closeRegistration = { registration?.close() },
                    )
                }
            }
            assertNotNull(registration)
            assertTrue(registration.isClosed)
            assertEquals(0, runtime.debugOpenCallbacks())
        } finally {
            fixture.close()
        }
    }

    private companion object {
        const val PENDING_HANDOFF_TIMEOUT_MS = 20_000L
    }
}
