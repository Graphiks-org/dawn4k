package org.graphiks.dawn4k.toolkit.testing

import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*

internal expect fun toolkitGpuEnv(name: String): String?
internal expect val toolkitGpuTarget: String
internal expect val toolkitGpuBackend: DawnBackend

/** Acceptance checks never swallow adapter, device, or test-body failures. */
internal object ToolkitGpuEnvironment {
    val backend: DawnBackend get() = toolkitGpuEnv("DAWN_TEST_BACKEND")?.let(DawnBackend::valueOf) ?: toolkitGpuBackend
    val options: DawnSessionOptions get() = DawnSessionOptions(config = DawnConfig(backend))
    fun report(session: DawnSession) {
        val required = toolkitGpuEnv("DAWN_REQUIRE_ADAPTER") ?: "1"
        require(required == "0" || required == "1") { "DAWN_REQUIRE_ADAPTER must be 0 or 1" }
        val info = session.adapter.info
        println("TOOLKIT_GPU target=${toolkitGpuEnv("DAWN_TOOLKIT_TARGET") ?: toolkitGpuTarget} backend=$backend required=$required vendor=${info.vendor} device=${info.device} description=${info.description}")
    }
}
