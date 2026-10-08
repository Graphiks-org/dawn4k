package org.graphiks.dawn4k.demo

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal data class ParticleControlState(
    val availableCounts: List<Int> = emptyList(),
    val count: Int = 4096,
    val paused: Boolean = false,
    val resetGeneration: Long = 0,
    val ready: Boolean = false,
    val error: String? = null,
    val sessionId: Long = 1,
    val accepting: Boolean = true,
)

/** UI requests are immutable snapshots; only the render loop owns the GPU scene. */
internal class ParticleControls {
    private val mutableState = MutableStateFlow(ParticleControlState())
    val state = mutableState.asStateFlow()

    internal fun beginSession(id: Long) {
        mutableState.value = ParticleControlState(sessionId = id)
    }

    internal fun deactivate() = mutableState.update { it.copy(ready = false, accepting = false) }

    fun initialize(maximum: Int, sessionId: Long = state.value.sessionId) {
        require(maximum > 0) { "The GPU cannot run the particle scene" }
        val choices = listOf(256, 1024, 4096, 16384, 65536).filter { it <= maximum }
            .ifEmpty { listOf(maximum) }
        mutableState.update { state ->
            // GPU setup can finish after the event owner has reported a disconnect.
            // A terminal error wins regardless of the order of these atomic updates.
            if (state.error != null || !state.accepting || state.sessionId != sessionId) state else ParticleControlState(
                sessionId = sessionId,
                availableCounts = choices,
                count = choices.lastOrNull { it <= 4096 } ?: choices.first(),
                ready = true,
            )
        }
    }

    fun togglePause() = mutableState.update { if (it.ready) it.copy(paused = !it.paused) else it }

    fun reset() = mutableState.update {
        if (it.ready) it.copy(resetGeneration = it.resetGeneration + 1) else it
    }

    fun selectCount(count: Int) = mutableState.update {
        if (it.ready && count in it.availableCounts) it.copy(count = count) else it
    }

    fun stepCount(direction: Int) = mutableState.update { state ->
        if (!state.ready || direction == 0) state else {
            val index = state.availableCounts.indexOf(state.count)
            val next = (index + direction.coerceIn(-1, 1))
                .coerceIn(0, state.availableCounts.lastIndex)
            state.copy(count = state.availableCounts[next])
        }
    }

    fun fail(message: String, sessionId: Long = state.value.sessionId) = mutableState.update {
        if (it.error != null || it.sessionId != sessionId || !it.accepting) it
        else it.copy(ready = false, error = message, accepting = false)
    }
}
