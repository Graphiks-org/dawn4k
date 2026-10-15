package org.graphiks.dawn4k.toolkit

import org.graphiks.dawn4k.DawnConfig
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPURequestAdapterOptions
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

sealed interface DawnExecutionMode {
    data class Managed(val owner: DawnExecutionOwner? = null) : DawnExecutionMode
    data class Manual(val owner: DawnExecutionOwner) : DawnExecutionMode
}

data class DawnSessionOptions(
    val config: DawnConfig = DawnConfig(),
    val adapterOptions: GPURequestAdapterOptions? = null,
    val deviceDescriptor: GPUDeviceDescriptor? = null,
    val execution: DawnExecutionMode = DawnExecutionMode.Managed(),
    val startupTimeout: Duration = 10.seconds,
    val shutdownTimeout: Duration = 10.seconds,
    val progressInterval: Duration = 1.milliseconds,
) {
    init {
        require(startupTimeout.isFinite() && startupTimeout > Duration.ZERO) { "startupTimeout must be positive and finite" }
        require(shutdownTimeout.isFinite() && shutdownTimeout > Duration.ZERO) { "shutdownTimeout must be positive and finite" }
        require(progressInterval.isFinite() && progressInterval > Duration.ZERO) { "progressInterval must be positive and finite" }
    }
}
