import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.testing.AbstractTestTask
import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
    id("ygdrasil.conventions.dawn-native-linkage")
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
        namespace = "org.graphiks.dawn4k.toolkit.surface"
    })
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k-toolkit-core"))
        api(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.atomicfu)
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.webgpu.descriptors)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.withType<AbstractTestTask>().configureEach {
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU availability must be checked at execution time") { true }
}
