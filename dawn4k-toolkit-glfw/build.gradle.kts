import org.gradle.api.tasks.testing.Test

plugins {
    kotlin("multiplatform")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
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
