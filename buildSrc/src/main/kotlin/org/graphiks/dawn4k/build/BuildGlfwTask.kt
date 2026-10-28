package org.graphiks.dawn4k.build

import kotlinx.serialization.json.*
import org.gradle.api.DefaultTask
import org.gradle.api.file.DirectoryProperty
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.*
import org.gradle.process.ExecOperations
import org.gradle.work.DisableCachingByDefault
import java.io.File
import java.security.MessageDigest
import java.util.Properties
import javax.inject.Inject

/** Produces a relocatable bundle on its matching OS, or verifies a bundle produced there. */
@DisableCachingByDefault(because = "Uses the matching host C compiler and SDK")
abstract class BuildGlfwTask @Inject constructor(private val exec: ExecOperations) : DefaultTask() {
    @get:Input abstract val targetName: Property<String>
    @get:InputFile @get:PathSensitive(PathSensitivity.RELATIVE) abstract val lockFile: RegularFileProperty
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) abstract val sourceDirectory: DirectoryProperty
    @get:InputDirectory @get:PathSensitive(PathSensitivity.RELATIVE) abstract val bridgeDirectory: DirectoryProperty
    @get:InputDirectory @get:Optional @get:PathSensitive(PathSensitivity.RELATIVE) abstract val nativeBundle: DirectoryProperty
    @get:OutputDirectory abstract val outputDirectory: DirectoryProperty

    @TaskAction fun build() {
        val target = targetName.get()
        val source = Json.parseToJsonElement(lockFile.get().asFile.readText()).jsonObject.getValue("source").jsonObject
        val checksum = source.getValue("sha256").jsonPrimitive.content
        val output = outputDirectory.get().asFile.apply { mkdirs() }
        if (nativeBundle.isPresent) {
            verifyGlfwBundle(nativeBundle.get().asFile, target, checksum)
            nativeBundle.get().asFile.copyRecursively(output, overwrite = true)
            return
        }
        val os = System.getProperty("os.name").lowercase()
        val arch = System.getProperty("os.arch").lowercase()
        val matching = when (target) {
            "macosArm64" -> "mac" in os && arch in setOf("aarch64", "arm64")
            "linuxX64" -> "linux" in os && arch in setOf("amd64", "x86_64")
            else -> false
        }
        check(matching) { "Build $target on its matching host, then supply -Pglfw.nativeBundle.$target=/verified/bundle" }
        val sourceRoot = sourceDirectory.get().asFile.resolve(source.getValue("root").jsonPrimitive.content)
        val projectDir = temporaryDir.resolve("project").apply { mkdirs() }
        val cmakeBuild = temporaryDir.resolve("cmake")
        fun path(file: File) = file.absolutePath.replace("\\", "/").replace("\"", "\\\"")
        projectDir.resolve("CMakeLists.txt").writeText("""
            cmake_minimum_required(VERSION 3.20)
            project(dawn4k_glfw LANGUAGES C)
            set(BUILD_SHARED_LIBS OFF CACHE BOOL "" FORCE)
            set(GLFW_BUILD_EXAMPLES OFF CACHE BOOL "" FORCE)
            set(GLFW_BUILD_TESTS OFF CACHE BOOL "" FORCE)
            set(GLFW_BUILD_DOCS OFF CACHE BOOL "" FORCE)
            set(GLFW_INSTALL OFF CACHE BOOL "" FORCE)
            set(GLFW_BUILD_X11 ON CACHE BOOL "" FORCE)
            set(GLFW_BUILD_WAYLAND ON CACHE BOOL "" FORCE)
            # Keep the archive linkable with Kotlin/Native's older glibc sysroot.
            set(HAVE_MEMFD_CREATE OFF CACHE BOOL "Use GLFW's portable SHM fallback" FORCE)
            add_subdirectory("${path(sourceRoot)}" glfw)
            add_library(dawn_glfw_handles STATIC "${path(bridgeDirectory.get().asFile.resolve("glfw_native_handles.c"))}")
            target_include_directories(dawn_glfw_handles PRIVATE "${path(sourceRoot.resolve("include"))}" "${path(bridgeDirectory.get().asFile)}")
            set_target_properties(glfw dawn_glfw_handles PROPERTIES ARCHIVE_OUTPUT_DIRECTORY "${path(output)}")
        """.trimIndent())
        val configure = mutableListOf("cmake", "-S", projectDir.absolutePath, "-B", cmakeBuild.absolutePath,
            "-DCMAKE_BUILD_TYPE=Release", "-DCMAKE_POSITION_INDEPENDENT_CODE=ON")
        if (target == "macosArm64") configure += listOf("-DCMAKE_OSX_ARCHITECTURES=arm64", "-DCMAKE_OSX_DEPLOYMENT_TARGET=11.0")
        exec.exec { commandLine(configure) }.assertNormalExitValue()
        exec.exec { commandLine("cmake", "--build", cmakeBuild.absolutePath, "--parallel", "4") }.assertNormalExitValue()
        output.resolve("bundle.properties").writeText(buildString {
            appendLine("target=$target"); appendLine("sourceSha256=$checksum")
            for (name in glfwLibraries) appendLine("$name=${glfwSha256(output.resolve(name))}")
        })
        verifyGlfwBundle(output, target, checksum)
    }
}

internal val glfwLibraries = listOf("libglfw3.a", "libdawn_glfw_handles.a")
internal fun glfwSha256(file: File): String = MessageDigest.getInstance("SHA-256").digest(file.readBytes())
    .joinToString("") { "%02x".format(it.toInt() and 255) }
internal fun verifyGlfwBundle(bundle: File, target: String, sourceChecksum: String) {
    val manifest = Properties().apply { bundle.resolve("bundle.properties").inputStream().use(::load) }
    require(manifest.getProperty("target") == target) { "GLFW bundle target mismatch" }
    require(manifest.getProperty("sourceSha256") == sourceChecksum) { "GLFW bundle source mismatch" }
    for (name in glfwLibraries) require(manifest.getProperty(name) == glfwSha256(bundle.resolve(name))) { "GLFW bundle checksum mismatch: $name" }
}
