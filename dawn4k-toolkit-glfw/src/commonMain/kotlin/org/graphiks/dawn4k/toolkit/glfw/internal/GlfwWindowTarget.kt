package org.graphiks.dawn4k.toolkit.glfw.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.glfw.DawnWindowEvent
import org.graphiks.dawn4k.toolkit.surface.DawnTargetRetention
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.PixelExtent

/**
 * Window-target machinery shared by every GLFW backend: target states, reference counting,
 * the event sink and one callback-application helper per GLFW callback. Platform backends
 * subclass this and add only their platform surface-target construction.
 *
 * The [onRetain]/[onRetire] hooks run inside [retention] around each reference-count change;
 * the JVM Cocoa target passes its CFRetain/CFRelease msgSend pair through them, the native
 * targets pass none (their platform layer applies its own references).
 */
internal open class GlfwWindowTarget(
    extent: PixelExtent,
    private val onRetain: suspend () -> Unit = {},
    private val onRetire: suspend () -> Unit = {},
) {
    val states = MutableStateFlow(DawnTargetState.fresh(extent, true))
    private val references = MutableStateFlow(0)
    var sink: (DawnWindowEvent) -> Unit = {}

    fun event(event: DawnWindowEvent) = sink(event)
    fun resize(extent: PixelExtent) { states.value = states.value.copy(extent = extent) }
    fun invalidate() { states.value = states.value.copy(available = false) }
    suspend fun awaitRetirement() { invalidate(); references.first { it == 0 } }

    val retention: DawnTargetRetention = object : DawnTargetRetention {
        override suspend fun retain() { onRetain(); references.value++ }
        override suspend fun retire() {
            check(references.value > 0)
            onRetire()
            references.value--
        }
    }

    /** Framebuffer-size callback: the extent change reaches both the target states and the sink. */
    fun onResized(width: Int, height: Int) {
        val extent = PixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0))
        resize(extent)
        event(DawnWindowEvent.Resized(extent))
    }

    /** Window-close button callback: the one backend path that emits the terminal [DawnWindowEvent.CloseRequested]. */
    fun onClose() { invalidate(); event(DawnWindowEvent.CloseRequested) }

    fun onKey(key: Int, scanCode: Int, action: Int, modifiers: Int) =
        event(DawnWindowEvent.Key(key, scanCode, action, modifiers))

    fun onPointerMoved(x: Double, y: Double) = event(DawnWindowEvent.PointerMoved(x, y))
    fun onPointerButton(button: Int, action: Int, modifiers: Int) =
        event(DawnWindowEvent.PointerButton(button, action, modifiers))

    fun onScroll(x: Double, y: Double) = event(DawnWindowEvent.Scroll(x, y))
}
