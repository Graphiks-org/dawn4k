import org.gradle.api.tasks.Exec
import org.gradle.api.tasks.JavaExec

plugins {
    id("ygdrasil.conventions.dawn-desktop")
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        implementation(project(":dawn4k-native"))
    }

    macosArm64 {
        binaries.executable {
            entryPoint = "org.graphiks.dawn4k.smoke.main"
        }
        binaries.all {
            linkerOpts(
                "-L${dawnNativeLibDir("macosArm64")}",
                "-lwebgpu_dawn",
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
        binaries.executable {
            entryPoint = "org.graphiks.dawn4k.smoke.main"
        }
        binaries.all {
            linkerOpts("-L${dawnNativeLibDir("linuxX64")}", "-lwebgpu_dawn", "-lpthread", "-ldl", "-lm")
        }
    }
}

fun dawnNativeLibDir(target: String): String =
    rootProject.project(":dawn4k-native").layout.buildDirectory
        .dir("native/$target/static/lib").get().asFile.absolutePath

val jvmMainCompilation = kotlin.jvm().compilations.getByName("main")

val runJvmSmoke = tasks.register<JavaExec>("runJvmSmoke") {
    group = "verification"
    description = "Run the real Dawn adapter/device/buffer smoke on the JVM."
    dependsOn(":dawn4k-native:prepareDawn")
    classpath = jvmMainCompilation.output.allOutputs + jvmMainCompilation.runtimeDependencyFiles
    mainClass.set("org.graphiks.dawn4k.smoke.JvmEntryKt")
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val runSmokeMacosArm64 = tasks.register<Exec>("runSmokeMacosArm64") {
    group = "verification"
    description = "Link and run the static Kotlin/Native Dawn smoke on macOS ARM64."
    dependsOn(":dawn4k-native:prepareDawn", "linkDebugExecutableMacosArm64")
    executable = layout.buildDirectory
        .file("bin/macosArm64/debugExecutable/native-smoke.kexe").get().asFile.absolutePath
}

val runSmokeLinuxX64 = tasks.register<Exec>("runSmokeLinuxX64") {
    group = "verification"
    description = "Link and run the static Kotlin/Native Dawn smoke on Linux x64."
    dependsOn(":dawn4k-native:prepareDawn", "linkDebugExecutableLinuxX64")
    executable = layout.buildDirectory
        .file("bin/linuxX64/debugExecutable/native-smoke.kexe").get().asFile.absolutePath
}
