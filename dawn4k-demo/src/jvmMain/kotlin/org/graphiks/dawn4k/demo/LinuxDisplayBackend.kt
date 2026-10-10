package org.graphiks.dawn4k.demo

internal enum class LinuxDisplayBackend { Wayland, X11 }

/** Pure selection: connection failure must be reported by the selected host, not retried as X11. */
internal fun selectLinuxDisplayBackend(
    environment: Map<String, String>,
    forced: String? = null,
): LinuxDisplayBackend {
    val wayland = !environment["WAYLAND_DISPLAY"].isNullOrBlank() ||
        !environment["WAYLAND_SOCKET"].isNullOrBlank()
    val x11 = !environment["DISPLAY"].isNullOrBlank()
    val hint = "launch from a graphical session with its display environment"
    return when (forced) {
        "wayland" -> {
            check(wayland) { "missing WAYLAND_DISPLAY or WAYLAND_SOCKET; $hint" }
            LinuxDisplayBackend.Wayland
        }
        "x11" -> {
            check(x11) { "missing DISPLAY for X11; $hint" }
            LinuxDisplayBackend.X11
        }
        null -> when {
            wayland -> LinuxDisplayBackend.Wayland
            x11 -> LinuxDisplayBackend.X11
            else -> error("missing WAYLAND_DISPLAY/WAYLAND_SOCKET and DISPLAY; $hint")
        }
        else -> throw IllegalArgumentException("unsupported platform '$forced'; use wayland or x11")
    }
}

internal fun parseLinuxBackendOverride(args: Array<String>, platform: DemoPlatform): String? {
    if (args.isEmpty()) return null
    require(platform == DemoPlatform.Linux) { "--platform is supported only on Linux" }
    require(args.size == 1 && args[0] in setOf("--platform=wayland", "--platform=x11")) {
        "usage: dawn4k-demo [--platform=wayland|--platform=x11]"
    }
    return args[0].substringAfter('=')
}
