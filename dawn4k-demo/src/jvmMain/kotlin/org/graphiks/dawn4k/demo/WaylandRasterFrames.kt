package org.graphiks.dawn4k.demo

/** Packed premultiplied BGRA bytes; one independently owned snapshot per frame. */
internal data class UiPixelFrame(val width: Int, val height: Int, val stride: Int, val bgra: ByteArray) {
    init {
        require(width >= 0 && height >= 0 && stride >= 0)
        require(stride.toLong() == width.toLong() * 4)
        require(stride.toLong() * height == bgra.size.toLong())
    }
}
