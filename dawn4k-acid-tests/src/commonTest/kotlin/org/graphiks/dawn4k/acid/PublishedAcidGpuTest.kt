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
        val json = Json { prettyPrint = true; encodeDefaults = true }
        fun report(results: List<CaseResult>, complete: Boolean) = AcidReport(
            targetName(), settings.backend?.name ?: "default", SuiteBuildIdentity.VERSION,
            SuiteBuildIdentity.COMMIT, results.toList(), complete,
        )
        val byId = cases.associateBy { it.id.id }
        var index = 0
        val results = collectCases(cases.map { it.id.id },
            run = { id ->
                println("ACID_CASE ${++index}/${cases.size}: $id")
                runDawnCase(byId.getValue(id), settings).also { result ->
                    println("ACID_RESULT ${result.id}: ${result.status}${result.diagnostic?.let { ": $it" } ?: ""}")
                }
            },
            publish = { results, complete -> writeReport(json.encodeToString(report(results, complete))) },
        )
        val finalReport = report(results, true)
        println("ACID_SUMMARY ${finalReport.summary}")
        finalReport.requireSuccessful(settings.requireAdapter)
    }
}
