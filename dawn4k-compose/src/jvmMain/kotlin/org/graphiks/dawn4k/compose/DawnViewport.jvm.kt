package org.graphiks.dawn4k.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget

// Desktop placeholder: the desktop composition goes through the toolkit's
// SurfaceTargets (metalLayer, win32, x11, wayland), created by the integrator.
// This actual only bridges the contract; the real desktop composition lands in
// task R4.
@Composable
actual fun DawnViewport(
    target: DawnSurfaceTarget,
    modifier: Modifier,
    onHost: (DawnViewportHost) -> Unit,
) {
    onHost(DawnViewportHost(target, DawnBackend.Vulkan))
}

