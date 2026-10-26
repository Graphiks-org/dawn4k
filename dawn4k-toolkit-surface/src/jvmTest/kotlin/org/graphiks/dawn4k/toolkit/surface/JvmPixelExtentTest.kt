package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import kotlin.test.*

/**
 * Spec F10: the X11/Win32 pixel-extent queries of the raw-handle surface. Handle validation
 * runs before the platform guard, and the guard runs before any native call, so every host
 * covers both stages without a display server; the guarded query body itself only runs where
 * libX11/user32 exist and stays untested off-host. Query results flow through [PixelExtent],
 * which refuses negative dimensions — an empty or inverted client rectangle fails loudly
 * instead of being clamped (the GLFW targets coerce; these queries require).
 */
class JvmPixelExtentTest {
    private val worker = DawnExecutionOwner.dedicated()
    private val owner get() = DawnPlatformOwner.fromExecutionOwner(worker)

    @AfterTest fun closeWorker() = worker.close()

    @Test fun x11QueryRequiresNonzeroDisplayAndWindow(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.x11PixelExtent(0, 0x1BuL, owner) }
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.x11PixelExtent(0x10, 0uL, owner) }
    }

    @Test fun win32QueryRequiresNonzeroHwnd(): Unit = runBlocking {
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.win32PixelExtent(0, owner) }
    }

    @Test fun validatedQueriesStillRespectTheHostGuard(): Unit = runBlocking {
        // Off the owning host the platform check rejects before the first native call; on the
        // owning host these arguments would reach the real display server, so the call itself
        // stays untested here.
        if (surfacePlatform != SurfacePlatform.Linux) {
            assertFailsWith<IllegalStateException> { DawnSurfaceTargets.x11PixelExtent(0x10, 0x1BuL, owner) }
        }
        if (surfacePlatform != SurfacePlatform.Windows) {
            assertFailsWith<IllegalStateException> { DawnSurfaceTargets.win32PixelExtent(0x10, owner) }
        }
    }

    @Test fun negativeQueryResultsCannotBecomeExtents() {
        assertFailsWith<IllegalArgumentException> { PixelExtent(-1, 8) }
        assertFailsWith<IllegalArgumentException> { PixelExtent(8, -1) }
    }
}
