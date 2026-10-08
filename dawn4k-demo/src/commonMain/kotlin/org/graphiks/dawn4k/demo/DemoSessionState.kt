package org.graphiks.dawn4k.demo

internal enum class DemoPhase { Initializing, Running, Stopping, Stopped, Failed }
internal data class DemoSessionState(
    val id: Long = 1,
    val phase: DemoPhase = DemoPhase.Initializing,
    val lifecycleActive: Boolean = true,
    val error: String? = null,
)
