package org.graphiks.dawn4k

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.testing.NativeFixture
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFeatureName
import org.graphiks.webgpu.GPUQueryType
import org.graphiks.webgpu.descriptors.QuerySetDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNull

class QuerySetContractGpuTest {
    @Test
    fun timestampWithoutEnabledFeatureThrowsWithoutScopeError() = runBlocking {
        if (!gpuTestEnvironment("QuerySetContractGpuTest.timestampWithoutEnabledFeatureThrowsWithoutScopeError")) return@runBlocking
        NativeFixture.open().use { fixture ->
            val device = DawnDevice(fixture.session, null, false)
            assertFalse(GPUFeatureName.TimestampQuery in device.features)
            device.pushErrorScope(GPUErrorFilter.Validation)
            try {
                device.createQuerySet(QuerySetDescriptor(GPUQueryType.Occlusion, 2u)).use {
                    assertEquals(GPUQueryType.Occlusion, it.type)
                }
                assertFailsWith<IllegalArgumentException> {
                    device.createQuerySet(QuerySetDescriptor(GPUQueryType.Timestamp, 2u)).close()
                }
            } finally {
                assertNull(device.popErrorScope().getOrThrow())
            }
            Unit
        }
    }
}
