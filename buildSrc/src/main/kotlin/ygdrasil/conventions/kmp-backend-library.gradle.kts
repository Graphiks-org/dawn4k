@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(25)

    // The backend matrix mirrors the :dawn4k-native targets as far as the
    // published Graphiks WebGPU snapshots allow: the snapshots now cover
    // tvOS, so Android alone remains out. webgpu-api-android still lacks
    // the upstream borrowed-address ArrayBuffer.wrap(ByteBuffer), and a
    // CPU copy would fake getMappedRange. See docs/docs/architecture.md
    // for the matrix and its upstream bound.
    jvm()
    macosArm64()
    iosArm64()
    iosSimulatorArm64()
    iosX64()
    tvosArm64()
    tvosSimulatorArm64()
    linuxX64()

    applyDefaultHierarchyTemplate()
    compilerOptions { optIn.add("kotlin.ExperimentalUnsignedTypes") }
    sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
}

// iOS/tvOS tests require a device or a booted simulator; this convention
// compiles those targets but runs its host-native tests on macOS/Linux,
// mirroring :dawn4k-native. The targets stay, the test binaries are skipped.
tasks.matching {
    it.name.contains("Test") && (it.name.contains("Ios") || it.name.contains("Tvos"))
}.configureEach {
    enabled = false
}
