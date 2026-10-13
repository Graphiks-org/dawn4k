package org.graphiks.dawn4k.acid

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable
import kotlin.time.Duration

internal suspend fun executeCase(id: String, timeout: Duration, body: suspend () -> CaseResult): CaseResult =
    withContext(Dispatchers.Default) {
        // Transport failures as values: coroutine stack-trace recovery can copy
        // exceptions and lose suppressed teardown errors across scope boundaries.
        var observedFailure: Throwable? = null
        val outcome = withTimeoutOrNull(timeout) {
            runCatching { body() }.also { observedFailure = it.exceptionOrNull() }
        } ?: return@withContext CaseResult(id, "failed",
            "Timed out after $timeout${observedFailure?.let { "\n${it.stackTraceToString()}" } ?: ""}",
            recoverable = observedFailure?.hasUnclosedDawnContext() != true)
        val failure = outcome.exceptionOrNull() ?: return@withContext outcome.getOrThrow()
        when {
            failure is TimeoutCancellationException -> CaseResult(id, "failed", failure.stackTraceToString(),
                recoverable = !failure.hasUnclosedDawnContext())
            failure is CancellationException -> throw failure
            failure is CompatibilityModeUnavailable && id == AcidCaseId.AdapterCompatibilityFeatureLevel.id &&
                failure.suppressedExceptions.isEmpty() -> CaseResult(id, "unsupported", failure.message)
            else -> CaseResult(id, "failed", failure.stackTraceToString(),
                recoverable = !failure.hasUnclosedDawnContext())
        }
    }

internal fun checkDeviceFeatures(required: Set<GPUFeatureName>, actual: Set<GPUFeatureName>) {
    check(actual.containsAll(required)) { "Device dropped requested features: ${required - actual}" }
}

internal fun preserveFailure(original: Throwable?, cleanup: Throwable) {
    if (original != null) original.addSuppressed(cleanup) else throw cleanup
}
