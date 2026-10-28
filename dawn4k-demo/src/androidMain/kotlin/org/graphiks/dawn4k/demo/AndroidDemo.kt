package org.graphiks.dawn4k.demo

import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import kotlinx.coroutines.*
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceView

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
            val viewport = remember { mutableStateOf<DawnSurfaceView?>(null) }
            AndroidView(
                factory = { ctx -> DawnSurfaceView(ctx).also { viewport.value = it } },
                modifier = modifier,
            )
            // The toolkit view owns its surface lifecycle; the bridge only adapts it to the
            // session controller, attaching while available and detaching on invalidation.
            LaunchedEffect(viewport.value) {
                val view = viewport.value ?: return@LaunchedEffect
                bridgeToolkitView(controller, DawnBackend.Vulkan, view.target, view::awaitRetirement)
            }
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
