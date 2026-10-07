package org.graphiks.dawn4k.demo

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.unit.dp
import androidx.compose.ui.awt.ComposeWindow
import java.awt.Rectangle
import java.util.concurrent.atomic.AtomicReference
import kotlin.math.roundToInt
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.jetbrains.skiko.MainUIDispatcher

@Composable
internal fun DemoApp(
    window: ComposeWindow,
    controls: ParticleControls = remember { ParticleControls() },
    onClose: () -> Unit = { window.dispose() },
) {
    val platform = remember { requireNotNull(detectDemoPlatform(System.getProperty("os.name"))) }
    val viewport = remember { AtomicReference(Rectangle()) }
    if (platform != DemoPlatform.MacOS) {
        Row(Modifier.fillMaxSize().background(Color(0xFF101820))) {
            Box(Modifier.width(300.dp).fillMaxHeight()) { ParticleControlPanel(controls, onClose) }
            Box(Modifier.weight(1f).fillMaxHeight().onGloballyPositioned {
                val bounds = it.boundsInWindow()
                viewport.set(Rectangle(bounds.left.roundToInt(), bounds.top.roundToInt(),
                    bounds.width.roundToInt(), bounds.height.roundToInt()))
            })
        }
    } else {
        ParticleControlPanel(controls, onClose)
    }

    // LaunchedEffect drives the GPU setup + render loop off the UI thread's
    // composition, but the actual native calls go through the bridge (worker).
    LaunchedEffect(window, controls) {
        try {
            withContext(Dispatchers.Default) {
                runDemo(window, controls, platform, viewport)
            }
        } catch (cancelled: CancellationException) {
            throw cancelled
        } catch (failure: Throwable) {
            failure.printStackTrace()
            controls.fail(failure.message ?: failure.toString())
        }
    }
}

/**
 * Opens the platform host, Dawn context and device, then renders the particle
 * scene until the composition is cancelled. The native host outlives the surface.
 */
private suspend fun runDemo(
    window: ComposeWindow, controls: ParticleControls, platform: DemoPlatform,
    viewport: AtomicReference<Rectangle>,
) {
    val host: SurfaceHost = when (platform) {
        DemoPlatform.MacOS -> awaitMetalLayerHost(window)
        DemoPlatform.Windows -> awaitWindowsSurfaceHost(window, viewport)
        DemoPlatform.Linux -> awaitLinuxSurfaceHost(window, viewport)
    }
    host.use {
        runParticleDemo(host, controls, negotiateCapabilities = platform == DemoPlatform.Linux)
    }
}

/** Keep handle lookup and native retention in one EDT operation, excluding disposal. */
@OptIn(org.graphiks.kffi.objc.PlatformAvailability::class)
internal suspend fun awaitMetalLayerHost(window: ComposeWindow): MetalLayerHost {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
        var host: MetalLayerHost? = null
        try {
            withContext(MainUIDispatcher) {
                if (!window.isDisplayable) throw CancellationException("the Compose window was closed")
                if (window.isShowing) {
                    val handle = window.windowHandle
                    if (handle != 0L) host = onAppKitThread {
                        val view = org.graphiks.kffi.objc.NSWindow(
                            java.lang.foreign.MemorySegment.ofAddress(handle)
                        ).contentView()
                        MetalLayerHost.attach(view.address())
                    }
                }
            }
        } catch (failure: Throwable) {
            // withContext may discard its result on cancellation after native attachment.
            host?.close()
            throw failure
        }
        host?.let { return it }
        delay(50)
    }
    error("timed out waiting for the Compose window's native handle")
}

internal suspend fun awaitWindowsSurfaceHost(
    window: ComposeWindow, viewport: AtomicReference<Rectangle>,
): WindowsSurfaceHost {
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
        var host: WindowsSurfaceHost? = null
        try {
            withContext(MainUIDispatcher) {
                if (!window.isDisplayable) throw CancellationException("the Compose window was closed")
                if (window.isShowing && window.windowHandle != 0L) {
                    host = WindowsSurfaceHost.attach(window.windowHandle, viewport)
                }
            }
        } catch (failure: Throwable) {
            host?.close()
            throw failure
        }
        host?.let { return it }
        delay(50)
    }
    error("timed out waiting for the Compose window's native handle")
}

internal suspend fun awaitLinuxSurfaceHost(
    window: ComposeWindow, viewport: AtomicReference<Rectangle>,
): LinuxSurfaceHost {
    check(!System.getenv("DISPLAY").isNullOrEmpty()) {
        "missing DISPLAY; launch through dawn4k-demo/docker's /opt/demo/demo.sh"
    }
    val deadline = System.nanoTime() + 5_000_000_000L
    while (System.nanoTime() < deadline) {
        var host: LinuxSurfaceHost? = null
        try {
            withContext(MainUIDispatcher) {
                if (!window.isDisplayable) throw CancellationException("the Compose window was closed")
                if (window.isShowing && window.windowHandle != 0L) {
                    host = LinuxSurfaceHost.attach(window.windowHandle, viewport)
                }
            }
        } catch (failure: Throwable) {
            host?.close()
            throw failure
        }
        host?.let { return it }
        delay(50)
    }
    error("timed out waiting for the Compose window's X11 handle")
}
