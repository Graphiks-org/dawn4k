package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.*

/** The caller owns this job and the UI target; cancel/join it before awaiting view retirement. */
suspend fun runSurfaceTriangleExample(target: DawnSurfaceTarget, onFrame: () -> Unit = {}) {
    target.state.first { it.available && it.extent.width > 0 && it.extent.height > 0 }
    withDawnSurface(target) { gpu, presentation ->
        renderTriangleFrames(gpu, presentation, onFrame = onFrame)
    }
}

internal suspend fun renderTriangleFrames(
    gpu: DawnSession,
    presentation: DawnPresentation,
    frameLimit: Int = 0,
    keepRendering: () -> Boolean = { true },
    onFrame: () -> Unit = {},
) {
    require(frameLimit >= 0)
    val renderer = TriangleRenderer(gpu, checkNotNull(presentation.configuration.value).format)
    var frames = 0
    try {
        while (currentCoroutineContext().isActive && keepRendering() && (frameLimit == 0 || frames < frameLimit)) {
            if (presentation.withFrame { frame -> renderer.render(frame.view, frame.extent); frame.present() }) {
                frames++
                if (frames == 1) { println("TOOLKIT_SAMPLE presented=1"); onFrame() }
            }
            delay(16)
        }
        println("TOOLKIT_SAMPLE frames=$frames")
    } finally {
        withContext(NonCancellable) {
            try { gpu.queue.onSubmittedWorkDone().getOrThrow() }
            finally { renderer.close() }
        }
    }
}
