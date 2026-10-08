@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(25)

    jvm()
    android { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
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

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k"
            compileSdk = 37
            minSdk = 24
        }
    )
}

// iOS/tvOS tests require a device or a booted simulator; this project compiles those
// targets but runs its host-native tests on macOS/Linux. The targets stay, the test
// binaries are skipped.
tasks.matching {
    it.name.contains("Test") && (it.name.contains("Ios") || it.name.contains("Tvos"))
}.configureEach {
    enabled = false
}
