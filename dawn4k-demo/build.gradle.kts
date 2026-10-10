import org.gradle.jvm.application.tasks.CreateStartScripts
import java.security.MessageDigest
import org.gradle.api.artifacts.type.ArtifactTypeDefinition
import org.gradle.api.tasks.options.Option
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
import org.gradle.process.ExecOperations
import java.io.ByteArrayOutputStream
import javax.inject.Inject

plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
    distribution
    alias(libs.plugins.compose.multiplatform)
    alias(libs.plugins.compose.compiler)
}

kotlin {
    jvmToolchain(25)
    jvm()
    android {
        namespace = "org.graphiks.dawn4k.demo"
        compileSdk = 37
        minSdk = 28
        androidResources { enable = true }
        compilerOptions { jvmTarget.set(JvmTarget.JVM_17) }
        withHostTestBuilder {}.configure {}
        withDeviceTestBuilder { sourceSetTreeName = "test" }.configure {
            instrumentationRunner = "androidx.test.runner.AndroidJUnitRunner"
        }
    }
    iosArm64()
    iosSimulatorArm64()
    applyDefaultHierarchyTemplate()
    compilerOptions { optIn.add("kotlin.ExperimentalUnsignedTypes") }
    sourceSets {
        commonMain.dependencies {
            implementation(project(":dawn4k"))
            implementation(libs.suite.demos)
            implementation(libs.webgpu.descriptors)
            implementation(libs.kotlinx.coroutines.core)
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
        }
        androidMain.dependencies { implementation("org.jetbrains.androidx.lifecycle:lifecycle-runtime-compose:2.10.0") }
        commonTest.dependencies {
            implementation(kotlin("test"))
            implementation(libs.kotlinx.coroutines.test)
        }
        // Android host tests use SDK stubs, not a Compose UI runtime. Keep
        // pure common tests there; UI tests still run on desktop and the device.
        val composeUiTest = create("composeUiTest") {
            dependsOn(commonTest.get())
            dependencies { implementation("org.jetbrains.compose.ui:ui-test:1.11.1") }
        }
        jvmTest.get().dependsOn(composeUiTest)
        getByName("androidDeviceTest").dependsOn(composeUiTest)
        getByName("iosTest").dependsOn(composeUiTest)
        jvmMain {
            dependencies {
                implementation(libs.kffi.objc.jvm)
                implementation(libs.compose.desktop.currentOs)
                runtimeOnly(libs.skiko.awt.runtime.macos.arm64)
                runtimeOnly(libs.skiko.awt.runtime.macos.x64)
                runtimeOnly(libs.skiko.awt.runtime.linux.x64)
                runtimeOnly(libs.skiko.awt.runtime.linux.arm64)
                runtimeOnly(libs.skiko.awt.runtime.windows.x64)
            }
        }
        jvmTest {
            dependencies { implementation(kotlin("test-junit")) }
        }
        getByName("androidDeviceTest").dependencies {
            implementation(kotlin("test-junit"))
            implementation("androidx.test:runner:1.7.0")
        }
    }
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.framework {
            baseName = "Dawn4kDemo"
            isStatic = true
        }
        val dawn = project(":dawn4k-native")
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(dawn.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(dawn.layout.buildDirectory.dir("native/$targetName/static/include"))
        }
        binaries.all {
            linkerOpts("-L${dawn.layout.buildDirectory.dir("native/$targetName/static/lib").get().asFile}", "-lwebgpu_dawn")
            listOf("Metal", "Foundation", "CoreGraphics", "QuartzCore", "IOKit", "IOSurface").forEach {
                linkerOpts("-framework", it)
            }
        }
    }
}

tasks.matching { it.name.startsWith("cinteropDawn") }.configureEach { dependsOn(":dawn4k-native:prepareDawn") }
val androidSdk = providers.environmentVariable("ANDROID_HOME")
    .orElse(providers.environmentVariable("ANDROID_SDK_ROOT"))
    .orElse("${System.getProperty("user.home")}/Library/Android/sdk")
val buildAndroidWindow = listOf("arm64-v8a" to "aarch64-linux-android", "x86_64" to "x86_64-linux-android").map { (abi, triple) ->
    tasks.register<Exec>("buildAndroidWindow${if (abi == "arm64-v8a") "Arm64" else "X64"}") {
        val output = layout.projectDirectory.file("src/androidMain/jniLibs/$abi/libdawn4k_demo_window.so")
        inputs.file("src/androidMain/c/android_window.c")
        inputs.property("ndk", "28.2.13676358")
        outputs.file(output)
        doFirst {
            val host = if (System.getProperty("os.name").lowercase().contains("mac")) "darwin-x86_64" else "linux-x86_64"
            val compiler = "${androidSdk.get()}/ndk/28.2.13676358/toolchains/llvm/prebuilt/$host/bin/${triple}24-clang"
            output.asFile.parentFile.mkdirs()
            commandLine(compiler, "-shared", "-fPIC", "-Wall", "-Wextra", "-Werror",
                "src/androidMain/c/android_window.c", "-landroid", "-o", output.asFile.absolutePath)
        }
    }
}
tasks.matching { it.name == "mergeAndroidMainJniLibFolders" || it.name == "bundleAndroidMainAar" }.configureEach {
    dependsOn(buildAndroidWindow)
}
val jvmTarget = kotlin.targets.getByName("jvm") as KotlinJvmTarget
val jvmMainCompilation = jvmTarget.compilations.getByName("main")
val jvmTestCompilation = jvmTarget.compilations.getByName("test")
val desktopClasspath = files(tasks.named("jvmJar"), jvmMainCompilation.runtimeDependencyFiles)
val desktopMainClass = "org.graphiks.dawn4k.demo.MainKt"
tasks.register<JavaExec>("run") {
    group = "application"
    mainClass.set(desktopMainClass)
    classpath = desktopClasspath
    javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
}
abstract class DemoJvmTestAlias : DefaultTask() {
    @Option(option = "tests", description = "Forwards test include patterns to the demo JVM test task.")
    fun includeTests(patterns: List<String>) {
        project.tasks.named<Test>("jvmTest").configure { filter.setIncludePatterns(*patterns.toTypedArray()) }
    }
}
tasks.register<DemoJvmTestAlias>("test") { group = "verification"; dependsOn("jvmTest") }
tasks.register("recordDemoDependencies") {
    group = "verification"
    description = "Records actually resolved mobile suite artifacts and their SHA-256 hashes."
    val report = layout.buildDirectory.file("demo-kmp/resolved-mobile-artifacts.tsv")
    outputs.file(report)
    outputs.upToDateWhen { false }
    doLast {
        val rows = listOf("androidCompileClasspath", "iosArm64CompileKlibraries", "iosSimulatorArm64CompileKlibraries")
            .flatMap { name ->
                val suite = configurations.getByName(name).incoming.artifactView {
                    componentFilter { it.displayName.startsWith("org.graphiks:suite-demos") }
                    if (name == "androidCompileClasspath") attributes {
                        attribute(ArtifactTypeDefinition.ARTIFACT_TYPE_ATTRIBUTE, "android-classes-jar")
                    }
                }.artifacts.artifacts
                check(suite.isNotEmpty()) { "no resolved suite-demos variant in $name" }
                suite.map { artifact ->
                    val hash = MessageDigest.getInstance("SHA-256").digest(artifact.file.readBytes())
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    "$name\t${artifact.id.componentIdentifier.displayName}\t${artifact.file.name}\t$hash"
                }
            }
        report.get().asFile.apply { parentFile.mkdirs(); writeText(rows.joinToString("\n", postfix = "\n")) }
    }
}
tasks.register<CreateStartScripts>("startScripts") {
    mainClass.set(desktopMainClass)
    applicationName = project.name
    classpath = desktopClasspath
    defaultJvmOpts = listOf("--enable-native-access=ALL-UNNAMED")
    outputDir = layout.buildDirectory.dir("scripts").get().asFile
}

// Native Wayland windowing belongs to the demo, not the public Dawn bindings.
val linuxHost = System.getProperty("os.name").lowercase().contains("linux")
val waylandHostTarget = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "linuxArm64" else "linuxX64"
val waylandTargets = providers.gradleProperty("wayland.targets")
    .map { it.split(',').map(String::trim).distinct() }
    .getOrElse(if (linuxHost) listOf(waylandHostTarget) else emptyList())
require(waylandTargets.all { it in setOf("linuxArm64", "linuxX64") }) { "wayland.targets must be linuxArm64 and/or linuxX64" }

abstract class WaylandCommands @Inject constructor(private val operations: ExecOperations) {
    fun run(directory: File, environment: Map<String, String>, arguments: List<String>): String {
        val output = ByteArrayOutputStream()
        operations.exec {
            workingDir(directory)
            environment(environment)
            commandLine(arguments)
            standardOutput = output
        }.assertNormalExitValue()
        return output.toString(Charsets.UTF_8)
    }
}
val waylandCommands = objects.newInstance<WaylandCommands>()

fun registerWaylandBuild(name: String, target: String, cross: Boolean, test: Boolean = false) = tasks.register(name) {
    onlyIf { linuxHost }
    inputs.files(fileTree("src/main/c"))
    if (test) inputs.files(fileTree("src/test/c"))
    inputs.property("target", target)
    val compiler = if (cross) "x86_64-linux-gnu-gcc" else System.getenv("CC") ?: "gcc"
    inputs.property("compiler", compiler)
    val sysroot = System.getenv("DAWN_WAYLAND_X64_SYSROOT")?.takeIf { it.isNotEmpty() }
    val protocol = System.getenv("WAYLAND_PROTOCOL_XML")?.takeIf { it.isNotEmpty() } ?: "/usr/share/wayland-protocols/stable/xdg-shell/xdg-shell.xml"
    val compilerFlags = if (cross) "--sysroot=$sysroot" else System.getenv("CFLAGS") ?: ""
    inputs.property("flags", compilerFlags)
    inputs.property("protocolPath", protocol)
    inputs.property("pkgConfigSysroot", if (cross) sysroot.orEmpty() else System.getenv("PKG_CONFIG_SYSROOT_DIR") ?: "")
    inputs.property("pkgConfigLibdir", if (cross) "$sysroot/usr/lib/x86_64-linux-gnu/pkgconfig:$sysroot/usr/share/pkgconfig"
        else System.getenv("PKG_CONFIG_LIBDIR") ?: "")
    inputs.property("pkgConfigPath", System.getenv("PKG_CONFIG_PATH") ?: "")
    if (linuxHost) {
        inputs.file(protocol)
        if (cross && sysroot != null) inputs.dir(sysroot)
    }
    val output = layout.buildDirectory.dir("wayland/$target")
    outputs.file(output.map { it.file("libdawn4k_wayland.so") })
    outputs.dir(output.map { it.dir("generated") })
    if (test) outputs.upToDateWhen { false }
    doLast {
        check(!cross || sysroot != null) { "Cross-compilation requires DAWN_WAYLAND_X64_SYSROOT" }
        val directory = output.get().asFile
        val generated = directory.resolve("generated").apply { mkdirs() }
        val environment = if (cross) mapOf(
            "PKG_CONFIG_SYSROOT_DIR" to checkNotNull(sysroot),
            "PKG_CONFIG_LIBDIR" to "$sysroot/usr/lib/x86_64-linux-gnu/pkgconfig:$sysroot/usr/share/pkgconfig",
        ) else emptyMap()
        fun run(vararg arguments: String) = waylandCommands.run(projectDir, environment, arguments.toList())
        fun flags(value: String) = value.trim().split(Regex("\\s+")).filter(String::isNotEmpty)
        run("wayland-scanner", "client-header", protocol, generated.resolve("xdg-shell-client-protocol.h").path)
        run("wayland-scanner", "private-code", protocol, generated.resolve("xdg-shell-protocol.c").path)
        val libraries = flags(run("pkg-config", "--cflags", "--libs", "wayland-client", "xkbcommon"))
        val common = listOf("-std=c11", "-Wall", "-Wextra", "-Werror") + flags(compilerFlags)
        val sources = listOf("-I$generated", "-Isrc/main/c", "src/main/c/wayland-host.c", generated.resolve("xdg-shell-protocol.c").path)
        waylandCommands.run(projectDir, environment, listOf(compiler) + common +
            listOf("-fPIC", "-shared", "-Wl,-z,defs") + sources + libraries +
            listOf("-o", directory.resolve("libdawn4k_wayland.so").path))
        if (test) {
            run("wayland-scanner", "server-header", protocol, generated.resolve("xdg-shell-server-protocol.h").path)
            waylandCommands.run(projectDir, environment, listOf(compiler) + common +
                listOf("-Isrc/main/c", "src/test/c/wayland-host-abi.c", "-o", directory.resolve("abi").path))
            print(run(directory.resolve("abi").path))
            val server = flags(run("pkg-config", "--cflags", "--libs", "wayland-server"))
            waylandCommands.run(projectDir, environment, listOf(compiler) + common +
                listOf("-fsanitize=address,undefined", "-g", "src/test/c/wayland-host-test.c") +
                sources + libraries + server + listOf("-pthread", "-o", directory.resolve("test").path))
            print(run(directory.resolve("test").path))
        }
    }
}
val buildWaylandBridge = registerWaylandBuild("buildWaylandBridge", waylandHostTarget, false)
val buildWaylandBridgeX64 = registerWaylandBuild("buildWaylandBridgeX64", "linuxX64", waylandHostTarget != "linuxX64")
val testWaylandBridge = registerWaylandBuild("testWaylandBridge", waylandHostTarget, false, test = true)
val stageWaylandResources = tasks.register<Sync>("stageWaylandResources") {
    into(layout.buildDirectory.dir("generated/waylandResources"))
    waylandTargets.forEach { target ->
        check(!linuxHost || target == waylandHostTarget || target == "linuxX64") {
            "cross-compiling the ARM64 bridge from x64 requires an ARM64 build host"
        }
        dependsOn(if (target == waylandHostTarget) buildWaylandBridge else buildWaylandBridgeX64)
        from(layout.buildDirectory.file("wayland/$target/libdawn4k_wayland.so")) {
            into(if (target == "linuxArm64") "linux-aarch64" else "linux-x86-64")
        }
    }
}
kotlin.sourceSets.getByName("jvmMain").resources.srcDir(stageWaylandResources.map { it.destinationDir })

// Keep the verified Windows DLLs beside the launcher in standalone distributions.
// The generated raw bindings already support java.library.path for external DLLs.
val windowsNativeBin = project(":dawn4k-native").layout.buildDirectory.dir("native/mingwX64/shared/bin")
distributions {
    main {
        contents {
            from(tasks.named("startScripts")) { into("bin") }
            from(desktopClasspath) { into("lib") }
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

tasks.register<Exec>("runComposeWaylandProbe") {
    dependsOn("jvmTestClasses")
    // Skiko gives the environment precedence over the system property. The
    // regular desktop launcher sets SOFTWARE; isolate this probe's renderer.
    environment("SKIKO_RENDER_API", "SOFTWARE_COMPAT")
    val candidate = providers.gradleProperty("qualification.java")
    doFirst {
        commandLine(candidate.get(), "--enable-native-access=ALL-UNNAMED",
            "-Dawt.toolkit.name=WLToolkit", "-Dskiko.renderApi=SOFTWARE_COMPAT",
            "--add-opens=java.desktop/sun.awt=ALL-UNNAMED",
            "--add-opens=java.desktop/sun.awt.wl=ALL-UNNAMED",
            "-cp", files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles).asPath,
            "org.graphiks.dawn4k.demo.ComposeWaylandProbeKt")
    }
}

tasks.register<Exec>("runComposeSceneWaylandProbe") {
    dependsOn("jvmTestClasses")
    // Normal toolchain JVM; no JBR/WLToolkit, display or JAWT surface.
    environment.remove("DISPLAY")
    val report = layout.buildDirectory.file("ui-qualification/compose-scene/result.json")
    environment("DAWN_SCENE_PROBE_REPORT", report.get().asFile.absolutePath)
    doFirst {
        report.get().asFile.parentFile.mkdirs()
        report.get().asFile.writeText("{\"status\":\"running\",\"sceneRaster\":false,\"awtToolkitFree\":false}\n")
        val java = javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) }.get()
        commandLine(java.executablePath.asFile.absolutePath, "--enable-native-access=ALL-UNNAMED",
            "-Djava.awt.headless=true",
            "-Xlog:class+init=info:file=${report.get().asFile.parent}/class-init.log",
            "-cp", files(jvmTestCompilation.output.allOutputs, jvmTestCompilation.runtimeDependencyFiles).asPath,
            "org.graphiks.dawn4k.demo.ComposeSceneWaylandProbeKt")
    }
    doLast {
        val file = report.get().asFile
        val trace = file.parentFile.resolve("class-init.log").readText()
        val toolkit = trace.contains("Initializing 'java/awt/Toolkit'")
        val headlessToolkit = trace.contains("Initializing 'sun/awt/HeadlessToolkit'")
        val window = trace.contains("Initializing 'java/awt/Window'")
        val hardware = trace.contains("Initializing 'org/jetbrains/skiko/HardwareLayer'") ||
            trace.contains("Initializing 'org/jetbrains/skiko/SkiaLayer'")
        file.writeText(file.readText().trimEnd().removeSuffix("}") +
            ",\"awtToolkitFree\":${!toolkit},\"headlessSnapshotDispatcher\":$headlessToolkit,\"awtWindowFree\":${!window},\"jawtHostFree\":${!hardware}}\n")
        check((!toolkit || headlessToolkit) && !window && !hardware) {
            "Only headless snapshot notifications are approved; AWT window/SkiaLayer/JAWT hosting is forbidden. See ${file.parent}."
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
    // Inherited environment is not otherwise a Gradle test input. An explicit
    // desktop opt-in must not reuse a successful run that gated native tests out.
    val desktopTests = providers.environmentVariable("DAWN_DESKTOP_TESTS").orElse("0").get()
    val missingDisplayTest = providers.environmentVariable("DAWN_MISSING_DISPLAY_TEST").orElse("0").get()
    val waylandTests = providers.environmentVariable("DAWN_WAYLAND_TESTS").orElse("0").get()
    val waylandPendingTests = providers.environmentVariable("DAWN_WAYLAND_PENDING_TESTS").orElse("0").get()
    inputs.property("dawn.desktopTests", desktopTests)
    inputs.property("dawn.missingDisplayTest", missingDisplayTest)
    inputs.property("dawn.waylandTests", waylandTests)
    inputs.property("dawn.waylandPendingTests", waylandPendingTests)
    if (waylandTests == "1" && linuxHost) dependsOn(testWaylandBridge)
    val liveDesktop = desktopTests == "1" || missingDisplayTest == "1" || waylandTests == "1"
    outputs.upToDateWhen { !liveDesktop }
    outputs.doNotCacheIf("native desktop tests require a live compositor") { liveDesktop }
}
