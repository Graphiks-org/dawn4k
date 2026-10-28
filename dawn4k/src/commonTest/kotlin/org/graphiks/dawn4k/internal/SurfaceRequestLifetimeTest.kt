package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals

class SurfaceRequestLifetimeTest {
    @Test
    fun cancellationRetainsSurfaceUntilNativeTerminal() = runTest {
        val owner = Any()
        var refs = 1
        val surface = SurfaceReferenceState(owner, 42L, { refs++ }, { refs-- })
        val lease = surface.retainForRequest(owner)
        val request = PendingOperation<Int> {}
        request.onNativeTerminal { lease.close() }
        val waiting = async(start = CoroutineStart.UNDISPATCHED) { request.await() }
        surface.close()
        waiting.cancelAndJoin()
        assertEquals(1, refs)
        request.complete(Result.success(7))
        assertEquals(1, refs, "outcome delivery is not proof of native quiescence")
        request.nativeTerminal()
        request.nativeTerminal()
        assertEquals(0, refs)
    }

    @Test
    fun issueFailureCanReleaseTheLeaseWithoutAnOutcome() {
        val owner = Any()
        var refs = 1
        val surface = SurfaceReferenceState(owner, 42L, { refs++ }, { refs-- })
        val lease = surface.retainForRequest(owner)
        val request = PendingOperation<Int> {}
        request.onNativeTerminal { lease.close() }
        request.nativeTerminal()
        assertEquals(1, refs)
        surface.close()
        assertEquals(0, refs)
    }

    @Test
    fun registeringAfterNativeTerminalObservesTheSettlement() {
        val request = PendingOperation<Int> {}
        request.nativeTerminal()
        var calls = 0
        request.onNativeTerminal { calls++ }
        assertEquals(1, calls)
    }
}
