import org.gradle.api.tasks.testing.Test
import org.graphiks.dawn4k.build.PrepareGlfwTask
import org.graphiks.dawn4k.build.BuildGlfwTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeHostTest

plugins {
    kotlin("multiplatform")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
}

val prepareGlfw by tasks.registering(PrepareGlfwTask::class) {
    lockFile.set(rootProject.layout.projectDirectory.file("bindings/glfw.lock.json"))
    outputDirectory.set(layout.buildDirectory.dir("glfw/source"))
}
val prepareGlfwBridge by tasks.registering(Sync::class) {
    from("src/nativeMain/c")
    from("src/nativeInterop/cinterop/glfw_native_handles.h")
    into(layout.buildDirectory.dir("glfw/bridge"))
}

kotlin {
    jvmToolchain(25)
    jvm()
    macosArm64()
    linuxX64()
    applyDefaultHierarchyTemplate()
    sourceSets.commonMain.dependencies {
        api(project(":dawn4k-toolkit-surface"))
        implementation(libs.kotlinx.atomicfu)
    }
    sourceSets.commonTest.dependencies {
        implementation(kotlin("test"))
        implementation(libs.kotlinx.coroutines.test)
        implementation(libs.webgpu.descriptors)
    }
    sourceSets.jvmTest.dependencies { implementation(kotlin("test-junit")) }
    sourceSets.jvmMain.dependencies {
        implementation(libs.lwjgl.core)
        implementation(libs.lwjgl.glfw)
        implementation(libs.kffi.objc.jvm)
        implementation(libs.kffi.win32.jvm)
        val classifiers = providers.gradleProperty("lwjgl.natives").map {
            it.split(',').map(String::trim).filter(String::isNotEmpty)
        }.getOrElse(listOf("natives-macos-arm64", "natives-windows", "natives-linux", "natives-linux-arm64"))
        classifiers.forEach { classifier ->
            runtimeOnly("org.lwjgl:lwjgl:${libs.versions.lwjgl.get()}:$classifier")
            runtimeOnly("org.lwjgl:lwjgl-glfw:${libs.versions.lwjgl.get()}:$classifier")
        }
    }
    targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name
        val buildGlfw = tasks.register<BuildGlfwTask>("buildGlfw${targetName.replaceFirstChar(Char::uppercaseChar)}") {
            dependsOn(prepareGlfw, prepareGlfwBridge)
            this.targetName.set(targetName)
            lockFile.set(rootProject.layout.projectDirectory.file("bindings/glfw.lock.json"))
            sourceDirectory.set(prepareGlfw.flatMap { it.outputDirectory })
            bridgeDirectory.set(layout.buildDirectory.dir("glfw/bridge"))
            providers.gradleProperty("glfw.nativeBundle.$targetName").orNull?.let { nativeBundle.set(file(it)) }
            outputDirectory.set(layout.buildDirectory.dir("glfw/$targetName"))
        }
        compilations.getByName("main").cinterops.create("glfw") {
            definitionFile.set(file("src/nativeInterop/cinterop/glfw.def"))
            includeDirs("src/nativeInterop/cinterop", layout.buildDirectory.dir("glfw/source/glfw-3.5.1/include"))
            extraOpts("-libraryPath", layout.buildDirectory.dir("glfw/$targetName").get().asFile.absolutePath)
        }.also { interop -> tasks.named(interop.interopProcessingTaskName) {
            dependsOn(buildGlfw)
            inputs.dir(buildGlfw.flatMap { it.outputDirectory })
                .withPropertyName("glfwNativeBundle").withPathSensitivity(PathSensitivity.RELATIVE)
        } }
    }
}
// The desktop target set is declared before the shared linkage convention inspects it.
apply(plugin = "ygdrasil.conventions.dawn-native-linkage")
tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
    inputs.property("dawn.desktopTests", providers.environmentVariable("DAWN_DESKTOP_TESTS").orElse("0"))
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GLFW window availability is checked at execution time") { true }
}
tasks.withType<KotlinNativeHostTest>().configureEach {
    for (name in listOf("DAWN_DESKTOP_TESTS", "DAWN_WAYLAND_TESTS")) {
        environment(name, providers.environmentVariable(name).getOrElse("0"))
    }
    doFirst { logger.lifecycle("Native GLFW acceptance flag: ${environment["DAWN_DESKTOP_TESTS"]}") }
}
