package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.SurfaceReferenceState
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class SurfaceReferenceTest {
    @Test
    fun rejectsWrongContextBeforeIssuingRequest() {
        val owner = Any()
        var retains = 0
        val surface = SurfaceReferenceState(owner, 42L, { retains++ }, {})
        assertFailsWith<IllegalArgumentException> { surface.retainForRequest(Any()) }
        assertEquals(0, retains)
        surface.close()
    }

    @Test
    fun rejectsNullAndClosedSurface() {
        assertFailsWith<IllegalArgumentException> { SurfaceReferenceState(Any(), 0L, {}, {}) }
        val owner = Any()
        var retains = 0
        val surface = SurfaceReferenceState(owner, 42L, { retains++ }, {})
        surface.close()
        assertFailsWith<IllegalStateException> { surface.nativeHandle() }
        assertFailsWith<IllegalStateException> { surface.retainForRequest(owner) }
        assertEquals(0, retains)
    }

    @Test
    fun closeDoesNotReleaseRequestLease() {
        val owner = Any()
        var refs = 1
        val surface = SurfaceReferenceState(owner, 42L, { assertEquals(42L, it); refs++ }, { refs-- })
        val request = surface.retainForRequest(owner)
        assertEquals(2, refs)
        surface.close()
        surface.close()
        assertEquals(1, refs)
        request.close()
        request.close()
        assertEquals(0, refs)
    }

    @Test
    fun requestCanSettleBeforeTheSurfaceOwnerCloses() {
        val owner = Any()
        var refs = 1
        val surface = SurfaceReferenceState(owner, 42L, { refs++ }, { refs-- })
        surface.retainForRequest(owner).close()
        assertEquals(1, refs)
        assertEquals(42L, surface.nativeHandle())
        surface.close()
        assertEquals(0, refs)
    }

    @Test
    fun failedRetainLeavesTheOwnedReferenceIntact() {
        var releases = 0
        val owner = Any()
        val surface = SurfaceReferenceState(owner, 42L, { error("retain failed") }, { releases++ })
        assertFailsWith<IllegalStateException> { surface.retainForRequest(owner) }
        assertEquals(42L, surface.nativeHandle())
        surface.close()
        assertEquals(1, releases)
    }

    @Test
    fun nullSurfaceCannotTransferOwnershipToContext() {
        DawnContext.create().use { context ->
            assertFailsWith<IllegalArgumentException> { context.adoptSurface(0L) }
        }
    }
}
