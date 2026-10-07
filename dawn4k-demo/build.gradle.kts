import org.gradle.jvm.application.tasks.CreateStartScripts

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
    runtimeOnly(libs.skiko.awt.runtime.linux.arm64)
    runtimeOnly(libs.skiko.awt.runtime.windows.x64)

    testImplementation(kotlin("test"))
}

application {
    mainClass.set("org.graphiks.dawn4k.demo.MainKt")
    applicationDefaultJvmArgs = listOf("--enable-native-access=ALL-UNNAMED")
}

// Keep the verified Windows DLLs beside the launcher in standalone distributions.
// The generated raw bindings already support java.library.path for external DLLs.
val windowsNativeBin = project(":dawn4k-native").layout.buildDirectory.dir("native/mingwX64/shared/bin")
distributions {
    main {
        contents {
            from(windowsNativeBin) {
                include("*.dll")
                into("lib/native/windows")
            }
        }
    }
}
tasks.matching { it.name in setOf("installDist", "distZip", "distTar") }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}
tasks.named<CreateStartScripts>("startScripts") {
    doLast {
        windowsScript.writeText(windowsScript.readText().replace(
            "set DEFAULT_JVM_OPTS=",
            "set \"PATH=%APP_HOME%\\lib\\native\\windows;%PATH%\"\r\n" +
                "set DEFAULT_JVM_OPTS=\"-Djava.library.path=%APP_HOME%\\lib\\native\\windows\" ",
        ).replace(
            // Gradle otherwise restores PATH before launching Java. The batch
            // file's local environment is still restored when it exits.
            "endlocal & \"%JAVA_EXE%\"",
            "\"%JAVA_EXE%\"",
        ))
    }
}

tasks.withType<JavaExec>().configureEach {
    // FFM is used by both the Objective-C bridge and the Win32 host.
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // Inherited environment is not otherwise a Gradle test input. An explicit
    // desktop opt-in must not reuse a successful run that gated native tests out.
    val desktopTests = providers.environmentVariable("DAWN_DESKTOP_TESTS").orElse("0").get()
    val missingDisplayTest = providers.environmentVariable("DAWN_MISSING_DISPLAY_TEST").orElse("0").get()
    inputs.property("dawn.desktopTests", desktopTests)
    inputs.property("dawn.missingDisplayTest", missingDisplayTest)
    val liveDesktop = desktopTests == "1" || missingDisplayTest == "1"
    outputs.upToDateWhen { !liveDesktop }
    outputs.doNotCacheIf("native desktop tests require a live compositor") { liveDesktop }
}
