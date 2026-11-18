package org.graphiks.dawn4k.webgpu.toolkit.internal

import kotlin.time.TimeSource
import kotlin.time.Duration.Companion.seconds

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.yield
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import org.graphiks.dawn4k.webgpu.toolkit.DawnExecutionOwner
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kTargetLease
import org.graphiks.webgpu.GPUAdapter
import org.graphiks.webgpu.GPUFeatureLevel
import org.graphiks.webgpu.GPURequestAdapterOptions
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Progression
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.TargetLease
import org.graphiks.webgpu.toolkit.ToolkitInstance
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle

/**
 * Dawn4k implementation of [ToolkitInstance].
 * 
 * This instance manages a [DawnContext] and provides WebGPU toolkit functionality.
 * Handles are borrowed from the context and never closed by callers.
 * The instance tracks all adapters it creates and closes them on close().
 */
internal class Dawn4kToolkitInstance(
    private val context: DawnContext,
    /**
     * The pinned thread that created [context] and drives it. A dawn4k context processes its
     * events only on that thread, so every native request and the pump itself come back here.
     */
    private val owner: DawnExecutionOwner,
    override val progression: Progression = Progression.EventLoop,
    /**
     * The session configuration this instance was opened with. A surface handle opens its own
     * dawn4k presentation, so it needs the device descriptor and timeouts to open that
     * presentation the way the session asked for.
     */
    private val sessionConfig: SessionConfig = SessionConfig(),
) : ToolkitInstance {
    /** The pinned thread that created [context]; every native call comes back to it. */
    internal val dawnOwner: DawnExecutionOwner get() = owner

    /**
     * The dispatcher of the thread this backend is bound to, which the toolkit runs every
     * device-touching operation on: startup requests, device close, loss await and [close].
     */
    override val executionOwner: CoroutineDispatcher get() = owner.dispatcher

    /**
     * The Dawn context this instance owns, which is what a caller needs to retain an additional
     * native surface on the same device.
     */
    internal val dawnContext: DawnContext get() = context

    /**
     * The adapter the session's device was requested from, or `null` before the request.
     *
     * The toolkit asks for exactly one adapter during startup, so the last one recorded is the
     * session's.
     */
    internal val lastAdapter: DawnAdapter?
        get() = synchronized(adaptersLock) { adapters.lastOrNull() }
    
    
    // Guarded by adaptersLock: requestAdapter is suspend and may be called concurrently,
    // while close() iterates and clears the list from a non-suspend context.
    private val adaptersLock = SynchronizedObject()
    private val adapters = mutableListOf<DawnAdapter>()

    override suspend fun requestAdapter(hints: AdapterHints): Result<GPUAdapter> {
        val options = hints.toRequestAdapterOptions()
        return requestAdapterProgressing(options)
    }
    
    override suspend fun requestAdapter(
        forTarget: TargetLease,
        hints: AdapterHints,
    ): Result<GPUAdapter> {
        // A dawn4k surface reference belongs to the context that created it, so a target-bound
        // adapter would be bound to a surface this context cannot present to. The surface handle
        // opens its own presentation on the retained target instead, and the adapter is chosen
        // for the device that presentation will use.
        return requestAdapterProgressing(hints.toRequestAdapterOptions())
    }

    override fun createSurface(adapter: GPUAdapter, lease: TargetLease): ToolkitSurfaceHandle {
        // Only Dawn4kTargetLease is supported for surface creation
        require(lease is Dawn4kTargetLease) {
            "Only Dawn4kTargetLease is supported for surface creation"
        }
        
        // The handle opens its own dawn4k presentation on the retained target when it acquires
        // its first frame; the configured device is the one the toolkit negotiated.
        val surfaceHandle = Dawn4kSurfaceHandle(
            surfaceTarget = lease.surfaceTarget,
            deviceDescriptor = sessionConfig.deviceDescriptor,
            startupTimeout = sessionConfig.startupTimeout,
            shutdownTimeout = sessionConfig.shutdownTimeout,
        )
        
        return surfaceHandle
    }
    
    override fun pumpEvents() {
        // Delegate to DawnContext.processEvents()
        context.processEvents()
    }

    /**
     * Requests an adapter on the owner's thread, where the pump delivers its result.
     *
     * The adapter is handed back as dawn4k produced it: the toolkit asks it for the device on
     * the same thread, and the pump that runs there settles that request too.
     */
    private suspend fun requestAdapterProgressing(options: GPURequestAdapterOptions?): Result<GPUAdapter> =
        progressing { context.requestAdapter(options) }.onSuccess { adapter ->
            if (adapter is DawnAdapter) synchronized(adaptersLock) { adapters.add(adapter) }
        }

    /**
     * Runs one native request to completion on the owner's thread.
     *
     * A dawn4k request settles through a callback that only [DawnContext.processEvents]
     * delivers, and the pump that delivers it runs on the owner. The request therefore goes to
     * that thread too, where the pump's iterations and the request interleave.
     */
    private suspend fun <T> progressing(request: suspend () -> T): T = try {
        owner.run { request() }
    } catch (f: Throwable) {
        throw f
    }

    /**
     * Starts the pump that drives this context for the life of the instance.
     *
     * `EventLoop` tells the toolkit that this extension drives its own loop, so nothing else
     * pumps. The loop runs on the owner's dispatcher — the thread that created the context —
     * and yields between iterations so requests marshalled onto the same thread progress.
     */
    internal fun startPump() {
        if (pumpJob != null) return
        pumpJob = pumpScope.launch(owner.dispatcher) {
            var dbg = 0
            while (isActive) {
                runCatching { context.processEvents() }
                // Yield to whatever else the owner thread has queued — a marshalled request,
                // for instance — before pumping again: a tight loop here would starve it.
                delay(PUMP_INTERVAL_MILLIS)
            }
        }
    }

    /** Stops the pump started by [startPump]. */
    internal fun stopPump() {
        pumpJob?.cancel()
        pumpJob = null
    }

    private companion object {
        /** Pause between two pump iterations: long enough for queued work to run, short enough for a frame. */
        private const val PUMP_INTERVAL_MILLIS = 1L

        /** How long close drains pending native work before giving up. */
        private val CLOSE_DRAIN_BUDGET = 2.seconds
    }

    /** The scope of the pump; cancelled with the instance. */
    private val pumpScope = CoroutineScope(SupervisorJob() + owner.dispatcher)
    private var pumpJob: Job? = null

    /** One caller runs the teardown; a second close returns immediately instead of racing it. */
    private val closeGuard = atomic(false)
    
    override fun close() {
        // The teardown must run on the owner's thread: the pump lives there and the context
        // refuses to close while one of its processEvents() iterations is still active. The
        // toolkit core marshals this call onto the execution owner, but the raw extension API
        // does not — marshal here so the contract holds for every caller.
        if (!closeGuard.compareAndSet(false, true)) return
        if (owner.isOnOwnerThread()) {
            closeOnOwner()
        } else {
            owner.runBlocking { closeOnOwner() }
        }
    }

    private fun closeOnOwner() {
        try {
            // The pump is stopped first: what remains pending is the device teardown it
            // already posted, and the context refuses a reentrant processEvents.
            stopPump()
            val clock = TimeSource.Monotonic
            val deadline = clock.markNow() + CLOSE_DRAIN_BUDGET
            while (context.hasPendingOperations() && clock.markNow() < deadline) {
                runCatching { context.processEvents() }
            }
            // Close all tracked adapters before closing the context
            val tracked = synchronized(adaptersLock) {
                adapters.toList().also { adapters.clear() }
            }
            tracked.forEach { adapter ->
                runCatching { adapter.close() }
            }
            context.close()
        } finally {
            // The owner thread is released last, and even when the teardown above failed.
            // On Kotlin/Native the worker's close() waits for its queued coroutines, which
            // would deadlock here — this very thread is the owner. Terminate from another.
            CoroutineScope(Dispatchers.Default).launch { owner.close() }
        }
    }
}
/**
 * Maps [AdapterHints] to [GPURequestAdapterOptions].
 * Creates an object implementing the GPURequestAdapterOptions interface with
 * the properties from AdapterHints.
 */
private fun AdapterHints.toRequestAdapterOptions(): GPURequestAdapterOptions? {
    if (powerPreference == null && forceFallbackAdapter == null) {
        return null
    }
    return object : GPURequestAdapterOptions {
        override val powerPreference = this@toRequestAdapterOptions.powerPreference
        override val forceFallbackAdapter: Boolean = this@toRequestAdapterOptions.forceFallbackAdapter ?: false
        // Use Core as default feature level when hints don't specify it
        override val featureLevel: GPUFeatureLevel = org.graphiks.webgpu.GPUFeatureLevel.Core
        override val xrCompatible: Boolean = false
    }
}
