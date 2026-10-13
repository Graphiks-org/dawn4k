package org.graphiks.dawn4k.acid

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.delay
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.descriptors.RequestAdapterOptions
import org.graphiks.webgpu.suite.AcidCase
import org.graphiks.webgpu.suite.AcidCaseId
import org.graphiks.webgpu.suite.AcidFamily
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.test.assertNull

class DawnCaseRunnerTest {
    @Test
    fun publicAdapterFactoryReturnsIndependentAdapters() = runBlocking {
        val case = fixture { context ->
            context.requestAdapter(RequestAdapterOptions()).getOrThrow().use { first ->
                context.requestAdapter(null).getOrThrow().use { second ->
                    assertTrue(first !== second)
                    assertTrue(first.limits.maxTextureDimension2D > 0u)
                    assertTrue(second.limits.maxTextureDimension2D > 0u)
                }
            }
        }
        assertEquals("passed", runDawnCase(case, AcidSettings(null, true)).status)
    }

    @Test
    fun unexpectedValidationErrorFailsTheCase() = runBlocking {
        val case = fixture { context ->
            context.device.createBuffer(BufferDescriptor(4uL, GPUBufferUsage.None)).close()
        }
        val result = runDawnCase(case, AcidSettings(null, true))
        assertEquals("failed", result.status)
        assertTrue(result.diagnostic!!.contains("uncaptured", ignoreCase = true))
    }

    @Test
    fun errorScopesSurviveCoroutineSuspension() = runBlocking {
        val result = runDawnCase(fixture { context ->
            repeat(50) {
                context.device.pushErrorScope(GPUErrorFilter.Validation)
                delay(1)
                assertNull(context.device.popErrorScope().getOrThrow())
            }
        }, AcidSettings(null, true))
        assertEquals("passed", result.status, result.diagnostic)
    }

    private fun fixture(body: suspend (org.graphiks.webgpu.suite.AcidContext) -> Unit) = AcidCase(
        AcidCaseId.AdapterCapabilities,
        AcidFamily.AdapterDeviceFeaturesLimits,
        emptyList(),
        run = body,
    )
}
