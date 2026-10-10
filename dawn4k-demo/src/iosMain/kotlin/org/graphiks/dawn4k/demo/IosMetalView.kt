@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)

package org.graphiks.dawn4k.demo

import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGRectMake
import platform.CoreGraphics.CGSizeMake
import platform.Foundation.NSThread
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta
import kotlin.math.roundToInt

/** UIKit alone owns layout; Compose and Dawn never present into the same layer. */
@ExportObjCClass
internal class IosMetalView(
    private val available: (IosMetalView, PixelExtent) -> Unit,
    private val removed: () -> Unit,
) : UIView(frame = CGRectMake(0.0, 0.0, 0.0, 0.0)) {
    companion object : UIViewMeta() {
        override fun layerClass(): ObjCClass = CAMetalLayer
    }

    override fun layoutSubviews() {
        super.layoutSubviews()
        check(NSThread.isMainThread) { "Metal viewport layout must run on UIKit's main thread" }
        val metal = layer as CAMetalLayer
        val extent = bounds.useContents {
            PixelExtent((size.width * contentScaleFactor).roundToInt(), (size.height * contentScaleFactor).roundToInt())
        }
        metal.contentsScale = contentScaleFactor
        metal.drawableSize = CGSizeMake(extent.width.toDouble(), extent.height.toDouble())
        if (window != null && extent.width > 0 && extent.height > 0) available(this, extent)
    }

    override fun didMoveToWindow() {
        super.didMoveToWindow()
        val attached = window
        if (attached == null) removed() else {
            contentScaleFactor = attached.screen.scale
            setNeedsLayout()
        }
    }

    fun retire() { removed() }
}
