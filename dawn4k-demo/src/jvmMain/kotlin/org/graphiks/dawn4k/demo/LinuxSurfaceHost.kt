package org.graphiks.dawn4k.demo

import java.awt.Rectangle
import java.lang.foreign.MemorySegment
import java.util.concurrent.Callable
import java.util.concurrent.ExecutionException
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.ScheduledThreadPoolExecutor
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.atomic.AtomicReference
import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge

/** Owns an X11 connection/child beside Compose; never borrows AWT's display. */
internal class LinuxSurfaceHost private constructor(
    private val parentWindow: Long,
    private val viewport: AtomicReference<Rectangle>,
) : SurfaceHost {
    private val closed = AtomicBoolean()
    internal val isClosed: Boolean get() = closed.get()
    private var ownerThread: Thread? = null
    private val owner = ScheduledThreadPoolExecutor(1) { operation ->
        Thread(operation, "dawn-x11-owner-${ids.incrementAndGet()}").apply {
            isDaemon = true
            ownerThread = this
        }
    }
    private var display = MemorySegment.NULL
    private var child = 0L
    private var childDestroyed = false
    private var mapped = false
    private var lastBounds: Rectangle? = null
    private var failure: Throwable? = null
    private var pump: ScheduledFuture<*>? = null

    override val backend: DawnBackend get() = DawnBackend.Vulkan

    companion object {
        private val ids = AtomicInteger()

        fun attach(parentWindow: Long, viewport: AtomicReference<Rectangle>): LinuxSurfaceHost {
            require(parentWindow != 0L) { "the Compose X11 window handle is null" }
            check(!System.getenv("DISPLAY").isNullOrEmpty()) {
                "missing DISPLAY; launch from an X11 graphical session with its display environment"
            }
            val host = LinuxSurfaceHost(parentWindow, viewport)
            try {
                host.onOwner {
                    host.display = Xlib.openDisplay()
                    Xlib.selectStructureEvents(host.display, parentWindow)
                    host.child = Xlib.createChild(host.display, parentWindow)
                    Xlib.sync(host.display)
                    check(host.child != 0L) { "XCreateSimpleWindow returned no window" }
                    Xlib.selectStructureEvents(host.display, host.child)
                    host.updateBounds()
                }
                host.pump = host.owner.scheduleWithFixedDelay({
                    if (!host.isClosed && host.failure == null) {
                        try { host.processEvents() } catch (failure: Throwable) { host.failure = failure }
                    }
                }, 0, 16, TimeUnit.MILLISECONDS)
                return host
            } catch (failure: Throwable) {
                host.close()
                throw failure
            }
        }
    }

    private fun <T> onOwner(operation: () -> T): T {
        if (Thread.currentThread() === ownerThread) return operation()
        return try { owner.submit(Callable { operation() }).get() }
        catch (failure: ExecutionException) { throw failure.cause ?: failure }
    }

    private fun processEvents() {
        if (Xlib.wasDestroyed(display)) {
            childDestroyed = true
            throw CancellationException("the Compose X11 window was closed")
        }
    }

    private fun updateBounds(): Pair<Int, Int> {
        val bounds = Rectangle(viewport.get())
        if (bounds.width <= 0 || bounds.height <= 0 || !Xlib.isViewable(display, parentWindow)) {
            if (mapped) {
                Xlib.unmap(display, child)
                Xlib.sync(display)
                mapped = false
            }
            lastBounds = bounds
            return 0 to 0
        }
        if (bounds != lastBounds || !mapped) {
            Xlib.moveResize(display, child, bounds.x, bounds.y, bounds.width, bounds.height)
            if (!mapped) Xlib.map(display, child)
            Xlib.sync(display)
            lastBounds = bounds
            mapped = true
        }
        return Xlib.size(display, child)
    }

    override fun pixelSize(): Pair<Int, Int> {
        check(!isClosed) { "the Linux surface host is closed" }
        return onOwner {
            failure?.let { throw it }
            processEvents()
            updateBounds()
        }
    }

    override fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface {
        check(!isClosed) { "the Linux surface host is closed" }
        onOwner {
            failure?.let { throw it }
            processEvents()
            Xlib.sync(display)
        }
        // XInitThreads permits the Vulkan WSI worker to use this owned display.
        return DawnSurface.createXlib(bridge, deviceHandle, display.address(), child)
    }

    override fun close() {
        if (!closed.compareAndSet(false, true)) return
        pump?.cancel(false)
        try {
            onOwner {
                if (display != MemorySegment.NULL) {
                    try {
                        if (child != 0L && !childDestroyed) Xlib.destroy(display, child)
                        try { Xlib.sync(display) } catch (_: CancellationException) {
                            // AWT may already have destroyed the child with its parent.
                        }
                    } finally {
                        Xlib.closeDisplay(display)
                        display = MemorySegment.NULL
                    }
                }
            }
        } finally {
            owner.shutdown()
            if (Thread.currentThread() !== ownerThread) {
                check(owner.awaitTermination(5, TimeUnit.SECONDS)) { "X11 owner did not terminate" }
            }
        }
    }
}
