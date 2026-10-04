package org.graphiks.dawn4k.internal

import org.graphiks.dawn4k.native.WGPUDeviceLostReason
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_CallbackCancelled
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_Destroyed
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_FailedCreation
import org.graphiks.dawn4k.native.WGPUErrorType
import org.graphiks.dawn4k.native.WGPUErrorType_Internal
import org.graphiks.dawn4k.native.WGPUErrorType_OutOfMemory
import org.graphiks.dawn4k.native.WGPUErrorType_Validation
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus
import org.graphiks.dawn4k.native.WGPURequestDeviceStatus
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUInternalError
import org.graphiks.webgpu.GPUOutOfMemoryError
import org.graphiks.webgpu.GPUValidationError

/**
 * Marker for the runtime-side bookkeeping of mapped native errors; the mapped
 * errors themselves implement the WebGPU leaf interfaces.
 */
internal interface DawnNativeError {
    val message: String
}

// GPUError is sealed inside the webgpu-api module, so the raw runtime maps each
// native error onto its leaf subtype — the only sanctioned indirect
// implementation of the sealed hierarchy from this module.

/** The validation error of an uncaptured WebGPU validation failure. */
internal class DawnValidationError(override val message: String) : GPUValidationError, DawnNativeError

/** The out-of-memory error of an uncaptured WebGPU OOM failure. */
internal class DawnOutOfMemoryError(override val message: String) : GPUOutOfMemoryError, DawnNativeError

/** The internal error of an uncaptured implementation-specific failure. */
internal class DawnInternalError(override val message: String) : GPUInternalError, DawnNativeError

/**
 * Maps an uncaptured-error type to its WebGPU error; null for the native
 * `NoError` and `Unknown` types, which have no WebGPU error interface.
 */
internal fun uncapturedError(type: WGPUErrorType, message: String): DawnNativeError? = when (type) {
    WGPUErrorType_Validation -> DawnValidationError(message)
    WGPUErrorType_OutOfMemory -> DawnOutOfMemoryError(message)
    WGPUErrorType_Internal -> DawnInternalError(message)
    else -> null
}

/** The factual loss report carried by the session's terminal marker. */
internal class DawnDeviceLost(
    override val reason: GPUDeviceLostReason,
    override val message: String,
) : GPUDeviceLostInfo

/** Maps the native device-lost reason onto its WebGPU enumeration. */
internal fun deviceLostReason(reason: WGPUDeviceLostReason): GPUDeviceLostReason = when (reason) {
    WGPUDeviceLostReason_Destroyed -> GPUDeviceLostReason.Destroyed
    WGPUDeviceLostReason_CallbackCancelled -> GPUDeviceLostReason.CallbackCancelled
    WGPUDeviceLostReason_FailedCreation -> GPUDeviceLostReason.FailedCreation
    else -> GPUDeviceLostReason.Unknown
}

/** Abandonment diagnostic: a device loss interrupted a wait. */
internal class DawnDeviceLostException(val info: DawnDeviceLost) :
    IllegalStateException("the device was lost (${info.reason}): ${info.message}")

/** Abandonment diagnostic: the runtime closed while an operation was in flight. */
internal class DawnRuntimeClosedException :
    IllegalStateException("the dawn runtime is closed")

/** A native adapter request completed without a usable adapter. */
internal class DawnRequestAdapterException(status: WGPURequestAdapterStatus, message: String) :
    IllegalStateException("the adapter request failed (status=$status): $message")

/** A native device request completed without a usable device. */
internal class DawnRequestDeviceException(status: WGPURequestDeviceStatus, message: String) :
    IllegalStateException("the device request failed (status=$status): $message")
