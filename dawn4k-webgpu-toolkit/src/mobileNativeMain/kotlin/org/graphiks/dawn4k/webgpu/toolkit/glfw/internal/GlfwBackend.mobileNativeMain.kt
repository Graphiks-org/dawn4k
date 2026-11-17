package org.graphiks.dawn4k.webgpu.toolkit.glfw.internal

import org.graphiks.dawn4k.webgpu.toolkit.glfw.DawnMainThreadLoop

/**
 * GLFW has no mobile build, so the desktop window layer is unavailable on iOS and tvOS.
 *
 * These actuals exist because the common declarations are `expect`; reaching one means the
 * caller tried to open a desktop window on a mobile target, which is a programming error rather
 * than a runtime condition.
 */
private const val MOBILE_MESSAGE =
    "GLFW windows are desktop-only; iOS and tvOS present through DawnMetalView instead"

internal actual fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend =
    throw UnsupportedOperationException(MOBILE_MESSAGE)

internal actual fun requireGlfwMainThread(): Unit = throw UnsupportedOperationException(MOBILE_MESSAGE)

internal actual fun pauseGlfwMainThread(): Unit = throw UnsupportedOperationException(MOBILE_MESSAGE)

internal actual fun writeGlfwStandardError(message: String): Unit =
    throw UnsupportedOperationException(MOBILE_MESSAGE)
