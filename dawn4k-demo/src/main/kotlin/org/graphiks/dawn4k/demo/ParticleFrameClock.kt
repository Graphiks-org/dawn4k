package org.graphiks.dawn4k.demo

/** Keeps wall-clock pauses and scene transitions out of simulation time. */
internal class ParticleFrameClock {
    private var previousState: ParticleControlState? = null
    private var previousTime = 0L

    fun advance(state: ParticleControlState, now: Long): Float {
        val previous = previousState
        val transition = previous == null || previous.paused != state.paused ||
            previous.count != state.count || previous.resetGeneration != state.resetGeneration
        val delta = if (state.paused || transition) 0f
            else ((now - previousTime) / 1e9).toFloat().coerceIn(0f, 0.05f)
        previousState = state
        previousTime = now
        return delta
    }
}
