package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.NativeBridge

class WaylandSurfaceTest {
    private val noNativeCalls = object : NativeBridge {
        override fun <T> call(block: () -> T): T = error("invalid handles must not call native code")
        override fun instanceHandle(): Long = error("invalid handles must not access instance")
    }
    @Test fun rejectsNullDisplayOrSurfaceBeforeCallingNativeCode() {
        assertFailsWith<IllegalArgumentException> { DawnSurface.createWayland(noNativeCalls, 1L, 0L, 1L) }
        assertFailsWith<IllegalArgumentException> { DawnSurface.createWayland(noNativeCalls, 1L, 1L, 0L) }
    }

    @Test fun negotiatesPresentsAndResizesARealNativeSurface() = runBlocking {
        if (!waylandTestsEnabled()) return@runBlocking
        assertTrue(System.getenv("DISPLAY").isNullOrBlank(), "Wayland validation must not use DISPLAY")
        WaylandSurfaceHost.open(ParticleControls()) {}.use { host ->
            val initial = awaitWaylandSize(host)
            DawnContext.create(DawnConfig(backend = DawnBackend.Vulkan, implicitDeviceSynchronization = true)).useWithDemoEventProgress { context, adapter, device ->
                    println("[test] Wayland Vulkan adapter: ${adapter.info}")
                        host.createSurface(context.nativeBridge(), device.nativeHandle()).use { surface ->
                            surface.configureForAdapter((adapter as DawnAdapter).nativeHandle())
                            surface.configure(initial.first, initial.second)
                            repeat(3) { surface.acquireFrame().use { surface.present(it) } }
                            val resize = ProcessBuilder("swaymsg", "[app_id=\"org.graphiks.dawn4k.demo\" pid=${ProcessHandle.current().pid()}] resize set width 900 px height 600 px").start()
                            assertTrue(resize.waitFor(5, TimeUnit.SECONDS))
                            assertEquals(0, resize.exitValue())
                            val changed = awaitWaylandSize(host) { it.first > 0 && it.second > 0 && it != initial }
                            surface.configure(changed.first, changed.second)
                            surface.acquireFrame().use { surface.present(it) }
                            assertEquals(changed.first, surface.width)
                            assertEquals(changed.second, surface.height)
                        }
            }
        }
    }
}
