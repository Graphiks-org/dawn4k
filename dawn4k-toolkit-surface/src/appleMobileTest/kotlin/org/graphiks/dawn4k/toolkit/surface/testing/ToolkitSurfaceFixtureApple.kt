@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface.testing

import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.dawn4k.toolkit.testing.ToolkitGpuEnvironment
import platform.CoreGraphics.CGRectMake
import platform.QuartzCore.CAMetalLayer

internal actual fun toolkitSurfaceTest(block: suspend (ToolkitSurfaceFixture) -> Unit) = appleMainTest {
    val owner = DawnPlatformOwners.appleMain()
    val view = owner.onOwner {
        DawnMetalView(CGRectMake(0.0, 0.0, 32.0, 32.0)).apply {
            contentScaleFactor = 1.0
            layoutSubviews()
        }
    }
    val state = MutableStateFlow(DawnTargetState(1, PixelExtent(32, 32), true))
    val target = metalLayerTarget(owner.onOwner { view.layer as CAMetalLayer }, state)
    val presented = openDawnSurface(target, ToolkitGpuEnvironment.options)
    try {
        presented.gpu.run {
            block(ToolkitSurfaceFixture(presented.gpu, presented.presentation) { width, height ->
                owner.onOwner {
                    view.setFrame(CGRectMake(0.0, 0.0, width.toDouble(), height.toDouble()))
                    view.layoutSubviews()
                    state.value = state.value.copy(extent = PixelExtent(width, height))
                }
            })
        }
    } finally { presented.shutdown() }
}
