package org.graphiks.dawn4k.webgpu.toolkit.testing

import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.webgpu.toolkit.interop.ToolkitSessionView
import org.graphiks.dawn4k.webgpu.toolkit.interop.toSessionConfig

internal expect fun toolkitGpuEnv(name: String): String?
internal expect val toolkitGpuTarget: String
internal expect val toolkitGpuBackend: DawnBackend

/** Acceptance checks never swallow adapter, device, or test-body failures. */
internal object ToolkitGpuEnvironment {
    val backend: DawnBackend get() = toolkitGpuEnv("DAWN_TEST_BACKEND")?.let(DawnBackend::valueOf) ?: toolkitGpuBackend
    val options: DawnSessionOptions get() = DawnSessionOptions(config = DawnConfig(backend))
    val config get() = options.toSessionConfig()

    /** The extension every GPU test opens, configured with the backend under test. */
    fun extension() = Dawn4kWebGpuToolkitExtension(DawnConfig(backend))

    /**
     * Runs [block] on a session opened with [extension], stopping manual progress before the
     * session shuts down.
     *
     * A dawn4k session is `Manual`: it hands its pump to the caller and refuses to shut down
     * while the hand-over is live, so every test must end it explicitly. [withSession] alone
     * cannot, because it shuts down in its `finally` before the caller can stop the driver.
     */
    suspend fun <T> withGpuSession(
        extension: Dawn4kWebGpuToolkitExtension = extension(),
        block: suspend (org.graphiks.webgpu.toolkit.Session) -> T,
    ): T {
        val opened = org.graphiks.webgpu.toolkit.openSession(extension, config)
        try {
            return block(opened)
        } finally {
            opened.manualProgress?.stop()
            opened.shutdown()
        }
    }
    fun report(session: ToolkitSessionView) {
        val required = toolkitGpuEnv("DAWN_REQUIRE_ADAPTER") ?: "1"
        require(required == "0" || required == "1") { "DAWN_REQUIRE_ADAPTER must be 0 or 1" }
        val info = session.adapter.info
        println("TOOLKIT_GPU target=${toolkitGpuEnv("DAWN_TOOLKIT_TARGET") ?: toolkitGpuTarget} backend=$backend required=$required vendor=${info.vendor} device=${info.device} description=${info.description}")
    }
}
