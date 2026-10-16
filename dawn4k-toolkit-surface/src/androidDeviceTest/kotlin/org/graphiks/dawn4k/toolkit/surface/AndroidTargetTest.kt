package org.graphiks.dawn4k.toolkit.surface

import android.app.Activity
import android.content.Intent
import android.graphics.SurfaceTexture
import android.os.Bundle
import android.view.Surface
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.surface.internal.TargetLifecycle
import kotlin.test.*

class AndroidTargetTest {
    private fun states() = MutableStateFlow(DawnTargetState(1, PixelExtent(16, 16), true))

    @Test fun releasedSurfaceIsRejected(): Unit = runBlocking {
        val texture = SurfaceTexture(false)
        val surface = Surface(texture)
        try {
            surface.release()
            assertFailsWith<IllegalArgumentException> {
                androidSurfaceTarget(surface, DawnPlatformOwners.androidMain(), states()).retain()
            }
        } finally { texture.release() }
    }

    @Test fun javaSurfaceReleaseDoesNotFreeRetainedNativeWindow() = runBlocking {
        val texture = SurfaceTexture(false)
        texture.setDefaultBufferSize(16, 16)
        val surface = Surface(texture)
        var lease: DawnTargetLease? = null
        try {
            lease = androidSurfaceTarget(surface, DawnPlatformOwners.androidMain(), states()).retain()
            val nativeLease = lease as AndroidTargetLease
            assertNotEquals(0L, nativeLease.nativeWindow)
            surface.release()
            assertTrue(AndroidNativeWindow.width(nativeLease.nativeWindow) > 0)
            assertNotEquals(0L, nativeLease.nativeWindow)
            lease.retire()
            lease.retire()
            assertEquals(0L, nativeLease.nativeWindow)
        } finally { lease?.retire(); surface.release(); texture.release() }
    }

    @Test fun oldSurfaceDestroyedCannotRetireNewGeneration() {
        val lifecycle = TargetLifecycle()
        val oldGeneration = lifecycle.available(PixelExtent(16, 16))
        val newGeneration = lifecycle.available(PixelExtent(32, 24))
        lifecycle.invalidate(oldGeneration)
        assertEquals(newGeneration, lifecycle.state.value.generation)
        assertTrue(lifecycle.state.value.available)
    }

    @Test fun destroyAcknowledgesFrameRetirement() = runBlocking {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val activity = instrumentation.startActivitySync(
            Intent(instrumentation.targetContext, ToolkitTestActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
        ) as ToolkitTestActivity
        val view = activity.surfaceView
        withTimeout(10_000) { view.target.state.first { it.available && it.extent.isPositive } }
        val presented = openDawnSurface(view.target)
        val frameEntered = CompletableDeferred<Unit>()
        val releaseFrame = CompletableDeferred<Unit>()
        var rendering: Deferred<Boolean>? = null
        try {
            rendering = async {
                presented.presentation.withFrame { frameEntered.complete(Unit); releaseFrame.await() }
            }
            withTimeout(10_000) { frameEntered.await() }
            DawnPlatformOwners.androidMain().onOwner { activity.finish() }
            withTimeout(5_000) { view.target.state.first { !it.available } }
            val acknowledgement = async { view.awaitRetirement() }
            delay(20)
            assertFalse(acknowledgement.isCompleted)
            releaseFrame.complete(Unit)
            assertTrue(rendering.await())
            withTimeout(10_000) { acknowledgement.await() }
        } finally {
            releaseFrame.complete(Unit)
            rendering?.join()
            presented.shutdown()
            DawnPlatformOwners.androidMain().onOwner { activity.finish() }
        }
    }
}

class ToolkitTestActivity : Activity() {
    lateinit var surfaceView: DawnSurfaceView
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        surfaceView = DawnSurfaceView(this)
        setContentView(surfaceView)
    }
}
