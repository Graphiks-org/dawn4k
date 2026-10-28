@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.*
import kotlinx.cinterop.useContents
import org.graphiks.dawn4k.toolkit.surface.DawnMetalView
import platform.CoreGraphics.CGRectMake
import platform.UIKit.*

/** UIKit owns the view. GPU cleanup suspends without blocking UIKit's main queue. */
fun ToolkitExampleViewController(): UIViewController = object : UIViewController(nibName = null, bundle = null) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var rendering: Job? = null
    private var cleanup: Job? = null
    private lateinit var metal: DawnMetalView
    private lateinit var status: UILabel

    override fun loadView() {
        metal = DawnMetalView(CGRectMake(0.0, 0.0, 640.0, 480.0))
        view = metal
        status = UILabel(CGRectMake(24.0, 50.0, 1100.0, 80.0)).apply {
            text = "Starting compute…"
            numberOfLines = 0
            textColor = UIColor.whiteColor
        }
        metal.addSubview(status)
    }

    override fun viewDidLayoutSubviews() {
        super.viewDidLayoutSubviews()
        status.setFrame(CGRectMake(24.0, 50.0, (metal.bounds.useContents { size.width } - 48.0).coerceAtLeast(0.0), 100.0))
    }

    override fun viewDidAppear(animated: Boolean) {
        super.viewDidAppear(animated)
        if (rendering != null) return
        status.text = "Starting compute…"
        rendering = scope.launchToolkitSample({ metal.target }, cleanup) { status.text = it }
    }

    override fun viewDidDisappear(animated: Boolean) {
        super.viewDidDisappear(animated)
        val previous = rendering
        rendering = null
        cleanup = scope.launchViewRetirement(previous) {
            metal.awaitRetirement()
            println("TOOLKIT_SAMPLE apple-view-retired")
        }
    }
}
