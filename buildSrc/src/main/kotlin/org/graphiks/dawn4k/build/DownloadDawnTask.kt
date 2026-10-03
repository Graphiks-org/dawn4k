package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.gzip.GzipCompressorInputStream
import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault
import java.io.BufferedInputStream
import java.io.File
import java.net.URI
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.security.MessageDigest

/**
 * Download, verify and atomically promote the pinned Dawn archives.
 *
 * The task refuses to extract anything before the archive checksum matches both the
 * lock file and the release `SHA256SUMS`, rejects absolute/traversing entry names and
 * non file/directory entries, caps extraction at [MAX_ARCHIVE_BYTES], then verifies the
 * shipped `manifest.json` (Dawn tag/revision, target, linkage and every artifact hash)
 * before promoting the temporary extraction into `build/native/<target>/<linkage>/`.
 */
@DisableCachingByDefault(because = "Downloads and extracts large pinned native archives; outputs are cheap to reproduce")
abstract class DownloadDawnTask : DefaultTask() {

    @get:Input
    abstract val targets: ListProperty<String>

    @get:InputFile
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val lockFile: RegularFileProperty

    @get:OutputDirectory
    abstract val outputDir: DirectoryProperty

    @TaskAction
    fun prepare() {
        val lock = DawnArtifacts.parseLock(lockFile.get().asFile.readText())
        val selected = lock.selectArchives(targets.get().toSet())

        val remoteChecksums = parseChecksums(downloadText(DawnArtifacts.checksumsUrl(lock)))
        for (archive in selected) {
            val expected = remoteChecksums[archive.name]
                ?: throw GradleException("Remote SHA256SUMS is missing ${archive.name}")
            if (expected != archive.sha256) {
                throw GradleException(
                    "Checksum drift for ${archive.name}: lock=${archive.sha256} remote=$expected",
                )
            }
        }

        val outputRoot = outputDir.get().asFile.toPath()
        val scratchRoot = scratchRoot()
        Files.createDirectories(outputRoot)
        Files.createDirectories(scratchRoot)

        for (archive in selected) {
            prepareArchive(lock, archive, scratchRoot, outputRoot)
        }
    }

    private fun prepareArchive(lock: DawnLock, archive: DawnArchive, scratchRoot: Path, outputRoot: Path) {
        val downloadDir = scratchRoot.resolve("downloads")
        val extractRoot = scratchRoot.resolve("extract")
        Files.createDirectories(downloadDir)

        val tarball = downloadDir.resolve(archive.name)
        downloadTo(DawnArtifacts.archiveUrl(lock, archive), tarball)

        val localSha = sha256(tarball)
        if (localSha != archive.sha256) {
            Files.deleteIfExists(tarball)
            throw GradleException(
                "Downloaded ${archive.name} has sha256=$localSha, expected ${archive.sha256}",
            )
        }

        val tempDir = extractRoot.resolve("${archive.target}-${archive.linkage}")
        deleteRecursively(tempDir)
        Files.createDirectories(tempDir)
        try {
            extract(tarball, tempDir)
            verifyManifest(tempDir, lock, archive)
            promote(tempDir, outputRoot.resolve(archive.target).resolve(archive.linkage))
        } finally {
            deleteRecursively(tempDir)
        }
    }

    private fun extract(tarball: Path, destination: Path) {
        var totalBytes = 0L
        Files.newInputStream(tarball).use { raw ->
            BufferedInputStream(raw).use { buffered ->
                GzipCompressorInputStream(buffered).use { gzip ->
                    TarArchiveInputStream(gzip).use { tar ->
                        while (true) {
                            val entry = tar.nextEntry ?: break
                            val target = safeArchivePath(destination, entry.name)
                            when {
                                entry.isDirectory -> Files.createDirectories(target)
                                entry.isFile -> {
                                    if (Files.exists(target)) {
                                        throw GradleException("Archive entry collision: ${entry.name}")
                                    }
                                    target.parent?.let { Files.createDirectories(it) }
                                    Files.newOutputStream(target).use { output ->
                                        val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                                        while (true) {
                                            val read = tar.read(buffer)
                                            if (read < 0) break
                                            totalBytes += read
                                            if (totalBytes > MAX_ARCHIVE_BYTES) {
                                                throw GradleException(
                                                    "Archive ${tarball.fileName} exceeds " +
                                                        "$MAX_ARCHIVE_BYTES extracted bytes",
                                                )
                                            }
                                            output.write(buffer, 0, read)
                                        }
                                    }
                                }
                                else -> throw GradleException(
                                    "Refusing ${entry.name}: only regular files and directories are allowed",
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun verifyManifest(directory: Path, lock: DawnLock, archive: DawnArchive) {
        val manifestFile = directory.resolve("manifest.json")
        if (!Files.isRegularFile(manifestFile)) {
            throw GradleException("Archive ${archive.name} is missing manifest.json")
        }
        val manifest = Json.parseToJsonElement(manifestFile.toFile().readText()).jsonObject
        val dawn = manifest.getValue("dawn").jsonObject
        val tag = dawn.getValue("tag").jsonPrimitive.content
        val revision = dawn.getValue("revision").jsonPrimitive.content
        val target = manifest.getValue("target").jsonObject.getValue("kotlinTarget").jsonPrimitive.content
        val linkage = manifest.getValue("linkage").jsonPrimitive.content

        if (tag != lock.tag) throw GradleException("${archive.name}: manifest tag=$tag, expected ${lock.tag}")
        if (revision != lock.revision) {
            throw GradleException("${archive.name}: manifest revision=$revision, expected ${lock.revision}")
        }
        if (target != archive.target) {
            throw GradleException("${archive.name}: manifest target=$target, expected ${archive.target}")
        }
        if (linkage != archive.linkage) {
            throw GradleException("${archive.name}: manifest linkage=$linkage, expected ${archive.linkage}")
        }

        for (element in manifest.getValue("artifacts").jsonArray) {
            val artifact = element.jsonObject
            val path = artifact.getValue("path").jsonPrimitive.content
            val expectedSha = artifact.getValue("sha256").jsonPrimitive.content
            val file = safeArchivePath(directory, path)
            if (!Files.isRegularFile(file)) {
                throw GradleException("${archive.name}: manifest artifact missing on disk: $path")
            }
            val actual = sha256(file)
            if (actual != expectedSha) {
                throw GradleException("${archive.name}: $path sha256=$actual, manifest=$expectedSha")
            }
        }
    }

    private fun promote(source: Path, destination: Path) {
        deleteRecursively(destination)
        destination.parent?.let { Files.createDirectories(it) }
        try {
            Files.move(source, destination, StandardCopyOption.ATOMIC_MOVE)
        } catch (_: Exception) {
            copyRecursively(source, destination)
            deleteRecursively(source)
        }
    }

    private fun scratchRoot(): Path =
        project.rootProject.layout.projectDirectory.dir(".dawn").asFile.toPath()

    private fun downloadText(url: String): String {
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        return connection.getInputStream().bufferedReader().use { it.readText() }
    }

    private fun downloadTo(url: String, destination: Path) {
        destination.parent?.let { Files.createDirectories(it) }
        val partial = destination.resolveSibling("${destination.fileName}.partial")
        Files.deleteIfExists(partial)
        val connection = URI(url).toURL().openConnection()
        connection.connectTimeout = CONNECT_TIMEOUT_MS
        connection.readTimeout = READ_TIMEOUT_MS
        connection.getInputStream().use { input ->
            Files.newOutputStream(partial).use { output -> input.copyTo(output) }
        }
        Files.move(partial, destination, StandardCopyOption.REPLACE_EXISTING)
    }

    private fun parseChecksums(text: String): Map<String, String> =
        text.lineSequence().mapNotNull { line ->
            val parts = line.trim().split(Regex("\\s+"))
            if (parts.size == 2) parts[1] to parts[0] else null
        }.toMap()

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

    private fun copyRecursively(source: Path, destination: Path) {
        Files.walk(source).use { stream ->
            stream.forEach { path ->
                val relative = source.relativize(path)
                val target = destination.resolve(relative.toString())
                if (Files.isDirectory(path)) {
                    Files.createDirectories(target)
                } else {
                    target.parent?.let { Files.createDirectories(it) }
                    Files.copy(path, target, StandardCopyOption.REPLACE_EXISTING)
                }
            }
        }
    }

    private fun deleteRecursively(path: Path) {
        if (!Files.exists(path)) return
        Files.walk(path).use { stream ->
            stream.sorted(Comparator.reverseOrder()).forEach { Files.deleteIfExists(it) }
        }
    }

    private companion object {
        const val MAX_ARCHIVE_BYTES = 512L * 1024 * 1024
        const val CONNECT_TIMEOUT_MS = 30_000
        const val READ_TIMEOUT_MS = 300_000
    }
}
