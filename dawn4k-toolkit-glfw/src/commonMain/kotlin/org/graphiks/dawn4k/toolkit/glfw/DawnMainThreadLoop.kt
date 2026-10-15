package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.locks.SynchronizedObject
import kotlinx.atomicfu.locks.synchronized
import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.glfw.internal.*
import org.graphiks.dawn4k.toolkit.surface.DawnPlatformOwner
import kotlin.coroutines.CoroutineContext

/** A running process-main-thread owner. Persistent windows cannot outlive this scope. */
class DawnMainThreadLoop private constructor(overrideBackend: GlfwBackend?) {
    private val lock = SynchronizedObject()
    private val queue = ArrayDeque<Runnable>()
    private var stopped = false
    private val dispatcher = object : CoroutineDispatcher() {
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            synchronized(lock) { check(!stopped) { "GLFW main loop is stopped" }; queue.addLast(block) }
        }
    }
    internal val owner = DawnPlatformOwner.fromDispatcher(dispatcher)
    internal val backend = overrideBackend ?: createGlfwBackend(this)
    internal val runtime = GlfwRuntime(backend)
    private val windows = mutableSetOf<DawnWindow>()
    private val cleanup = mutableMapOf<DawnWindow, Job>()
    private val scope = CoroutineScope(SupervisorJob() + dispatcher)

    internal fun register(window: DawnWindow) { windows += window }
    internal fun unregister(window: DawnWindow) { windows -= window; cleanup.remove(window) }

    private fun drain() {
        while (true) {
            val work = synchronized(lock) { queue.removeFirstOrNull() } ?: return
            work.run()
        }
    }

    companion object {
        private val active = atomic(false)

        fun <T> run(block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T = runLoop(null, block)

        internal fun <T> run(backend: GlfwBackend, block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T =
            runLoop(backend, block)

        private fun <T> runLoop(backend: GlfwBackend?, block: suspend CoroutineScope.(DawnMainThreadLoop) -> T): T {
            requireGlfwMainThread()
            check(active.compareAndSet(false, true)) { "a GLFW main loop is already running" }
            val main = DawnMainThreadLoop(backend)
            val task = main.scope.async(start = CoroutineStart.UNDISPATCHED) { block(main) }
            try {
                while (!task.isCompleted || main.windows.isNotEmpty()) {
                    main.drain()
                    if (main.runtime.isActive) main.backend.pollEvents()
                    if (task.isCompleted) {
                        main.windows.toList().forEach { window ->
                            if (main.cleanup[window]?.isActive != true) {
                                main.cleanup[window] = main.scope.launch(Dispatchers.Default) {
                                    window.gpu.manualProgress?.stop()
                                    runCatching { window.shutdown() }
                                    delay(10)
                                }
                            }
                        }
                    }
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
