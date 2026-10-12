package org.graphiks.dawn4k.internal

import java.util.concurrent.CountDownLatch
import java.util.concurrent.atomic.AtomicInteger
import kotlin.concurrent.thread
import kotlin.test.Test
import kotlin.test.assertEquals

class ConcurrentOwnedReferenceTest {
    @Test
    fun concurrentClosesReleaseExactlyOnce() {
        val start = CountDownLatch(1)
        val releases = AtomicInteger()
        val owner = OwnedReference(42, release = { releases.incrementAndGet() })
        val threads = List(16) { thread { start.await(); repeat(100) { owner.close() } } }
        start.countDown()
        threads.forEach { it.join() }
        assertEquals(1, releases.get())
    }
}
