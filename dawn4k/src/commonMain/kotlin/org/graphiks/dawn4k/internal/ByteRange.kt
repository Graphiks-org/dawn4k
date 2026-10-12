package org.graphiks.dawn4k.internal

/** A byte range `[offset, offset + size)` within some buffer. */
data class ByteRange(val offset: ULong, val size: ULong)

/**
 * Normalizes a requested sub-range against a [total] length: [offset] must not
 * exceed [total], and a null [size] means "the remainder". The computation
 * never adds [offset] and [size], so a range that would wrap past the end of
 * the address space is rejected instead of silently wrapping.
 */
fun checkedRange(total: ULong, offset: ULong, size: ULong?): ByteRange {
    require(offset <= total) { "Offset $offset exceeds $total" }
    val remaining = total - offset
    val count = size ?: remaining
    require(count <= remaining) { "Range size $count exceeds remaining $remaining" }
    return ByteRange(offset, count)
}
