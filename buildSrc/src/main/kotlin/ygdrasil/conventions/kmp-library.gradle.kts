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

    // A module opts out of a target by listing it in the `kmp.skipTargets` extra
    // property (a comma-separated list of target names), set on the project before
    // its plugins block runs. The opt-out has to happen here, before the targets
    // exist: the Kotlin plugin registers a target's tasks when it is created, so
    // removing the target afterwards leaves those tasks behind.
    val skippedTargets: Set<String> = (project.extensions.extraProperties.properties["kmp.skipTargets"] as? String)
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?.toSet()
        ?: emptySet()
    // A misspelt entry would otherwise be ignored silently and the target declared
    // anyway, which is the failure this opt-out exists to prevent.
    val knownTargets = setOf(
        "macosArm64",
        "iosArm64",
        "iosSimulatorArm64",
        "iosX64",
        "tvosArm64",
        "tvosSimulatorArm64",
        "linuxX64",
    )
    require(skippedTargets.all { it in knownTargets }) {
        "Unknown kmp.skipTargets entries: ${skippedTargets - knownTargets}. Known targets: $knownTargets"
    }
    fun enabled(targetName: String) = targetName !in skippedTargets

    jvm()
    android { compilerOptions { jvmTarget.set(JvmTarget.JVM_17) } }
    if (enabled("macosArm64")) macosArm64()
    if (enabled("iosArm64")) iosArm64()
    if (enabled("iosSimulatorArm64")) iosSimulatorArm64()
    if (enabled("iosX64")) iosX64()
    if (enabled("tvosArm64")) tvosArm64()
    if (enabled("tvosSimulatorArm64")) tvosSimulatorArm64()
    if (enabled("linuxX64") && linuxX64TargetEnabled(providers)) linuxX64()

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
