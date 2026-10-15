package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.glfw.*

/** Invoke on process main; the GLFW loop remains there while encoding uses the GPU owner. */
fun launchTriangleExample(frameLimit: Int = 0) = runBlocking { withDawnWindow(
    windowOptions = DawnWindowOptions(title = "Dawn4k toolkit triangle"),
) { gpu, window, presentation ->
    var closeRequested = false
    val closeWatcher = launch { window.events.first { it == DawnWindowEvent.CloseRequested }; closeRequested = true }
    try { renderTriangleFrames(gpu, presentation, frameLimit, { !closeRequested }) }
    finally { closeWatcher.cancelAndJoin() }
}
}
