@file:Suppress("UnstableApiUsage")
package ygdrasil.conventions

plugins {
    id("org.jetbrains.kotlin.multiplatform")
}

kotlin {
    jvmToolchain(25)

    jvm()
    macosArm64()
    linuxX64()

    applyDefaultHierarchyTemplate()
    compilerOptions { optIn.add("kotlin.ExperimentalUnsignedTypes") }
    sourceSets.commonTest.dependencies { implementation(kotlin("test")) }
}
