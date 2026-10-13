plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("ygdrasil.conventions.dawn-jvm-runtime")
}

kotlin {
    jvmToolchain(25)
    jvm()
    macosArm64()
    linuxX64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":dawn4k"))
            implementation(libs.suite.acid.tests)
            implementation(libs.webgpu.descriptors)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        jvmTest.dependencies { implementation(kotlin("test-junit")) }
    }
}
