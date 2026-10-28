@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface.testing

import org.graphiks.dawn4k.toolkit.glfw.c.*

internal actual fun toolkitDesktopPresentationKind(): String = when (glfwGetPlatform()) {
    GLFW_PLATFORM_COCOA -> "metal"
    GLFW_PLATFORM_WIN32 -> "win32"
    GLFW_PLATFORM_X11 -> "x11"
    GLFW_PLATFORM_WAYLAND -> "wayland"
    else -> error("No active desktop presentation platform")
}
