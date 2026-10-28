package org.graphiks.dawn4k.toolkit.surface

import android.content.Context
import android.util.AttributeSet
import android.view.SurfaceHolder
import android.view.SurfaceView
import org.graphiks.dawn4k.toolkit.surface.internal.TargetLifecycle
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement

/** Hosts a SurfaceHolder while GPU surface references retire asynchronously on their owners. */
class DawnSurfaceView @JvmOverloads constructor(context: Context, attrs: AttributeSet? = null) :
    SurfaceView(context, attrs), SurfaceHolder.Callback {
    private val owner = DawnPlatformOwners.androidMain()
    private val lifecycle = TargetLifecycle()
    private val retirement = TargetRetirement()
    val target: DawnSurfaceTarget = object : DawnSurfaceTarget {
        override val state = lifecycle.state
        override suspend fun retain(): DawnTargetLease {
            val currentSurface = owner.onOwner { holder.surface }
            return androidSurfaceTarget(currentSurface, owner, state, retirement = retirement).retain()
        }
    }

    init { holder.addCallback(this) }

    override fun surfaceCreated(holder: SurfaceHolder) {
        lifecycle.available(PixelExtent(holder.surfaceFrame.width().coerceAtLeast(0), holder.surfaceFrame.height().coerceAtLeast(0)))
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        lifecycle.resize(lifecycle.state.value.generation, PixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0)))
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        lifecycle.invalidate(lifecycle.state.value.generation)
    }
    override fun onDetachedFromWindow() {
        lifecycle.invalidate(lifecycle.state.value.generation)
        super.onDetachedFromWindow()
    }

    /** Suspends the caller, leaving the UI owner runnable for native release acknowledgement. */
    suspend fun awaitRetirement() { retirement.await(lifecycle.lastInvalidatedGeneration) }
}
