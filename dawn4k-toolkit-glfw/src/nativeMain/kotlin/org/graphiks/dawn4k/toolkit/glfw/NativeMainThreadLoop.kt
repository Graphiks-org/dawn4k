@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw.internal

import platform.posix.usleep

internal actual fun pauseGlfwMainThread() { usleep(1_000u) }
