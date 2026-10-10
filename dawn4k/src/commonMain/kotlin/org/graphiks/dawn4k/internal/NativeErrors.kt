package org.graphiks.dawn4k.internal

import org.graphiks.dawn4k.native.WGPUDeviceLostReason
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_CallbackCancelled
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_Destroyed
import org.graphiks.dawn4k.native.WGPUDeviceLostReason_FailedCreation
import org.graphiks.dawn4k.native.WGPUErrorType
import org.graphiks.dawn4k.native.WGPUErrorType_Internal
import org.graphiks.dawn4k.native.WGPUErrorType_OutOfMemory
import org.graphiks.dawn4k.native.WGPUErrorType_Validation
import org.graphiks.dawn4k.native.WGPUPopErrorScopeStatus
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus
import org.graphiks.dawn4k.native.WGPURequestDeviceStatus
import org.graphiks.dawn4k.native.WGPUStatus
import org.graphiks.dawn4k.native.WGPUStatus_Success
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.GPUDeviceLostReason
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUInternalError
import org.graphiks.webgpu.GPUOutOfMemoryError
import org.graphiks.webgpu.GPUValidationError

/**
 * Marker for the runtime-side bookkeeping of mapped native errors; the mapped
 * errors themselves implement the WebGPU leaf interfaces, so [gpuError] hands
 * out the public face without a cast.
 */
internal interface DawnNativeError {
    val message: String

    /** The WebGPU error face of this mapped error (this very object). */
    val gpuError: GPUError
}

// GPUError is sealed inside the webgpu-api module, so the raw runtime maps each
// native error onto its leaf subtype — the only sanctioned indirect
// implementation of the sealed hierarchy from this module.

/** The validation error of an uncaptured WebGPU validation failure. */
internal class DawnValidationError(override val message: String) : GPUValidationError, DawnNativeError {
    override val gpuError: GPUError get() = this
}

/** The out-of-memory error of an uncaptured WebGPU OOM failure. */
internal class DawnOutOfMemoryError(override val message: String) : GPUOutOfMemoryError, DawnNativeError {
    override val gpuError: GPUError get() = this
}

/** The internal error of an uncaptured implementation-specific failure. */
internal class DawnInternalError(override val message: String) : GPUInternalError, DawnNativeError {
    override val gpuError: GPUError get() = this
}

/**
 * Maps a native error type onto its WebGPU error for the error-scope and
 * uncaptured-error routes: null for the native `NoError` and `Unknown` types,
 * which have no WebGPU error interface.
 */
internal fun scopeError(type: WGPUErrorType, message: String): GPUError? = when (type) {
    WGPUErrorType_Validation -> DawnValidationError(message)
    WGPUErrorType_OutOfMemory -> DawnOutOfMemoryError(message)
    WGPUErrorType_Internal -> DawnInternalError(message)
    else -> null
}

/**
 * Maps an uncaptured-error type to its WebGPU error; null for the native
 * `NoError` and `Unknown` types, which have no WebGPU error interface.
 */
internal fun uncapturedError(type: WGPUErrorType, message: String): DawnNativeError? =
    scopeError(type, message) as DawnNativeError?

/** Requires a native status call to have succeeded, with the call named in the diagnostic. */
internal fun requireWgpuSuccess(status: WGPUStatus, call: String) {
    if (status != WGPUStatus_Success) {
        throw IllegalStateException("$call failed (status=$status)")
    }
}

/** A native error-scope pop completed without a usable outcome. */
internal class DawnPopErrorScopeException(status: WGPUPopErrorScopeStatus, message: String) :
    IllegalStateException("the error scope pop failed (status=$status): $message")

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
internal class DawnRequestAdapterException(val status: WGPURequestAdapterStatus, message: String) :
    IllegalStateException("the adapter request failed (status=$status): $message")

/** A native device request completed without a usable device. */
internal class DawnRequestDeviceException(status: WGPURequestDeviceStatus, message: String) :
    IllegalStateException("the device request failed (status=$status): $message")
