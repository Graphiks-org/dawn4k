package org.graphiks.dawn4k

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertSame

class CallerThreadGpuTest {
    @Test fun bridgeExecutesOnEachCallerAndReturnsNullExactlyOnce() {
        DawnContext.create().use { context ->
            val bridge = context.nativeBridge()
            val executor = Executors.newSingleThreadExecutor()
            try {
                fun verify() {
                    val caller = Thread.currentThread()
                    assertSame(caller, bridge.call { Thread.currentThread() })
                    var calls = 0
                    assertNull(bridge.call { calls++; null })
                    assertEquals(1, calls)
                }
                verify()
                executor.submit { verify() }.get(10, TimeUnit.SECONDS)
            } finally { executor.shutdownNow() }
        }
    }
}
