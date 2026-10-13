package org.graphiks.dawn4k.acid

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AcidReportTest {
    @Test
    fun rejectsEmptyCatalogue() {
        assertFailsWith<IllegalArgumentException> { validateIds(emptyList()) }
    }

    @Test
    fun rejectsDuplicateAndBlankIds() {
        assertFailsWith<IllegalArgumentException> { validateIds(listOf("a", "a")) }
        assertFailsWith<IllegalArgumentException> { validateIds(listOf(" ")) }
        validateIds(listOf("a", "b"))
    }

    @Test
    fun diagnosticRoundTrips() {
        val value = CaseResult("buffer.test", "failed", "λ \"quoted\"\n\\path")
        assertEquals(value, Json.decodeFromString<CaseResult>(Json.encodeToString(value)))
    }

    @Test
    fun failedCasesFailTheInvocation() {
        assertFailsWith<IllegalStateException> { report("failed").requireSuccessful(false) }
    }

    @Test
    fun strictModeRejectsNotRunButNotUnsupported() {
        assertFailsWith<IllegalStateException> { report("not-run").requireSuccessful(true) }
        report("unsupported").requireSuccessful(true)
        report("not-run").requireSuccessful(false)
        report("passed").requireSuccessful(true)
    }

    @Test
    fun rejectsUnknownStatus() {
        assertFailsWith<IllegalStateException> { report("unknown").requireSuccessful(false) }
    }

    @Test
    fun incompleteReportCannotPass() {
        assertFailsWith<IllegalStateException> { report("passed").copy(complete = false).requireSuccessful(false) }
    }

    @Test
    fun summaryDoesNotCountUnsupportedAsPassed() {
        val report = AcidReport("jvm", "default", "v", "c", listOf(
            CaseResult("a", "passed"), CaseResult("b", "unsupported"),
            CaseResult("c", "failed"), CaseResult("d", "not-run"),
        ))
        assertEquals(mapOf("passed" to 1, "failed" to 1, "unsupported" to 1, "not-run" to 1), report.summary)
        assertEquals(4, report.summary.values.sum())
        val decoded = Json.decodeFromString<AcidReport>(Json.encodeToString(report))
        assertEquals(report.summary, decoded.summary)
    }

    private fun report(status: String) = AcidReport("jvm", "default", "v", "c", listOf(CaseResult("a", status)))
}
