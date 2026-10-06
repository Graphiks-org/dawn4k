package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPURequestAdapterOptions

/**
 * The public entry point of the Dawn backend: one context owns one [DawnRuntime]
 * (the WGPUInstance and the native dispatcher). Adapters and the devices they
 * create are requested through it; adapters and sessions close before the
 * context does.
 *
 * The configured [DawnConfig] backend rides along with every adapter request —
 * the public [GPURequestAdapterOptions] carry no backend field; the runtime
 * path merges it into the native request options.
 */
class DawnContext internal constructor(private val runtime: DawnRuntime) : AutoCloseable {

    companion object {
        /** Creates a context owning a fresh Dawn instance and dispatcher. */
        fun create(config: DawnConfig = DawnConfig()): DawnContext = DawnContext(DawnRuntime(config))
    }

    /**
     * Requests an adapter on the context's instance. Every call issues a fresh
     * native adapter request and returns a fresh [DawnAdapter] owning its own
     * reference — there is no shared adapter cache. Business failures arrive
     * as [Result.failure]; a cancellation stays a cancellation.
     */
    suspend fun requestAdapter(options: GPURequestAdapterOptions? = null): Result<GPUAdapter> = try {
        val adapter = runtime.requestAdapter(options)
        Result.success(DawnAdapter(runtime, adapter, options?.forceFallbackAdapter == true))
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    /** Progresses the instance's events once, on the runtime's worker. */
    suspend fun drainEvents() = runtime.drainEvents()

    /**
     * Returns a [NativeBridge] over this context's runtime, for platform
     * integrators (window/surface bridges). Not part of the WebGPU contract:
     * the handles are raw native pointers valid only while this context is
     * open, and [NativeBridge.call] must be used to touch them.
     */
    fun nativeBridge(): NativeBridge = object : NativeBridge {
        override fun <T> call(block: () -> T): T = runtime.dispatcher.call(block)
        override fun instanceHandle(): Long =
            runtime.currentInstance()?.handler?.rawValue ?: 0L
    }

    /**
     * Closes the context's runtime (instance and dispatcher); idempotent.
     * Adapters and device sessions must be closed before it.
     */
    override fun close() = runtime.close()
}
