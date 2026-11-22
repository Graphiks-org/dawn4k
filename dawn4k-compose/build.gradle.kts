// Targets are declared by ygdrasil.conventions.kmp-library; the skip list lives
// in the root build.gradle.kts.
import ygdrasil.conventions.applyDawnLinkage

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.kmp-publish")
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

// The Android library needs its own namespace; the convention only sets the
// compile and min SDKs. The device tests are instrumentation tests: they need a
// real Surface, the AndroidComposeView and the Dawn Android library.
extensions.configure<com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k.compose"
            withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
                instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
            }
        },
    )
}

// Compose 1.11.1 refuses to configure a module that has a macOS target unless
// this flag is set; the macOS viewport is the point of this module. It must be an
// extra property rather than a plugin DSL setting because the Compose plugin
// reads it while it configures targets, before this script's `kotlin {}` block
// runs.
project.extensions.extraProperties["org.jetbrains.compose.experimental.macos.enabled"] = "true"

kotlin {
    sourceSets {
        commonMain.dependencies {
            api(project(":dawn4k"))
            // The viewport hosts a toolkit DawnSurfaceTarget: the views and the
            // target contracts come from there, the Compose glue lives here.
            api(project(":dawn4k-webgpu-toolkit"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        androidMain.dependencies {
            // The host schedules its own work off the main thread; the API
            // dependency of :dawn4k does not expose coroutines.
            implementation(libs.kotlinx.coroutines.core)
        }
        commonTest.dependencies {
            implementation(libs.kotlinx.coroutines.test)
        }
    }
}

// :dawn4k links Dawn into its own binaries only: a consumer's test binary that
// references Dawn symbols resolves them from the static archives itself, so it
// needs the same search path and frameworks. The options and the framework list
// come from ygdrasil.conventions.DawnLinkage, shared with :dawn4k, so a new
// framework cannot be added to one module and forgotten in the other.
val dawnNativeProject = project(":dawn4k-native")
kotlin {
    listOf("iosArm64", "iosSimulatorArm64").forEach { nativeTarget ->
        (targets.getByName(nativeTarget) as org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget)
            .applyDawnLinkage(dawnNativeProject, linkage = "static")
    }
}

// The Android device-test source set is declared by finalizeDsl, after the
// kotlin {} block above, so its dependencies are configured here.
kotlin.sourceSets.matching { it.name == "androidDeviceTest" }.configureEach {
    dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.androidx.test.runner)
        implementation(libs.androidx.test.ext.junit)
        implementation(libs.androidx.activity.compose)
        implementation(libs.compose.ui.test.junit4)
    }
}

// The Compose plugin registers an assets-copy task for every Android test
// variant but, for the KMP device-test variant, never wires its output
// directory, which fails task validation. The viewport has no Compose
// resources, so the task has nothing to copy: point it at an empty directory.
// The task class is internal to the plugin, so its one output property is set
// by name. The property lookup fails loudly, with the plugin version, if the
// method moves; a renamed or removed task silently stops matching, and the
// plugin's own validation failure resurfaces at the next device-test run.
val assetsTaskName = "copyAndroidDeviceTestComposeResourcesToAndroidAssets"
tasks.matching { it.name == assetsTaskName }.configureEach {
    val destination = layout.buildDirectory.dir("generated/composeAssets/androidDeviceTest")
    @Suppress("UNCHECKED_CAST")
    val output = try {
        javaClass.getMethod("getOutputDirectory").invoke(this) as org.gradle.api.provider.Property<Any>
    } catch (failure: NoSuchMethodException) {
        throw GradleException(
            "The Compose ${libs.versions.compose.multiplatform.get()} workaround for $name no longer applies: " +
                "the task has no getOutputDirectory(). Update the workaround in dawn4k-compose/build.gradle.kts.",
            failure,
        )
    }
    output.set(destination)
}
