import com.android.build.api.dsl.KotlinMultiplatformAndroidLibraryExtension
import com.android.build.api.variant.KotlinMultiplatformAndroidComponentsExtension
import org.graphiks.dawn4k.build.DownloadDawnTask
import org.graphiks.dawn4k.build.DumpGeneratedAbiTask
import org.graphiks.dawn4k.build.GenerateDawnBindingsTask
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import java.security.MessageDigest

plugins {
    id("ygdrasil.conventions.kmp-library")
    id("ygdrasil.conventions.dawn-jvm-runtime")
    id("ygdrasil.conventions.kmp-publish")
    id("ygdrasil.conventions.kmp-dokka")
    alias(libs.plugins.dokka.mkdocs)
}

dokka {
    // Document the portable binding API rather than backend-specific generated storage helpers.
    dokkaSourceSets.configureEach {
        if (name != "commonMain") suppress.set(true)
    }
}

extensions.configure<KotlinMultiplatformAndroidComponentsExtension> {
    finalizeDsl(
        org.gradle.api.Action<KotlinMultiplatformAndroidLibraryExtension> {
            namespace = "org.graphiks.dawn4k.bindings"
            optimization.consumerKeepRules.apply {
                publish = true
                file("consumer-rules.pro")
            }
        },
    )
}

val dawnTargets: List<String> =
    (project.findProperty("dawn.targets") as? String)
        ?.split(',')
        ?.map { it.trim() }
        ?.filter { it.isNotEmpty() }
        ?: listOf(
            "mingwX64",
            "macosArm64",
            "linuxX64",
            "linuxArm64",
            "androidNativeArm64",
            "androidNativeX64",
            "iosArm64",
            "iosSimulatorArm64",
            "iosX64",
            "tvosArm64",
            "tvosSimulatorArm64",
        )

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

val prepareLinuxDawnCarrier = tasks.register<Exec>("prepareLinuxDawnCarrier") {
    group = "dawn"
    description = "Build or verify the relocatable Linux Dawn cinterop carrier."
    dependsOn(prepareDawn)
    val script = rootProject.file("scripts/build-dawn-linux-bundle.py")
    val prepared = layout.buildDirectory.dir("native/linuxX64/shared")
    val output = layout.buildDirectory.dir("dawnCarrier/linuxX64")
    val bundle = providers.gradleProperty("dawn.nativeBundle.linuxX64")
    inputs.file(script)
    inputs.file(dawnLockFile)
    inputs.dir(prepared)
    bundle.orNull?.let { inputs.dir(it) }
    outputs.dir(output)
    doFirst {
        commandLine(buildList {
            addAll(listOf("python3", script.absolutePath, "--prepared", prepared.get().asFile.absolutePath,
                "--lock", dawnLockFile.asFile.absolutePath, "--output", output.get().asFile.absolutePath))
            bundle.orNull?.let { addAll(listOf("--bundle", file(it).absolutePath)) }
        })
    }
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
    from(layout.buildDirectory.dir("native/linuxArm64/shared/lib")) {
        include("libwebgpu_dawn.so")
        into("linux-aarch64")
    }
    from(layout.buildDirectory.dir("native/mingwX64/shared/bin")) {
        include("*.dll")
        into("win32-x86-64")
    }
    doLast {
        val windows = destinationDir.resolve("win32-x86-64")
        if (windows.isDirectory) {
            val manifest = destinationDir.resolve("dawn4k/windows-runtime.properties")
            manifest.parentFile.mkdirs()
            manifest.writeText(listOf("vcruntime140.dll", "vcruntime140_1.dll", "msvcp140.dll", "msvcp140_atomic_wait.dll")
                .joinToString("\n", postfix = "\n") { name ->
                    val digest = MessageDigest.getInstance("SHA-256").digest(windows.resolve(name).readBytes())
                        .joinToString("") { "%02x".format(it.toInt() and 255) }
                    "$name=$digest"
                })
        }
    }
}

/**
 * Regenerate the raw Dawn bindings into `build/regenerated/src`. This is deliberately
 * independent from ordinary compilation: review the diff, then promote the output into
 * the versioned `generated/src`.
 */
/**
 * Extract the Android shared libraries from the verified archives into the Android
 * `jniLibs` source directory. The directory is git-ignored: the libraries are always
 * produced from the downloaded/verified archives, never versioned.
 */
val extractAndroidNativeLibs = tasks.register<Sync>("extractAndroidNativeLibs") {
    group = "dawn"
    description = "Extract the verified Dawn Android shared libraries into the Android jniLibs."
    dependsOn(prepareDawn)
    into(layout.projectDirectory.dir("src/androidMain/jniLibs"))
    from(layout.buildDirectory.dir("native/androidNativeArm64/shared/lib")) {
        include("libwebgpu_dawn.so")
        into("arm64-v8a")
    }
    from(layout.buildDirectory.dir("native/androidNativeX64/shared/lib")) {
        include("libwebgpu_dawn.so")
        into("x86_64")
    }
}

tasks.matching {
    it.name in setOf("mergeAndroidMainJniLibFolders", "bundleAndroidMainAar", "assembleAndroidMain")
}.configureEach {
    dependsOn(extractAndroidNativeLibs)
}

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
    targetPackage.set("org.graphiks.dawn4k.native")
    libraryName.set("webgpu_dawn")
    outputDir.set(layout.buildDirectory.dir("regenerated/src"))
}

val generatedJvmFile =
    layout.projectDirectory.file("generated/src/jvmMain/kotlin/org/graphiks/dawn4k/native/webgpu_hJvm.kt")

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

kotlin {
    compilerOptions {
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
    sourceSets.getByName("commonMain").dependencies {
        api(libs.kffi)
    }
    sourceSets.getByName("commonMain").kotlin.srcDir("generated/src/commonMain/kotlin")
    sourceSets.getByName("nativeMain").kotlin.srcDir("generated/src/nativeMain/kotlin")
    sourceSets.getByName("jvmMain").kotlin.srcDir("generated/src/jvmMain/kotlin")
    sourceSets.getByName("androidMain").kotlin.srcDir("generated/src/androidMain/kotlin")
    // The generated JVM bootstrap loads `darwin-aarch64/libwebgpu_dawn.dylib` and
    // `linux-x86-64/libwebgpu_dawn.so` from the classpath; stage them into the jar.
    sourceSets.getByName("jvmMain").resources.srcDir(layout.buildDirectory.dir("generated/nativeResources"))

    listOf(
        "macosArm64", "iosArm64", "iosSimulatorArm64", "iosX64", "tvosArm64", "tvosSimulatorArm64",
    ).forEach { appleTarget ->
        (targets.getByName(appleTarget) as KotlinNativeTarget).apply {
            compilations.getByName("main").cinterops.create("dawn") {
                defFile(project.file("src/nativeInterop/cinterop/dawn.def"))
                includeDirs(includeDir(appleTarget), project.file("src/nativeInterop/cinterop"))
                extraOpts("-libraryPath", libDir(appleTarget).get().asFile.absolutePath)
            }.also { interop ->
                tasks.named(interop.interopProcessingTaskName) {
                    inputs.file(libDir(appleTarget).map { it.file("libwebgpu_dawn.a") })
                        .withPropertyName("embeddedDawnArchive").withPathSensitivity(PathSensitivity.RELATIVE)
                }
            }
        }
    }

    linuxX64 {
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(project.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(layout.buildDirectory.dir("native/linuxX64/shared/include"), project.file("src/nativeInterop/cinterop"))
            extraOpts("-libraryPath", layout.buildDirectory.dir("dawnCarrier/linuxX64").get().asFile.absolutePath)
        }
    }
}

tasks.matching { it.name.startsWith("cinteropDawn") }.configureEach {
    dependsOn(prepareDawn)
}
tasks.matching { it.name == "cinteropDawnLinuxX64" }.configureEach {
    dependsOn(prepareLinuxDawnCarrier)
    inputs.dir(prepareLinuxDawnCarrier.map { layout.buildDirectory.dir("dawnCarrier/linuxX64").get() })
        .withPropertyName("embeddedDawnCarrier").withPathSensitivity(PathSensitivity.RELATIVE)
}

tasks.named("jvmProcessResources") {
    dependsOn(stageJvmNativeResources)
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
