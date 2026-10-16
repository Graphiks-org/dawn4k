@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExportObjCClass
import kotlinx.cinterop.ObjCClass
import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.useContents
import org.graphiks.dawn4k.toolkit.surface.internal.TargetLifecycle
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement
import platform.CoreGraphics.CGRect
import platform.CoreGraphics.CGSizeMake
import platform.QuartzCore.CAMetalLayer
import platform.UIKit.UIView
import platform.UIKit.UIViewMeta

private fun checkedMainFrame(frame: CValue<CGRect>): CValue<CGRect> {
    requireAppleMainThread()
    return frame
}

/** UIKit owns layout; the GPU session owns presentation into this view's Metal layer. */
@ExportObjCClass
class DawnMetalView(frame: CValue<CGRect>) : UIView(frame = checkedMainFrame(frame)) {
    companion object : UIViewMeta() {
        override fun layerClass(): ObjCClass = CAMetalLayer
    }

    private val owner = DawnPlatformOwners.appleMain()
    private val lifecycle = TargetLifecycle()
    private val retirement = TargetRetirement()
    private var windowAddress = 0L

    val target: DawnSurfaceTarget = object : DawnSurfaceTarget {
        override val state = lifecycle.state
        override suspend fun retain(): DawnTargetLease {
            val metal = owner.onOwner { layer as CAMetalLayer }
            return metalLayerTarget(metal, state, retirement).retain()
        }
    }

    private fun updateDrawable(): PixelExtent {
        requireAppleMainThread()
        val extent = bounds.useContents { PixelExtent.fromLogical(size.width, size.height, contentScaleFactor) }
        (layer as CAMetalLayer).apply {
            contentsScale = contentScaleFactor
            drawableSize = CGSizeMake(extent.width.toDouble(), extent.height.toDouble())
        }
        return extent
    }

    override fun layoutSubviews() {
        requireAppleMainThread()
        super.layoutSubviews()
        val extent = updateDrawable()
        if (window != null) {
            if (!lifecycle.state.value.available) lifecycle.available(extent)
            else lifecycle.resize(lifecycle.state.value.generation, extent)
        }
    }

    override fun didMoveToWindow() {
        requireAppleMainThread()
        super.didMoveToWindow()
        val attached = window
        if (attached == null) {
            lifecycle.invalidate(lifecycle.state.value.generation)
            windowAddress = 0L
        } else {
            contentScaleFactor = attached.screen.scale
            val address = attached.objcPtr().toLong()
            val extent = updateDrawable()
            if (address != windowAddress || !lifecycle.state.value.available) lifecycle.available(extent)
            else lifecycle.resize(lifecycle.state.value.generation, extent)
            windowAddress = address
            setNeedsLayout()
        }
    }

    /** Await the last detached generation without blocking UIKit's main queue. */
    suspend fun awaitRetirement() { retirement.await(lifecycle.lastInvalidatedGeneration) }
}
