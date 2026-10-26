package org.graphiks.dawn4k.demo

import kotlin.time.Duration.Companion.seconds

import kotlin.test.*
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*

class WaylandSurfaceTest {
    @Test fun rejectsNullDisplayOrSurfaceBeforeCallingNativeCode() {
        val owner = DawnPlatformOwners.awt()
        val states = MutableStateFlow(DawnTargetState(1, org.graphiks.dawn4k.toolkit.surface.PixelExtent(16, 16), true))
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.wayland(0L, 1L, owner, states, CountingTargetRetention()) }
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.wayland(1L, 0L, owner, states, CountingTargetRetention()) }
    }

    @Test fun negotiatesPresentsAndResizesARealNativeSurface(): Unit = runBlocking {
        if (!waylandTestsEnabled()) return@runBlocking
        assertTrue(System.getenv("DISPLAY").isNullOrBlank(), "Wayland validation must not use DISPLAY")
        WaylandSurfaceHost.open(ParticleControls()) {}.use { host ->
            val initial = awaitWaylandSize(host)
            val lease = DesktopSurfaceLease(1, host)
            lease.pixelExtent()
            try {
                withDawnSurface(lease.target, DawnSessionOptions(config = DawnConfig(DawnBackend.Vulkan), startupTimeout = 60.seconds)) { gpu, presentation ->
                    repeat(3) { assertTrue(presentDemoTestFrame(gpu, presentation)) }
                    val device = gpu.device
                    val resize = ProcessBuilder("swaymsg", "[app_id=\"org.graphiks.dawn4k.demo\" pid=${ProcessHandle.current().pid()}] resize set width 900 px height 600 px").start()
                    assertTrue(resize.waitFor(5, TimeUnit.SECONDS))
                    assertEquals(0, resize.exitValue())
                    val changed = awaitWaylandSize(host) { it.first > 0 && it.second > 0 && it != initial }
                    lease.pixelExtent()
                    assertTrue(presentDemoTestFrame(gpu, presentation))
                    assertEquals(changed.first, presentation.configuration.value?.extent?.width)
                    assertEquals(changed.second, presentation.configuration.value?.extent?.height)
                    assertSame(device, gpu.device)
                }
            } finally { withContext(NonCancellable) { lease.awaitRetirement() } }
        }
    }
}
