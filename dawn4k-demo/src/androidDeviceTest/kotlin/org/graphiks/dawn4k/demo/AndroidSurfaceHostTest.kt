package org.graphiks.dawn4k.demo

import android.graphics.SurfaceTexture
import android.view.Surface
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AndroidSurfaceHostTest {
    @Test fun nullAndReleasedSurfacesCannotBecomeNativePresentationTargets() {
        assertEquals(0L, AndroidNativeWindow.acquire(null))
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            surface.release()
            assertFailsWith<IllegalArgumentException> { AndroidSurfaceHost(1, surface, PixelExtent(32, 32)) }
        } finally { texture.release() }
    }

    @Test fun retainedWindowSurvivesJavaSurfaceReleaseUntilLeaseRetirement() {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            val lease = AndroidSurfaceHost(1, surface, PixelExtent(32, 32))
            assertTrue(lease.nativeWindow != 0L)
            surface.release()
            assertTrue(lease.valid)
            lease.invalidate()
            assertFalse(lease.valid)
            assertTrue(lease.nativeWindow != 0L, "invalidation must not release before surface retirement")
            lease.close()
            lease.close()
            assertEquals(0L, lease.nativeWindow)
        } finally { surface.release(); texture.release() }
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
