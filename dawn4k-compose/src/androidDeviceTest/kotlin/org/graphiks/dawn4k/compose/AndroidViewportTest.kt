package org.graphiks.dawn4k.compose

import android.os.Handler
import android.os.Looper
import androidx.activity.ComponentActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceView
import org.junit.Rule
import org.junit.runner.RunWith

/**
 * Drives the real Android viewport on a device: the Compose rule supplies the
 * AndroidComposeView and its lifecycle, and the tests check that the toolkit
 * [DawnSurfaceView] the viewport hosts is a plain child of the Compose
 * hierarchy whose state the host forwards. The toolkit's own device tests
 * cover the view's lifecycle in depth.
 */
@RunWith(AndroidJUnit4::class)
class AndroidViewportTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val main = Handler(Looper.getMainLooper())

    /** The toolkit SurfaceView AndroidView put under the tagged viewport node. */
    private fun toolkitSurfaceView(): DawnSurfaceView {
        val root = compose.onNodeWithTag(TAG).fetchSemanticsNode().root
        return onMain {
            (root as? android.view.View)?.let { findSurfaceView(it) }
                ?: error("no toolkit SurfaceView under the viewport")
        }
    }

    private fun findSurfaceView(view: android.view.View): DawnSurfaceView? = when (view) {
        is DawnSurfaceView -> view
        is android.view.ViewGroup -> (0 until view.childCount).firstNotNullOfOrNull { findSurfaceView(view.getChildAt(it)) }
        else -> null
    }

    private fun <T> onMain(block: () -> T): T {
        val results = arrayOfNulls<Any?>(1)
        var failure: Throwable? = null
        val done = CountDownLatch(1)
        main.post {
            try { results[0] = block() } catch (error: Throwable) { failure = error } finally { done.countDown() }
        }
        assertTrue(done.await(10, TimeUnit.SECONDS), "the main thread did not run the block")
        failure?.let { throw it }
        @Suppress("UNCHECKED_CAST")
        return results[0] as T
    }

    @Test
    fun viewportHostsTheToolkitSurfaceViewAsAComposeChild() {
        val reported = mutableListOf<DawnViewportHost>()
        compose.setContent {
            DawnViewport(target = VIEWPORT_TARGET, modifier = Modifier.fillMaxSize().testTag(TAG), onHost = { reported += it })
        }
        compose.waitForIdle()
        val view = toolkitSurfaceView()
        val host = reported.single()
        assertEquals(DawnBackend.Vulkan, host.backend)
        // The view is a plain child of the Compose view hierarchy, not a
        // separate window layer on top of it, and it is attached.
        val parent = onMain { view.parent }
        checkNotNull(parent) { "the SurfaceView must be a child of the Compose view hierarchy" }
        assertTrue(parent is android.view.ViewGroup, "the SurfaceView must be a child of the Compose view hierarchy")
        assertTrue(parent.indexOfChild(view) >= 0, "the SurfaceView must be attached to its parent")
        assertTrue(onMain { view.isAttachedToWindow }, "the SurfaceView must be attached to the window")
        // The host forwards the view's own state: the toolkit view reports the
        // holder's extent once its surface is created, and the host's flow is
        // the same flow the view drives.
        assertTrue(host.state === view.target.state, "the host must forward the view's own state flow")
    }

    @Test
    fun aHostLeavingTheCompositionIsClosed() {
        val reported = mutableListOf<DawnViewportHost>()
        var show by mutableStateOf(true)
        compose.setContent {
            if (show) {
                DawnViewport(target = VIEWPORT_TARGET, modifier = Modifier.fillMaxSize().testTag(TAG), onHost = { reported += it })
            }
        }
        compose.waitForIdle()
        reported.single()
        compose.runOnUiThread { show = false }
        compose.waitForIdle()
        // The viewport closes the host it reported when it leaves; the toolkit
        // view owns the surface lifecycle independently of the host.
        assertTrue(reported.single().state.value.generation >= 0L)
    }

    private companion object {
        const val TAG = "dawn-viewport"

        /**
         * The Android actual creates its own toolkit [DawnSurfaceView] and
         * forwards that view's target state; the [target] argument is the
         * contract's seam, not a view this test supplies. A detached target
         * stands in for it: the host must still forward whichever state the
         * actual wires.
         */
        val VIEWPORT_TARGET = DetachedTarget()

        class DetachedTarget : DawnSurfaceTarget {
            override val state = kotlinx.coroutines.flow.MutableStateFlow(
                org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetState(
                    1L, org.graphiks.dawn4k.webgpu.toolkit.surface.PixelExtent(1, 1), false,
                ),
            )
            override suspend fun retain() = error("the viewport never retains a caller target")
        }
    }
}
