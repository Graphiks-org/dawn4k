package org.graphiks.dawn4k.acid

import java.io.File
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlin.test.Test
import kotlin.test.assertEquals

class PlatformReportTest {
    @Test
    fun writesUtf8ReportAtTheConfiguredPath() {
        val report = AcidReport("jvm", "default", "v", "c", listOf(CaseResult("a", "failed", "λ\n\\\"")))
        // Separate from the GPU report: this CPU test must never overwrite execution evidence.
        val path = requireNotNull(environment("DAWN_ACID_IO_TEST_PATH"))
        writeReportFile(path, Json.encodeToString(report))
        assertEquals(report, Json.decodeFromString<AcidReport>(File(path).readText(Charsets.UTF_8)))
    }
}
