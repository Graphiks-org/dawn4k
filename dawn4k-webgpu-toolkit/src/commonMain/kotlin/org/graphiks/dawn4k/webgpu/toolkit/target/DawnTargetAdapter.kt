package org.graphiks.dawn4k.webgpu.toolkit.target

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SurfaceTarget
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.TargetState

/**
 * Presents a dawn4k [DawnSurfaceTarget] through the toolkit's [SurfaceTarget] contract.
 *
 * Both contracts describe the same native surface — a generation, a physical extent and an
 * availability flag — so this adapter forwards the dawn4k state and hands out
 * [Dawn4kSurfaceTarget]s that retain the dawn4k surface themselves. It is what lets
 * `ToolkitSurface.replaceTarget` accept a target someone already holds in dawn4k terms.
 */
class DawnTargetAdapter(
    val dawnTarget: DawnSurfaceTarget,
    private val platformType: PlatformSurfaceType = PlatformSurfaceType.MetalLayer,
    /** The dawn4k policy the presentation opened over this target applies. */
    private val presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
) : SurfaceTarget {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private val _state = MutableStateFlow(dawnTarget.state.value.toTargetState())

    override val state: StateFlow<TargetState> = _state.asStateFlow()

    init {
        scope.launch { dawnTarget.state.collect { _state.value = it.toTargetState() } }
    }

    override suspend fun retain(): TargetLease {
        val snapshot = dawnTarget.state.value
        return Dawn4kSurfaceTarget(
            dawnTarget = dawnTarget,
            platformType = platformType,
            initialExtent = snapshot.extent.toExtent2D(),
            presentationOptions = presentationOptions,
        ).retain()
    }

    override suspend fun retire(lease: TargetLease) {
        // The lease fronts a dawn4k surface and retires it; a stale lease is a no-op there.
        if (lease is Dawn4kTargetLease) runCatching { lease.surfaceTarget.retire(lease) }
    }

    /** Stops forwarding the dawn4k state; the target itself belongs to the caller. */
    fun close() = scope.cancel()
}

private fun org.graphiks.dawn4k.toolkit.surface.DawnTargetState.toTargetState(): TargetState =
    TargetState(generation, extent.toExtent2D(), available)
