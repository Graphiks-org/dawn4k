package org.graphiks.dawn4k.demo

import kotlin.math.roundToInt

internal data class LogicalViewport(val x: Float, val y: Float, val width: Float, val height: Float)
internal data class PixelExtent(val width: Int, val height: Int)

internal enum class LayoutMode { Wide, Compact }
internal fun demoLayoutMode(widthDp: Float, heightDp: Float): LayoutMode =
    if (widthDp >= 720f && heightDp >= 360f) LayoutMode.Wide else LayoutMode.Compact

internal fun metalViewport(windowHeight: Float, viewport: LogicalViewport, flipped: Boolean): LogicalViewport =
    viewport.copy(y = if (flipped) viewport.y else windowHeight - viewport.y - viewport.height)

internal fun scaledExtent(viewport: LogicalViewport, scale: Float): PixelExtent =
    PixelExtent((viewport.width * scale).roundToInt(), (viewport.height * scale).roundToInt())
