package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.internal.*
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.dawn4k.native.*
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class DeviceLossTest {
    @Test
    fun progressFailureInterruptsWaiters() = runTest {
        val owner = DawnExecutionOwner.dedicated()
        val lifecycle = SessionLifecycle()
        val entered = CompletableDeferred<Unit>()
        val failure = IllegalStateException("progress failed")
        val events = EventProgress(owner, 1.milliseconds, { throw failure }, { false })
        events.onFailure = lifecycle::fail
        try {
            val waiter = launch {
                owner.run { lifecycle.track { entered.complete(Unit); awaitCancellation() } }
            }
            entered.await()
            owner.run { events.start() }
            waiter.join()
            assertTrue(waiter.isCancelled)
            assertEquals(failure, assertIs<DawnSessionState.Failed>(lifecycle.state.value).cause)
        } finally { owner.run { events.stop() }; owner.close() }
    }
}
