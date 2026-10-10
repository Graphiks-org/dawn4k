package org.graphiks.dawn4k.native

import org.graphiks.kffi.memoryScope
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Runtime layout checks on the generated value types. These exercise the offsets baked into
 * the generated accessors (a wrong offset would alias another field). These checks do not
 * compare the generated layouts with the real C header.
 */
class GeneratedLayoutTest {

    @Test
    fun stringViewRoundTripsDistinctFields() = memoryScope { allocator ->
        val view = WGPUStringView.allocate(allocator)

        view.length = 42uL
        assertEquals(42uL, view.length)
        assertEquals(null, view.data)
    }

    @Test
    fun futureRoundTripsItsId() = memoryScope { allocator ->
        val future = WGPUFuture.allocate(allocator)

        future.id = 0x0102030405060708uL
        assertEquals(0x0102030405060708uL, future.id)
    }

    @Test
    fun bufferDescriptorRoundTripsItsFields() = memoryScope { allocator ->
        val descriptor = WGPUBufferDescriptor.allocate(allocator)

        descriptor.usage = 0x0000000000000011uL
        descriptor.size = 16uL
        descriptor.mappedAtCreation = 1u

        assertEquals(16uL, descriptor.size)
        assertEquals(0x0000000000000011uL, descriptor.usage)
        assertEquals(1u, descriptor.mappedAtCreation)
    }
}
