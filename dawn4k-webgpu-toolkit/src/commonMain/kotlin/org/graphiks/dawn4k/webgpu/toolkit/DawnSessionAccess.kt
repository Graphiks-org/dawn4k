package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kToolkitInstance
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionInitialization
import org.graphiks.webgpu.toolkit.openSession

/**
 * Opens a session the dawn4k way: like [openSession], plus a binding of the session to the dawn
 * instance it opened, so [Dawn4kWebGpuToolkitExtension.dawnContext] and friends resolve **this**
 * session's handles. Startups through the same extension are serialized, which is what makes the
 * binding exact under concurrent opens. The instance is captured inside the startup's
 * preparation phase — immediately after the extension opened it — so a concurrent startup
 * cannot shadow the binding.
 *
 * A session opened through plain [openSession] instead stays unbound: the dawn accessors reject
 * it, naming this function. Interleaving raw [openSession] startups with [openDawnSession] on
 * the same extension is not supported: keep one startup style per extension.
 */
suspend fun openDawnSession(
    extension: Dawn4kWebGpuToolkitExtension,
    config: SessionConfig = SessionConfig(),
    hints: AdapterHints = AdapterHints(),
    initialization: SessionInitialization = SessionInitialization.None,
): Session = extension.startupMutex.withLock {
    var startupInstance: Dawn4kToolkitInstance? = null
    val wrapped = SessionInitialization {
        // Runs inside `openSession`'s startup, right after the extension opened this session's
        // instance: the capture window is too narrow for another startup to shadow it under the
        // serialization above.
        startupInstance = extension.currentInstance()
        initialization.prepare()
    }
    val session = openSession(extension, config, hints, wrapped)
    extension.attach(session, checkNotNull(startupInstance) { "the startup prepared no instance" })
    session
}
