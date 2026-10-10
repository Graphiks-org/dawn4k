package org.graphiks.dawn4k.internal

import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentResourceRegistryTest {
    @Test fun concurrentAcquisitionsAreAllReleasedExactlyOnce() {
        val registry = ResourceRegistry()
        val releases = AtomicInteger()
        val executor = Executors.newFixedThreadPool(4)
        try {
            val jobs = List(1000) {
                executor.submit { registry.own(Any(), null) { releases.incrementAndGet() } }
            }
            jobs.forEach { it.get(10, TimeUnit.SECONDS) }
            assertEquals(1000, registry.debugRemainingRefs())
            registry.close()
            registry.close()
            assertEquals(1000, releases.get())
        } finally { executor.shutdownNow() }
    }
}
