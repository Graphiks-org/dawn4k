@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.examples

import kotlinx.cinterop.toKString
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.withDawnSession
import platform.posix.getenv
import kotlin.time.Duration.Companion.seconds

fun computeMain() = runBlocking {
    withDawnSession(DawnSessionOptions(startupTimeout = 60.seconds)) {
        println(runComputeExample(it).contentToString())
    }
}
fun triangleMain() { launchTriangleExample(getenv("DAWN_EXAMPLE_FRAMES")?.toKString()?.toInt() ?: 0) }
