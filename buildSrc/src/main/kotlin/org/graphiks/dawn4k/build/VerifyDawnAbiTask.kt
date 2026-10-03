package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.nio.file.Files

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

/**
 * Compile and run the C oracle against the original Dawn header, compare it with the
 * layouts baked into the generated JVM bindings, and fail on any difference.
 */
@DisableCachingByDefault(because = "Compiles and runs a host C program")
abstract class VerifyDawnAbiTask : DefaultTask() {

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val oracleSource: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val generatedJvmSource: RegularFileProperty

    @get:Input
    abstract val hostName: Property<String>

    @get:Input
    abstract val compiler: Property<String>

    @get:OutputFile
    abstract val report: RegularFileProperty

    @get:OutputDirectory
    abstract val workDir: DirectoryProperty

    @TaskAction
    fun verify() {
        val work = workDir.get().asFile.toPath()
        Files.createDirectories(work)
        val executable = work.resolve("dawn_abi")
        Files.deleteIfExists(executable)

        val include = includeDir.get().asFile.absolutePath
        val compile = ProcessBuilder(
            compiler.get(), "-std=c11", "-I$include", oracleSource.get().asFile.absolutePath,
            "-o", executable.toString(),
        ).redirectErrorStream(true).start()
        val compileOutput = compile.inputStream.bufferedReader().readText()
        if (compile.waitFor() != 0) {
            throw GradleException("failed to compile the dawn ABI oracle:\n$compileOutput")
        }

        val run = ProcessBuilder(executable.toString()).redirectErrorStream(true).start()
        val oracleOutput = run.inputStream.bufferedReader().readText()
        if (run.waitFor() != 0) {
            throw GradleException("dawn ABI oracle exited with a failure:\n$oracleOutput")
        }

        val oracle = DawnAbi.parseOracleOutput(oracleOutput)
        val generated = DawnAbi.parseGeneratedJvmAbi(generatedJvmSource.get().asFile.readText())
        val problems = DawnAbi.compare(oracle, generated)

        val reportFile = report.get().asFile
        reportFile.parentFile?.mkdirs()
        reportFile.writeText(prettyJson.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(),
            dumpJson(hostName.get(), generated, problems),
        ) + "\n")

        if (problems.isNotEmpty()) {
            throw GradleException(
                "dawn ABI mismatch between the C header and the generated bindings:\n" +
                    problems.joinToString("\n") { "  - $it" },
            )
        }
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
