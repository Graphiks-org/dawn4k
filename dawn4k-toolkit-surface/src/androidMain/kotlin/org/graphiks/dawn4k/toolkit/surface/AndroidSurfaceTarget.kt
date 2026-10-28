package org.graphiks.dawn4k.toolkit.surface

import android.os.Handler
import android.os.Looper
import android.view.Surface
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement
import org.graphiks.dawn4k.toolkit.surface.internal.retainedHandleTarget
import kotlin.coroutines.CoroutineContext

fun androidSurfaceTarget(
    surface: Surface, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>,
    retention: DawnTargetRetention = EmptyTargetRetention,
): DawnSurfaceTarget = androidSurfaceTarget(surface, owner, states, retention, null)

internal fun androidSurfaceTarget(
    surface: Surface, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>,
    retention: DawnTargetRetention = EmptyTargetRetention, retirement: TargetRetirement?,
): DawnSurfaceTarget = retainedHandleTarget(
    source = { NativeSurfaceSource.AndroidWindow(it) },
    acquire = {
        require(surface.isValid) { "Android Surface is released" }
        val window = AndroidNativeWindow.acquire(surface)
        check(window != 0L) { "ANativeWindow_fromSurface failed" }
        window
    },
    release = { AndroidNativeWindow.release(it) },
    owner = owner,
    states = states,
    retention = retention,
    retirement = retirement,
)

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
