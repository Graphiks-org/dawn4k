import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test
import org.graphiks.dawn4k.build.BuildGlfwTask
import org.graphiks.dawn4k.build.PrepareGlfwTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
    id("ygdrasil.conventions.kmp-dokka")
    alias(libs.plugins.dokka.mkdocs)
    id("ygdrasil.conventions.dawn-native-linkage")
}

// GLFW is built from the pinned source and linked into the native targets, exactly as the
// former dawn4k-toolkit-glfw module did before the desktop window layer moved here.
val prepareGlfw by tasks.registering(PrepareGlfwTask::class) {
    lockFile.set(rootProject.layout.projectDirectory.file("bindings/glfw.lock.json"))
    outputDirectory.set(layout.buildDirectory.dir("glfw/source"))
}
val prepareGlfwBridge by tasks.registering(Sync::class) {
    from("src/nativeMain/c")
    from("src/nativeInterop/cinterop/glfw_native_handles.h")
    into(layout.buildDirectory.dir("glfw/bridge"))
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k.webgpu.toolkit"
            minSdk = 28
        },
    )
}

kotlin {
    android {
        withHostTestBuilder { sourceSetTreeName = "test" }.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets.getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
    sourceSets.getByName("androidDeviceTest").dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.androidx.test.runner)
    }
    
    val jvmSharedMain = sourceSets.create("jvmSharedMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jvmMain").dependsOn(jvmSharedMain)
    sourceSets.getByName("androidMain").dependsOn(jvmSharedMain)

    // GLFW is desktop-only, so its native backend and tests live in desktop source sets rather
    // than in nativeMain, which the mobile targets also see.
    val desktopNativeMain = sourceSets.create("desktopNativeMain") {
        dependsOn(sourceSets.getByName("nativeMain"))
    }
    val desktopNativeTest = sourceSets.create("desktopNativeTest") {
        dependsOn(sourceSets.getByName("nativeTest"))
    }
    // Mobile targets answer the GLFW expects with an explicit unsupported failure.
    val mobileNativeMain = sourceSets.create("mobileNativeMain") {
        dependsOn(sourceSets.getByName("nativeMain"))
    }
    listOf("iosMain", "tvosMain").forEach { sourceSets.findByName(it)?.dependsOn(mobileNativeMain) }
    listOf("macosArm64Main", "linuxX64Main").forEach { sourceSets.findByName(it)?.dependsOn(desktopNativeMain) }
    listOf("macosArm64Test", "linuxX64Test").forEach { sourceSets.findByName(it)?.dependsOn(desktopNativeTest) }

    // The GPU test sources carry the expect declarations their platform actuals answer.
    val gpuTest = sourceSets.create("gpuTest") { dependsOn(sourceSets.getByName("commonTest")) }
    listOf("jvmTest", "nativeTest", "androidDeviceTest").forEach {
        sourceSets.findByName(it)?.dependsOn(gpuTest)
    }

    // The mobile views live in appleMobileMain, the parent of iosMain and tvosMain; appleMain
    // also covers macOS, where DawnMetalView does not exist.
    val appleMobileMain = sourceSets.create("appleMobileMain") {
        dependsOn(sourceSets.getByName("appleMain"))
    }
    sourceSets.getByName("iosMain").dependsOn(appleMobileMain)
    sourceSets.getByName("tvosMain").dependsOn(appleMobileMain)

    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k"))
        // The toolkit artifacts carry the published webgpu-api klib; the repository's own
        // :dawn4k already brings it, so keep the toolkit graph out of the native link.
        implementation(libs.webgpu.toolkit.core)
        implementation(libs.webgpu.toolkit.surface)
        // Offscreen render textures are built from the descriptor types.
        implementation(libs.webgpu.descriptors)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.atomicfu)
    }
    
    // Add GLFW dependency only to JVM source sets (GLFW is JVM-only)
    sourceSets.getByName("jvmMain").dependencies {
        // LWJGL natives for GLFW
        implementation(libs.lwjgl.core)
        implementation(libs.lwjgl.glfw)
        implementation(libs.kffi.objc.jvm)
        implementation(libs.kffi.win32.jvm)
        implementation(libs.kffi.x11.jvm)
        val classifiers = providers.gradleProperty("lwjgl.natives").map {
            it.split(',').map(String::trim).filter(String::isNotEmpty)
        }.getOrElse(listOf("natives-macos-arm64", "natives-windows", "natives-linux", "natives-linux-arm64"))
        classifiers.forEach { classifier ->
            runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:$classifier")
            runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:$classifier")
        }
    }
    // The contract kit opens real Dawn contexts, so it stays on the JVM target: linking it
    // into the native targets would embed the published webgpu-api klib a second time.
    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.webgpu.descriptors)
    }
    sourceSets.getByName("jvmTest").dependencies {
        implementation(libs.webgpu.toolkit.tck)
    }

    // Each native target links GLFW built from the pinned source; the cinterop needs the
    // produced library on its path, so the bundle task runs before interop processing.
    // GLFW is a desktop window library: only the desktop native targets build and link it.
    val glfwTargets = setOf("macosArm64", "macosX64", "linuxX64")
    targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name
        if (targetName !in glfwTargets) return@configureEach
        val buildGlfw = tasks.register<BuildGlfwTask>("buildGlfw${targetName.replaceFirstChar(Char::uppercaseChar)}") {
            dependsOn(prepareGlfw, prepareGlfwBridge)
            this.targetName.set(targetName)
            lockFile.set(rootProject.layout.projectDirectory.file("bindings/glfw.lock.json"))
            sourceDirectory.set(prepareGlfw.flatMap { it.outputDirectory })
            bridgeDirectory.set(layout.buildDirectory.dir("glfw/bridge"))
            providers.gradleProperty("glfw.nativeBundle.$targetName").orNull?.let { nativeBundle.set(file(it)) }
            outputDirectory.set(layout.buildDirectory.dir("glfw/$targetName"))
        }
        compilations.getByName("main").cinterops.create("glfw") {
            definitionFile.set(file("src/nativeInterop/cinterop/glfw.def"))
            includeDirs("src/nativeInterop/cinterop", layout.buildDirectory.dir("glfw/source/glfw-3.5.1/include"))
            extraOpts("-libraryPath", layout.buildDirectory.dir("glfw/$targetName").get().asFile.absolutePath)
        }.also { interop ->
            tasks.named(interop.interopProcessingTaskName) {
                dependsOn(buildGlfw)
                inputs.dir(buildGlfw.flatMap { it.outputDirectory })
                    .withPropertyName("glfwNativeBundle").withPathSensitivity(PathSensitivity.RELATIVE)
            }
        }
    }
}

// The module ships no native test sources: its suites live in commonTest and jvmTest.
// Linking a native test binary would embed the published webgpu-api klib next to the
// repository's own api, which ld.lld rejects as duplicate symbols.
tasks.matching { it.name.startsWith("linkDebugTest") || it.name.startsWith("linkReleaseTest") }
    .configureEach { enabled = false }

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // GLFW owns the process main thread on macOS, so the desktop tests must start there.
    if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
    inputs.property("dawn.desktopTests", providers.environmentVariable("DAWN_DESKTOP_TESTS").orElse("0"))
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GLFW window availability is checked at execution time") { true }
}
tasks.withType<KotlinNativeHostTest>().configureEach {
    for (name in listOf("DAWN_DESKTOP_TESTS", "DAWN_WAYLAND_TESTS")) {
        environment(name, providers.environmentVariable(name).getOrElse("0"))
    }
    doFirst { logger.lifecycle("Native GLFW acceptance flag: ${environment["DAWN_DESKTOP_TESTS"]}") }
}
tasks.withType<AbstractTestTask>().configureEach {
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU availability must be checked at execution time") { true }
}
