package org.graphiks.dawn4k.acid

import java.io.File

internal actual fun environment(name: String): String? = System.getenv(name)
internal actual fun targetName(): String = "jvm"
internal actual fun writeReportFile(path: String, json: String) {
    File(path).writeText(json, Charsets.UTF_8)
}
