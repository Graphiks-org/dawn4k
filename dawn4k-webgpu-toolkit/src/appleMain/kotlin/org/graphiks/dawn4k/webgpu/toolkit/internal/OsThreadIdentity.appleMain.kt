@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.webgpu.toolkit.internal

import kotlinx.cinterop.rawValue
import platform.posix.pthread_self

internal actual fun currentOsThreadId(): ULong = pthread_self()!!.rawValue.toLong().toULong()
