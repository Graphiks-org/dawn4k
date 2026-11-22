@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnMetalView
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import platform.CoreGraphics.CGRectMake

/**
 * Puts a toolkit [DawnMetalView] under the Compose content and reports its host
 * on the main thread. The toolkit view owns its Metal layer, its layout updates
 * and its asynchronous retirement; the host forwards its state, and the
 * composable creates the view with a zero frame — UIKit lays it out.
 */
@Composable
actual fun DawnViewport(
    target: DawnSurfaceTarget,
    modifier: Modifier,
    onHost: (DawnViewportHost) -> Unit,
) {
    val currentOnHost by rememberUpdatedState(onHost)
    UIKitView(
        factory = {
            val view = DawnMetalView(CGRectMake(0.0, 0.0, 0.0, 0.0))
            currentOnHost(DawnViewportHost(view.target, DawnBackend.Metal))
            view
        },
        modifier = modifier,
        onRelease = { },
    )
}

