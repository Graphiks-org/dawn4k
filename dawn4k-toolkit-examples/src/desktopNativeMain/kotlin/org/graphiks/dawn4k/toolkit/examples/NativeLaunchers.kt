@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.examples

import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.withDawnSession
import platform.posix.getenv

fun computeMain() = runBlocking { withDawnSession { println(runComputeExample(it).contentToString()) } }
fun triangleMain() { launchTriangleExample(getenv("DAWN_EXAMPLE_FRAMES")?.toKString()?.toInt() ?: 0) }
