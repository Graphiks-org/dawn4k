package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kTargetLease
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.toolkit.Extent2D
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class Dawn4kSurfaceTargetTest {

    @Test
    fun initialStateHasGeneration1AndProvidedExtent() = runTest {
        val initialExtent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = initialExtent
        )
        
        val state = target.state.value
        assertEquals(1L, state.generation)
        assertEquals(initialExtent, state.extent)
        assertTrue(state.available)
        
        target.close()
    }

    @Test
    fun initialStateWithZeroExtentIsNotAvailable() = runTest {
        val zeroExtent = Extent2D(width = 0u, height = 0u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.Win32,
            initialExtent = zeroExtent
        )
        
        val state = target.state.value
        assertEquals(1L, state.generation)
        assertEquals(zeroExtent, state.extent)
        assertFalse(state.available)
        
        target.close()
    }

    @Test
    fun retainReturnsLeaseWithCurrentGeneration() = runTest {
        val extent = Extent2D(width = 1024u, height = 768u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.X11,
            initialExtent = extent
        )
        
        val lease = target.retain() as Dawn4kTargetLease
        assertEquals(1L, lease.generation)
        assertEquals(extent, lease.extent)
        assertTrue(lease.isValid())
        
        target.close()
    }

    @Test
    fun retainWithZeroExtentThrows() = runTest {
        val zeroExtent = Extent2D(width = 0u, height = 0u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.Wayland,
            initialExtent = zeroExtent
        )
        
        try {
            target.retain()
            assertTrue(false, "Expected IllegalStateException")
        } catch (e: IllegalStateException) {
            assertEquals("Cannot retain surface: not available or extent is zero", e.message)
        }
        
        target.close()
    }

    @Test
    fun updateExtentIncreasesGenerationAndUpdatesState() = runTest {
        val initialExtent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = initialExtent
        )
        
        // Obtain initial lease
        val lease1 = target.retain()
        assertEquals(1L, lease1.generation)
        
        // Update extent
        val newExtent = Extent2D(width = 1280u, height = 720u)
        target.updateExtent(newExtent)
        
        // Check new state
        val state = target.state.value
        assertEquals(2L, state.generation)
        assertEquals(newExtent, state.extent)
        assertTrue(state.available)
        
        // Old lease should be stale
        assertFalse((lease1 as Dawn4kTargetLease).isValid())
        
        // New lease should have new generation
        val lease2 = target.retain()
        assertEquals(2L, lease2.generation)
        assertTrue((lease2 as Dawn4kTargetLease).isValid())
        
        target.close()
    }

    @Test
    fun retireWithCurrentGenerationLeaseMarksItRetired() = runTest {
        val extent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.X11,
            initialExtent = extent
        )
        
        val lease = target.retain() as Dawn4kTargetLease
        assertTrue(lease.isValid())
        
        // Retire the lease
        target.retire(lease)
        
        // Lease should no longer be valid
        assertFalse(lease.isValid())
        
        target.close()
    }

    @Test
    fun retireWithStaleLeaseIsNoOp() = runTest {
        val initialExtent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.Win32,
            initialExtent = initialExtent
        )
        
        val lease = target.retain()
        
        // Update extent to make lease stale
        target.updateExtent(Extent2D(width = 1024u, height = 768u))
        
        // Retire stale lease - should be no-op
        target.retire(lease)
        
        // Should not throw
        target.close()
    }

    @Test
    fun retireWithForeignLeaseIsNoOp() = runTest {
        val extent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = extent
        )
        
        // Create a foreign lease (not from this target)
        val foreignLease = object : org.graphiks.webgpu.toolkit.TargetLease {
            override val generation: Long = 999L
        }
        
        // Retire foreign lease - should be no-op
        target.retire(foreignLease)
        
        // Should not throw
        target.close()
    }

    @Test
    fun setAvailableUpdatesAvailability() = runTest {
        val extent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.Wayland,
            initialExtent = extent
        )
        
        // Initially available
        assertTrue(target.state.value.available)
        
        // Set to unavailable
        target.setAvailable(false)
        assertFalse(target.state.value.available)
        
        // Set back to available
        target.setAvailable(true)
        assertTrue(target.state.value.available)
        
        target.close()
    }

    @Test
    fun doubleRetainBeforeRetireWorks() = runTest {
        val extent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.X11,
            initialExtent = extent
        )
        
        val lease1 = target.retain()
        val lease2 = target.retain()
        
        // Both leases should be valid and have the same generation
        assertTrue((lease1 as Dawn4kTargetLease).isValid())
        assertTrue((lease2 as Dawn4kTargetLease).isValid())
        assertEquals(lease1.generation, lease2.generation)
        
        target.close()
    }

    @Test
    fun platformTypeIsPreserved() = runTest {
        val extent = Extent2D(width = 800u, height = 600u)
        
        val target = Dawn4kSurfaceTarget(
            dawnTarget = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = extent
        )
        
        assertEquals(PlatformSurfaceType.MetalLayer, target.platformType)
        
        target.close()
    }
}
