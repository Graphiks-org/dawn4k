package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUSurface
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class CompatibleSurfaceOptionsTest {
    @Test
    fun headlessOptionsRemainNull() = memoryScope { allocator ->
        assertNull(allocator.allocateRequestAdapterOptions(null, null).compatibleSurface)
    }

    @Test
    fun selectedSurfaceIsPassedToTheNativeRequestOptions() = memoryScope { allocator ->
        // This address is only round-tripped through an allocated descriptor;
        // no Dawn function is ever called with a synthetic surface.
        val surface = WGPUSurface(NativeAddress(42L))
        val options = allocator.allocateRequestAdapterOptions(null, null, surface)
        assertEquals(42L, options.compatibleSurface?.handler?.rawValue)
    }
}
