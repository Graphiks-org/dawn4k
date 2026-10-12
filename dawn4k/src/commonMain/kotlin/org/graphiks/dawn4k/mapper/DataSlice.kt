package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.checkedRange

/** A byte range `[offset, offset + size)` within some upload data source. */
internal data class DataSlice(val offset: ULong, val size: ULong)

/**
 * Normalizes a requested data sub-range against a [total] length: [offset] must
 * not exceed [total], and a null [size] means "the remainder". Reuses the
 * buffer-range normalization, so a range that would wrap past the end of the
 * address space is rejected instead of silently wrapping, and the remainder is
 * never computed by an unchecked subtraction.
 */
internal fun dataSlice(total: ULong, offset: ULong, size: ULong?): DataSlice {
    val range = checkedRange(total, offset, size)
    return DataSlice(range.offset, range.size)
}
