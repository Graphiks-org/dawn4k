@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension

plugins {
    id("org.jetbrains.kotlin.multiplatform")
    id("com.android.kotlin.multiplatform.library")
}

kotlin {
    jvmToolchain(25)

    jvm()
    android {}
    macosArm64()
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
