package org.graphiks.dawn4k.webgpu.toolkit.examples

import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.openSession
import kotlin.time.Duration.Companion.seconds

/** Shared by the JVM and native desktop launchers; invoke on process main. */
fun computeMain() = runBlocking {
    val extension = Dawn4kWebGpuToolkitExtension()
    val session = openSession(extension, SessionConfig(startupTimeout = 60.seconds))
    try {
        println(runComputeExample(session).contentToString())
    } finally {
        // The session drives its own pump; stopping manual progress keeps shutdown admissible
        // for a progression that handed a driver over (none for the EventLoop extension).
        session.manualProgress?.stop()
        session.shutdown()
    }
}
