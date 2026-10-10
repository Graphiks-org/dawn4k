package org.graphiks.dawn4k.demo

/** Stable private qualification format shared by both mobile launchers. */
internal fun DemoEvidenceSnapshot.toJson(): String {
    fun quote(value: String?): String = value?.let { text ->
        buildString {
            append('"')
            for (char in text) when (char) {
                '"' -> append("\\\"")
                '\\' -> append("\\\\")
                '\n' -> append("\\n")
                '\r' -> append("\\r")
                '\t' -> append("\\t")
                else -> if (char.code < 32) append("\\u" + char.code.toString(16).padStart(4, '0')) else append(char)
            }
            append('"')
        }
    } ?: "null"
    fun bounds(area: LogicalViewport?): String = area?.let {
        "{\"x\":${it.x},\"y\":${it.y},\"width\":${it.width},\"height\":${it.height}}"
    } ?: "null"
    return "{" + listOf(
        "\"sessionId\":$sessionId", "\"phase\":${quote(phase.name)}",
        "\"backend\":${quote(backend)}", "\"adapter\":${quote(adapter)}",
        "\"count\":$count", "\"paused\":$paused", "\"resetGeneration\":$resetGeneration",
        "\"lifecycleActive\":$lifecycleActive", "\"firstError\":${quote(firstError)}",
        "\"frameCount\":$frameCount", "\"surfaceGeneration\":$surfaceGeneration",
        "\"deviceCreations\":$deviceCreations", "\"surfaceCreations\":$surfaceCreations",
        "\"physicalExtent\":{\"width\":${physicalExtent.width},\"height\":${physicalExtent.height}}",
        "\"viewport\":${bounds(viewport)}",
        "\"buttons\":{${buttons.entries.joinToString(",") { quote(it.key) + ":" + bounds(it.value) }}}",
        "\"simulationFingerprint\":${quote(simulationFingerprint)}",
    ).joinToString(",") + "}"
}
