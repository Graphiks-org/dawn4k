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
