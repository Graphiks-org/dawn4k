package org.graphiks.dawn4k.webgpu.toolkit.interop

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.DawnQueue
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.withSession

/**
 * Exposes the dawn4k toolkit's own session API on top of the generic WebGPU toolkit.
 *
 * The two toolkits are both current: dawn4k publishes `DawnSession`, `DawnWindow` and
 * `DawnPresentation`, while the Graphiks toolkit publishes `Session`, `Window` and
 * `ToolkitSurface`. This object lets code written against the dawn4k types run on the generic
 * toolkit, so an application can move to `org.graphiks.webgpu:toolkit-*` without rewriting its
 * render loop.
 *
 * **Important:** the views borrow handles (device, queue, context); the session owns them and
 * closes them. `shutdown()` on a view is a no-op for that reason.
 * 
 * **Note:** `withDawnWindow` needs GLFW, so it lives in the JVM-only [DawnToolkitInteropJvm].
 * 
 * **Usage:**
 * ```kotlin
 * DawnToolkitInterop.withDawnSession { session ->
 *     // session.device, session.queue, session.run { ... }
 * }
 * ```
 */
public object DawnToolkitInterop {
    
    private val extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension()
    
    /**
     * Opens a Dawn session using the new WebGPU toolkit and executes the given block.
     * 
     * Delegates to the toolkit's `withSession`; the session owns the device, queue and
     * context, and the view never closes them.
     * 
     * @param config Session configuration of the new toolkit
     * @param hints Adapter request hints of the new toolkit
     * @param block The code to execute with the session
     * @return The result of the block
     */
    public suspend fun <T> withDawnSession(
        config: SessionConfig = SessionConfig(),
        hints: AdapterHints = AdapterHints(),
        block: suspend CoroutineScope.(ToolkitSessionView) -> T,
    ): T {
        // Delegate to the new toolkit's withSession
        // The new toolkit handles all lifecycle management including cleanup
        return withSession(
            extension = extension,
            config = config,
            hints = hints,
        ) { newSession ->
            val view = ToolkitSessionView(newSession)

            coroutineScope {
                block(view)
            }
        }
    }
}

/**
 * The dawn4k `DawnSession` surface over a toolkit [Session].
 *
 * Every call delegates to the toolkit session, which owns the device, queue and context; the
 * view never closes them. This is not `org.graphiks.dawn4k.toolkit.DawnSession`, it only
 * offers the same members so dawn4k-style code keeps compiling.
 */
public class ToolkitSessionView(
    private val newSession: Session,
) {
    /**
     * The device from the underlying session.
     * This is a DawnDevice that can be used for WebGPU operations.
     */
    public val device: DawnDevice
        get() = newSession.device as DawnDevice
    
    /**
     * The queue from the underlying session.
     * This is a DawnQueue that can be used for WebGPU operations.
     */
    public val queue: DawnQueue
        get() = device.queue as DawnQueue
    
    /**
     * Executes a block with this session as the receiver.
     */
    public suspend fun <T> run(block: suspend CoroutineScope.(ToolkitSessionView) -> T): T {
        return coroutineScope {
            block(this@ToolkitSessionView)
        }
    }
    
    /**
     * Awaits device loss.
     */
    public suspend fun awaitLost(): Result<GPUDeviceLostInfo> {
        return device.awaitLost()
    }
    
    /**
     * Shutdown is a no-op because the new toolkit's withSession handles all lifecycle management.
     * The facade NEVER closes borrowed handles (device/queue/context) - the session owns them.
     */
    public suspend fun shutdown() {
        // No-op: the new toolkit's withSession handles cleanup automatically
        // The facade never closes borrowed handles
    }
}
