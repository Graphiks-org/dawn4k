@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.webgpu.toolkit.examples

import kotlinx.cinterop.toKString
import platform.posix.getenv

fun triangleMain() { launchTriangleExample(getenv("DAWN_EXAMPLE_FRAMES")?.toKString()?.toInt() ?: 0) }
