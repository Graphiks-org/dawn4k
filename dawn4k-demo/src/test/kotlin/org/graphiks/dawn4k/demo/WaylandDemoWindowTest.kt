package org.graphiks.dawn4k.demo

import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class WaylandDemoWindowTest {
    @Test fun rendersParticlesAndAppliesControlsThroughTheSharedRunner() {
        if (!waylandTestsEnabled()) return
        val controls = ParticleControls()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val original = System.out
        val trace = ByteArrayOutputStream()
        val capture = PrintStream(trace, true)
        System.setOut(capture)
        val host = WaylandSurfaceHost.open(controls) { scope.cancel() }
        try {
            awaitWaylandSize(host)
            val rendering = scope.launch { runParticleDemo(host, controls, true) }
            try {
                awaitWaylandTrace(trace, controls, "first frame") { it.contains("frame 1 rendered") }
                controls.togglePause()
                controls.selectCount(256)
                awaitWaylandTrace(trace, controls, "paused count") { it.contains("particles=256, paused=true, delta=0") }
                controls.reset()
                awaitWaylandTrace(trace, controls, "reset") { it.contains("scene reset (256 particles)") }
                controls.togglePause()
                awaitWaylandTrace(trace, controls, "resume") { it.contains("particles=256, paused=false") }
                assertEquals(null, controls.state.value.error)
            } finally {
                scope.cancel()
                runBlocking { withTimeout(10_000) { rendering.join() } }
            }
        } finally {
            scope.cancel()
            host.close()
            System.setOut(original)
            original.print(trace.toString())
            capture.close()
        }
        assertTrue(Thread.getAllStackTraces().keys.none {
            it.isAlive && it.name.startsWith("dawn-wayland-owner-")
        })
    }
}

internal fun awaitWaylandTrace(trace: ByteArrayOutputStream, controls: ParticleControls,
    description: String, predicate: (String) -> Boolean) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(60)
    while (System.nanoTime() < deadline) {
        controls.state.value.error?.let { error("$description failed: $it\n$trace") }
        if (predicate(trace.toString())) return
        Thread.sleep(20)
    }
    error("$description timed out\n$trace")
}
