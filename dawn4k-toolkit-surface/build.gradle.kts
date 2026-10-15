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
    android {
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets.getByName("androidDeviceTest").dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.androidx.test.runner)
    }
    sourceSets.getByName("jvmMain").dependencies {
        implementation(libs.kffi.objc.jvm)
        implementation(libs.kffi.win32.jvm)
        implementation(libs.kffi.x11.jvm)
    }
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

val androidSdk = providers.environmentVariable("ANDROID_HOME")
    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
    .orElse("${System.getProperty("user.home")}/Library/Android/sdk")
val buildAndroidWindow = listOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android").map { (abi, triple) ->
    tasks.register<Exec>("buildToolkitAndroidWindow${if (abi == "arm64-v8a") "Arm64" else "X64"}") {
        val output = layout.projectDirectory.file("src/androidMain/jniLibs/$abi/libdawn4k_toolkit_window.so")
        inputs.file("src/androidMain/c/android_window.c")
        inputs.property("ndk", "28.2.13676358")
        outputs.file(output)
        doFirst {
            val os = System.getProperty("os.name").lowercase()
            val host = when {
                "mac" in os -> "darwin-x86_64"
                "windows" in os -> "windows-x86_64"
                else -> "linux-x86_64"
            }
            val suffix = if ("windows" in os) ".cmd" else ""
            val compiler = "${androidSdk.get()}/ndk/28.2.13676358/toolchains/llvm/prebuilt/$host/bin/${triple}24-clang$suffix"
            output.asFile.parentFile.mkdirs()
            commandLine(compiler, "-shared", "-fPIC", "-Wall", "-Wextra", "-Werror",
                "src/androidMain/c/android_window.c", "-landroid", "-o", output.asFile.absolutePath)
        }
    }
}
tasks.matching {
    it.name in setOf("mergeAndroidMainJniLibFolders", "mergeAndroidDeviceTestJniLibFolders", "mergeAndroidDeviceTestNativeLibs", "bundleAndroidMainAar")
}.configureEach { dependsOn(buildAndroidWindow) }
