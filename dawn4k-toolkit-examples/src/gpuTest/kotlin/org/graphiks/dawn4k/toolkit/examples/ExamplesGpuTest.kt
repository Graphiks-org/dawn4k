package org.graphiks.dawn4k.webgpu.toolkit.examples

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.openDawnSession
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.dawn4k.webgpu.toolkit.surface.PixelExtent
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import kotlin.test.*
import kotlin.time.Duration.Companion.seconds

/**
 * Runs [block] on a dawn4k session. The extension drives `EventLoop`, so its own pump settles
 * submitted work; the block itself runs through the session's owner admission, on the thread
 * that owns the Dawn context, because native calls must not leave it.
 */
private suspend fun <T> withExampleSession(block: suspend (Dawn4kWebGpuToolkitExtension, Session) -> T): T {
    val extension = Dawn4kWebGpuToolkitExtension()
    val session = openDawnSession(extension, SessionConfig(startupTimeout = 60.seconds))
    return try {
        session.run {
            withContext(checkNotNull(session.executionOwner) { "the session pins no execution owner" }) {
                block(extension, session)
            }
        }
    } finally {
        session.shutdown()
    }
}

class ExamplesGpuTest {
    @Test fun computeExampleReturnsDoubledValues() = runBlocking {
        withExampleSession { extension, session ->
            assertContentEquals(intArrayOf(2, 4, 6, 8), runComputeExample(session))
            println("TOOLKIT_SAMPLE compute=passed adapter=${extension.dawnAdapter(session).info.description}")
        }
    }
    @Test fun triangleExampleRendersExpectedPixels() = runBlocking {
        withExampleSession { extension, session ->
            val errors = mutableListOf<GPUError>()
            val observation = (session.device as org.graphiks.dawn4k.DawnDevice).observeUncapturedErrors { errors += it }
            try {
                extension.createRenderTexture(session, 16u, 16u, GPUTextureFormat.RGBA8Unorm).use { texture ->
                    TriangleRenderer(session, GPUTextureFormat.RGBA8Unorm).use { renderer ->
                        texture.createView().use { renderer.render(it, PixelExtent(16, 16)) }
                        session.device.createBuffer(BufferDescriptor(4096uL, GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { buffer ->
                            (session.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
                                encoder.copyTextureToBuffer(TexelCopyTextureInfo(texture),
                                    TexelCopyBufferInfo(buffer, bytesPerRow = 256u, rowsPerImage = 16u), Extent3D(16u, 16u))
                                encoder.finish().use { session.queue.submit(listOf(it)) }
                            }
                            buffer.mapAsync(GPUMapMode.Read).getOrThrow()
                            val bytes = try { buffer.getMappedRange().toByteArray() } finally { buffer.unmap() }
                            val middle = 8 * 256 + 8 * 4
                            assertContentEquals(byteArrayOf(-1, 0, 0, -1), bytes.copyOfRange(middle, middle + 4))
                            assertContentEquals(byteArrayOf(0, 0, -1, -1), bytes.copyOfRange(0, 4))
                        }
                    }
                }
                session.queue.onSubmittedWorkDone().getOrThrow()
                extension.dawnContext(session).processEvents()
                assertTrue(errors.isEmpty(), errors.joinToString { it.message })
                println("TOOLKIT_SAMPLE triangle=passed adapter=${extension.dawnAdapter(session).info.description}")
            } finally { observation.close() }
        }
    }
}
