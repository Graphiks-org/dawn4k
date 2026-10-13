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

    private fun report(status: String) = AcidReport("jvm", "default", "v", "c", listOf(CaseResult("a", status)))
}
