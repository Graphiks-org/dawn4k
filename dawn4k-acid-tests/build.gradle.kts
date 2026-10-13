import org.gradle.api.tasks.testing.Test
import org.gradle.api.tasks.testing.AbstractTestTask
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.targets.native.tasks.KotlinNativeTest
import groovy.json.JsonSlurper
import org.jetbrains.kotlin.gradle.testing.internal.KotlinTestReport

plugins {
    kotlin("multiplatform")
    alias(libs.plugins.kotlin.serialization)
    id("ygdrasil.conventions.dawn-jvm-runtime")
}

kotlin {
    jvmToolchain(25)
    jvm()
    macosArm64()
    linuxX64()
    applyDefaultHierarchyTemplate()
    sourceSets {
        commonMain.dependencies {
            implementation(project(":dawn4k"))
            implementation(libs.suite.acid.tests)
            implementation(libs.webgpu.descriptors)
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.serialization.json)
        }
        commonTest.dependencies {
            implementation(kotlin("test"))
        }
        jvmTest.dependencies { implementation(kotlin("test-junit")) }
    }
    targets.withType<KotlinNativeTarget>().configureEach {
        val dawn = project(":dawn4k-native")
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(dawn.file("src/nativeInterop/cinterop/dawn.def"))
            includeDirs(dawn.layout.buildDirectory.dir("native/$targetName/shared/include"))
        }
        binaries.all {
            val lib = dawn.layout.buildDirectory.dir("native/$targetName/shared/lib").get().asFile.absolutePath
            linkerOpts("-L$lib", "-lwebgpu_dawn", "-rpath", lib)
            if (targetName == "macosArm64") {
                listOf("Metal", "Foundation", "CoreGraphics", "QuartzCore", "IOKit", "IOSurface").forEach {
                    linkerOpts("-framework", it)
                }
            } else {
                linkerOpts("-lpthread", "-ldl", "-lm")
            }
        }
    }
}

tasks.matching { it.name.startsWith("cinteropDawn") || it.name.startsWith("linkDebugTest") }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}
tasks.withType<Test>().configureEach { jvmArgs("--enable-native-access=ALL-UNNAMED") }

val advisoryResults = providers.gradleProperty("dawn.acid.ignoreFailures").map { it.toBoolean() }.orElse(false)
tasks.withType<KotlinTestReport>().configureEach {
    // Kotlin resets this flag when it wires aggregate reporting into the task graph.
    doFirst { ignoreFailures = advisoryResults.get() }
}

tasks.withType<AbstractTestTask>().configureEach {
    // Advisory in CI only; local execution remains strict by default.
    ignoreFailures = advisoryResults.get()
    testLogging.showStandardStreams = true
    listOf("DAWN_TEST_BACKEND", "DAWN_REQUIRE_ADAPTER", "VK_DRIVER_FILES", "VK_ICD_FILENAMES").forEach {
        inputs.property(it, providers.environmentVariable(it).orElse(""))
    }
    outputs.upToDateWhen { false }
    outputs.doNotCacheIf("GPU availability must be checked at execution time") { true }
    val report = layout.buildDirectory.file("reports/acid/$name.json")
    when (this) {
        is Test -> {
            environment("DAWN_ACID_REPORT_PATH", report.get().asFile.absolutePath)
        }
        is KotlinNativeTest -> environment("DAWN_ACID_REPORT_PATH", report.get().asFile.absolutePath)
    }
    if (ignoreFailures && (this is Test || this is KotlinNativeTest)) {
        doLast {
            val output = report.get().asFile
            check(output.isFile) { "Acid execution did not produce a report: $output" }
            val result = JsonSlurper().parse(output) as Map<*, *>
            check(result["complete"] == true) { "Acid execution is incomplete: $output" }
            logger.lifecycle("Advisory acid results for $name: ${result["summary"]}; report: $output")
        }
    }
    doFirst {
        report.get().asFile.apply {
            parentFile.mkdirs()
            check(!exists() || delete()) { "Cannot remove stale acid report: $this" }
        }
    }
}

if (!System.getProperty("os.name").lowercase().contains("linux")) {
    tasks.matching { it.name == "linkDebugTestLinuxX64" || it.name == "linuxX64Test" }.configureEach {
        enabled = false
    }
}
