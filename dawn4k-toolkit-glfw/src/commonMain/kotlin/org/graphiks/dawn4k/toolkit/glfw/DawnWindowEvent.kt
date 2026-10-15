package org.graphiks.dawn4k.toolkit.glfw

import org.graphiks.dawn4k.toolkit.surface.PixelExtent

sealed interface DawnWindowEvent {
    data object CloseRequested : DawnWindowEvent
    data class Resized(val extent: PixelExtent) : DawnWindowEvent
    data class Key(val key: Int, val scanCode: Int, val action: Int, val modifiers: Int) : DawnWindowEvent
    data class PointerMoved(val x: Double, val y: Double) : DawnWindowEvent
    data class PointerButton(val button: Int, val action: Int, val modifiers: Int) : DawnWindowEvent
    data class Scroll(val x: Double, val y: Double) : DawnWindowEvent
}
