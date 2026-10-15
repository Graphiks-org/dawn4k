package org.graphiks.dawn4k.toolkit.examples.app

import android.app.Activity
import android.os.Bundle
import android.widget.LinearLayout
import android.widget.TextView
import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.withDawnSession
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceView
import org.graphiks.dawn4k.toolkit.examples.*

class SampleActivity : Activity() {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private lateinit var surface: DawnSurfaceView
    private lateinit var status: TextView
    private var rendering: Job? = null
    private var cleanup: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        status = TextView(this).apply { text = "Starting compute…"; setTextColor(-1); setPadding(24, 24, 24, 24) }
        surface = DawnSurfaceView(this)
        setContentView(LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            // These accessors also exist on API 28; target 37 draws behind system bars.
            @Suppress("DEPRECATION")
            setOnApplyWindowInsetsListener { root, insets ->
                root.setPadding(insets.systemWindowInsetLeft, insets.systemWindowInsetTop,
                    insets.systemWindowInsetRight, insets.systemWindowInsetBottom)
                insets
            }
            addView(status, LinearLayout.LayoutParams(-1, -2))
            addView(surface, LinearLayout.LayoutParams(-1, 0, 1f))
        })
    }

    override fun onStart() {
        super.onStart()
        status.text = "Starting compute…"
        val previousCleanup = cleanup
        rendering = scope.launch {
            try {
                previousCleanup?.join()
                val values = withDawnSession { runComputeExample(it) }
                check(values.contentEquals(intArrayOf(2, 4, 6, 8)))
                status.text = "Compute: ${values.contentToString()}"
                runSurfaceTriangleExample(surface.target) {
                    scope.launch { status.text = "Compute: ${values.contentToString()} · triangle presented" }
                }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Throwable) { status.text = "Failed: ${failure.message}" }
        }
    }

    override fun onStop() {
        val previous = rendering
        rendering = null
        cleanup = scope.launchViewRetirement(previous) {
            surface.awaitRetirement()
            println("TOOLKIT_SAMPLE android-view-retired")
        }
        super.onStop()
    }

    override fun onDestroy() { scope.cancel(); super.onDestroy() }
}
