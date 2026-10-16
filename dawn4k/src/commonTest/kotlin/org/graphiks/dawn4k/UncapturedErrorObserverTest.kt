package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.*
import kotlin.test.*

class UncapturedErrorObserverTest {
    @Test
    fun observersPreserveUserCallbackAndDetachWithoutHistory() {
        val callbacks = DeviceCallbacks()
        var userCalls = 0
        var observed = 0
        callbacks.uncapturedErrorSink = { userCalls++ }
        val lease = callbacks.observeUncapturedErrors { observed++ }
        callbacks.handleUncapturedError(DawnOutOfMemoryError("oom"))
        assertEquals(1, userCalls)
        assertEquals(1, observed)
        lease.close()
        lease.close()
        callbacks.handleUncapturedError(DawnValidationError("validation"))
        assertEquals(2, userCalls)
        assertEquals(1, observed)
        val fresh = callbacks.observeUncapturedErrors { observed++ }
        assertEquals(1, observed)
        callbacks.close()
        fresh.close()
        assertFailsWith<IllegalStateException> { callbacks.observeUncapturedErrors { } }
    }

    @Test
    fun failingObserverDoesNotSuppressOtherObserversOrUserCallback() {
        val callbacks = DeviceCallbacks()
        var userCalls = 0
        var observed = 0
        callbacks.uncapturedErrorSink = { userCalls++ }
        callbacks.observeUncapturedErrors { error("observer failure") }
        callbacks.observeUncapturedErrors { observed++ }
        callbacks.handleUncapturedError(DawnOutOfMemoryError("oom"))
        assertEquals(1, userCalls)
        assertEquals(1, observed)
        callbacks.close()
    }
}
