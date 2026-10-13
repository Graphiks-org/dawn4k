package org.graphiks.dawn4k.acid

import kotlinx.serialization.Serializable

@Serializable
internal data class CaseResult(
    val id: String,
    val status: String,
    val diagnostic: String? = null,
    val missingFeatures: List<String> = emptyList(),
    val adapterDescription: String? = null,
)

@Serializable
internal data class AcidReport(
    val target: String,
    val backend: String,
    val suiteVersion: String,
    val suiteCommit: String,
    val cases: List<CaseResult>,
)

internal fun validateIds(ids: List<String>) {
    require(ids.isNotEmpty()) { "Acid catalogue is empty" }
    require(ids.all { it.isNotBlank() }) { "Blank acid case ID" }
    require(ids.distinct().size == ids.size) { "Duplicate acid case IDs" }
}

internal fun AcidReport.requireSuccessful(requireAdapter: Boolean) {
    validateIds(cases.map { it.id })
    check(cases.all { it.status in setOf("passed", "failed", "unsupported", "not-run") }) {
        "Unknown acid case status"
    }
    val failures = cases.filter { it.status == "failed" || (requireAdapter && it.status == "not-run") }
    check(failures.isEmpty()) {
        failures.joinToString("\n") { "${it.id}: ${it.status}: ${it.diagnostic}" }
    }
}
