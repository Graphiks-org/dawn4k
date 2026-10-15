package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.cancel
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnSurfaceReference
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.descriptors.BufferDescriptor
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

class DawnSessionTest {
    @Test
    fun cancellationAfterDeviceAcquisitionHasOnlyOneCleanupOwner() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        var retirements = 0
        try {
            val opening = launch {
                val caller = currentCoroutineContext()[Job]!!
                val descriptor = object : GPUDeviceDescriptor by DeviceDescriptor() {
                    override val label: String get() { caller.cancel(); return "cancel-after-acquisition" }
                }
                openDawnSession(
                    DawnSessionOptions(deviceDescriptor = descriptor, execution = DawnExecutionMode.Managed(owner)),
                    object : DawnSessionInitialization {
                        override suspend fun prepare(context: DawnContext) = object : DawnPreparedInitialization {
                            override val compatibleSurface: DawnSurfaceReference? = null
                            override suspend fun retire() { retirements++ }
                        }
                    },
                )
            }
            opening.join()
            owner.run { delay(50) }
            assertTrue(opening.isCancelled)
            assertEquals(1, retirements)
        } finally { owner.close() }
    }

    @Test
    fun headlessNeedsNoWindowLibrary() = runTest {
        var context: DawnContext? = null
        withDawnSession { gpu ->
            context = gpu.context
            gpu.device.createBuffer(BufferDescriptor(16uL, GPUBufferUsage.CopyDst)).use { buffer ->
                assertEquals(16uL, buffer.size)
            }
        }
        assertEquals(0L, assertNotNull(context).nativeBridge().instanceHandle())
    }

    @Test
    fun progressesAdapterAndDeviceRequests() = runTest {
        val session = openDawnSession()
        try {
            session.run { gpu ->
                assertNotEquals(0L, gpu.adapter.nativeHandle())
                assertNotEquals(0L, gpu.device.nativeHandle())
                assertTrue(gpu.queue.onSubmittedWorkDone().isSuccess)
            }
        } finally { session.shutdown() }
    }

    @Test
    fun nestedRunUsesSameAdmission() = runTest {
        withDawnSession { gpu ->
            assertEquals(42, gpu.run { it.run { 42 } })
        }
    }

    @Test
    fun cancelledBootstrapClosesContextAfterHookCleanup() = runTest {
        val entered = CompletableDeferred<DawnContext>()
        var hookCleaned = false
        val bootstrap = launch {
            openDawnSession(initialization = object : DawnSessionInitialization {
                override suspend fun prepare(context: DawnContext): DawnPreparedInitialization {
                    try { entered.complete(context); awaitCancellation() }
                    finally { hookCleaned = true }
                }
            })
        }
        val context = entered.await()
        bootstrap.cancelAndJoin()
        assertTrue(hookCleaned)
        assertEquals(0L, context.nativeBridge().instanceHandle())
    }

    @Test
    fun preparedInitializationRetiresBeforeSuccessfulContextShutdown() = runTest {
        var retired = false
        val session = openDawnSession(initialization = object : DawnSessionInitialization {
            override suspend fun prepare(context: DawnContext): DawnPreparedInitialization {
                return object : DawnPreparedInitialization {
                    override val compatibleSurface: DawnSurfaceReference? = null
                    override suspend fun retire() {
                        assertNotEquals(0L, context.nativeBridge().instanceHandle())
                        retired = true
                    }
                }
            }
        })
        val context = session.context
        session.shutdown()
        assertTrue(retired)
        assertEquals(0L, context.nativeBridge().instanceHandle())
    }
}
