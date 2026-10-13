package org.graphiks.dawn4k.acid

import kotlinx.coroutines.runBlocking
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.graphiks.webgpu.suite.acid.SuiteBuildIdentity
import org.graphiks.webgpu.suite.acid.foundationCases
import kotlin.test.Test

class PublishedAcidGpuTest {
    @Test
    fun publishedFoundationRegistry() = runBlocking {
        val cases = foundationCases()
        validateIds(cases.map { it.id.id })
        val settings = AcidSettings.parse(environment("DAWN_TEST_BACKEND"), environment("DAWN_REQUIRE_ADAPTER"))
        val results = mutableListOf<CaseResult>()
        val json = Json { prettyPrint = true; encodeDefaults = true }
        fun report(complete: Boolean) = AcidReport(
            targetName(), settings.backend?.name ?: "default", SuiteBuildIdentity.VERSION,
            SuiteBuildIdentity.COMMIT, results.toList(), complete,
        )
        writeReport(json.encodeToString(report(false)))
        for (case in cases) {
            println("ACID_CASE ${results.size + 1}/${cases.size}: ${case.id.id}")
            val result = runDawnCase(case, settings)
            check(result.id == case.id.id) { "Runner returned the wrong case ID" }
            results += result
            writeReport(json.encodeToString(report(false)))
            println("ACID_RESULT ${result.id}: ${result.status}${result.diagnostic?.let { ": $it" } ?: ""}")
        }
        check(results.map { it.id } == cases.map { it.id.id }) { "Incomplete registry accounting" }
        val finalReport = report(true)
        writeReport(json.encodeToString(finalReport))
        println("ACID_SUMMARY ${finalReport.summary}")
        finalReport.requireSuccessful(settings.requireAdapter)
    }
}
