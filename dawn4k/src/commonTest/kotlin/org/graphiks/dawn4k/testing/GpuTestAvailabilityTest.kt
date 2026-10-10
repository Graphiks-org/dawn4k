package org.graphiks.dawn4k.testing

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.internal.DawnRequestAdapterException
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus_Error
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus_CallbackCancelled
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus_Unavailable
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class GpuTestAvailabilityTest {
    @Test
    fun onlyUnavailablePermitsOptionalNonExecution() {
        assertTrue(isUnavailable(DawnRequestAdapterException(WGPURequestAdapterStatus_Unavailable, "none")))
        assertFalse(isUnavailable(DawnRequestAdapterException(WGPURequestAdapterStatus_Error, "error")))
        assertFalse(isUnavailable(DawnRequestAdapterException(WGPURequestAdapterStatus_CallbackCancelled, "closed")))
        assertFalse(isUnavailable(IllegalStateException("device creation failed")))
        assertFalse(isUnavailable(CancellationException("cancelled")))
    }

    @Test
    fun optionalAbsenceWarnsAndDoesNotRunTheBody() = runTest {
        val warnings = mutableListOf<String>()
        var ran = false
        if (checkGpuTestEnvironment("readback", false,
                probe = { Result.failure(unavailable()) }, report = warnings::add)) {
            ran = true
        }
        assertFalse(ran)
        assertEquals(1, warnings.size)
        assertTrue(warnings.single().contains("readback"))
        assertTrue(warnings.single().contains("GPU_TEST_NOT_EXECUTED"))
    }

    @Test
    fun strictAbsenceFailsInsteadOfWarningOnly() = runTest {
        val absence = unavailable()
        val warnings = mutableListOf<String>()
        val thrown = assertFailsWith<DawnRequestAdapterException> {
            checkGpuTestEnvironment("readback", true,
                probe = { Result.failure(absence) }, report = warnings::add)
        }
        assertSame(absence, thrown)
        assertTrue(warnings.isEmpty())
    }

    @Test
    fun adapterErrorsAndCancellationAreNotAvailabilityWarnings() = runTest {
        val failures = listOf(
            DawnRequestAdapterException(WGPURequestAdapterStatus_Error, "bad request"),
            DawnRequestAdapterException(WGPURequestAdapterStatus_CallbackCancelled, "closed"),
            CancellationException("cancelled"),
            IllegalStateException("device failed"),
        )
        for (failure in failures) {
            val thrown = assertFailsWith<Throwable> {
                checkGpuTestEnvironment("readback", false,
                    probe = { Result.failure(failure) }, report = { error("must not warn") })
            }
            assertSame(failure, thrown)
        }
        val cancellation = CancellationException("probe cancelled")
        val thrown = assertFailsWith<CancellationException> {
            checkGpuTestEnvironment("readback", false,
                probe = { throw cancellation }, report = { error("must not warn") })
        }
        assertSame(cancellation, thrown)
    }

    @Test
    fun availableAdapterExecutesTheBodyAndItsFailurePropagates() = runTest {
        var ran = false
        val bodyFailure = AssertionError("readback differs")
        val thrown = assertFailsWith<AssertionError> {
            if (checkGpuTestEnvironment("readback", true,
                    probe = { Result.success(Unit) }, report = { error("must not warn") })) {
                ran = true
                throw bodyFailure
            }
        }
        assertTrue(ran)
        assertSame(bodyFailure, thrown)
    }

    @Test
    fun testSettingsSelectBackendAndStrictMode() {
        assertEquals(GpuTestSettings(null, false), GpuTestSettings.parse(null, null))
        assertEquals(GpuTestSettings(DawnBackend.Vulkan, true), GpuTestSettings.parse("Vulkan", "1"))
        assertEquals(GpuTestSettings(DawnBackend.Metal, false), GpuTestSettings.parse("Metal", "0"))
        assertEquals(GpuTestSettings(DawnBackend.D3D12, false), GpuTestSettings.parse("D3D12", null))
    }

    @Test
    fun invalidSettingsFailRatherThanSilentlyUsingDefaults() {
        for (backend in listOf("", "vulkan", "software")) {
            assertFailsWith<IllegalArgumentException> { GpuTestSettings.parse(backend, null) }
        }
        for (strict in listOf("", "true", "2")) {
            assertFailsWith<IllegalArgumentException> { GpuTestSettings.parse(null, strict) }
        }
    }

    private fun unavailable() = DawnRequestAdapterException(WGPURequestAdapterStatus_Unavailable, "no adapter")
}
