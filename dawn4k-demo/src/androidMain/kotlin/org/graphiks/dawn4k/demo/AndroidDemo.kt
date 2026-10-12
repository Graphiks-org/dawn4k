package org.graphiks.dawn4k.demo

import android.content.Context
import android.view.SurfaceHolder
import android.view.SurfaceView
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import java.util.concurrent.atomic.AtomicLong

/** Packaging activity delegates here; simulation and controls stay in the KMP module. */
@Composable
fun AndroidDemo() {
    val scope = rememberCoroutineScope()
    val controller = remember { DemoSessionController(scope) }
    AndroidDemo(controller)
}

@Composable
internal fun AndroidDemo(controller: DemoSessionController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val context = LocalContext.current
    val generations = remember { AtomicLong() }
    DisposableEffect(lifecycle, controller) {
        val observer = LifecycleEventObserver { _, _ ->
            controller.lifecycle(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        controller.lifecycle(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        onDispose { lifecycle.removeObserver(observer) }
    }
    ParticleDemoContent(controller, true, {
        scope.launch {
            if (controller.state.value.phase in listOf(DemoPhase.Stopped, DemoPhase.Failed)) controller.restart()
            else controller.stop()
        }
    }, {}, { modifier ->
        key(state.id) {
            AndroidView(factory = { ctx ->
                ParticleSurfaceView(ctx, controller, generations)
            }, modifier = modifier)
        }
    })
    // Private app sandbox evidence for qualification; no exported IPC or server.
    LaunchedEffect(controller) {
        while (isActive) {
            val evidence = controller.evidence.snapshot()
            context.filesDir.resolve("demo-evidence.json").writeText(evidence.toJson())
            delay(100)
        }
    }
}

private class ParticleSurfaceView(
    context: Context,
    private val controller: DemoSessionController,
    private val generations: AtomicLong,
) : SurfaceView(context), SurfaceHolder.Callback {
    private var lease: AndroidSurfaceHost? = null
    init { holder.addCallback(this) }
    override fun surfaceCreated(holder: SurfaceHolder) {
        try {
            val created = AndroidSurfaceHost(generations.incrementAndGet(), holder.surface,
                PixelExtent(width, height))
            lease = created
            controller.attach(created)
        } catch (failure: Throwable) {
            controller.failed(controller.state.value.id, failure.message ?: failure.toString())
        }
    }
    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        lease?.resize(width, height)
    }
    override fun surfaceDestroyed(holder: SurfaceHolder) {
        val retired = lease ?: return
        lease = null
        retired.invalidate()
        // Never block Android's main thread; controller retires under frame lock.
        CoroutineScope(Dispatchers.Default).launch {
            try { controller.detach(retired.generation) }
            finally { retired.close() }
        }
    }
}
