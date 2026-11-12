import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
    id("ygdrasil.conventions.kmp-dokka")
    alias(libs.plugins.dokka.mkdocs)
    id("ygdrasil.conventions.dawn-native-linkage")
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k.webgpu.toolkit"
            minSdk = 28
        },
    )
}

kotlin {
    android {
        withHostTestBuilder { sourceSetTreeName = "test" }.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets.getByName("androidHostTest").dependencies { implementation(kotlin("test-junit")) }
    sourceSets.getByName("androidDeviceTest").dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.androidx.test.runner)
    }
    
    val jvmSharedMain = sourceSets.create("jvmSharedMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jvmMain").dependsOn(jvmSharedMain)
    sourceSets.getByName("androidMain").dependsOn(jvmSharedMain)

    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k"))
        api(libs.webgpu.toolkit.core)
        api(libs.webgpu.toolkit.surface)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.atomicfu)
    }
    
    // Add GLFW dependency only to JVM source sets (GLFW is JVM-only)
    sourceSets.getByName("jvmMain").dependencies {
        api(project(":dawn4k-toolkit-glfw"))
        // LWJGL natives for GLFW
        runtimeOnly(libs.lwjgl.core)
        runtimeOnly(libs.lwjgl.glfw)
        runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:natives-macos-arm64")
        runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:natives-macos-arm64")
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.webgpu.descriptors)
        implementation(libs.webgpu.toolkit.tck)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.withType<AbstractTestTask>().configureEach {
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU availability must be checked at execution time") { true }
}
