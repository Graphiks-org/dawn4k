package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.api.DefaultTask
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

private val prettyJson = Json { prettyPrint = true; prettyPrintIndent = "  " }

/** Emit the layout the generated JVM bindings bake in, in the ABI report schema. */
@DisableCachingByDefault(because = "Reads generated sources and writes a small report")
abstract class DumpGeneratedAbiTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedJvmSource: RegularFileProperty

    @get:Input
    abstract val hostName: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @TaskAction
    fun dump() {
        val dump = DawnAbi.parseGeneratedJvmAbi(generatedJvmSource.get().asFile.readText())
        val reportFile = report.get().asFile
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(prettyJson.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            dumpJson(hostName.get(), dump, emptyList()),
        ) + "\n")
    }
}

private fun dumpJson(host: String, dump: AbiDump, problems: List<String>) = buildJsonObject {
    put("host", host)
    put("pointerSize", dump.pointerSize)
    put("types", buildJsonObject {
        dump.types.toSortedMap().forEach { (name, type) ->
            put(name, buildJsonObject {
                put("size", type.size)
                put("align", type.align)
                put("offsets", buildJsonObject {
                    type.offsets.toSortedMap().forEach { (field, offset) ->
                        put(field, offset)
                    }
                })
            })
        }
    })
    put("problems", buildJsonArray { problems.forEach { add(kotlinx.serialization.json.JsonPrimitive(it)) } })
}
