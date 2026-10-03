import org.graphiks.dawn4k.build.DownloadDawnTask
import org.graphiks.dawn4k.build.DumpGeneratedAbiTask
import org.graphiks.dawn4k.build.GenerateDawnBindingsTask
import org.graphiks.dawn4k.build.VerifyDawnAbiTask
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.kmp-library")
}

val dawnTargets: List<String> =
    (project.findProperty("dawn.targets") as? String)
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf("macosArm64", "linuxX64")

val nativeDir = layout.buildDirectory.dir("native")
val dawnLockFile = rootProject.layout.projectDirectory.file("bindings/dawn.lock.json")

fun includeDir(target: String) = layout.buildDirectory.dir("native/$target/static/include")
fun libDir(target: String) = layout.buildDirectory.dir("native/$target/static/lib")

val prepareDawn = tasks.register<DownloadDawnTask>("prepareDawn") {
    group = "dawn"
    description = "Download, verify and extract the pinned Dawn prebuilt archives."
    lockFile.set(dawnLockFile)
    targets.set(dawnTargets)
    outputDir.set(nativeDir)
}

/**
 * Stage the verified shared libraries under the platform directories the generated
 * JVM bootstrap expects (`darwin-aarch64`, `linux-x86-64`), using their original names.
 */
val stageJvmNativeResources = tasks.register<Sync>("stageJvmNativeResources") {
    group = "dawn"
    description = "Stage the verified Dawn shared libraries for the generated JVM bootstrap."
    dependsOn(prepareDawn)
    into(layout.buildDirectory.dir("generated/nativeResources"))
    from(layout.buildDirectory.dir("native/macosArm64/shared/lib")) {
        include("libwebgpu_dawn.dylib")
        into("darwin-aarch64")
    }
    from(layout.buildDirectory.dir("native/linuxX64/shared/lib")) {
        include("libwebgpu_dawn.so")
        into("linux-x86-64")
    }
}

/**
 * Regenerate the raw Dawn bindings into `build/regenerated/src`. This is deliberately
 * independent from ordinary compilation: review the diff, then promote the output into
 * the versioned `generated/src`.
 */
val generateBindingsFromHeader = tasks.register<GenerateDawnBindingsTask>("generateBindingsFromHeader") {
    group = "dawn"
    description = "Regenerate raw Dawn bindings with the pinned kextract tool into build/regenerated/src."
    dependsOn(prepareDawn, stageJvmNativeResources)
    kextractHome.set(layout.buildDirectory.dir("tooling/kextract/checkout/build/kextract"))
    headerFile.set(layout.buildDirectory.file("native/macosArm64/shared/include/dawn/webgpu.h"))
    includeDir.set(layout.buildDirectory.dir("native/macosArm64/shared/include"))
    callbackBindings.set(rootProject.layout.projectDirectory.file("bindings/callback-bindings.yml"))
    kextractVersion.set(rootProject.layout.projectDirectory.file("bindings/kextract.version"))
    jvmNativeResourcesDir.set(layout.buildDirectory.dir("generated/nativeResources"))
    targetPackage.set("org.graphiks.dawn4k.raw")
    libraryName.set("webgpu_dawn")
    outputDir.set(layout.buildDirectory.dir("regenerated/src"))
    generationManifest.set(rootProject.layout.projectDirectory.file("bindings/generation.json"))
}

val generatedJvmFile =
    layout.projectDirectory.file("generated/src/jvmMain/kotlin/org/graphiks/dawn4k/raw/webgpu_hJvm.kt")

val abiHost: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        os.contains("mac") -> "macos"
        os.contains("linux") -> "linux"
        else -> os.replace(Regex("[^a-z0-9]+"), "-")
    }
    val archName = when (arch) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> arch
    }
    "$osName-$archName"
}

val dumpGeneratedAbi = tasks.register<DumpGeneratedAbiTask>("dumpGeneratedAbi") {
    group = "verification"
    description = "Emit the layout baked into the generated JVM bindings as JSON."
    generatedJvmSource.set(generatedJvmFile)
    hostName.set(abiHost)
    report.set(layout.buildDirectory.file("reports/abi/generated.json"))
}

val verifyDawnAbi = tasks.register<VerifyDawnAbiTask>("verifyDawnAbi") {
    group = "verification"
    description = "Compile the C ABI oracle, run it, and compare it with the generated bindings."
    dependsOn(prepareDawn)
    oracleSource.set(rootProject.layout.projectDirectory.file("tests/abi/dawn_abi.c"))
    includeDir.set(layout.buildDirectory.dir("native/macosArm64/shared/include"))
    generatedJvmSource.set(generatedJvmFile)
    hostName.set(abiHost)
    compiler.set(System.getenv("CC")?.takeIf { it.isNotBlank() } ?: "cc")
    report.set(layout.buildDirectory.file("reports/abi/$abiHost.json"))
    workDir.set(layout.buildDirectory.dir("abi"))
}

val helperLibName =
    if (abiHost.startsWith("macos")) "libdawn_abi_helper.dylib" else "libdawn_abi_helper.so"
val helperLibFile = layout.buildDirectory.file("abi/helper/$helperLibName")

val buildDawnAbiHelper = tasks.register<Exec>("buildDawnAbiHelper") {
    group = "verification"
    description = "Build the small C helper that passes a CallbackInfo by value to a Kotlin callback."
    dependsOn(prepareDawn)
    val output = helperLibFile.get().asFile
    doFirst { output.parentFile.mkdirs() }
    commandLine(
        System.getenv("CC")?.takeIf { it.isNotBlank() } ?: "cc",
        "-std=c11", "-DDAWN_ABI_NO_MAIN", "-shared", "-fPIC",
        "-I${layout.buildDirectory.dir("native/macosArm64/shared/include").get().asFile.absolutePath}",
        rootProject.layout.projectDirectory.file("tests/abi/dawn_abi.c").asFile.absolutePath,
        "-o", output.absolutePath,
    )
    outputs.file(output)
}

tasks.named<Test>("jvmTest") {
    dependsOn(buildDawnAbiHelper)
    systemProperty("dawn.abi.helper", helperLibFile.get().asFile.absolutePath)
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api("org.graphiks:kffi:1.0.0-SNAPSHOT")
    }
    sourceSets.getByName("commonMain").kotlin.srcDir("generated/src/commonMain/kotlin")
    sourceSets.getByName("nativeMain").kotlin.srcDir("generated/src/nativeMain/kotlin")
    sourceSets.getByName("jvmMain").kotlin.srcDir("generated/src/jvmMain/kotlin")
    sourceSets.getByName("androidMain").kotlin.srcDir("generated/src/androidMain/kotlin")
    // The generated JVM bootstrap loads `darwin-aarch64/libwebgpu_dawn.dylib` and
    // `linux-x86-64/libwebgpu_dawn.so` from the classpath; stage them into the jar.
    sourceSets.getByName("jvmMain").resources.srcDir(layout.buildDirectory.dir("generated/nativeResources"))

    macosArm64 {
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(project.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(includeDir("macosArm64"))
        }
        binaries.all {
            linkerOpts("-L${libDir("macosArm64").get().asFile.absolutePath}", "-lwebgpu_dawn")
            linkerOpts(
                "-framework", "Metal",
                "-framework", "Foundation",
                "-framework", "CoreGraphics",
                "-framework", "QuartzCore",
                "-framework", "IOKit",
                "-framework", "IOSurface",
            )
        }
    }

    linuxX64 {
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(project.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(includeDir("linuxX64"))
        }
        binaries.all {
            linkerOpts("-L${libDir("linuxX64").get().asFile.absolutePath}", "-lwebgpu_dawn")
            linkerOpts("-lpthread", "-ldl", "-lm")
        }
    }
}

tasks.matching { it.name.startsWith("cinteropDawn") }.configureEach {
    dependsOn(prepareDawn)
}

tasks.named("jvmProcessResources") {
    dependsOn(stageJvmNativeResources)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// The Dawn Linux archives are built against a newer glibc/libstdc++ than the
// Kotlin/Native macOS sysroot, so the Linux test binary can only be linked on a
// Linux host. The Linux klib is still compiled everywhere.
if (!abiHost.startsWith("linux")) {
    tasks.matching { it.name == "linkDebugTestLinuxX64" || it.name == "linuxX64Test" }
        .configureEach { enabled = false }
}
