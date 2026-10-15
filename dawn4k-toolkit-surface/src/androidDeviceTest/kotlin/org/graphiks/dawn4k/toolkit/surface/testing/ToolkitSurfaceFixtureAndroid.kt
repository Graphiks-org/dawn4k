package org.graphiks.dawn4k.toolkit.surface.testing

import android.content.Intent
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.dawn4k.toolkit.testing.ToolkitGpuEnvironment

internal actual fun toolkitSurfaceTest(block: suspend (ToolkitSurfaceFixture) -> Unit): Unit = runBlocking {
    val instrumentation = InstrumentationRegistry.getInstrumentation()
    val activity = instrumentation.startActivitySync(
        Intent(instrumentation.targetContext, ToolkitTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
    ) as ToolkitTestActivity
    val view = activity.surfaceView
    try {
        withTimeout(10_000) { view.target.state.first { it.available && it.extent.isPositive } }
        val presented = openDawnSurface(view.target, ToolkitGpuEnvironment.options)
        try {
            presented.gpu.run {
                block(ToolkitSurfaceFixture(presented.gpu, presented.presentation) { width, height ->
                    val previous = view.target.state.value.extent
                    DawnPlatformOwners.androidMain().onOwner {
                        view.layoutParams = view.layoutParams.apply { this.width = width; this.height = height }
                    }
                    withTimeout(10_000) { view.target.state.first { it.extent != previous && it.extent.isPositive } }
                })
            }
        } finally { presented.shutdown() }
    } finally {
        DawnPlatformOwners.androidMain().onOwner { activity.finish() }
        view.awaitRetirement()
    }
}
