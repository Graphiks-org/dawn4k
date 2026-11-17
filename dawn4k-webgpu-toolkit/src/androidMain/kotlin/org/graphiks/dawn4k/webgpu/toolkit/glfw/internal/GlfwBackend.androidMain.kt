package org.graphiks.dawn4k.webgpu.toolkit.glfw.internal

import org.graphiks.dawn4k.webgpu.toolkit.glfw.DawnMainThreadLoop

/**
 * GLFW has no Android build, so the desktop window layer is unavailable there.
 *
 * These actuals exist because the common declarations are `expect`; reaching one means the
 * caller tried to open a desktop window on Android, which is a programming error rather than a
 * runtime condition. Android presents through DawnSurfaceView instead.
 */
private const val ANDROID_MESSAGE =
    "GLFW windows are desktop-only; Android presents through DawnSurfaceView instead"

internal actual fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend =
    throw UnsupportedOperationException(ANDROID_MESSAGE)

internal actual fun requireGlfwMainThread(): Unit = throw UnsupportedOperationException(ANDROID_MESSAGE)

internal actual fun pauseGlfwMainThread(): Unit = throw UnsupportedOperationException(ANDROID_MESSAGE)

internal actual fun writeGlfwStandardError(message: String): Unit =
    throw UnsupportedOperationException(ANDROID_MESSAGE)
