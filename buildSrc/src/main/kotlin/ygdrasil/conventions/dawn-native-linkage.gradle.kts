package ygdrasil.conventions

import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins { id("org.jetbrains.kotlin.multiplatform") }

val dawnNativeProject = project(":dawn4k-native")
kotlin {
    // Resolve the C types referenced by raw bindings. Dawn is embedded once in
    // that dependency's cinterop, including portable linker metadata.
    targets.withType<KotlinNativeTarget>().configureEach {
        val targetName = name
        compilations.getByName("main").cinterops.create("dawn") {
            defFile(dawnNativeProject.file("src/nativeInterop/cinterop/dawn-headers.def"))
            val linkage = if (targetName == "linuxX64") "shared" else "static"
            includeDirs(dawnNativeProject.layout.buildDirectory.dir("native/$targetName/$linkage/include"))
        }
    }
}
tasks.matching { it.name.startsWith("cinteropDawn") }.configureEach {
    dependsOn(":dawn4k-native:prepareDawn")
}
