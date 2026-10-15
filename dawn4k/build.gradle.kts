import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeSimulatorTest

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
    id("ygdrasil.conventions.dawn-native-linkage")
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k"
        },
    )
}

kotlin {
    // JVM and Android share their platform actuals.
    val jvmSharedMain = sourceSets.create("jvmSharedMain") {
        dependsOn(sourceSets.getByName("commonMain"))
    }
    sourceSets.getByName("jvmMain").dependsOn(jvmSharedMain)
    sourceSets.getByName("androidMain").dependsOn(jvmSharedMain)

    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k-native"))
        api(libs.webgpu.api)
        implementation(libs.kotlinx.coroutines.core)
        implementation(libs.kotlinx.atomicfu)
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation(libs.webgpu.descriptors)
        implementation(libs.kotlinx.coroutines.test)
    }


}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<AbstractTestTask>().configureEach {
    testLogging.showStandardStreams = true
    listOf("DAWN_TEST_BACKEND", "DAWN_REQUIRE_ADAPTER", "VK_DRIVER_FILES", "VK_ICD_FILENAMES").forEach { variable ->
        inputs.property(variable, providers.environmentVariable(variable).orElse(""))
    }
    // Adapter/driver availability is external state, not a reproducible Gradle input.
    // In particular, a warning-only run must not become cached GPU coverage.
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU adapter availability must be checked at execution time") { true }
}

tasks.withType<KotlinNativeSimulatorTest>().configureEach {
    // simctl only forwards explicitly prefixed variables to its child process.
    listOf("DAWN_TEST_BACKEND", "DAWN_REQUIRE_ADAPTER").forEach { variable ->
        providers.environmentVariable(variable).orNull?.let { value ->
            environment("SIMCTL_CHILD_$variable", value)
        }
    }
}
