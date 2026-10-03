import org.graphiks.dawn4k.build.DownloadDawnTask
import org.graphiks.dawn4k.build.GenerateDawnBindingsTask
import org.gradle.api.tasks.Sync
import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.dawn-desktop")
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
    jvmNativeResourcesDir.set(layout.buildDirectory.dir("generated/nativeResources"))
    targetPackage.set("org.graphiks.dawn4k.raw")
    libraryName.set("webgpu_dawn")
    outputDir.set(layout.buildDirectory.dir("regenerated/src"))
    generationManifest.set(rootProject.layout.projectDirectory.file("bindings/generation.json"))
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api("org.graphiks:kffi:1.0.0-SNAPSHOT")
    }
    sourceSets.getByName("commonMain").kotlin.srcDir("generated/src/commonMain/kotlin")
    sourceSets.getByName("nativeMain").kotlin.srcDir("generated/src/nativeMain/kotlin")
    sourceSets.getByName("jvmMain").kotlin.srcDir("generated/src/jvmMain/kotlin")

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

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
