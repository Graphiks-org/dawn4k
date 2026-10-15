package org.graphiks.dawn4k.demo

import android.graphics.SurfaceTexture
import android.view.Surface
import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.DawnTargetLease
import kotlin.test.*

class AndroidSurfaceHostTest {
    @Test fun releasedSurfacesCannotBecomeNativePresentationTargets() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            surface.release()
            assertFailsWith<IllegalArgumentException> { AndroidSurfaceHost(1, surface, PixelExtent(32, 32)) }
        } finally { texture.release() }
    }

    @Test fun retainedWindowSurvivesJavaSurfaceReleaseUntilLeaseRetirement(): Unit = runBlocking {
        val texture = SurfaceTexture(false)
        texture.setDefaultBufferSize(32, 32)
        val surface = Surface(texture)
        var retained: DawnTargetLease? = null
        var host: AndroidSurfaceHost? = null
        try {
            val lease = AndroidSurfaceHost(1, surface, PixelExtent(32, 32)).also { host = it }
            val native = lease.target.retain().also { retained = it }
            surface.release()
            assertTrue(lease.valid)
            withDawnSession(DawnSessionOptions(config = DawnConfig(DawnBackend.Vulkan))) { gpu ->
                native.createSurface(gpu.context).use { reference -> assertNotEquals(0L, reference.nativeHandle()) }
            }
            lease.invalidate()
            assertFalse(lease.valid)
            val retired = async { lease.awaitRetirement(); lease.close() }
            yield()
            assertFalse(retired.isCompleted, "the UI host must wait for toolkit target acknowledgment")
            native.retire()
            retired.await()
            lease.close()
            assertFailsWith<IllegalStateException> { lease.target.retain() }
        } finally {
            withContext(NonCancellable) { retained?.retire(); host?.awaitRetirement(); host?.close() }
            surface.release(); texture.release()
        }
    }

    @Test fun replacementGenerationHasIndependentRetainedReferenceAndExtent() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            AndroidSurfaceHost(1, surface, PixelExtent(32, 32)).use { first ->
                AndroidSurfaceHost(2, surface, PixelExtent(64, 48)).use { second ->
                    first.close()
                    assertTrue(second.valid)
                    second.resize(80, 60)
                    assertEquals(PixelExtent(80, 60), second.pixelExtent())
                    assertEquals(2L, second.generation)
                }
            }
        } finally { surface.release(); texture.release() }
    }
}
