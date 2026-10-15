package org.graphiks.dawn4k.native

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*

class WindowsRuntimeDependenciesTest {
    private val names = listOf("vcruntime140.dll", "vcruntime140_1.dll", "msvcp140.dll", "msvcp140_atomic_wait.dll")
    private val resources = names.associateWith { "verified-$it".toByteArray() }
    private val checksums = resources.mapValues { (_, bytes) -> sha256(bytes) }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun directory(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("dawn-windows-test-")
        try { block(root) }
        finally { Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) } }
    }

    @Test fun verifiesAndLoadsAllDependenciesBeforeDawnInImportOrder() = directory { root ->
        val loaded = mutableListOf<String>()
        WindowsRuntimeDependencies.prepare(root, checksums, resources::getValue) { path ->
            assertEquals(root.toAbsolutePath(), path.parent)
            assertContentEquals(resources.getValue(path.fileName.toString()), Files.readAllBytes(path))
            loaded += path.fileName.toString()
        }
        assertEquals(names, loaded)
    }

    @Test fun modifiedCacheIsReplacedByVerifiedResourceBeforeNativeLoading() = directory { root ->
        WindowsRuntimeDependencies.prepare(root, checksums, resources::getValue) {}
        Files.writeString(root.resolve("msvcp140.dll"), "modified")
        WindowsRuntimeDependencies.prepare(root, checksums, resources::getValue) { path ->
            assertContentEquals(resources.getValue(path.fileName.toString()), Files.readAllBytes(path))
        }
    }

    @Test fun badFourthResourcePreventsEveryNativeLoad() = directory { root ->
        var calls = 0
        assertFailsWith<IllegalStateException> {
            WindowsRuntimeDependencies.prepare(root, checksums, { name ->
                if (name == "msvcp140_atomic_wait.dll") byteArrayOf(0) else resources.getValue(name)
            }) { calls++ }
        }
        assertEquals(0, calls)
    }

    @Test fun missingDependencyIdentityPreventsEveryNativeLoad() = directory { root ->
        var calls = 0
        assertFailsWith<IllegalArgumentException> {
            WindowsRuntimeDependencies.prepare(root, checksums - "vcruntime140_1.dll", resources::getValue) { calls++ }
        }
        assertEquals(0, calls)
    }
}
