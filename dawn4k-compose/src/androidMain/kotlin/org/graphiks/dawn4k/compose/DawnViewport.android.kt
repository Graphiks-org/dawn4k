package org.graphiks.dawn4k.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceView

/**
 * Puts a toolkit [DawnSurfaceView] under the Compose content and reports its
 * host on the main thread. The toolkit view owns the SurfaceHolder lifecycle —
 * generations, availability and asynchronous retirement — and the host forwards
 * its state; the composable creates the view, hands its toolkit target to the
 * host, and lets Compose release the view.
 */
@Composable
actual fun DawnViewport(
    target: DawnSurfaceTarget,
    modifier: Modifier,
    onHost: (DawnViewportHost) -> Unit,
) {
    val currentOnHost by rememberUpdatedState(onHost)
    AndroidView(
        factory = { context ->
            val view = DawnSurfaceView(context)
            currentOnHost(DawnViewportHost(view.target, DawnBackend.Vulkan))
            view
        },
        modifier = modifier,
        onRelease = { },
    )
}

