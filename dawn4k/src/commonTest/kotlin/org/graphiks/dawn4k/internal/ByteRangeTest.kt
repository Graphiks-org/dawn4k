package org.graphiks.dawn4k.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

/**
 * Pure range-normalization tests for [checkedRange] and the host-address
 * representability guard [requireRepresentableSize]. No GPU is involved.
 */
class ByteRangeTest {

    @Test
    fun overflowCannotWrapBackInsideTheBuffer() {
        assertFailsWith<IllegalArgumentException> {
            checkedRange(16uL, ULong.MAX_VALUE - 3uL, 8uL)
        }
        assertEquals(ByteRange(4uL, 12uL), checkedRange(16uL, 4uL, null))
    }

    @Test
    fun representableSizeRejectsLongOverflow() {
        // A size that does not fit a host Long must be rejected before any FFI.
        assertFailsWith<IllegalArgumentException> {
            requireRepresentableSize(ULong.MAX_VALUE)
        }
        // The exact boundary is still representable.
        requireRepresentableSize(Long.MAX_VALUE.toULong())
    }
}
