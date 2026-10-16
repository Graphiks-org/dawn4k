package org.graphiks.dawn4k.native

import java.nio.channels.FileChannel
import java.nio.file.AtomicMoveNotSupportedException
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption.*
import java.nio.file.StandardOpenOption.*
import java.security.MessageDigest
import java.util.Properties

public actual fun prepareDawnRuntime() {
    if (System.getProperty("os.name").startsWith("Windows", ignoreCase = true)) {
        WindowsRuntimeDependencies.loadOnce()
    }
}

/** Explicit PE import order; System.load uses plain LoadLibrary on JDK 25. */
internal object WindowsRuntimeDependencies {
    private val names = listOf("vcruntime140.dll", "vcruntime140_1.dll", "msvcp140.dll", "msvcp140_atomic_wait.dll")
    private var loaded = false

    @Synchronized fun loadOnce() {
        if (loaded) return
        val properties = Properties().apply {
            checkNotNull(WindowsRuntimeDependencies::class.java.getResourceAsStream("/dawn4k/windows-runtime.properties")) {
                "Dawn Windows runtime dependency manifest is missing"
            }.use(::load)
        }
        val checksums = properties.stringPropertyNames().associateWith(properties::getProperty)
        val identity = sha256(names.joinToString("\n") { checksums.getValue(it) }.toByteArray())
        val cache = Path.of(System.getProperty("java.io.tmpdir"), "dawn4k-msvc", identity)
        prepare(cache, checksums, { name ->
            checkNotNull(WindowsRuntimeDependencies::class.java.getResourceAsStream("/win32-x86-64/$name")) {
                "Dawn Windows runtime dependency is missing: $name"
            }.use { it.readBytes() }
        }) { System.load(it.toString()) }
        loaded = true
    }

    fun prepare(
        directory: Path,
        checksums: Map<String, String>,
        readResource: (String) -> ByteArray,
        loadLibrary: (Path) -> Unit,
    ) {
        require(checksums.keys == names.toSet()) { "Dawn Windows runtime dependency identities are incomplete" }
        require(checksums.values.all { it.matches(Regex("[a-f0-9]{64}")) }) { "Invalid Dawn Windows runtime checksum" }
        // Validate every input before any native side effect, including on a cache hit.
        val resources = names.associateWith { name ->
            readResource(name).also { check(sha256(it) == checksums.getValue(name)) { "Dawn Windows resource checksum mismatch: $name" } }
        }
        val cache = directory.toAbsolutePath().normalize()
        Files.createDirectories(cache)
        FileChannel.open(cache.resolve(".lock"), CREATE, WRITE).use { channel ->
            channel.lock().use {
                val paths = names.map { name ->
                    val target = cache.resolve(name)
                    if (!Files.isRegularFile(target) || sha256(Files.readAllBytes(target)) != checksums.getValue(name)) {
                        val temporary = Files.createTempFile(cache, name, ".tmp")
                        try {
                            Files.write(temporary, resources.getValue(name))
                            try { Files.move(temporary, target, ATOMIC_MOVE, REPLACE_EXISTING) }
                            catch (_: AtomicMoveNotSupportedException) { Files.move(temporary, target, REPLACE_EXISTING) }
                        } finally { Files.deleteIfExists(temporary) }
                    }
                    target
                }
                paths.forEach(loadLibrary)
            }
        }
    }

    private fun sha256(bytes: ByteArray): String = MessageDigest.getInstance("SHA-256").digest(bytes)
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
