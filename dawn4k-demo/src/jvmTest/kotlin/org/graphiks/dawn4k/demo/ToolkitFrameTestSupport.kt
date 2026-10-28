package org.graphiks.dawn4k.demo

import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import org.graphiks.dawn4k.DawnCommandEncoder
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.DawnPresentation
import org.graphiks.webgpu.*
import org.graphiks.webgpu.descriptors.*

internal suspend fun presentDemoTestFrame(gpu: DawnSession, presentation: DawnPresentation,
    beforePresent: suspend () -> Unit = {}): Boolean = presentation.withFrame { frame ->
    (gpu.device.createCommandEncoder() as DawnCommandEncoder).use { encoder ->
        encoder.beginRenderPass(RenderPassDescriptor(colorAttachments = listOf(
            RenderPassColorAttachment(frame.view, loadOp = GPULoadOp.Clear, storeOp = GPUStoreOp.Store,
                clearValue = Color(0.0, 0.0, 1.0, 1.0)),
        ))).use { it.end() }
        encoder.finish().use { gpu.queue.submit(listOf(it)) }
    }
    gpu.queue.onSubmittedWorkDone().getOrThrow()
    beforePresent()
    currentCoroutineContext().ensureActive()
    frame.present()
}
