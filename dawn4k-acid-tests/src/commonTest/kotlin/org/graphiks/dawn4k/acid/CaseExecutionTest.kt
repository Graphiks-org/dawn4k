package org.graphiks.dawn4k.acid

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.runBlocking
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.CompatibilityModeUnavailable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

class CaseExecutionTest {
    @Test
    fun silentlyDroppedFeatureFails() {
        assertFailsWith<IllegalStateException> {
            checkDeviceFeatures(setOf(GPUFeatureName.TimestampQuery), emptySet())
        }
        checkDeviceFeatures(setOf(GPUFeatureName.TimestampQuery), setOf(GPUFeatureName.TimestampQuery))
    }

    @Test
    fun timeoutIsAFailure() = runBlocking {
        val result = executeCase("slow", 20.milliseconds) { awaitCancellation() }
        assertEquals("slow", result.id)
        assertEquals("failed", result.status)
        assertTrue(result.diagnostic!!.contains("Timed out"))
    }

    @Test
    fun cancellationPropagates() = runBlocking {
        val cancellation = CancellationException("external")
        assertEquals("external", assertFailsWith<CancellationException> {
            executeCase("cancelled", 1.seconds) { throw cancellation }
        }.message)
    }

    @Test
    fun exceptionsKeepTheCaseId() = runBlocking {
        val result = executeCase("broken", 1.seconds) { error("oracle failure") }
        assertEquals("broken", result.id)
        assertEquals("failed", result.status)
        assertTrue(result.diagnostic!!.contains("oracle failure"))
    }

    @Test
    fun successfulCaseKeepsItsResult() = runBlocking {
        val expected = CaseResult("good", "passed", adapterDescription = "GPU")
        assertEquals(expected, executeCase("good", 1.seconds) { expected })
    }

    @Test
    fun cleanupRetainsTheOriginalFailure() {
        val original = IllegalStateException("oracle")
        val cleanup = IllegalArgumentException("cleanup")
        preserveFailure(original, cleanup)
        assertEquals(listOf(cleanup), original.suppressedExceptions)
        assertSame(cleanup, assertFailsWith<IllegalArgumentException> { preserveFailure(null, cleanup) })
    }

    @Test
    fun onlyCompatibilityCaseMayReportCoreFallback() = runBlocking {
        val compatibility = executeCase(AcidCaseId.AdapterCompatibilityFeatureLevel.id, 1.seconds) {
            throw CompatibilityModeUnavailable()
        }
        assertEquals("unsupported", compatibility.status)
        val ordinary = executeCase("buffers.map-write-round-trip", 1.seconds) {
            throw CompatibilityModeUnavailable()
        }
        assertEquals("failed", ordinary.status)
    }

    @Test
    fun cleanupFailurePreventsUnsupportedFallback() = runBlocking {
        val result = executeCase(AcidCaseId.AdapterCompatibilityFeatureLevel.id, 1.seconds) {
            throw CompatibilityModeUnavailable().also { it.addSuppressed(IllegalStateException("cleanup")) }
        }
        assertEquals("failed", result.status)
    }

    @Test
    fun timeoutRetainsCleanupDiagnostics() = runBlocking {
        val result = executeCase("slow-cleanup", 20.milliseconds) {
            var original: Throwable? = null
            try { awaitCancellation() }
            catch (failure: Throwable) { original = failure; throw failure }
            finally {
                withContext(NonCancellable) { preserveFailure(original, IllegalStateException("CLEANUP_SENTINEL")) }
            }
        }
        assertEquals("failed", result.status)
        assertTrue(result.diagnostic!!.contains("CLEANUP_SENTINEL"), result.diagnostic)
    }

    @Test
    fun timeoutWithUnclosedContextCannotContinue() = runBlocking {
        val result = executeCase("slow-unclosed", 20.milliseconds) {
            var original: Throwable? = null
            try { awaitCancellation() }
            catch (failure: Throwable) { original = failure; throw failure }
            finally {
                withContext(NonCancellable) {
                    cleanupDawnContext(original, {}, {}, { error("OPEN_CONTEXT_SENTINEL") })
                }
            }
        }
        assertEquals("failed", result.status)
        assertEquals(false, result.recoverable)
        assertTrue(result.diagnostic!!.contains("OPEN_CONTEXT_SENTINEL"))
    }
}
