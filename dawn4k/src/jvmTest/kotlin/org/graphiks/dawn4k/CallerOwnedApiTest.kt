package org.graphiks.dawn4k

import kotlin.test.Test
import kotlin.test.assertFalse

class CallerOwnedApiTest {
    @Test fun nativeBridgeDoesNotExposeACallWrapper() {
        assertFalse(NativeBridge::class.java.methods.any { it.name == "call" })
    }

    @Test fun contextExposesOnlyExplicitEventProgression() {
        assertFalse(DawnContext::class.java.methods.any { it.name == "drainEvents" })
    }
}
