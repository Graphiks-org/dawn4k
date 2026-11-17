package org.graphiks.dawn4k.webgpu.toolkit.glfw

import org.graphiks.dawn4k.webgpu.toolkit.surface.PixelExtent

/**
 * One event a window publishes. Positions are in logical content pixels, the same space
 * [DawnWindow.resize] accepts, so a renderer can map them onto its own layout without
 * knowing the display scale.
 */
sealed interface DawnWindowEvent {
    data object CloseRequested : DawnWindowEvent

    /** The framebuffer changed size; [extent] is in physical pixels. */
    data class Resized(val extent: PixelExtent) : DawnWindowEvent

    /**
     * A key changed state. [key] is the platform key code, [scanCode] the physical key, and
     * [action] a GLFW-style action (`0` release, `1` press, `2` repeat). [text] carries the
     * character the key produced, when the platform reports one.
     */
    data class Key(
        val key: Int,
        val scanCode: Int,
        val action: Int,
        val modifiers: Int,
        val text: String? = null,
    ) : DawnWindowEvent

    /** The pointer moved to ([x], [y]) in logical pixels relative to the content origin. */
    data class PointerMoved(val x: Double, val y: Double) : DawnWindowEvent

    /** The pointer left the window, or its position became unknown. */
    data object PointerExited : DawnWindowEvent

    /**
     * A pointer button changed state. [button] is the platform button index and [action] a
     * GLFW-style action (`0` release, `1` press).
     */
    data class PointerButton(val button: Int, val action: Int, val modifiers: Int) : DawnWindowEvent

    /** A wheel or trackpad scroll of ([x], [y]) logical pixels. */
    data class Scroll(val x: Double, val y: Double) : DawnWindowEvent

    /** The window gained or lost input focus. */
    data class FocusChanged(val focused: Boolean) : DawnWindowEvent
}
