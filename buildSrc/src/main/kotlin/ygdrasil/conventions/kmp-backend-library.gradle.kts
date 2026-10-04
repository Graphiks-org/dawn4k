@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(25)

    // The backend matrix mirrors the :dawn4k-native targets as far as the
    // published Graphiks WebGPU snapshots allow: webgpu-api and
    // webgpu-descriptors publish iOS variants but no tvOS variants, and
    // Android needs an upstream borrowed-address ArrayBuffer.wrap(ByteBuffer)
    // on webgpu-api-android (a CPU copy would fake getMappedRange). See
    // docs/docs/architecture.md for the bounded targets.
    jvm()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    linuxX64()

    applyDefaultHierarchyTemplate()
    compilerOptions { optIn.add("kotlin.ExperimentalUnsignedTypes") }
    sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
}

// iOS tests require a device or a booted simulator; this convention compiles
// those targets but runs its host-native tests on macOS/Linux, mirroring
// :dawn4k-native. The targets stay, the test binaries are skipped.
tasks.matching {
    it.name.contains("Test") && it.name.contains("Ios")
}.configureEach {
    enabled = false
}
