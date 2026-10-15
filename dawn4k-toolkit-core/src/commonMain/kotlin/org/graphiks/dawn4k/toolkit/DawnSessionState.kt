package org.graphiks.dawn4k.toolkit

import org.graphiks.webgpu.GPUDeviceLostInfo

sealed interface DawnSessionState {
    data object Ready : DawnSessionState
    data object ShuttingDown : DawnSessionState
    data object ShutdownTimedOut : DawnSessionState
    data class Lost(val info: GPUDeviceLostInfo) : DawnSessionState
    data class Failed(val cause: Throwable) : DawnSessionState
    data object Closed : DawnSessionState
}
