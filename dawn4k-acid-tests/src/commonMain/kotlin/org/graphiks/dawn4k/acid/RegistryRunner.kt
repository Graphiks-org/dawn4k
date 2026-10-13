package org.graphiks.dawn4k.acid

internal suspend fun collectCases(
    ids: List<String>,
    run: suspend (String) -> CaseResult,
    publish: (List<CaseResult>, Boolean) -> Unit,
): List<CaseResult> {
    validateIds(ids)
    val results = mutableListOf<CaseResult>()
    publish(emptyList(), false)
    for (id in ids) {
        val result = run(id)
        check(result.id == id) { "Runner returned the wrong case ID: ${result.id} instead of $id" }
        results += result
        publish(results.toList(), false)
        check(result.recoverable) { "Unrecoverable teardown for $id; acid report remains incomplete" }
    }
    check(results.map { it.id } == ids) { "Incomplete registry accounting" }
    publish(results.toList(), true)
    return results.toList()
}
