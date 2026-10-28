package org.graphiks.dawn4k.toolkit

import org.graphiks.dawn4k.DawnConfig
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPURequestAdapterOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

/** How Dawn events progress once a session runs. */
sealed interface DawnExecutionMode {
    /** Drives event progression for the session; `null` (only [Managed]) makes the session create a dedicated owner. */
    val owner: DawnExecutionOwner?

    /** The toolkit pumps Dawn events on [owner]: borrowed when supplied, a session-owned dedicated dispatcher otherwise. */
    data class Managed(override val owner: DawnExecutionOwner? = null) : DawnExecutionMode

    /** The application pumps Dawn events itself through [DawnSession.manualProgress], from one driver on [owner]. */
    data class Manual(override val owner: DawnExecutionOwner) : DawnExecutionMode
}

/**
 * Options for opening a session. Every timeout bounds one attempt, not the whole
 * operation: a timed-out shutdown keeps its handles for a retry, and a failed
 * startup may additionally wait up to [shutdownTimeout] for rollback.
 */
data class DawnSessionOptions(
    /** Backend configuration used to create the session's Dawn context. */
    val config: DawnConfig = DawnConfig(),
    /** Optional WebGPU options applied when requesting the session's adapter. */
    val adapterOptions: GPURequestAdapterOptions? = null,
    /** Optional descriptor applied when requesting the session's device. */
    val deviceDescriptor: GPUDeviceDescriptor? = null,
    /** Event progression policy; see [DawnExecutionMode]. */
    val execution: DawnExecutionMode = DawnExecutionMode.Managed(),
    /** Deadline for one startup attempt: context, adapter, device and target attachment. */
    val startupTimeout: Duration = 10.seconds,
    /** Deadline for one shutdown attempt; a timeout leaves handles alive and retryable. */
    val shutdownTimeout: Duration = 10.seconds,
    /** Delay between event pumps in managed progression and in shutdown drains. */
    val progressInterval: Duration = 1.milliseconds,
) {
    init {
        require(startupTimeout.isFinite() && startupTimeout > Duration.ZERO) { "startupTimeout must be positive and finite" }
        require(shutdownTimeout.isFinite() && shutdownTimeout > Duration.ZERO) { "shutdownTimeout must be positive and finite" }
        require(progressInterval.isFinite() && progressInterval > Duration.ZERO) { "progressInterval must be positive and finite" }
    }
}
