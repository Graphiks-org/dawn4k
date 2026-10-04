package org.graphiks.dawn4k.build

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputDirectory
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path

/**
 * Run the pinned kextract distribution over the prepared Dawn header.
 *
 * Ordinary compilation never depends on this task: it regenerates into
 * `build/regenerated/src` so a human can review the diff before promoting the output
 * into the versioned `generated/src`.
 */
@DisableCachingByDefault(because = "Invokes an external native generator; output is reviewed then promoted by hand")
abstract class GenerateDawnBindingsTask : DefaultTask() {

    /** The built kextract image root (contains `bin/kextract`, `lib/`, `runtime/`). */
    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kextractHome: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val headerFile: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val includeDir: DirectoryProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val callbackBindings: RegularFileProperty

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val kextractVersion: RegularFileProperty

    @get:InputDirectory
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val jvmNativeResourcesDir: DirectoryProperty

    @get:Input
    abstract val targetPackage: Property<String>

    @get:Input
    abstract val libraryName: Property<String>

    @get:Input
    abstract val extraArgs: ListProperty<String>

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun generate() {
        val launcher = kextractHome.get().asFile.toPath().resolve("bin/kextract")
        if (!Files.isExecutable(launcher)) {
            throw GradleException(
                "kextract launcher not found at $launcher. " +
                    "Run scripts/generate-dawn-bindings.sh to build the pinned tool first.",
            )
        }

        val output = outputDir.get().asFile.toPath()
        deleteRecursively(output)
        Files.createDirectories(output)

        val args = buildList {
            add("--multiplatform")
            add("--target-package")
            add(targetPackage.get())
            add("--library")
            add(libraryName.get())
            add("--jvm-native-library")
            add(libraryName.get())
            add("--jvm-native-resources")
            add(jvmNativeResourcesDir.get().asFile.absolutePath)
            add("--callback-bindings")
            add(callbackBindings.get().asFile.absolutePath)
            add("--allow-non-void-callbacks")
            add("--allow-64-bit-scalars")
            add("--restrict-to-header-paths")
            add("--init-method")
            add("--output")
            add(output.toFile().absolutePath)
            add("-D")
            add("WGPU_SKIP_PROCS")
            add("--include-path")
            add(includeDir.get().asFile.absolutePath)
            add(headerFile.get().asFile.absolutePath)
            addAll(extraArgs.get())
        }

        logger.lifecycle("kextract: ${launcher.fileName} ${args.joinToString(" ")}")
        val builder = ProcessBuilder(listOf(launcher.toFile().absolutePath) + args)
            .redirectErrorStream(true)
        macosSdkRoot()?.let { builder.environment()["SDKROOT"] = it }
        val process = builder.start()
        val lines = process.inputStream.bufferedReader().readLines()
        val exit = process.waitFor()
        lines.forEach { logger.lifecycle("[kextract] $it") }
        if (exit != 0) {
            throw GradleException("kextract exited with $exit")
        }
    }

    private fun macosSdkRoot(): String? {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return null
        return runCatching {
            val process = ProcessBuilder("xcrun", "--sdk", "macosx", "--show-sdk-path").start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) path.takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
