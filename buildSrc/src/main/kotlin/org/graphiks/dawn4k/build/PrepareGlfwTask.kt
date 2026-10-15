package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.compress.archivers.zip.ZipFile
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.tasks.*
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

@DisableCachingByDefault(because = "Prepares a verified pinned source archive")
abstract class PrepareGlfwTask : DefaultTask() {
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val lockFile: RegularFileProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction fun prepare() {
        val lock = Json.parseToJsonElement(lockFile.get().asFile.readText()).jsonObject
        val source = lock.getValue("source").jsonObject
        val download = temporaryDir.resolve("glfw-source.zip")
        val connection = URI(source.getValue("url").jsonPrimitive.content).toURL().openConnection().apply {
            connectTimeout = 30_000; readTimeout = 120_000
        }
        connection.getInputStream().use { input -> download.outputStream().use(input::copyTo) }
        val destination = outputDirectory.get().asFile
        destination.parentFile.mkdirs()
        val staging = Files.createTempDirectory(destination.parentFile.toPath(), "glfw-verified-").toFile()
        try {
            prepareGlfwArchive(download, source.getValue("sha256").jsonPrimitive.content, staging)
            val root = source.getValue("root").jsonPrimitive.content
            check(safeArchivePath(staging.toPath(), "$root/include/GLFW/glfw3.h").toFile().isFile) { "GLFW archive has no public header" }
            destination.deleteRecursively()
            try { Files.move(staging.toPath(), destination.toPath(), StandardCopyOption.ATOMIC_MOVE) }
            catch (_: java.nio.file.AtomicMoveNotSupportedException) { Files.move(staging.toPath(), destination.toPath()) }
        } finally { staging.deleteRecursively() }
    }
}

internal fun prepareGlfwArchive(archive: File, expectedSha256: String, destination: File) {
    val digest = MessageDigest.getInstance("SHA-256")
    archive.inputStream().buffered().use { input ->
        val buffer = ByteArray(8192)
        while (true) { val read = input.read(buffer); if (read < 0) break; digest.update(buffer, 0, read) }
    }
    val actual = digest.digest().joinToString("") { "%02x".format(it.toInt() and 255) }
    require(actual == expectedSha256) { "GLFW source checksum mismatch: expected $expectedSha256, actual $actual" }
    destination.mkdirs()
    var total = 0L
    var entries = 0
    ZipFile.builder().setFile(archive).get().use { zip ->
        val files = zip.entries
        while (files.hasMoreElements()) {
            val entry = files.nextElement()
            require(++entries <= 20_000) { "GLFW archive has too many entries" }
            require(!entry.isUnixSymlink && (entry.unixMode and 0xF000) in setOf(0, 0x4000, 0x8000)) { "GLFW archive contains a special entry" }
            val target = safeArchivePath(destination.toPath(), entry.name).toFile()
            if (entry.isDirectory) target.mkdirs() else {
                check(!target.exists()) { "GLFW archive entry collision: ${entry.name}" }
                target.parentFile.mkdirs()
                zip.getInputStream(entry).use { input -> target.outputStream().use { output ->
                    val buffer = ByteArray(8192)
                    while (true) {
                        val read = input.read(buffer); if (read < 0) break
                        total += read
                        require(total <= 128L * 1024 * 1024) { "GLFW archive exceeds extraction limit" }
                        output.write(buffer, 0, read)
                    }
                } }
            }
        }
    }
}
