package org.graphiks.dawn4k.build

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import java.nio.file.Path

/** One pinned Dawn archive: a (target, linkage) pair with its expected archive checksum. */
data class DawnArchive(
    val target: String,
    val linkage: String,
    val name: String,
    val sha256: String,
)

/** Immutable identity of the Dawn release consumed by this build. */
data class DawnLock(
    val release: String,
    val tag: String,
    val revision: String,
    val archives: List<DawnArchive>,
) {
    /**
     * Select every archive for the requested targets. A target may ship a single linkage
     * (for example Android, which is only consumed as a shared library); desktop targets
     * ship both. An unknown target is rejected by name.
     */
    fun selectArchives(targets: Set<String>): List<DawnArchive> {
        require(targets.isNotEmpty()) { "No dawn.targets requested" }
        val known = archives.map { it.target }.toSet()
        val unknown = targets - known
        require(unknown.isEmpty()) { "Unknown dawn target(s): ${unknown.sorted().joinToString(", ")}" }
        return targets.sorted().flatMap { target ->
            val targetArchives = archives.filter { it.target == target }
            require(targetArchives.isNotEmpty()) { "Target $target has no archive" }
            targetArchives.sortedBy { it.linkage }
        }
    }
}

object DawnArtifacts {
    const val RELEASE_BASE_URL = "https://github.com/Graphiks-org/dawn-packer/releases/download"

    fun parseLock(json: String): DawnLock {
        val root = Json.parseToJsonElement(json).jsonObject
        val archives = root.getValue("archives").jsonArray.map { element ->
            val entry = element.jsonObject
            DawnArchive(
                target = entry.getValue("target").jsonPrimitive.content,
                linkage = entry.getValue("linkage").jsonPrimitive.content,
                name = entry.getValue("name").jsonPrimitive.content,
                sha256 = entry.getValue("sha256").jsonPrimitive.content,
            )
        }
        return DawnLock(
            release = root.getValue("release").jsonPrimitive.content,
            tag = root.getValue("tag").jsonPrimitive.content,
            revision = root.getValue("revision").jsonPrimitive.content,
            archives = archives,
        )
    }

    fun archiveUrl(lock: DawnLock, archive: DawnArchive): String =
        "$RELEASE_BASE_URL/${lock.release}/${archive.name}"

    fun checksumsUrl(lock: DawnLock): String = "$RELEASE_BASE_URL/${lock.release}/SHA256SUMS"
}

/**
 * Resolve a tar entry name under [root], refusing absolute paths and `..` traversal.
 * Raises [IllegalArgumentException] for either.
 */
fun safeArchivePath(root: Path, entry: String): Path {
    require(entry.isNotBlank()) { "Empty archive entry" }
    require(!Path.of(entry).isAbsolute) { "Absolute archive entry: $entry" }
    val normalizedRoot = root.normalize()
    val destination = normalizedRoot.resolve(entry).normalize()
    require(destination.startsWith(normalizedRoot)) { "Archive traversal: $entry" }
    return destination
}
