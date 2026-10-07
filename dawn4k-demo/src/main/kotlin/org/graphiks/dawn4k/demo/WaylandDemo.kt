package org.graphiks.dawn4k.demo

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.cancel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeout
import java.util.concurrent.atomic.AtomicBoolean

/** Native application entry point: no Compose, AWT or Xlib initialization. */
internal fun runWaylandDemo() {
    val controls = ParticleControls()
    val closeRequested = AtomicBoolean()
    try {
        runBlocking {
            coroutineScope {
                val host = WaylandSurfaceHost.open(controls) {
                    closeRequested.set(true)
                    cancel("Wayland window closed or connection failed")
                }
                host.use {
                    withTimeout(10_000) {
                        while (host.pixelSize().first <= 0) delay(10)
                    }
                    withContext(Dispatchers.Default) { runParticleDemo(host, controls, true) }
                }
            }
        }
    } catch (cancelled: CancellationException) {
        // Close/Escape are normal exits; a connection failure remains a nonzero exit.
        finishWaylandCancellation(cancelled, closeRequested.get(), controls.state.value.error)
        println("[demo] Wayland window closed")
    }
}

/** A setup timeout or unrelated cancellation must not masquerade as the user's Close action. */
internal fun finishWaylandCancellation(cancelled: CancellationException, closeRequested: Boolean, failure: String?) {
    failure?.let { throw IllegalStateException(it, cancelled) }
    if (cancelled is TimeoutCancellationException || !closeRequested) throw cancelled
}
