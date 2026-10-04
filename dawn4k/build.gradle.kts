import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.KotlinCompilation
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.NativeBuildType
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    id("ygdrasil.conventions.kmp-desktop-library")
}

val abiHost: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        os.contains("mac") -> "macos"
        os.contains("linux") -> "linux"
        else -> os.replace(Regex("[^a-z0-9]+"), "-")
    }
    val archName = when (arch) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> arch
    }
    "$osName-$archName"
}

/** Provides the Dawn headers, shared libraries and cinterop def consumed here. */
val dawnNativeProject = project(":dawn4k-native")

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k-native"))
        api("org.graphiks:webgpu-api:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation("org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT")
        implementation("org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.test)
    }

    // The :dawn4k-native klib references `webgpu.native` types, so every native
    // compilation and binary of this module must resolve the same platform
    // library and link the same shared library, mirroring its build exactly.
    listOf("macosArm64", "linuxX64").forEach { nativeTarget ->
        (targets.getByName(nativeTarget) as KotlinNativeTarget).apply {
            compilations.getByName("main").cinterops.create("dawn") {
                defFile(dawnNativeProject.file("src/nativeInterop/cinterop/dawn.def"))
                includeDirs(
                    dawnNativeProject.layout.buildDirectory.dir("native/$nativeTarget/shared/include"),
                )
            }
            binaries.all {
                val dawnLibDir = dawnNativeProject.layout.buildDirectory
                    .dir("native/$nativeTarget/shared/lib").get().asFile.absolutePath
                linkerOpts("-L$dawnLibDir", "-lwebgpu_dawn")
                // The test binary references Dawn symbols, so its runtime
                // loader must find the shared library next to its build path.
                linkerOpts("-rpath", dawnLibDir)
                if (nativeTarget == "macosArm64") {
                    linkerOpts(
                        "-framework", "Metal",
                        "-framework", "Foundation",
                        "-framework", "CoreGraphics",
                        "-framework", "QuartzCore",
                        "-framework", "IOKit",
                        "-framework", "IOSurface",
                    )
                } else {
                    linkerOpts("-lpthread", "-ldl", "-lm")
                }
            }
        }
    }
}

// The Dawn headers and shared libraries are staged by :dawn4k-native:prepareDawn.
tasks.matching { it.name.startsWith("cinteropDawn") }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}
tasks.matching { it.name == "linkDebugTestMacosArm64" || it.name == "linkDebugTestLinuxX64" }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// GPU tests live in commonTest but only the gpuTest* tasks may execute them:
// the standard test tasks exclude the *GpuTest classes, and the GPU tasks run
// exclusively those classes and fail when no adapter is available.

val jvmTestCompilation = kotlin.run {
    val jvmTarget = targets.getByName("jvm") as KotlinJvmTarget
    jvmTarget.compilations.getByName(KotlinCompilation.TEST_COMPILATION_NAME)
}
tasks.register<Test>("gpuTestJvm") {
    group = "verification"
    description = "Runs the *GpuTest classes on the JVM target; requires a real GPU adapter."
    testClassesDirs = jvmTestCompilation.output.classesDirs
    classpath = jvmTestCompilation.runtimeDependencyFiles
    filter.setIncludePatterns("*GpuTest")
}

tasks.withType<Test>().matching { it.name == "jvmTest" }.configureEach {
    filter.setExcludePatterns("*GpuTest")
}

// The Kotlin/Native GPU tasks reuse the standard test binaries with a class
// filter: same executable, restricted to *GpuTest.
kotlin.targets.withType<KotlinNativeTarget>().configureEach {
    val nativeTarget = this
    val gpuTaskName = "gpuTest" + nativeTarget.targetName.replaceFirstChar { it.uppercase() }
    val testBinary = nativeTarget.binaries.getTest(NativeBuildType.DEBUG)
    tasks.register(gpuTaskName, KotlinNativeHostTest::class.java) {
        group = "verification"
        description = "Runs the *GpuTest classes for ${nativeTarget.targetName}; requires a real GPU adapter."
        targetName = nativeTarget.targetName
        workingDir = nativeTarget.project.projectDir.absolutePath
        executable(testBinary.linkTaskProvider.map { it.outputFile.get() })
        filter.setIncludePatterns("*GpuTest")
        // The conventions KGP applies to its own test tasks; the hand-rolled
        // registration must carry them itself.
        reports.html.outputLocation.convention(
            project.layout.buildDirectory.dir("reports/tests/$name"),
        )
        reports.junitXml.outputLocation.convention(
            project.layout.buildDirectory.dir("test-results/$name"),
        )
        binaryResultsDirectory.convention(
            project.layout.buildDirectory.dir("test-results/$name/binary"),
        )
    }
}

tasks.withType<KotlinNativeHostTest>().configureEach {
    if (!name.startsWith("gpuTest")) {
        filter.setExcludePatterns("*GpuTest")
    }
}

// The Dawn Linux archives are built against a newer glibc/libstdc++ than the
// Kotlin/Native macOS sysroot, so the Linux test binary can only be linked on a
// Linux host. The Linux klib is still compiled everywhere.
if (!abiHost.startsWith("linux")) {
    tasks.matching {
        it.name == "linkDebugTestLinuxX64" ||
            it.name == "linuxX64Test" ||
            it.name == "gpuTestLinuxX64"
    }.configureEach { enabled = false }
}
