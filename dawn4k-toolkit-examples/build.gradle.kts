import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.dawn-native-linkage")
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(Action<KotlinMultiplatformAndroidLibraryExtension> { namespace = "org.graphiks.dawn4k.toolkit.examples" })
}

kotlin {
    android {
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    sourceSets.commonMain.dependencies {
        api(project(":dawn4k-toolkit-surface"))
        implementation(libs.webgpu.descriptors)
    }
    val desktopMain = sourceSets.create("desktopMain") {
        dependsOn(sourceSets.commonMain.get())
        dependencies { implementation(project(":dawn4k-toolkit-glfw")) }
    }
    // linuxX64 is only declared on Linux x86_64 hosts or with verified bundles; the
    // desktop sets stay wired for whichever native desktop targets exist.
    listOf("jvmMain", "macosArm64Main", "linuxX64Main").forEach { sourceSets.findByName(it)?.dependsOn(desktopMain) }
    val desktopNativeMain = sourceSets.create("desktopNativeMain") { dependsOn(desktopMain) }
    listOf("macosArm64Main", "linuxX64Main").forEach { sourceSets.findByName(it)?.dependsOn(desktopNativeMain) }
    val appleMobileMain = sourceSets.create("appleMobileMain") { dependsOn(sourceSets.commonMain.get()) }
    listOf("iosMain", "tvosMain").forEach { sourceSets.getByName(it).dependsOn(appleMobileMain) }
    targets.withType<KotlinNativeTarget>().configureEach {
        if (name in setOf("macosArm64", "linuxX64")) {
            binaries.executable("compute") { entryPoint = "org.graphiks.dawn4k.toolkit.examples.computeMain" }
            binaries.executable("triangle") { entryPoint = "org.graphiks.dawn4k.toolkit.examples.triangleMain" }
        } else binaries.framework { baseName = "ToolkitExamples"; isStatic = true }
    }
    sourceSets.commonTest.dependencies { implementation(libs.kotlinx.coroutines.test) }
    val gpuTest = sourceSets.create("gpuTest") { dependsOn(sourceSets.commonTest.get()) }
    listOf("jvmTest", "nativeTest", "androidDeviceTest").forEach { sourceSets.getByName(it).dependsOn(gpuTest) }
    sourceSets.jvmTest.dependencies { implementation(kotlin("test-junit")) }
    sourceSets.getByName("androidDeviceTest").dependencies {
        implementation(kotlin("test-junit"))
        implementation(libs.androidx.test.runner)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    outputs.upToDateWhen { false }
}

val jvm = kotlin.targets.getByName("jvm") as KotlinJvmTarget
for ((name, launcher) in listOf("runCompute" to "compute", "runTriangle" to "triangle")) {
    tasks.register<JavaExec>(name) {
        group = "application"
        mainClass.set("org.graphiks.dawn4k.toolkit.examples.$launcher.MainKt")
        classpath = files(tasks.named("jvmJar"), jvm.compilations.getByName("main").runtimeDependencyFiles)
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        if (launcher == "triangle") {
            if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
            args(providers.gradleProperty("toolkit.example.frames").getOrElse("0"))
        }
    }
}
