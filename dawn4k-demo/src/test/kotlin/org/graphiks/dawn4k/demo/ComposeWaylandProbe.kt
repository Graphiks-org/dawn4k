package org.graphiks.dawn4k.demo

import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import androidx.compose.ui.window.rememberWindowState
import java.awt.Component
import java.awt.Rectangle
import java.awt.Toolkit
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.StandardCopyOption
import java.util.concurrent.ConcurrentHashMap
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive

/** Qualification entry point only, never dispatched by the shipped application. */
fun main() {
    check(System.getenv("DISPLAY").isNullOrBlank()) { "qualification requires DISPLAY absent" }
    val toolkit = Toolkit.getDefaultToolkit().javaClass.name
    check(toolkit == "sun.awt.wl.WLToolkit") { "native Wayland toolkit required, got $toolkit" }
    val properties = Class.forName("org.jetbrains.skiko.SkikoProperties")
    val renderApi = properties.getMethod("getRenderApi")
        .invoke(properties.getField("INSTANCE").get(null)).toString()
    check(renderApi == "SOFTWARE_COMPAT") { "qualification requires SOFTWARE_COMPAT, got $renderApi" }
    val output = Path.of(requireNotNull(System.getenv("DAWN_UI_PROBE_STATE")))
    val controls = ParticleControls()
    val bounds = ConcurrentHashMap<String, Rectangle>()
    println("[qualification] toolkit=$toolkit renderApi=$renderApi pid=${ProcessHandle.current().pid()}")
    application {
        Window(onCloseRequest = ::exitApplication, undecorated = true,
            state = rememberWindowState(size = DpSize(500.dp, 700.dp)),
            title = "dawn4k Compose Wayland qualification") {
            ParticleControlPanel(controls, ::exitApplication, onControlBounds = { name, rectangle ->
                bounds[name] = rectangle
            })
            LaunchedEffect(Unit) {
                controls.initialize(65536)
                while (isActive) {
                    val state = controls.state.value
                    val buttons = bounds.entries.sortedBy { it.key }.joinToString(",") { (name, r) ->
                        "${quote(name)}:{\"x\":${r.x},\"y\":${r.y},\"width\":${r.width},\"height\":${r.height}}"
                    }
                    val parent = inspectParent(window)
                    val json = """{"toolkit":${quote(toolkit)},"pid":${ProcessHandle.current().pid()},"count":${state.count},"paused":${state.paused},"resetGeneration":${state.resetGeneration},"ready":${state.ready},"error":${state.error?.let(::quote) ?: "null"},"bounds":{$buttons},"parent":$parent}"""
                    Files.createDirectories(output.parent)
                    val temporary = output.resolveSibling("${output.fileName}.tmp")
                    Files.writeString(temporary, json)
                    Files.move(temporary, output, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE)
                    delay(50)
                }
            }
        }
    }
}

private fun quote(value: String): String = "\"" + value.replace("\\", "\\\\")
    .replace("\"", "\\\"").replace("\n", "\\n").replace("\r", "\\r") + "\""

/** Inspected method names are checked against the actual pinned candidate, not guessed handles. */
private fun inspectParent(window: Component): String {
    val toolkit = Class.forName("sun.awt.SunToolkit")
    toolkit.getMethod("awtLock").invoke(null)
    return try {
        val accessorClass = Class.forName("sun.awt.AWTAccessor")
        val accessor = accessorClass.getMethod("getComponentAccessor").invoke(null)
        val api = Class.forName("sun.awt.AWTAccessor\$ComponentAccessor")
        val peer = api.getMethod("getPeer", Component::class.java).invoke(accessor, window)
        if (peer == null) "{\"ready\":false}" else {
            val surface = peer.javaClass.getMethod("getSurface").invoke(peer)
            val pointer = surface?.javaClass?.getMethod("getWlSurfacePtr")?.invoke(surface) as? Long ?: 0L
            val displayClass = Class.forName("sun.awt.wl.WLDisplay")
            val instance = displayClass.getDeclaredMethod("getInstance").apply { isAccessible = true }.invoke(null)
            val display = displayClass.getDeclaredMethod("getDisplayPtr").apply { isAccessible = true }.invoke(instance) as Long
            "{\"ready\":${pointer != 0L && display != 0L},\"display\":$display,\"surface\":$pointer,\"peer\":${quote(peer.javaClass.name)}}"
        }
    } catch (failure: Throwable) {
        "{\"ready\":false,\"error\":${quote(failure.toString())}}"
    } finally { toolkit.getMethod("awtUnlock").invoke(null) }
}
