package org.graphiks.dawn4k.acid

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

class DawnCleanupTest {
    @Test
    fun attemptsCloseAfterDrainFailureAndRetainsOriginal() = runBlocking {
        val calls = mutableListOf<String>()
        val original = IllegalArgumentException("oracle")
        val drain = IllegalStateException("drain")
        val result = cleanupDawnContext(original,
            stopEvents = { calls += "stop" },
            drain = { calls += "drain"; throw drain },
            close = { calls += "close" },
        )
        assertEquals(listOf("stop", "drain", "close"), calls)
        assertSame(original, result)
        assertEquals(listOf(drain), original.suppressedExceptions)
    }

    @Test
    fun failedCloseMakesTheCaseUnrecoverable() = runBlocking {
        val result = executeCase("unclosed", 1.seconds) {
            val failure = cleanupDawnContext(null, {}, { error("drain") }, { error("open children") })
            throw requireNotNull(failure)
        }
        assertEquals("failed", result.status)
        assertEquals(false, result.recoverable)
        assertTrue(result.diagnostic!!.contains("open children"))
        assertTrue(result.diagnostic.contains("drain"))
    }

    @Test
    fun failedEventJoinDoesNotDrainConcurrently() = runBlocking {
        val calls = mutableListOf<String>()
        val failure = cleanupDawnContext(null,
            stopEvents = { calls += "stop"; error("join") },
            drain = { calls += "drain" },
            close = { calls += "close" },
        )
        assertEquals(listOf("stop", "close"), calls)
        assertTrue(failure!!.message!!.contains("join"))
    }
}
