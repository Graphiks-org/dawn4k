package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.DawnTexture
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.GPUTextureUsage
import org.graphiks.webgpu.descriptors.Extent3D
import org.graphiks.webgpu.descriptors.TextureDescriptor
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.openSession

/**
 * Opens a session the dawn4k way: like [openSession], plus a binding of the session to the dawn
 * instance it opened, so [Dawn4kWebGpuToolkitExtension.dawnContext] and friends resolve **this**
 * session's handles. Startups through the same extension are serialized, which is what makes the
 * binding exact under concurrent opens.
 *
 * A session opened through plain [openSession] instead stays unbound: the dawn accessors reject
 * it, naming this function.
 */
suspend fun openDawnSession(
    extension: Dawn4kWebGpuToolkitExtension,
    config: SessionConfig = SessionConfig(),
    hints: AdapterHints = AdapterHints(),
): Session = extension.startupMutex.withLock {
    val session = openSession(extension, config, hints)
    val instance = extension.currentInstance()
        ?: error("the extension opened no instance for the session it just started")
    extension.attach(session, instance)
    session
}
