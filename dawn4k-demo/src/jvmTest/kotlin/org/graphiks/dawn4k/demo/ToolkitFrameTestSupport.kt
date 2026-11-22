package org.graphiks.dawn4k.demo

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnPresentation
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*
import org.graphiks.webgpu.toolkit.Session

internal suspend fun presentDemoTestFrame(session: Session, presentation: DawnPresentation,
    beforePresent: suspend () -> Unit = {}): Boolean = presentation.withFrame { frame ->
    (session.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
        encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
            RenderPassColorAttachment(frame.view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                clearValue = Color(0.0, 0.0, 1.0, 1.0)),
        ))).use { it.end() }
        encoder.finish().use { session.queue.submit(listOf(it)) }
    }
    session.queue.onSubmittedWorkDone().getOrThrow()
    beforePresent()
    currentCoroutineContext().ensureActive()
    frame.present()
}
