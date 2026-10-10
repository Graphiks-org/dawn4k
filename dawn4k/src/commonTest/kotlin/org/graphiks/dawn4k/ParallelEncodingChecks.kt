package org.graphiks.dawn4k

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.testing.gpuTestConfig
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.GPUValidationError
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.assertContentEquals
import kotlin.test.assertIs
import kotlin.test.assertNull

/** Same-device oracle shared by JVM and Native, with exclusively owned encoders. */
internal suspend fun checkParallelEncoding(workers: List<CoroutineDispatcher>) {
    DawnContext.create(gpuTestConfig()).useWithProgress { context ->
        context.requestAdapter().getOrThrow().use { adapter ->
            adapter.requestDevice().getOrThrow().use { device ->
                val shared = device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopySrc, mappedAtCreation = true))
                val sources = List(2) { device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopySrc or GPUBufferUsage.CopyDst)) }
                val readbacks = List(2) { device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.MapRead or GPUBufferUsage.CopyDst)) }
                try {
                    val expected = uintArrayOf(5u, 7u, 11u, 13u)
                    shared.getMappedRange().setUInts(0uL, expected)
                    shared.unmap()
                    for (copyShared in listOf(false, true)) {
                        val ready = List(2) { CompletableDeferred<Unit>() }
                        val encoded = withTimeout(10_000) {
                            coroutineScope {
                                workers.mapIndexed { index, worker -> async(worker) {
                                    device.createCommandEncoder().use { encoder ->
                                        ready[index].complete(Unit)
                                        ready[1 - index].await()
                                        if (copyShared) encoder.copyBufferToBuffer(shared, 0uL, sources[index], 0uL, 16uL)
                                        else encoder.clearBuffer(sources[index], 0uL, 16uL)
                                        encoder.copyBufferToBuffer(sources[index], 0uL, readbacks[index], 0uL, 16uL)
                                        encoder.finish()
                                    }
                                } }.awaitAll()
                            }
                        }
                        try {
                            device.queue.submit(encoded)
                            for (buffer in readbacks) {
                                buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                                try { assertContentEquals(if (copyShared) expected else UIntArray(4), buffer.getMappedRange().toUIntArray()) }
                                finally { buffer.unmap() }
                            }
                        } finally { encoded.forEach { it.close() } }
                    }
                } finally {
                    readbacks.forEach { it.close() }
                    sources.forEach { it.close() }
                    shared.close()
                }
            }
        }
    }
}

internal suspend fun checkThreadLocalErrorScopes(workers: List<CoroutineDispatcher>) {
    DawnContext.create(gpuTestConfig()).useWithProgress { context ->
        context.requestAdapter().getOrThrow().use { adapter ->
            adapter.requestDevice().getOrThrow().use { device ->
                val ready = List(2) { CompletableDeferred<Unit>() }
                val errors = withTimeout(10_000) {
                    coroutineScope {
                        workers.mapIndexed { index, worker -> async(worker) {
                            device.pushErrorScope(GPUErrorFilter.Validation)
                            ready[index].complete(Unit)
                            ready[1 - index].await()
                            if (index == 0) device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
                            else device.createCommandEncoder().use { it.finish().close() }
                            device.popErrorScope().getOrThrow()
                        } }.awaitAll()
                    }
                }
                assertIs<GPUValidationError>(errors[0])
                assertNull(errors[1])
            }
        }
    }
}
