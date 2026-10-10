package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPURequestAdapterOptions

/**
 * The public entry point of the Dawn backend: one context owns one [DawnRuntime]
 * (the WGPUInstance, without scheduling). Adapters and the devices they
 * create are requested through it; adapters and sessions close before the
 * context does.
 *
 * The configured [DawnConfig] backend rides along with every adapter request —
 * the public [GPURequestAdapterOptions] carry no backend field; the runtime
 * path merges it into the native request options.
 */
class DawnContext internal constructor(private val runtime: DawnRuntime) : AutoCloseable {

    companion object {
        /** Creates a fresh Dawn instance without creating any thread or event pump. */
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

    /** Runs one event iteration and callback settlements on the caller. Never concurrent/reentrant. */
    fun processEvents() = runtime.processEvents()

    /** Snapshot, not a shutdown barrier: stop producers before using this to drain. */
    fun hasPendingOperations(): Boolean = runtime.hasPendingOperations()

    @Deprecated("Use processEvents(); progression is caller-owned")
    suspend fun drainEvents() = processEvents()

    /**
     * Returns a [NativeBridge] over this context's runtime, for platform
     * integrators (window/surface bridges). Not part of the WebGPU contract:
     * the handles are raw native pointers valid only while this context is
     * open. Calls are inline; integrators own synchronization and lifetime.
     */
    fun nativeBridge(): NativeBridge = object : NativeBridge {
        @Deprecated("Executes inline without synchronization or scheduling")
        override fun <T> call(block: () -> T): T = block()
        override fun instanceHandle(): Long =
            runtime.currentInstance()?.handler?.rawValue ?: 0L
    }

    /**
     * Closes the instance; idempotent after successful closure. Stop/join
     * producers, settle operations, close children and drain device teardown
     * first. Refuses active progression, pending operations or open children.
     */
    override fun close() = runtime.close()
}
