package org.graphiks.dawn4k

import org.graphiks.dawn4k.testing.gpuTestEnvironment
import org.graphiks.dawn4k.testing.gpuTestConfig

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue
import kotlinx.coroutines.test.runTest

class NativeBridgeTest {

    @Test
    fun nativeBridgeRunsBlocksOnTheWorkerAndExposesTheInstanceHandle() = runTest {
        val context = DawnContext.create()
        try {
            val bridge = context.nativeBridge()
            // call() executes the block and returns its result.
            val marker = bridge.call { 42 }
            assertEquals(42, marker)
            // instanceHandle() is a non-zero native pointer while the context is open.
            val handle = bridge.instanceHandle()
            assertNotEquals(0L, handle, "the instance handle must be non-zero while open")
        } finally {
            context.close()
        }
        // After close, the instance handle reads as zero.
        assertEquals(0L, context.nativeBridge().instanceHandle())
    }

    @Test
    fun deviceAndAdapterExposeTheirNativeHandles() = runTest {
        if (!gpuTestEnvironment("NativeBridgeTest.deviceAndAdapterExposeTheirNativeHandles")) return@runTest
        val context = DawnContext.create(gpuTestConfig())
        try {
            context.requestAdapter().getOrThrow().use { adapter ->
                val dawnAdapter = adapter as DawnAdapter
                assertNotEquals(0L, dawnAdapter.nativeHandle())
                adapter.requestDevice().getOrThrow().use { device ->
                    val dawnDevice = device as DawnDevice
                    assertNotEquals(0L, dawnDevice.nativeHandle())
                }
            }
        } finally {
            context.close()
        }
    }
}
