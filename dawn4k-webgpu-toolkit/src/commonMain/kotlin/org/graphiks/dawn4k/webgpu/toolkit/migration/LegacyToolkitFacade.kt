package org.graphiks.dawn4k.webgpu.toolkit.migration

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.DawnQueue
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
import org.graphiks.webgpu.GPUDeviceLostInfo
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.withSession

/**
 * Migration facade that provides backward compatibility with the old dawn4k-toolkit entry points.
 * 
 * This facade exposes the old `withDawnSession` function but delegates to the new WebGPU
 * toolkit's `withSession` function.
 * 
 * **Important:** The facade NEVER closes borrowed handles (device/queue/context) — the session
 * owns them. This is enforced by the delegation to the new toolkit which manages lifecycle correctly.
 * The wrapper's shutdown() method is a no-op because the new toolkit's withSession handles cleanup.
 * 
 * **Note:** `withDawnWindow` is not available in the common module as it requires GLFW which is
 * JVM-only. Use the JVM-specific overload in `LegacyToolkitFacadeJvm`.
 * 
 * **Usage:**
 * ```kotlin
 * // Old API
 * withDawnSession(options) { session ->
 *     // use session.device, session.queue, etc.
 * }
 * ```
 */
public object LegacyToolkitFacade {
    
    private val extension: Dawn4kWebGpuToolkitExtension = Dawn4kWebGpuToolkitExtension()
    
    /**
     * Opens a Dawn session using the new WebGPU toolkit and executes the given block.
     * 
     * This is a migration facade that delegates to the new toolkit's `withSession` function.
     * The session and its handles (device, queue, context) are managed by the new toolkit
     * and are NOT closed by this facade.
     * 
     * @param options Session configuration (mapped to new SessionConfig)
     * @param block The code to execute with the session
     * @return The result of the block
     */
    public suspend fun <T> withDawnSession(
        options: DawnSessionOptions = DawnSessionOptions(),
        block: suspend CoroutineScope.(LegacyDawnSession) -> T,
    ): T {
        // Map old DawnSessionOptions to new SessionConfig
        val config = options.toSessionConfig()
        
        // Delegate to the new toolkit's withSession
        // The new toolkit handles all lifecycle management including cleanup
        return withSession(
            extension = extension,
            config = config,
            hints = AdapterHints(),
        ) { newSession ->
            // Wrap the new Session in a legacy-compatible wrapper
            val legacySession = LegacyDawnSession(newSession)
            
            // Call the old block with a CoroutineScope receiver
            coroutineScope {
                block(legacySession)
            }
        }
    }
}

/**
 * Legacy-compatible session wrapper that provides the same API as DawnSession.
 * 
 * This wrapper delegates all calls to the underlying new Session and its components.
 * It NEVER closes any handles - the underlying session owns them and the new toolkit
 * handles lifecycle management.
 * 
 * **Important:** This is NOT the actual DawnSession class from dawn4k-toolkit-core.
 * It provides a compatible API surface for migration purposes.
 */
public class LegacyDawnSession(
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
    public suspend fun <T> run(block: suspend CoroutineScope.(LegacyDawnSession) -> T): T {
        return coroutineScope {
            block(this@LegacyDawnSession)
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

/**
 * Maps old DawnSessionOptions to new SessionConfig.
 */
private fun DawnSessionOptions.toSessionConfig(): SessionConfig {
    return SessionConfig(
        // TODO: Map relevant options from DawnSessionOptions to SessionConfig
        // For now, return an empty config as the new toolkit handles defaults
    )
}
