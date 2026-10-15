package org.graphiks.dawn4k.toolkit.surface.internal

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.graphiks.dawn4k.toolkit.surface.*

internal class PresentationController(
    attachment: SurfaceAttachment,
    private val options: DawnPresentationOptions,
    private val recreate: () -> NativeSurface,
) {
    private var attachment: SurfaceAttachment? = attachment
    private var active: DawnFrame? = null
    private var capabilities: SurfaceCapabilities? = null
    private var reconfigure = false
    private val selected = MutableStateFlow<DawnSurfaceConfiguration?>(null)
    val configuration = selected.asStateFlow()
    val currentAttachment: SurfaceAttachment? get() = attachment
    val activeGeneration: Long? get() = attachment?.generation

    fun initialize() {
        val current = checkNotNull(attachment)
        capabilities = current.surface.capabilities()
        synchronizeConfiguration(force = true)
    }

    private fun synchronizeConfiguration(force: Boolean = false): DawnTargetState {
        val current = checkNotNull(attachment) { "target is detached" }
        val latest = current.lease.state.value
        val state = if (latest.generation == current.generation) latest
            else latest.copy(generation = current.generation, available = false)
        val config = checkNotNull(capabilities).select(options, state.extent)
        if (force || reconfigure || config != selected.value) {
            if (state.available && state.extent.isPositive) current.surface.configure(config)
            else if (selected.value?.extent?.isPositive == true) current.surface.unconfigure()
            selected.value = config
            reconfigure = false
        }
        return state
    }

    suspend fun acquireFrame(): DawnFrame? {
        check(active == null) { "a frame is already active" }
        val current = attachment ?: return null
        val state = synchronizeConfiguration()
        if (!state.available) { detachTarget(state.generation); return null }
        if (!state.extent.isPositive) return null
        repeat(2) { attempt ->
            val surface = current.surface
            when (val acquired = surface.acquire()) {
                is SurfaceAcquisition.Texture -> {
                    val frame = DawnFrame(acquired, state.extent, surface, {
                        val latest = current.lease.state.value
                        attachment === current && latest.available && latest.extent.isPositive && latest.generation == state.generation
                    }) { active = null; if (acquired.suboptimal) reconfigure = true }
                    active = frame
                    return frame
                }
                SurfaceAcquisition.Timeout -> return null
                SurfaceAcquisition.Outdated -> if (attempt == 0) synchronizeConfiguration(force = true)
                SurfaceAcquisition.Lost -> if (attempt == 0) {
                    surface.unconfigure()
                    reconfigure = true
                    val replacement = recreate()
                    try {
                        val caps = replacement.capabilities()
                        val config = caps.select(options, state.extent)
                        if (state.extent.isPositive) replacement.configure(config)
                        current.replaceNative(replacement)
                        capabilities = caps
                        selected.value = config
                        reconfigure = false
                    } catch (failure: Throwable) {
                        replacement.close()
                        throw failure
                    }
                }
                is SurfaceAcquisition.Error -> throw acquired.cause
            }
        }
        return null
    }

    suspend fun replaceTarget(target: DawnSurfaceTarget, replacement: SurfaceAttachment) {
        check(active == null) { "cannot replace a target during frame use" }
        val state = replacement.lease.state.value
        require(state.generation == target.state.value.generation) { "retained target changed generation" }
        require(state.generation != activeGeneration) { "replacement must use a fresh target generation" }
        try {
            val caps = replacement.surface.capabilities()
            val config = caps.select(options, state.extent)
            if (state.available && state.extent.isPositive) replacement.surface.configure(config)
            attachment?.retire()
            attachment = replacement
            capabilities = caps
            selected.value = config
            reconfigure = false
        } catch (failure: Throwable) {
            try { replacement.retire() } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
            throw failure
        }
    }
    suspend fun detachTarget(generation: Long) {
        if (activeGeneration == generation) retire()
    }
    suspend fun retire() {
        active?.close()
        attachment?.retire()
        attachment = null
        selected.value = null
    }
}
