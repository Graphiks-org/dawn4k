import org.graphiks.dawn4k.build.DownloadDawnTask
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

kotlin {
    sourceSets {
        commonMain.dependencies {
            api("org.graphiks:kffi:1.0.0-SNAPSHOT")
        }
    }

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
