package org.graphiks.dawn4k.acid

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class RegistryRunnerTest {
    @Test
    fun continuesAfterRecoverableFailureAndPublishesCompleteAccounting() = runBlocking {
        val snapshots = mutableListOf<Pair<List<String>, Boolean>>()
        val results = collectCases(listOf("bad", "good"),
            run = { CaseResult(it, if (it == "bad") "failed" else "passed") },
            publish = { cases, complete -> snapshots += cases.map { it.id } to complete },
        )
        assertEquals(listOf("bad", "good"), results.map { it.id })
        assertEquals(listOf(emptyList<String>() to false, listOf("bad") to false,
            listOf("bad", "good") to false, listOf("bad", "good") to true), snapshots)
    }

    @Test
    fun unrecoverableCleanupStopsWithAnIncompleteReport() = runBlocking {
        val executed = mutableListOf<String>()
        val snapshots = mutableListOf<Pair<List<String>, Boolean>>()
        assertFailsWith<IllegalStateException> {
            collectCases(listOf("unclosed", "never"),
                run = { executed += it; CaseResult(it, "failed", recoverable = false) },
                publish = { cases, complete -> snapshots += cases.map { it.id } to complete },
            )
        }
        assertEquals(listOf("unclosed"), executed)
        assertEquals(listOf(emptyList<String>() to false, listOf("unclosed") to false), snapshots)
    }
}
