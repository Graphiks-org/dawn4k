package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUStringView
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.memoryScope
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertNull

class StringViewMappingTest {

    @Test
    fun asciiUsesAnExplicitByteLength() = assertStringView("abc", byteArrayOf(97, 98, 99))

    @Test
    fun utf8LengthCountsBytesRatherThanCharacters() =
        assertStringView("é😀", byteArrayOf(0xc3.toByte(), 0xa9.toByte(), 0xf0.toByte(), 0x9f.toByte(), 0x98.toByte(), 0x80.toByte()))

    @Test
    fun embeddedNulPreservesTheSuffix() = assertStringView("abc\u0000def", byteArrayOf(97, 98, 99, 0, 100, 101, 102))

    @Test
    fun emptyStringHasZeroLength() = assertStringView("", byteArrayOf())

    @Test
    fun nullableStringPreservesEmbeddedNulAndUtf8() = memoryScope { allocator ->
        val view = WGPUStringView.allocate(allocator)
        view.initNullable("é\u0000x", allocator)
        assertEquals(4uL, view.length)
        val bytes = ByteArray(4)
        MemoryBuffer(assertNotNull(view.data).handler, view.length).readBytes(bytes)
        assertContentEquals(byteArrayOf(0xc3.toByte(), 0xa9.toByte(), 0, 120), bytes)
    }

    @Test
    fun nullResetsTheViewToAnAbsentString() = memoryScope { allocator ->
        val view = WGPUStringView.allocate(allocator)
        view.initFrom("previous", allocator)
        view.initNullable(null, allocator)
        assertNull(view.data)
        assertEquals(WGPU_STRLEN, view.length)
    }

    private fun assertStringView(value: String, expected: ByteArray) = memoryScope { allocator ->
        val view = WGPUStringView.allocate(allocator)
        view.initFrom(value, allocator)
        assertEquals(expected.size.toULong(), view.length)
        val bytes = ByteArray(expected.size)
        MemoryBuffer(assertNotNull(view.data).handler, view.length).readBytes(bytes)
        assertContentEquals(expected, bytes)
    }
}
