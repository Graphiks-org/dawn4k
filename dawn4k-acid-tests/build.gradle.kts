import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

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
    targets.withType<KotlinNativeTarget>().configureEach {
        val dawn = project(":dawn4k-native")
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(dawn.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(dawn.layout.buildDirectory.dir("native/$targetName/shared/include"))
        }
        binaries.all {
            val lib = dawn.layout.buildDirectory.dir("native/$targetName/shared/lib").get().asFile.absolutePath
            linkerOpts("-L$lib", "-lwebgpu_dawn", "-rpath", lib)
            if (targetName == "macosArm64") {
                listOf("Metal", "Foundation", "CoreGraphics", "QuartzCore", "IOKit", "IOSurface").forEach {
                    linkerOpts("-framework", it)
                }
            } else {
                linkerOpts("-lpthread", "-ldl", "-lm")
            }
        }
    }
}

tasks.matching { it.name.startsWith("cinteropDawn") || it.name.startsWith("linkDebugTest") }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}
tasks.withType<Test>().configureEach { jvmArgs("--enable-native-access=ALL-UNNAMED") }

if (!System.getProperty("os.name").lowercase().contains("linux")) {
    tasks.matching { it.name == "linkDebugTestLinuxX64" || it.name == "linuxX64Test" }.configureEach {
        enabled = false
    }
}
