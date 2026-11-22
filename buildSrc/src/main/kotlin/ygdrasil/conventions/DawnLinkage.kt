package ygdrasil.conventions

import org.gradle.api.Project
import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget
import org.jetbrains.kotlin.gradle.tasks.KotlinNativeLink

/**
 * The Apple frameworks Dawn's Metal backend links against.
 *
 * This list is the single source of truth for every module that links Dawn into
 * a native binary. A framework missing here makes a consumer's binary fail to
 * link with an opaque `Undefined symbols` error, so a module must never keep its
 * own copy.
 */
val dawnAppleFrameworks: List<String> = listOf(
    "Metal", "Foundation", "CoreGraphics", "QuartzCore", "IOKit", "IOSurface",
)

/**
 * Links the staged Dawn archive for this target into every binary of this
 * target, using the archive flavor [linkage] publishes.
 *
 * `:dawn4k` configures its own binaries this way; a module whose *test* binary
 * references Dawn symbols (through `:dawn4k`'s klib) must call this too, because
 * linker options do not flow through a project dependency. Both callers read
 * [dawnAppleFrameworks], so the two can never drift.
 *
 * [dawnNativeProject] is the `:dawn4k-native` project that stages the archives.
 * Every link task of this target depends on its `prepareDawn` task, because the
 * archive must exist before the link runs; the dependency is wired on the link
 * task type, so debug, release and framework links are all covered.
 */
fun KotlinNativeTarget.applyDawnLinkage(
    dawnNativeProject: Project,
    linkage: String,
) {
    val dawnLibDir = dawnNativeProject.layout.buildDirectory
        .dir("native/$name/$linkage/lib").get().asFile.absolutePath
    binaries.all {
        linkerOpts("-L$dawnLibDir", "-lwebgpu_dawn")
        when (linkage) {
            // The desktop targets link the shared flavor, so the runtime loader
            // must find the library next to the binary's build path.
            "shared" -> {
                linkerOpts("-rpath", dawnLibDir)
                if (name.startsWith("linux")) {
                    // Dawn's Linux build needs these; the Apple targets get the
                    // frameworks instead.
                    linkerOpts("-lpthread", "-ldl", "-lm")
                } else {
                    dawnAppleFrameworks.forEach { linkerOpts("-framework", it) }
                }
            }
            // iOS/tvOS only publish the static flavor: the frameworks are still
            // needed, but there is no shared library to find at run time.
            "static" -> dawnAppleFrameworks.forEach { linkerOpts("-framework", it) }
            else -> error("Unknown Dawn linkage flavor '$linkage' for target $name")
        }
    }
    // Every link task of this target consumes the archive, so every one of them
    // depends on the task that stages it. The link task names are
    // `link<Binary><Target>`, so the target name identifies them; matching the
    // task type keeps this off unrelated tasks, and it covers debug, release
    // and framework links alike.
    val targetSuffix = name.replaceFirstChar { it.uppercaseChar() }
    project.tasks.withType(KotlinNativeLink::class.java).matching {
        it.name.endsWith(targetSuffix)
    }.configureEach {
        dependsOn(dawnNativeProject.tasks.named("prepareDawn"))
    }
}
