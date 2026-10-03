package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
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
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest

/**
 * Run the pinned kextract distribution over the prepared Dawn header and record the
 * generation provenance.
 *
 * Ordinary compilation never depends on this task: it regenerates into
 * `build/regenerated/src` so a human can review the diff before promoting the output
 * into the versioned `generated/src`. No local absolute path is written to
 * [generationManifest].
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

    @get:OutputFile
    abstract val generationManifest: RegularFileProperty

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

        writeManifest(output)
    }

    private fun writeManifest(output: Path) {
        val lock = DawnArtifacts.parseLock(dawnLockFile().toFile().readText())
        val kextractCommit = kextractVersion.get().asFile.readText().trim()

        val files = Files.walk(output).use { stream ->
            stream.filter(Files::isRegularFile)
                .map { output.relativize(it).toString().replace('\\', '/') }
                .sorted()
                .toList()
        }

        val jvmBundles = buildJsonObject {
            for (platform in listOf("darwin-aarch64", "linux-x86-64")) {
                val platformRoot = jvmNativeResourcesDir.get().asFile.toPath().resolve(platform)
                if (!Files.isDirectory(platformRoot)) continue
                put(platform, buildJsonObject {
                    Files.list(platformRoot).use { stream ->
                        stream.filter(Files::isRegularFile).sorted().forEach { file ->
                            put(file.fileName.toString(), sha256(file))
                        }
                    }
                })
            }
        }

        val manifest = buildJsonObject {
            put("dawn", buildJsonObject {
                put("release", lock.release)
                put("tag", lock.tag)
                put("revision", lock.revision)
            })
            put("kextract", buildJsonObject {
                put("commit", kextractCommit)
            })
            put("targetPackage", targetPackage.get())
            put("library", libraryName.get())
            put("callbackBindingsSha256", sha256(callbackBindings.get().asFile.toPath()))
            put("callbacks", callbackInventory())
            put("jvmBundles", jvmBundles)
            put("inventory", buildJsonArray {
                files.forEach { path ->
                    add(buildJsonObject {
                        put("path", path)
                        put("sha256", sha256(output.resolve(path)))
                    })
                }
            })
        }

        val manifestFile = generationManifest.get().asFile
        manifestFile.parentFile?.mkdirs()
        manifestFile.writeText(Json { prettyPrint = true; prettyPrintIndent = "  " }.encodeToString(
            kotlinx.serialization.json.JsonObject.serializer(), manifest,
        ) + "\n")
    }

    /**
     * Inventory of every Dawn function-pointer typedef. `contract` marks the ones registered
     * as safe-callback helpers through `callback-bindings.yml`; the others (for example the
     * Dawn cache callbacks) are emitted as raw functional interfaces without a registered owner.
     */
    private fun callbackInventory() = buildJsonArray {
        val headerText = headerFile.get().asFile.readText()
        val bindingsText = callbackBindings.get().asFile.readText()
        Regex("""typedef\b[^;]*?\(\*(WGPU\w+)\)""", RegexOption.DOT_MATCHES_ALL)
            .findAll(headerText)
            .map { it.groupValues[1] }
            .filterNot { it.startsWith("WGPUProc") }
            .distinct()
            .sorted()
            .forEach { name ->
                add(buildJsonObject {
                    put("typedef", name)
                    put("contract", bindingsText.contains("typedef:$name"))
                })
            }
    }

    private fun dawnLockFile(): Path {
        val root = project.rootProject.layout.projectDirectory.asFile.toPath()
        return root.resolve("bindings/dawn.lock.json")
    }

    private fun macosSdkRoot(): String? {
        if (!System.getProperty("os.name").lowercase().contains("mac")) return null
        return runCatching {
            val process = ProcessBuilder("xcrun", "--sdk", "macosx", "--show-sdk-path").start()
            val path = process.inputStream.bufferedReader().readText().trim()
            if (process.waitFor() == 0) path.takeIf { it.isNotBlank() } else null
        }.getOrNull()
    }

    private fun sha256(file: Path): String {
        val digest = MessageDigest.getInstance("SHA-256")
        Files.newInputStream(file).use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }
}
