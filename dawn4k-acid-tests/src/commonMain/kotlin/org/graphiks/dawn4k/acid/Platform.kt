package org.graphiks.dawn4k.acid

internal expect fun environment(name: String): String?
internal expect fun targetName(): String
internal expect fun writeReportFile(path: String, json: String)

internal fun writeReport(json: String) {
    val path = requireNotNull(environment("DAWN_ACID_REPORT_PATH")) { "Missing DAWN_ACID_REPORT_PATH" }
    writeReportFile(path, json)
}
