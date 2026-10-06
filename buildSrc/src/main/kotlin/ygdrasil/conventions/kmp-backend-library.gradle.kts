@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.Action

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(25)

    // The backend matrix mirrors the :dawn4k-native Kotlin targets: JVM,
    // Android, desktop and Apple targets, all covered by the published
    // Graphiks WebGPU snapshots. Android landed last, after webgpu-api
    // gained the borrowed-address ArrayBuffer.wrap(address, size) that a
    // GPU-mapped range requires. See docs/docs/architecture.md for the
    // matrix and its bounds.
    jvm()
    android {}
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    tvosArm64()
    tvosSimulatorArm64()
    linuxX64()

    applyDefaultHierarchyTemplate()
    compilerOptions { optIn.add("kotlin.ExperimentalUnsignedTypes") }

    sourceSets {
        // The JVM and Android runtimes share java.util.concurrent and the
        // platform monitor: their dispatcher and lock actuals live here.
        val jvmSharedMain = create("jvmSharedMain") { dependsOn(commonMain.get()) }
        jvmMain.get().dependsOn(jvmSharedMain)
        androidMain.get().dependsOn(jvmSharedMain)

        commonTest.dependencies { implementation(kotlin("test")) }
    }
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k"
            compileSdk = 37
            minSdk = 24
        },
    )
}

// iOS/tvOS tests require a device or a booted simulator; this convention
// compiles those targets but runs its host-native tests on macOS/Linux,
// mirroring :dawn4k-native. The targets stay, the test binaries are skipped.
// Android is likewise compiled only: like :dawn4k-native, no host-test
// compilation is declared for it.
tasks.matching {
    it.name.contains("Test") && (it.name.contains("Ios") || it.name.contains("Tvos"))
}.configureEach {
    enabled = false
}
