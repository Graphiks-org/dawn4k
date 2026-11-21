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
        // A dawn4k session is `Manual`: its driver ends before the session may shut down.
        session.manualProgress?.stop()
        session.shutdown()
    }
}
