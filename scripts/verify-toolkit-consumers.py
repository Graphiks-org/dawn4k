#!/usr/bin/env python3
"""Build a standalone coordinate-only copy of the public toolkit examples.

No project dependency, repository buildSrc, cinterop definition, local Dawn/GLFW
search path or substitute project is made available to this consumer.
"""
import argparse
import json
import os
from pathlib import Path
import platform
import shutil
import subprocess
import tomllib

import toolkit_ci

BUILD = r'''
@file:OptIn(org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeCacheApi::class)
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.jvm.KotlinJvmTarget
plugins {
    kotlin("multiplatform")
    id("com.android.kotlin.multiplatform.library")
}
val toolkitVersion: String by project
kotlin {
    jvmToolchain(25)
    jvm()
    android { namespace = "org.graphiks.toolkit.consumer"; compileSdk = 37; minSdk = 28
        compilerOptions.jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
    macosArm64(); linuxX64(); iosArm64(); iosSimulatorArm64(); iosX64(); tvosArm64(); tvosSimulatorArm64()
    applyDefaultHierarchyTemplate()
    sourceSets.commonMain.dependencies {
        api("org.graphiks:dawn4k-toolkit-surface:$toolkitVersion")
        implementation("org.graphiks:webgpu-descriptors:@WEBGPU@")
    }
    val desktopMain = sourceSets.create("desktopMain") {
        dependsOn(sourceSets.commonMain.get())
        dependencies { implementation("org.graphiks:dawn4k-toolkit-glfw:$toolkitVersion") }
    }
    listOf("jvmMain", "macosArm64Main", "linuxX64Main").forEach { sourceSets.getByName(it).dependsOn(desktopMain) }
    val desktopNativeMain = sourceSets.create("desktopNativeMain") { dependsOn(desktopMain) }
    listOf("macosArm64Main", "linuxX64Main").forEach { sourceSets.getByName(it).dependsOn(desktopNativeMain) }
    val appleMobileMain = sourceSets.create("appleMobileMain") { dependsOn(sourceSets.commonMain.get()) }
    listOf("iosMain", "tvosMain").forEach { sourceSets.getByName(it).dependsOn(appleMobileMain) }
    targets.withType<KotlinNativeTarget>().configureEach {
        binaries.configureEach {
            disableNativeCache(
                org.jetbrains.kotlin.gradle.plugin.mpp.DisableCacheInKotlinVersion.`@KOTLIN_CACHE_VERSION@`,
                "Verify that published Maven klibs link without caches from repository builds",
            )
        }
        if (name in setOf("macosArm64", "linuxX64")) {
            binaries.executable("compute") { entryPoint = "org.graphiks.dawn4k.toolkit.examples.computeMain" }
            binaries.executable("triangle") { entryPoint = "org.graphiks.dawn4k.toolkit.examples.triangleMain" }
        } else binaries.framework { baseName = "ToolkitConsumer"; isStatic = true }
    }
}
val jvm = kotlin.targets.getByName("jvm") as KotlinJvmTarget
tasks.register("verifyConsumerNativeCacheIsolation") {
    doLast {
        tasks.withType<org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink>().forEach { link ->
            // Fail closed if the pinned plugin changes this verification input.
            val settings = link.inputs.properties.entries.single {
                it.key.startsWith("effectiveCacheSettings")
            }.value.toString()
            check(settings == "Configured(kind=NONE)" || "NonCacheableTarget" in settings) {
                "Consumer must link published klibs without Native caches: ${link.path}: $settings"
            }
        }
        println("MAVEN_CONSUMER native-cache=disabled")
    }
}
for ((name, launcher) in listOf("runCompute" to "compute", "runTriangle" to "triangle")) {
    tasks.register<JavaExec>(name) {
        mainClass.set("org.graphiks.dawn4k.toolkit.examples.$launcher.MainKt")
        classpath = files(tasks.named("jvmJar"), jvm.compilations.getByName("main").runtimeDependencyFiles)
        javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
        jvmArgs("--enable-native-access=ALL-UNNAMED")
        if (launcher == "triangle") {
            if (System.getProperty("os.name").lowercase().contains("mac")) jvmArgs("-XstartOnFirstThread")
            args("60")
        }
    }
}
'''


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--maven-root", type=Path, required=True)
    parser.add_argument("--version", default="1.0.0-SNAPSHOT")
    parser.add_argument("--directory", type=Path, default=Path("build/toolkit-consumer"))
    parser.add_argument("--compile-only", action="store_true")
    args = parser.parse_args()
    repo = Path(__file__).resolve().parents[1]
    root = args.directory.resolve()
    root.mkdir(parents=True, exist_ok=True)
    versions = tomllib.loads((repo / "gradle/libs.versions.toml").read_text())["versions"]
    settings = r'''
pluginManagement { repositories { google(); mavenCentral(); gradlePluginPortal() } }
dependencyResolutionManagement {
    repositories {
        maven { url = uri(@MAVEN@); content { includeGroup("org.graphiks") } }
        google(); mavenCentral()
        maven {
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content { includeGroup("org.graphiks"); excludeModuleByRegex("org\\.graphiks", "dawn4k(?:-.*)?") }
        }
    }
}
rootProject.name = "standalone-toolkit-consumer"
include(":consumer", ":androidApp")
'''.replace("@MAVEN@", json.dumps(args.maven_root.resolve().as_uri()))
    (root / "settings.gradle.kts").write_text(settings)
    (root / "build.gradle.kts").write_text('plugins {\n kotlin("multiplatform") version "' + versions["kotlin"] + '" apply false\n id("com.android.kotlin.multiplatform.library") version "' + versions["agp"] + '" apply false\n id("com.android.application") version "' + versions["agp"] + '" apply false\n}\n')
    consumer = root / "consumer"
    consumer.mkdir(exist_ok=True)
    (consumer / "build.gradle.kts").write_text(BUILD.replace("@WEBGPU@", versions["webgpu"])
        .replace("@KOTLIN_CACHE_VERSION@", versions["kotlin"].replace(".", "_")))
    source = repo / "dawn4k-toolkit-examples/src"
    for directory in source.iterdir():
        if directory.is_dir() and directory.name.endswith("Main"):
            shutil.copytree(directory, consumer / "src" / directory.name, dirs_exist_ok=True)
    android = root / "androidApp"
    shutil.copytree(repo / "dawn4k-toolkit-examples/androidApp/src", android / "src", dirs_exist_ok=True)
    app = (repo / "dawn4k-toolkit-examples/androidApp/build.gradle.kts").read_text()
    app = app.replace('project(":dawn4k-toolkit-examples")', 'project(":consumer")')
    app = app.replace('${libs.versions.coroutines.get()}', versions["coroutines"])
    app += '\nandroid { buildTypes.getByName("release") { isMinifyEnabled = true; signingConfig = signingConfigs.getByName("debug"); proguardFiles(getDefaultProguardFile("proguard-android-optimize.txt")) } }\n'
    (android / "build.gradle.kts").write_text(app)
    # Keep standalone compilation reproducible without importing root gradle.properties.
    # Link the published klibs directly, without reusing dependency caches from project builds.
    (root / "gradle.properties").write_text(
        "org.gradle.jvmargs=-Xmx2048m -Dfile.encoding=UTF-8\n"
        "android.useAndroidX=true\nkotlin.native.ignoreDisabledTargets=true\n"
        "kotlin.compiler.execution.strategy=in-process\n")
    targets = ["MacosArm64", "LinuxX64"] if platform.system() == "Darwin" else ["LinuxX64"]
    tasks = [":consumer:verifyConsumerNativeCacheIsolation"]
    tasks += [f":consumer:link{binary}DebugExecutable{target}" for target in targets for binary in ("Compute", "Triangle")]
    if platform.system() == "Darwin":
        tasks += [":consumer:linkDebugFramework" + target for target in ("IosArm64", "IosSimulatorArm64", "IosX64", "TvosArm64", "TvosSimulatorArm64")]
    tasks += [":androidApp:assembleRelease"]
    if not args.compile_only:
        tasks += [":consumer:runCompute", ":consumer:runTriangle"]
    gradlew = repo / ("gradlew.bat" if platform.system() == "Windows" else "gradlew")
    command = [str(gradlew), "-p", str(root), "--daemon", "--console=plain", "--stacktrace", "--max-workers=2", "-PtoolkitVersion=" + args.version,
               "-Dmaven.repo.local=" + str(args.maven_root.resolve()), *tasks]
    log = root / "consumer-build.log"
    with log.open("w") as stream:
        result = subprocess.run(command, stdout=stream, stderr=subprocess.STDOUT)
    print(log.read_text(), flush=True)
    result.check_returncode()
    artifacts = repo / "build/toolkit-consumer-artifacts"
    artifacts.mkdir(parents=True, exist_ok=True)
    for target in targets:
        for binary in ("compute", "triangle"):
            files = list((consumer / "build/bin" / (target[0].lower() + target[1:]) / (binary + "DebugExecutable")).glob("*.kexe"))
            if len(files) != 1:
                raise RuntimeError(f"Missing {target}/{binary} executable: {files}")
            if target == "LinuxX64":
                shutil.copyfile(files[0], artifacts / (binary + ".kexe"))
            elif not args.compile_only:
                environment = os.environ.copy()
                environment["DAWN_EXAMPLE_FRAMES"] = "60"
                marker = toolkit_ci.COMPUTE_RESULT if binary == "compute" else toolkit_ci.SAMPLE_FRAMES_MARKER
                toolkit_ci.run_and_require(files[0], marker, environment)
    apk = android / "build/outputs/apk/release/androidApp-release.apk"
    if not apk.is_file():
        raise RuntimeError("Missing minified, signed consumer APK")
    shutil.copyfile(apk, artifacts / "toolkit-consumer-minified.apk")
    shutil.copyfile(log, artifacts / "consumer-build.log")
    print(f"Coordinate-only consumer build and execution verified: {root}")


if __name__ == "__main__":
    main()
