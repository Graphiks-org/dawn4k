package org.graphiks.dawn4k.webgpu.toolkit.glfw

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import org.graphiks.dawn4k.webgpu.toolkit.glfw.internal.*
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnPlatformOwner
import kotlin.coroutines.CoroutineContext

/** A running process-main-thread owner. Persistent windows cannot outlive this scope. */
class DawnMainThreadLoop private constructor(overrideBackend: GlfwBackend?) {
    private val lock = SynchronizedObject()
    private val queue = ArrayDeque<Runnable>()
    private var stopped = false
    private var stopRequested = false
    private val dispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            synchronized(lock) { check(!stopped) { "GLFW main loop is stopped" }; queue.addLast(block) }
        }
    }
    /** The GLFW main-thread owner: run all native GLFW work through [DawnPlatformOwner.onOwner], exactly like the backend does. */
    val owner = DawnPlatformOwner.fromDispatcher(dispatcher)
    internal val backend = overrideBackend ?: createGlfwBackend(this)
    internal val runtime = GlfwRuntime(backend)
    private val windows = mutableSetOf<DawnWindow>()
    private val cleanup = mutableMapOf<DawnWindow, Job>()
    private val partialCleanup = mutableSetOf<Job>()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    internal fun register(window: DawnWindow) { windows += window }
    internal fun unregister(window: DawnWindow) { windows -= window; cleanup.remove(window) }

    internal suspend fun retireFailedStartup(retire: suspend () -> Unit) = owner.onOwner {
        val job = scope.launch {
            try {
                while (true) {
                    try { retire(); break }
                    catch (failure: Throwable) {
                        // Unbounded retry is deliberate (transient platform states) — but never silent.
                        writeGlfwStandardError(
                            "GLFW: failed startup cleanup, retrying in 100 ms:\n${failure.stackTraceToString()}\n",
                        )
                        currentCoroutineContext().ensureActive(); delay(100)
                    }
                }
            } finally { partialCleanup.remove(currentCoroutineContext().job) }
        }
        partialCleanup += job
    }

    private fun drain() {
        while (true) {
            val work = synchronized(lock) { queue.removeFirstOrNull() } ?: return
            work.run()
        }
    }

    /**
     * Serves the calling thread — the process main thread — until [stopServing]: it drains
     * dispatched work, polls events, and on a stop shuts every registered window down before
     * returning, exactly like the session mode does at the end of its block. The loop is
     * reusable after a serve ends; serving and [run] share the one-loop-per-process gate.
     */
    internal fun serve() {
        requireGlfwMainThread()
        check(active.compareAndSet(false, true)) { "a GLFW main loop is already running" }
        try {
            while (!stopRequested || windows.isNotEmpty() || partialCleanup.isNotEmpty()) {
                drain()
                if (runtime.isActive) backend.pollEvents()
                if (stopRequested) beginRetirement()
                pauseGlfwMainThread()
            }
        } finally {
            scope.cancel()
            drain()
            synchronized(lock) { stopped = true }
            active.value = false
        }
    }

    /** Asks a served loop to end; harmless before and after a serve. */
    internal fun stopServing() {
        synchronized(lock) { stopRequested = true }
    }

    /** Begins the retirement of every registered window; re-launches nothing still retiring. */
    private fun beginRetirement() {
        windows.toList().forEach { window ->
            if (cleanup[window]?.isActive != true) {
                cleanup[window] = scope.launch(Dispatchers.Default) {
                    window.gpu.manualProgress?.stop()
                    runCatching { window.shutdown() }
                    delay(10)
                }
            }
        }
    }

    companion object {
        private val active = atomic(false)

        fun <T> run(block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T = runLoop(null, block)

        internal fun <T> run(backend: GlfwBackend, block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T =
            runLoop(backend, block)

        /**
         * Creates a loop for [serve] on a caller-provided backend. Unlike [run], construction
         * takes no gate: the gate is taken when the loop actually serves, so a created but
         * never-served loop leaves the process free for another one.
         */
        internal fun createForServing(backend: GlfwBackend? = null): DawnMainThreadLoop =
            DawnMainThreadLoop(backend)

        private fun <T> runLoop(backend: GlfwBackend?, block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T {
            requireGlfwMainThread()
            check(active.compareAndSet(false, true)) { "a GLFW main loop is already running" }
            val main = DawnMainThreadLoop(backend)
            val task = main.scope.async(start = CoroutineStart.UNDISPATCHED) { block(main) }
            try {
                while (!task.isCompleted || main.windows.isNotEmpty() || main.partialCleanup.isNotEmpty()) {
                    main.drain()
                    if (main.runtime.isActive) main.backend.pollEvents()
                    if (task.isCompleted) main.beginRetirement()
                    pauseGlfwMainThread()
                }
                return runBlocking { task.await() }
            } finally {
                main.scope.cancel()
                main.drain()
                synchronized(main.lock) { main.stopped = true }
                active.value = false
            }
        }
    }
}
