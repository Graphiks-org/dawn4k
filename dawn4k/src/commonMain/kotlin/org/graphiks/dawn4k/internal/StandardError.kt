package org.graphiks.dawn4k.internal

import org.graphiks.webgpu.GPUInternalError
import org.graphiks.webgpu.GPUOutOfMemoryError
import org.graphiks.webgpu.GPUValidationError

/** A fallback diagnostic, not an error history or a second GPU callback. */
internal fun reportUncapturedError(error: DawnNativeError, failure: Throwable? = null) {
    val type = when (error.gpuError) {
        is GPUValidationError -> "Validation"
        is GPUOutOfMemoryError -> "OutOfMemory"
        is GPUInternalError -> "Internal"
    }
    writeStandardError(buildString {
        appendLine("dawn4k: uncaptured $type error: ${error.message}")
        if (failure != null) {
            appendLine("dawn4k: uncaptured-error callback failed:")
            appendLine(failure.stackTraceToString())
        }
    })
}

internal expect fun writeStandardError(message: String)
