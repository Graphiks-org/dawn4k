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
                swayCommand("[app_id=\"org.graphiks.dawn4k.demo\" pid=${ProcessHandle.current().pid()}] focus")
                nativeCommand("wtype", "-k", "space", "-k", "minus", "-k", "minus")
                awaitWaylandTrace(trace, controls, "paused count") { it.contains("particles=256, paused=true, delta=0") }
                swayCommand("input type:keyboard xkb_layout fr")
                try {
                    nativeCommand("wtype", "+")
                    awaitWaylandTrace(trace, controls, "plus under another layout") { it.contains("particles=1024, paused=true") }
                    nativeCommand("wtype", "-k", "minus")
                    awaitWaylandCondition("count returned to 256") { controls.state.value.count == 256 }
                    nativeCommand("wtype", "R")
                } finally { swayCommand("input type:keyboard xkb_layout us") }
                awaitWaylandTrace(trace, controls, "reset") { it.contains("scene reset (256 particles)") }
                val criteria = "[app_id=\"org.graphiks.dawn4k.demo\" pid=${ProcessHandle.current().pid()}]"
                for (width in listOf(900, 850, 950)) {
                    swayCommand("$criteria resize set width $width px height 600 px")
                    awaitWaylandTrace(trace, controls, "resize and presentation at $width") {
                        it.contains("configuring surface ${width}x600") && it.contains("rendered (${width}x600,")
                    }
                }
                swayCommand("output HEADLESS-1 scale 2")
                try {
                    awaitWaylandTrace(trace, controls, "integer scale 2") {
                        it.contains("configuring surface 1900x1200") && it.contains("rendered (1900x1200,")
                    }
                    assertEquals(1900 to 1200, host.pixelSize())
                } finally { swayCommand("output HEADLESS-1 scale 1") }
                awaitWaylandCondition("scale restored") { host.pixelSize() == (950 to 600) }
                val paused = controls.state.value
                swayCommand("[app_id=\"foot\"] focus")
                Thread.sleep(100)
                assertEquals(paused, controls.state.value)
                swayCommand("$criteria focus")
                nativeCommand("wtype", "-k", "space")
                awaitWaylandTrace(trace, controls, "resume") { it.contains("particles=256, paused=false") }
                assertEquals(null, controls.state.value.error)
                nativeCommand("wtype", "-k", "Escape")
                runBlocking { withTimeout(10_000) { rendering.join() } }
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

    @Test fun cancellationDuringGpuStartupDoesNotLeakNativeOwners() {
        if (!waylandTestsEnabled()) return
        repeat(3) {
            val controls = ParticleControls()
            val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
            val host = WaylandSurfaceHost.open(controls) { scope.cancel() }
            try {
                awaitWaylandSize(host)
                val rendering = scope.launch { runParticleDemo(host, controls, true) }
                scope.cancel()
                runBlocking { withTimeout(10_000) { rendering.join() } }
            } finally { scope.cancel(); host.close() }
        }
        assertTrue(Thread.getAllStackTraces().keys.none {
            it.isAlive && it.name.startsWith("dawn-wayland-owner-")
        })
    }
}

internal fun nativeCommand(vararg command: String) {
    val process = ProcessBuilder(*command).redirectErrorStream(true).start()
    assertTrue(process.waitFor(5, TimeUnit.SECONDS), "native command timed out: ${command.toList()}")
    assertEquals(0, process.exitValue(), process.inputStream.bufferedReader().readText())
}

internal fun swayCommand(command: String) = nativeCommand("swaymsg", command)

internal fun awaitWaylandCondition(description: String, predicate: () -> Boolean) {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
    while (System.nanoTime() < deadline) {
        if (predicate()) return
        Thread.sleep(20)
    }
    error("$description timed out")
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
