package org.graphiks.dawn4k.demo

import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DemoShutdownOrderingTest {
    @Test fun childClosureWaitsForConcurrentProgressionToReturn() = checkShutdown(cancelled = false)
    @Test fun cancelledConsumerJoinsProgressionBeforeChildClosure() = checkShutdown(cancelled = true)

    private fun checkShutdown(cancelled: Boolean) = runBlocking {
        Executors.newFixedThreadPool(2).asCoroutineDispatcher().use { dispatcher ->
            val progressing = AtomicBoolean()
            val closed = AtomicBoolean()
            val enteredProgress = CountDownLatch(1)
            val blockDone = CountDownLatch(1)
            val releaseProgress = CountDownLatch(1)
            val consumer = async(dispatcher) {
                withEventProgress(
                    progress = {
                        progressing.set(true)
                        enteredProgress.countDown()
                        check(releaseProgress.await(10, TimeUnit.SECONDS))
                        progressing.set(false)
                    },
                    afterProgress = {
                        assertFalse(progressing.get(), "children cannot close during processEvents")
                        closed.set(true)
                    },
                ) {
                    check(enteredProgress.await(10, TimeUnit.SECONDS))
                    blockDone.countDown()
                    if (cancelled) awaitCancellation()
                }
            }
            try {
                assertTrue(blockDone.await(10, TimeUnit.SECONDS))
                assertFalse(closed.get())
                if (cancelled) consumer.cancel()
                releaseProgress.countDown()
                if (cancelled) consumer.join() else consumer.await()
                assertTrue(closed.get())
            } finally {
                releaseProgress.countDown()
                withContext(kotlinx.coroutines.NonCancellable) { consumer.join() }
            }
        }
    }
}
