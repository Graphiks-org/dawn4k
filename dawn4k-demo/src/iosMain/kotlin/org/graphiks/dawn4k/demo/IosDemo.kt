@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.UIKitView
import androidx.compose.ui.window.ComposeUIViewController
import kotlinx.coroutines.*
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSString
import platform.Foundation.NSUTF8StringEncoding
import platform.Foundation.NSUserDomainMask
import platform.Foundation.writeToFile
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIApplicationState
import platform.UIKit.UIApplicationWillResignActiveNotification
import platform.UIKit.UIViewController
import platform.QuartzCore.CAMetalLayer

/** Packaging entry point; Swift contains neither controls nor simulation. */
fun DemoViewController(): UIViewController = ComposeUIViewController {
    val scope = rememberCoroutineScope()
    val controller = remember { DemoSessionController(scope) }
    Box(Modifier.fillMaxSize().windowInsetsPadding(WindowInsets.safeDrawing)) { IosDemo(controller) }
}

@Composable
private fun IosDemo(controller: DemoSessionController) {
    val state by controller.state.collectAsState()
    val scope = rememberCoroutineScope()
    var nextGeneration by remember { mutableStateOf(0L) }
    DisposableEffect(controller) {
        val center = NSNotificationCenter.defaultCenter
        val active = center.addObserverForName(UIApplicationDidBecomeActiveNotification, null, null) {
            controller.lifecycle(true)
        }
        val inactive = center.addObserverForName(UIApplicationWillResignActiveNotification, null, null) {
            controller.lifecycle(false)
        }
        controller.lifecycle(UIApplication.sharedApplication.applicationState == UIApplicationState.UIApplicationStateActive)
        onDispose {
            center.removeObserver(active)
            center.removeObserver(inactive)
        }
    }
    ParticleDemoContent(controller, true, {
        scope.launch {
            if (controller.state.value.phase in listOf(DemoPhase.Stopped, DemoPhase.Failed)) controller.restart()
            else controller.stop()
        }
    }, {}, { modifier ->
        key(state.id) {
            var lease: IosSurfaceHost? = null
            fun retire() {
                val old = lease ?: return
                lease = null
                old.invalidate()
                // Never block UIKit. The lease retains its layer through native cleanup.
                CoroutineScope(Dispatchers.Default).launch {
                    try { controller.detach(old.generation) }
                    finally { old.close() }
                }
            }
            UIKitView(
                factory = {
                    IosMetalView({ view, extent ->
                        val current = lease
                        if (current != null) current.resize(extent)
                        else {
                            val created = IosSurfaceHost(++nextGeneration, view.layer as CAMetalLayer, extent)
                            lease = created
                            controller.attach(created)
                        }
                    }, ::retire)
                },
                modifier = modifier,
                onRelease = { it.retire() },
            )
        }
    })
    LaunchedEffect(controller) {
        val directory = NSFileManager.defaultManager.URLsForDirectory(NSDocumentDirectory, NSUserDomainMask)
            .first() as platform.Foundation.NSURL
        val path = checkNotNull(directory.path) + "/demo-evidence.json"
        while (isActive) {
            (controller.evidence.snapshot().toJson() as NSString).writeToFile(path, true, NSUTF8StringEncoding, null)
            delay(100)
        }
    }
}
