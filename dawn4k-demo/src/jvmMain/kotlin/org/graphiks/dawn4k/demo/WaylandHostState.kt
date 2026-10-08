package org.graphiks.dawn4k.demo

/** Owner-thread configure state: logical suggestions are not necessarily buffer pixels. */
internal class WaylandHostState(initialWidth: Int, initialHeight: Int) {
    private var width = initialWidth
    private var height = initialHeight
    var scale: Int = 1
        private set
    private var configured = false
    private var closed = false

    init { require(initialWidth > 0 && initialHeight > 0) { "Wayland initial dimensions must be positive" } }

    fun configure(width: Int, height: Int, scale: Int) {
        check(!closed) { "the Wayland host is closed" }
        require(width >= 0 && height >= 0 && scale > 0) { "invalid Wayland configure: ${width}x$height scale=$scale" }
        val nextWidth = if (width == 0) this.width else width
        val nextHeight = if (height == 0) this.height else height
        require(nextWidth <= Int.MAX_VALUE / scale && nextHeight <= Int.MAX_VALUE / scale) {
            "Wayland pixel dimensions overflow: ${nextWidth}x$nextHeight scale=$scale"
        }
        this.width = nextWidth
        this.height = nextHeight
        this.scale = scale
        configured = true
    }

    fun pixelSize(): Pair<Int, Int> {
        check(!closed) { "the Wayland host is closed" }
        return if (configured) width * scale to height * scale else 0 to 0
    }

    fun close() { closed = true }
}
