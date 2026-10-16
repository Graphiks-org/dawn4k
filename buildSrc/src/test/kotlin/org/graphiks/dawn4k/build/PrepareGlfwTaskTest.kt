package org.graphiks.dawn4k.build

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File
import java.security.MessageDigest
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream
import kotlin.test.*

class PrepareGlfwTaskTest {
    @get:Rule val directory = TemporaryFolder()

    @Test fun downloadRejectsChecksumMismatch() {
        val archive = archive()
        val destination = File(directory.root, "output")
        assertFailsWith<IllegalArgumentException> { prepareGlfwArchive(archive, "0".repeat(64), destination) }
        assertFalse(destination.exists())
    }

    @Test fun verifiedArchiveIsPreparedThroughGradleTestKit() {
        val archive = archive()
        File(directory.root, "dawn4k-native").mkdirs()
        File(directory.root, "settings.gradle.kts").writeText("rootProject.name = \"glfw-fixture\"\ninclude(\":dawn4k-native\")\n")
        File(directory.root, "glfw.lock.json").writeText("""{"version":"3.5.1","source":{"url":"${archive.toURI()}","sha256":"${sha256(archive)}","root":"glfw-3.5.1"}}""")
        File(directory.root, "build.gradle.kts").writeText("""
            import org.graphiks.dawn4k.build.PrepareGlfwTask
            plugins { id("ygdrasil.conventions.dawn-native-linkage") }
            kotlin { jvm() }
            tasks.register<PrepareGlfwTask>("prepareFixture") {
                lockFile.set(layout.projectDirectory.file("glfw.lock.json"))
                outputDirectory.set(layout.buildDirectory.dir("verified"))
            }
        """.trimIndent())
        val result = GradleRunner.create()
            .withProjectDir(directory.root)
            .withTestKitDir(File(System.getProperty("test.testkit.dir")))
            .withGradleInstallation(File(System.getProperty("test.gradle.home")))
            .withPluginClasspath()
            .withArguments("--console=plain", "prepareFixture")
            .build()
        assertEquals(TaskOutcome.SUCCESS, result.task(":prepareFixture")?.outcome)
        assertTrue(File(directory.root, "build/verified/glfw-3.5.1/include/GLFW/glfw3.h").isFile)
    }

    @Test fun crossCinteropDoesNotNeedHostWindowHeaders() {
        val include = File(directory.root, "include/GLFW").apply { mkdirs() }
        File(include, "glfw3.h").writeText("typedef struct GLFWwindow GLFWwindow;\n")
        val header = File(System.getProperty("test.repo.root"), "dawn4k-toolkit-glfw/src/nativeInterop/cinterop/glfw_native_handles.h")
        val compiler = if (System.getProperty("os.name").lowercase().contains("windows")) "clang.exe" else "clang"
        val command = ProcessBuilder(compiler, "-fsyntax-only", "-x", "c", "-I${include.parentFile}", header.absolutePath)
            .redirectErrorStream(true).start()
        val output = command.inputStream.bufferedReader().readText()
        assertEquals(0, command.waitFor(), output)
    }

    @Test fun nativeBundleRejectsWrongTargetAndModifiedLibraries() {
        val bundle = File(directory.root, "bundle").apply { mkdirs() }
        for (name in glfwLibraries) File(bundle, name).writeText("fixture archive $name")
        val source = "a".repeat(64)
        File(bundle, "bundle.properties").writeText(buildString {
            appendLine("target=linuxX64"); appendLine("sourceSha256=$source")
            for (name in glfwLibraries) appendLine("$name=${sha256(File(bundle, name))}")
        })
        verifyGlfwBundle(bundle, "linuxX64", source)
        assertFailsWith<IllegalArgumentException> { verifyGlfwBundle(bundle, "macosArm64", source) }
        assertFailsWith<IllegalArgumentException> { verifyGlfwBundle(bundle, "linuxX64", "b".repeat(64)) }
        File(bundle, "libglfw3.a").appendText("corruption")
        assertFailsWith<IllegalArgumentException> { verifyGlfwBundle(bundle, "linuxX64", source) }
    }

    private fun archive(): File = File(directory.root, "glfw.zip").also { file ->
        ZipOutputStream(file.outputStream()).use { archive ->
            archive.putNextEntry(ZipEntry("glfw-3.5.1/include/GLFW/glfw3.h"))
            archive.write("typedef struct GLFWwindow GLFWwindow;\n".toByteArray())
            archive.closeEntry()
        }
    }
    private fun sha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
        .joinToString("") { "%02x".format(it.toInt() and 255) }
}
