package org.graphiks.dawn4k.mapper

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure range-normalization tests for [dataSlice]: a null size means the
 * remainder, an offset past the end is refused, and an explicit size is never
 * allowed to wrap past the end of the address space.
 */
class DataSliceTest {

    @Test
    fun omittedDataSizeMeansRemainingBytes() {
        assertEquals(DataSlice(4uL, 12uL), dataSlice(16uL, 4uL, null))
        assertFailsWith<IllegalArgumentException> { dataSlice(16uL, 17uL, null) }
    }

    @Test
    fun explicitSizeIsBoundsCheckedWithoutOverflow() {
        assertEquals(DataSlice(4uL, 8uL), dataSlice(16uL, 4uL, 8uL))
        assertFailsWith<IllegalArgumentException> { dataSlice(16uL, 4uL, 13uL) }
        assertFailsWith<IllegalArgumentException> { dataSlice(16uL, ULong.MAX_VALUE - 3uL, 8uL) }
    }
}
