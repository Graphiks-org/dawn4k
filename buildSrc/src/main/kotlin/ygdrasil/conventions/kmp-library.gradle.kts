@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

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

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    // Standalone simctl has no service access: Metal returns nil and Dawn can crash.
    // scripts/boot-apple-simulators.py boots devices and exports their dynamic selection.
    standalone.set(false)
    val simulatorVariable = when {
        name.startsWith("ios") -> "DAWN_IOS_SIMULATOR"
        name.startsWith("tvos") -> "DAWN_TVOS_SIMULATOR"
        else -> null
    }
    simulatorVariable?.let { variable ->
        providers.environmentVariable(variable).orNull?.let { device.set(it) }
    }
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        Action<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension> {
            compileSdk = 37
            minSdk = 24
        }
    )
}
