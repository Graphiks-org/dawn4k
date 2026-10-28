package org.graphiks.dawn4k.toolkit

import org.graphiks.webgpu.GPUDeviceLostInfo

/**
 * The observable lifecycle of a [DawnSession], published as [DawnSession.state].
 *
 * Only [Ready] admits work: `run` and the frame presentation entry points
 * reject every other state. Device loss, fatal errors and out-of-memory
 * interrupts arrive
 * only in [Ready], move it to [Lost] or [Failed] and cancel admitted work; the
 * session never returns to [Ready], and cleanup continues in the background
 * until shutdown finishes. A timeout never frees handles: it publishes
 * [ShutdownTimedOut], cleanup keeps retrying on the session's owner, and the
 * application may retry `shutdown()` at any time. [Closed] is final and
 * idempotent.
 */
sealed interface DawnSessionState {
    /** The state after successful startup; the only state that admits work. */
    data object Ready : DawnSessionState
    /** A shutdown attempt is in flight; new work is rejected until it ends. */
    data object ShuttingDown : DawnSessionState
    /** The last shutdown attempt exceeded its deadline; not-yet-closed handles stay alive and another attempt may follow. */
    data object ShutdownTimedOut : DawnSessionState
    /** The native device reported loss; admitted work was cancelled and cleanup runs in the background. */
    data class Lost(val info: GPUDeviceLostInfo) : DawnSessionState
    /** A fatal error stopped the session: progression failure, out of memory, a failed loss wait, or a failed shutdown attempt. */
    data class Failed(val cause: Throwable) : DawnSessionState
    /** Final: every native handle is closed and a session-owned execution owner, if any, is released. */
    data object Closed : DawnSessionState
}
