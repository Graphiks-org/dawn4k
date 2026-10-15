@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw.internal

import cnames.structs.GLFWwindow
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.glfw.DawnMainThreadLoop
import org.graphiks.dawn4k.toolkit.glfw.c.*
import org.graphiks.dawn4k.toolkit.surface.*
import platform.posix.getpid
import platform.posix.syscall

internal actual fun requireGlfwMainThread() {
    // Linux x64 SYS_gettid. Calling syscall also works on the compiler's older glibc sysroot.
    check(syscall(186) == getpid().toLong()) { "GLFW requires the process main thread" }
}

internal actual fun nativeSurfaceTarget(
    window: CPointer<GLFWwindow>, platform: GlfwPlatform, main: DawnMainThreadLoop,
    states: MutableStateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget = when (platform) {
    GlfwPlatform.Wayland -> DawnSurfaceTargets.wayland(
        checkNotNull(dawn_glfw_wayland_display()).rawValue.toLong(),
        checkNotNull(dawn_glfw_wayland_window(window)).rawValue.toLong(), main.owner, states, retention,
    )
    GlfwPlatform.X11 -> DawnSurfaceTargets.x11(
        checkNotNull(dawn_glfw_x11_display()).rawValue.toLong(),
        dawn_glfw_x11_window(window), main.owner, states, retention,
    )
    else -> throw UnsupportedOperationException("Linux requires an active GLFW X11 or Wayland platform")
}
