package org.graphiks.dawn4k.demo

import java.lang.foreign.MemorySegment
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge

/** Owns the Wayland objects. A close event cancels rendering, but never destroys Dawn's live handles. */
internal class WaylandSurfaceHost private constructor(
    private val controls: ParticleControls,
    private val onClose: () -> Unit,
) : SurfaceHost {
    private val closed = AtomicBoolean()
    private var ownerThread: Thread? = null
    private val owner = ScheduledThreadPoolExecutor(1) { operation ->
        Thread(operation, "dawn-wayland-owner-${ids.incrementAndGet()}").apply {
            isDaemon = true
            ownerThread = this
        }
    }
    @Volatile private var native = MemorySegment.NULL
    private var pump: ScheduledFuture<*>? = null
    private val state = WaylandHostState(800, 600)
    private var appliedScale = 1
    private var failure: Throwable? = null
    private var lastControls: ParticleControlState? = null
    override val backend: DawnBackend get() = DawnBackend.Vulkan

    companion object {
        private val ids = AtomicInteger()
        fun open(controls: ParticleControls, onClose: () -> Unit): WaylandSurfaceHost {
            val host = WaylandSurfaceHost(controls, onClose)
            try {
                host.onOwner { host.native = WaylandNative.open("dawn4k — Wayland / Vulkan", 800, 600) }
                host.pump = host.owner.scheduleWithFixedDelay({ host.processEvents() }, 0, 1, TimeUnit.MILLISECONDS)
                return host
            } catch (failure: Throwable) {
                host.close()
                throw failure
            }
        }
    }

    private fun <T> onOwner(operation: () -> T): T {
        if (Thread.currentThread() === ownerThread) return operation()
        return try { owner.submit(Callable { operation() }).get(7, TimeUnit.SECONDS) }
        catch (failure: ExecutionException) { throw failure.cause ?: failure }
    }

    private fun processEvents() {
        if (closed.get() || failure != null) return
        try {
            // Bound both the native socket wait and the drain under a busy compositor.
            var event = WaylandNative.nextEvent(native, 16)
            var count = 0
            while (event != null && count++ < 256 && !closed.get() && failure == null) {
                when (event.type) {
                    1 -> {
                        state.configure(event.width, event.height, event.scale)
                        println("[Wayland] configure ${event.width}x${event.height} scale=${event.scale} serial=${event.serial}")
                    }
                    2 -> controls.togglePause()
                    3 -> controls.reset()
                    4 -> controls.stepCount(1)
                    5 -> controls.stepCount(-1)
                    6 -> {
                        failure = CancellationException("the Wayland window was closed")
                        onClose()
                    }
                    else -> error("unknown Wayland event ${event.type} code=${event.code}")
                }
                if (failure == null) event = WaylandNative.nextEvent(native, 0)
            }
            if (failure == null) updateTitle()
        } catch (problem: Throwable) {
            failure = problem
            if (problem !is CancellationException) controls.fail(problem.message ?: problem.toString())
            onClose()
        }
    }

    private fun updateTitle() {
        val current = controls.state.value
        if (current == lastControls) return
        WaylandNative.title(native, "dawn4k — Wayland / Vulkan · ${current.count} particles · " +
            (if (current.paused) "paused" else "running") + " · Space: pause · R: reset · +/-: count · Esc: close")
        lastControls = current
    }

    override fun pixelSize(): Pair<Int, Int> {
        check(!closed.get()) { "the Wayland host is closed" }
        return onOwner {
            failure?.let { throw it }
            val size = state.pixelSize()
            if (size.first > 0 && appliedScale != state.scale) {
                WaylandNative.scale(native, state.scale)
                appliedScale = state.scale
            }
            size
        }
    }

    override fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface {
        check(!closed.get()) { "the Wayland host is closed" }
        val (display, surface) = onOwner {
            failure?.let { throw it }
            check(state.pixelSize().first > 0) { "Wayland must acknowledge initial configure before creating the Dawn surface" }
            WaylandNative.display(native).address() to WaylandNative.surface(native).address()
        }
        return DawnSurface.createWayland(bridge, deviceHandle, display, surface)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pump?.cancel(false)
        // The owner cannot autonomously free native; wake completes before destruction is queued.
        if (native != MemorySegment.NULL) WaylandNative.wake(native)
        try {
            onOwner {
                state.close()
                if (native != MemorySegment.NULL) {
                    WaylandNative.close(native)
                    native = MemorySegment.NULL
                }
            }
        } finally {
            owner.shutdown()
            if (Thread.currentThread() !== ownerThread)
                check(owner.awaitTermination(5, TimeUnit.SECONDS)) { "Wayland owner did not terminate" }
        }
    }
}
