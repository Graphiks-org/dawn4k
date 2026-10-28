package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.dawn4k.toolkit.SharedRetirement
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource

internal enum class SurfacePlatform { Apple, Windows, Linux, Android }
internal expect val surfacePlatform: SurfacePlatform

/** Advanced native integration. All identifiers must remain valid until their retained leases retire. */
object DawnSurfaceTargets {
    fun metalLayer(layerAddress: Long, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retention: DawnTargetRetention): DawnSurfaceTarget {
        require(layerAddress != 0L) { "Metal layer is null" }
        requirePlatform(SurfacePlatform.Apple)
        return retained(NativeSurfaceSource.Metal(layerAddress), owner, states, retention)
    }
    fun win32(hwnd: Long, hinstance: Long, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retention: DawnTargetRetention): DawnSurfaceTarget {
        require(hwnd != 0L && hinstance != 0L) { "Win32 window and instance must be nonzero" }
        requirePlatform(SurfacePlatform.Windows)
        return retained(NativeSurfaceSource.Win32(hwnd, hinstance), owner, states, retention)
    }
    fun x11(display: Long, window: ULong, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retention: DawnTargetRetention): DawnSurfaceTarget {
        require(display != 0L && window != 0uL) { "X11 display and window must be nonzero" }
        requirePlatform(SurfacePlatform.Linux)
        return retained(NativeSurfaceSource.X11(display, window), owner, states, retention)
    }
    fun wayland(display: Long, surface: Long, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retention: DawnTargetRetention): DawnSurfaceTarget {
        require(display != 0L && surface != 0L) { "Wayland display and surface must be nonzero" }
        requirePlatform(SurfacePlatform.Linux)
        return retained(NativeSurfaceSource.Wayland(display, surface), owner, states, retention)
    }
    private fun requirePlatform(required: SurfacePlatform) {
        if (surfacePlatform != required) throw UnsupportedOperationException("$required target is unavailable on $surfacePlatform")
    }

    internal fun retained(
        source: NativeSurfaceSource, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>, retention: DawnTargetRetention,
    ): DawnSurfaceTarget = object : DawnSurfaceTarget {
        override val state = states

        override suspend fun retain(): DawnTargetLease {
            var acquired = false
            var generation = 0L
            try {
                owner.onOwner {
                    val snapshot = states.value
                    check(snapshot.available) { "presentation target is unavailable" }
                    generation = snapshot.generation
                    retention.retain()
                    acquired = true
                    check(states.value.available && states.value.generation == generation) { "target changed during retention" }
                }
                return PlatformTargetLease(source, owner, states, retention, generation)
            } catch (failure: Throwable) {
                withContext(NonCancellable) {
                    if (acquired) try { owner.onOwner { retention.retire() } }
                    catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
                }
                throw failure
            }
        }
    }
}

private class PlatformTargetLease(
    private val source: NativeSurfaceSource,
    private val owner: DawnPlatformOwner,
    override val state: StateFlow<DawnTargetState>,
    private val retention: DawnTargetRetention,
    private val generation: Long,
) : DawnTargetLease {
    private val retired = kotlinx.atomicfu.atomic(false)
    private val retirement = SharedRetirement {
        owner.onOwner { retention.retire() }
        retired.value = true
    }
    override fun createSurface(context: DawnContext): DawnSurfaceReference {
        val current = state.value
        check(!retired.value && current.available && current.generation == generation) { "retained presentation target is unavailable" }
        return source.createSurface(context)
    }
    override suspend fun retire() = retirement.retireOnce()
}
