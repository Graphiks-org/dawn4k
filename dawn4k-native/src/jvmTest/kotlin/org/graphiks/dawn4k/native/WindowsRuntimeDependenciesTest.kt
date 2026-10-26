package org.graphiks.dawn4k.native

import java.nio.file.Files
import java.nio.file.Path
import java.security.MessageDigest
import kotlin.test.*

class WindowsRuntimeDependenciesTest {
    // The build-generated manifest is the single source for the dependency set and its
    // PE import order; the expectations here derive from it instead of a copied list.
    private val manifest = WindowsRuntimeDependencies.readManifest(
        checkNotNull(WindowsRuntimeDependencies::class.java.getResourceAsStream("/dawn4k/windows-runtime.properties")) {
            "Dawn Windows runtime dependency manifest is missing"
        }
    )
    private val names = manifest.keys.toList()
    private val resources = names.associateWith { "verified-$it".toByteArray() }
    private val checksums = resources.mapValues { (_, bytes) -> sha256(bytes) }

    private fun sha256(bytes: ByteArray) = MessageDigest.getInstance("SHA-256").digest(bytes).joinToString("") { "%02x".format(it.toInt() and 255) }

    private fun directory(block: (Path) -> Unit) {
        val root = Files.createTempDirectory("dawn-windows-test-")
        try { block(root) }
        finally { Files.walk(root).use { paths -> paths.sorted(Comparator.reverseOrder()).forEach(Files::delete) } }
    }

    @Test fun manifestEnumeratesWellFormedRuntimeDependencies() {
        assertTrue(names.isNotEmpty())
        assertTrue(names.all { it.endsWith(".dll") && it.matches(Regex("[A-Za-z0-9_.-]+")) })
        assertTrue(manifest.values.all { it.matches(Regex("[a-f0-9]{64}")) })
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
        Files.writeString(root.resolve(names.first()), "modified")
        WindowsRuntimeDependencies.prepare(root, checksums, resources::getValue) { path ->
            assertContentEquals(resources.getValue(path.fileName.toString()), Files.readAllBytes(path))
        }
    }

    @Test fun badLastResourcePreventsEveryNativeLoad() = directory { root ->
        var calls = 0
        assertFailsWith<IllegalStateException> {
            WindowsRuntimeDependencies.prepare(root, checksums, { name ->
                if (name == names.last()) byteArrayOf(0) else resources.getValue(name)
            }) { calls++ }
        }
        assertEquals(0, calls)
    }

    @Test fun manifestMissingANameEnumeratesExactlyTheRemainingStagedSet() = directory { root ->
        val missing = names.last()
        val partial = checksums - missing
        val partialResources = partial.keys.associateWith(resources::getValue)
        val loaded = mutableListOf<String>()
        WindowsRuntimeDependencies.prepare(root, partial, partialResources::getValue) { path ->
            loaded += path.fileName.toString()
        }
        assertEquals(partial.keys.toList(), loaded)
        assertTrue(missing !in loaded)
    }

    @Test fun manifestKeyWithoutStagedResourceSurfacesMismatchBeforeAnyNativeLoad() = directory { root ->
        val unstaged = "missing-from-staged.dll"
        val extra = checksums + (unstaged to sha256("unavailable".toByteArray()))
        var calls = 0
        val error = assertFailsWith<IllegalStateException> {
            WindowsRuntimeDependencies.prepare(root, extra, { name ->
                checkNotNull(resources[name]) { "Dawn Windows runtime dependency is missing: $name" }
            }) { calls++ }
        }
        assertEquals("Dawn Windows runtime dependency is missing: $unstaged", error.message)
        assertEquals(0, calls)
    }
}
