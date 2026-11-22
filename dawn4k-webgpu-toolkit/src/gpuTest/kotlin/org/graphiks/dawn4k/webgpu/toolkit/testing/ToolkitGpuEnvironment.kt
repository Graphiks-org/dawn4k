package org.graphiks.dawn4k.webgpu.toolkit.testing

import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.dawn4k.webgpu.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.webgpu.toolkit.openDawnSession
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig

internal expect fun toolkitGpuEnv(name: String): String?
internal expect val toolkitGpuTarget: String
internal expect val toolkitGpuBackend: DawnBackend

/** Acceptance checks never swallow adapter, device, or test-body failures. */
internal object ToolkitGpuEnvironment {
    val backend: DawnBackend get() = toolkitGpuEnv("DAWN_TEST_BACKEND")?.let(DawnBackend::valueOf) ?: toolkitGpuBackend
    val options: DawnSessionOptions get() = DawnSessionOptions(config = DawnConfig(backend))
    val config: SessionConfig get() = SessionConfig(
        startupTimeout = options.startupTimeout,
        shutdownTimeout = options.shutdownTimeout,
        deviceDescriptor = options.deviceDescriptor,
    )

    /** The extension every GPU test opens, configured with the backend under test. */
    fun extension() = Dawn4kWebGpuToolkitExtension(DawnConfig(backend))

    /**
     * Runs [block] on a session opened with [extension], stopping manual progress before the
     * session shuts down.
     *
     * A dawn4k session is `EventLoop` through this extension: it drives its own pump, so no
     * caller-side driver runs, but the stop is kept for a progression that hands one over.
     */
    suspend fun <T> withGpuSession(
        extension: Dawn4kWebGpuToolkitExtension = extension(),
        block: suspend (Session) -> T,
    ): T {
        val opened = openDawnSession(extension, config)
        try {
            return block(opened)
        } finally {
            opened.manualProgress?.stop()
            opened.shutdown()
        }
    }

    /** Admits [block] as the session's GPU work, on the thread that owns the Dawn context. */
    suspend fun <T> Dawn4kWebGpuToolkitExtension.runGpu(session: Session, block: () -> T): T = session.run {
        withContext(checkNotNull(session.executionOwner) { "the session pins no execution owner" }) { block() }
    }

    fun report(extension: Dawn4kWebGpuToolkitExtension, session: Session) {
        val required = toolkitGpuEnv("DAWN_REQUIRE_ADAPTER") ?: "1"
        require(required == "0" || required == "1") { "DAWN_REQUIRE_ADAPTER must be 0 or 1" }
        val info = extension.dawnAdapter(session).info
        println("TOOLKIT_GPU target=${toolkitGpuEnv("DAWN_TOOLKIT_TARGET") ?: toolkitGpuTarget} backend=$backend required=$required vendor=${info.vendor} device=${info.device} description=${info.description}")
    }
}

/** Admits [block] as the session's GPU work, on the thread that owns the Dawn context. */
internal suspend fun <T> Dawn4kWebGpuToolkitExtension.runGpu(session: Session, block: suspend () -> T): T = session.run {
    withContext(checkNotNull(session.executionOwner) { "the session pins no execution owner" }) { block() }
}
