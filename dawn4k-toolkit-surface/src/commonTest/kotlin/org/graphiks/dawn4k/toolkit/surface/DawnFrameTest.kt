package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*

class DawnFrameTest {
    @Test fun frameReleaseNeverDestroysSurfaceTexture() {
        val native = CountingSurface()
        val resource = native.texture()
        val frame = DawnFrame(resource, PixelExtent(16, 16), native, { true }) {}
        frame.close()
        frame.close()
        assertEquals(listOf("view", "texture"), native.releases)
        assertEquals(0, native.presentations)
    }
    @Test fun doublePresentAndCrossSurfaceFrameAreRejected() {
        val first = CountingSurface()
        val second = CountingSurface()
        val resource = first.texture()
        val frame = DawnFrame(resource, PixelExtent(16, 16), first, { true }) {}
        frame.present()
        assertFailsWith<IllegalStateException> { frame.present() }
        assertFailsWith<IllegalArgumentException> { frame.requireOwner(second) }
        frame.close()
        assertEquals(1, first.presentations)
    }
    @Test fun disappearedTargetCannotPresentAnAlreadyAcquiredFrame() {
        val native = CountingSurface()
        val frame = DawnFrame(native.texture(), PixelExtent(16, 16), native, { false }) {}
        try { assertFailsWith<IllegalStateException> { frame.present() } }
        finally { frame.close() }
        assertEquals(0, native.presentations)
        assertEquals(listOf("view", "texture"), native.releases)
    }
}
