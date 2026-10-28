package org.graphiks.dawn4k.toolkit.surface

import org.graphiks.kffi.memoryScope
import org.graphiks.dawn4k.toolkit.surface.internal.readSurfaceEnums
import kotlin.test.*

class SurfaceCapabilitiesMemoryTest {
    @Test fun capabilityMembersAreCopiedBeforeBeingFreed() {
        val snapshot = memoryScope { allocator ->
            val values = allocator.allocateBuffer(8uL)
            values.writeUInts(uintArrayOf(23u, 19u))
            val copied = readSurfaceEnums(values.handler, 2uL)
            values.writeUInts(uintArrayOf(0u, 0u))
            copied
        }
        assertEquals(listOf(23u, 19u), snapshot)
    }
}
