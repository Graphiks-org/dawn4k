package org.graphiks.dawn4k.toolkit.surface

import android.os.Handler
import android.os.Looper
import android.view.Surface
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.AtomicLong
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement
import kotlin.coroutines.CoroutineContext

fun androidSurfaceTarget(
    surface: Surface, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>,
): DawnSurfaceTarget = androidSurfaceTarget(surface, owner, states, null)

internal fun androidSurfaceTarget(
    surface: Surface, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retirement: TargetRetirement?,
): DawnSurfaceTarget = object : DawnSurfaceTarget {
    override val state = states
    override suspend fun retain(): DawnTargetLease {
        val window = atomic(0L)
        var generation = 0L
        val retention = object : DawnTargetRetention {
            override suspend fun retain() {
                require(surface.isValid) { "Android Surface is released" }
                generation = states.value.generation
                val address = AndroidNativeWindow.acquire(surface)
                check(address != 0L) { "ANativeWindow_fromSurface failed" }
                try { retirement?.acquired(generation); window.value = address }
                catch (failure: Throwable) { AndroidNativeWindow.release(address); throw failure }
            }
            override suspend fun retire() {
                val address = window.value
                if (address != 0L) {
                    AndroidNativeWindow.release(address)
                    window.value = 0L
                    retirement?.retired(generation)
                }
            }
        }
        val target = DawnSurfaceTargets.retained(NativeSurfaceSource.AndroidWindow { window.value }, owner, states, retention)
        return AndroidTargetLease(target.retain(), window)
    }
}

internal class AndroidTargetLease(
    private val delegate: DawnTargetLease,
    private val window: AtomicLong,
) : DawnTargetLease by delegate {
    val nativeWindow: Long get() = window.value
}

object DawnPlatformOwners {
    fun androidMain(): DawnPlatformOwner {
        val main = Looper.getMainLooper()
        val handler = Handler(main)
        return DawnPlatformOwner.fromDispatcher(object : CoroutineDispatcher() {
            override fun isDispatchNeeded(context: CoroutineContext) = Looper.myLooper() !== main
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                check(handler.post(block)) { "Android main looper is stopped" }
            }
        })
    }
}
