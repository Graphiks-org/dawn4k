package ygdrasil.conventions

import org.gradle.api.provider.ProviderFactory

/**
 * The linuxX64 Dawn carrier embeds an ELF payload through the Linux toolchain and the
 * GLFW archives link Linux X11/Wayland clients, so their producer tasks only run on a
 * Linux x86_64 host. Kotlin/Native cross-compiles linuxX64 from any host, but its
 * cinterop klibs statically link those archives and IDE sync builds every declared
 * target's klibs. The target is therefore declared only where the archives can be
 * produced or when both verified bundles are supplied (`dawn.nativeBundle.linuxX64`,
 * `glfw.nativeBundle.linuxX64`), exactly like CI does. This mirrors `wayland.targets`
 * in `:dawn4k-demo` and keeps foreign-host Gradle imports green.
 */
fun linuxX64TargetEnabled(
    os: String,
    arch: String,
    dawnBundle: String?,
    glfwBundle: String?,
): Boolean {
    val hostBuildsArchives = os.lowercase().contains("linux") &&
        arch.lowercase() in setOf("amd64", "x86_64")
    return hostBuildsArchives || (dawnBundle != null && glfwBundle != null)
}

fun linuxX64TargetEnabled(providers: ProviderFactory): Boolean = linuxX64TargetEnabled(
    os = System.getProperty("os.name").orEmpty(),
    arch = System.getProperty("os.arch").orEmpty(),
    dawnBundle = providers.gradleProperty("dawn.nativeBundle.linuxX64").orNull,
    glfwBundle = providers.gradleProperty("glfw.nativeBundle.linuxX64").orNull,
)
