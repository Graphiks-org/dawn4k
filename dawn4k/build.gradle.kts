import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    id("ygdrasil.conventions.kmp-library")
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k"
        },
    )
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
    // JVM and Android share their dispatcher and platform monitor actuals.
    val jvmSharedMain = sourceSets.create("jvmSharedMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jvmMain").dependsOn(jvmSharedMain)
    sourceSets.getByName("androidMain").dependsOn(jvmSharedMain)

    // expect/actual classes (the portable SynchronizedObject of the internal
    // package) opt into the promoted model: the beta warning is not a warning
    // we want in the build output, and this flag becomes the default anyway.
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }

    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k-native"))
        api("org.graphiks:webgpu-api:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation("org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.test)
    }

    // The published acid suite ships no iOS variants, so it cannot stay in
    // commonTest now that the backend declares the iOS targets: it moves to a
    // desktop-only intermediate test source set shared by the host test
    // targets. webgpu-descriptors publishes iOS variants and stays common.
    val desktopTest = sourceSets.create("desktopTest")
    desktopTest.dependsOn(sourceSets.getByName("commonTest"))
    desktopTest.dependencies {
        implementation("org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT")
    }
    listOf("jvmTest", "macosArm64Test", "linuxX64Test").forEach { testSetName ->
        sourceSets.getByName(testSetName).dependsOn(desktopTest)
    }

    // The :dawn4k-native klib references `webgpu.native` types, so every native
    // compilation and binary of this module must resolve the same platform
    // library and link the same library, mirroring its build exactly. The
    // desktop targets consume the shared flavor of the Dawn archives; the iOS
    // archives only publish a static flavor.
    val appleFrameworks = listOf(
        "Metal", "Foundation", "CoreGraphics", "QuartzCore", "IOKit", "IOSurface",
    )
    listOf(
        "macosArm64" to "shared",
        "linuxX64" to "shared",
        "iosArm64" to "static",
        "iosSimulatorArm64" to "static",
        "iosX64" to "static",
        "tvosArm64" to "static",
        "tvosSimulatorArm64" to "static",
    ).forEach { (nativeTarget, linkage) ->
        (targets.getByName(nativeTarget) as KotlinNativeTarget).apply {
            compilations.getByName("main").cinterops.create("dawn") {
                defFile(dawnNativeProject.file("src/nativeInterop/cinterop/dawn.def"))
                includeDirs(
                    dawnNativeProject.layout.buildDirectory.dir("native/$nativeTarget/$linkage/include"),
                )
            }
            binaries.all {
                val dawnLibDir = dawnNativeProject.layout.buildDirectory
                    .dir("native/$nativeTarget/$linkage/lib").get().asFile.absolutePath
                linkerOpts("-L$dawnLibDir", "-lwebgpu_dawn")
                when (nativeTarget) {
                    "macosArm64" -> {
                        // The test binary references Dawn symbols, so its runtime
                        // loader must find the shared library next to its build path.
                        linkerOpts("-rpath", dawnLibDir)
                        appleFrameworks.forEach { linkerOpts("-framework", it) }
                    }
                    "linuxX64" -> linkerOpts("-lpthread", "-ldl", "-lm")
                    // iOS/tvOS link static Dawn and the Apple frameworks.
                    else -> appleFrameworks.forEach { linkerOpts("-framework", it) }
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

tasks.withType<AbstractTestTask>().configureEach {
    testLogging.showStandardStreams = true
    listOf("DAWN_TEST_BACKEND", "DAWN_REQUIRE_ADAPTER", "VK_DRIVER_FILES", "VK_ICD_FILENAMES").forEach { variable ->
        inputs.property(variable, providers.environmentVariable(variable).orElse(""))
    }
    // Adapter/driver availability is external state, not a reproducible Gradle input.
    // In particular, a warning-only run must not become cached GPU coverage.
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU adapter availability must be checked at execution time") { true }
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    // simctl only forwards explicitly prefixed variables to its child process.
    listOf("DAWN_TEST_BACKEND", "DAWN_REQUIRE_ADAPTER").forEach { variable ->
        providers.environmentVariable(variable).orNull?.let { value ->
            environment("SIMCTL_CHILD_$variable", value)
        }
    }
}

// The Dawn Linux archives are built against a newer glibc/libstdc++ than the
// Kotlin/Native macOS sysroot, so the Linux test binary can only be linked on a
// Linux host. The Linux klib is still compiled everywhere.
if (!abiHost.startsWith("linux")) {
    tasks.matching {
        it.name == "linkDebugTestLinuxX64" ||
            it.name == "linuxX64Test"
    }.configureEach { enabled = false }
}
