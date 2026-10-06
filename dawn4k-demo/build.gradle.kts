plugins {
    kotlin("jvm")
    application
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)
}

dependencies {
    implementation(project(":dawn4k"))
    implementation(libs.suite.demos.jvm)
    implementation(libs.kffi.objc.jvm)
    implementation(libs.webgpu.descriptors)
    implementation(libs.compose.desktop.currentOs)
    implementation(libs.kotlinx.coroutines.core)

    // Skiko native runtime (required at runtime for Compose Desktop rendering).
    // compose.desktop.currentOs brings the JVM classes but not the native runtime
    // in a kotlin("jvm") module — add it explicitly for the host OS.
    runtimeOnly(libs.skiko.awt.runtime.macos.arm64)
    runtimeOnly(libs.skiko.awt.runtime.macos.x64)
    runtimeOnly(libs.skiko.awt.runtime.linux.x64)
    runtimeOnly(libs.skiko.awt.runtime.windows.x64)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.graphiks.dawn4k.demo.MainKt")
}

// The demo targets macOS only: the ObjC bridge and the Metal surface compile
// everywhere (pure JVM + FFM) but only run on macOS. The main function guards
// the OS at startup.
tasks.withType<JavaExec>().configureEach {
    // FFM (kffi-objc) needs native access enabled.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
