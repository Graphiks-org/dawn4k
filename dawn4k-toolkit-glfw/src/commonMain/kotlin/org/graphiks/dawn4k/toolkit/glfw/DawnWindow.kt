package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.*
import kotlinx.atomicfu.atomic
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.graphiks.dawn4k.toolkit.glfw.internal.*

internal interface WindowSession {
    val gpu: DawnSession
    val presentation: DawnPresentation
    suspend fun shutdown()
}

private class PresentedWindowSession(private val session: DawnPresentedSession) : WindowSession {
    override val gpu get() = session.gpu
    override val presentation get() = session.presentation
    override suspend fun shutdown() = session.shutdown()
}

class DawnWindow internal constructor(
    private val main: DawnMainThreadLoop,
    private val native: NativeWindowLease,
    private val presented: WindowSession,
) {
    private val closed = atomic(false)
    private val closing = Mutex()
    val gpu: DawnSession get() = presented.gpu
    val presentation: DawnPresentation get() = presented.presentation
    val extent = native.state.extent
    val events = native.state.events
    internal val nativeHandle: Long get() { check(!closed.value); native.requireLive(); return native.handle }

    /** Requests a content size in window coordinates; [extent] reports the resulting framebuffer pixels. */
    suspend fun resize(width: Int, height: Int) {
        require(width > 0 && height > 0) { "window dimensions must be positive" }
        main.owner.onOwner {
            check(!closed.value) { "window is closed" }
            native.requireLive()
            main.backend.resizeWindow(native.handle, width, height)
        }
    }

    suspend fun requestClose() = main.owner.onOwner {
        check(!closed.value) { "window is closed" }
        native.requireLive()
        main.backend.requestClose(native.handle)
        native.state.event(DawnWindowEvent.CloseRequested)
    }

    suspend fun shutdown() {
        if (closed.value) return
        // Keep the caller's admission marker: the core rejects shutdown from its own GPU block.
        presented.shutdown()
        withContext(NonCancellable) {
            closing.withLock {
                if (!closed.value) main.owner.onOwner {
                    native.retire()
                    closed.value = true
                    main.unregister(this@DawnWindow)
                }
            }
        }
    }
}

internal class WindowSurfaceState(backend: GlfwBackend, handle: Long) {
    private val size = MutableStateFlow(backend.pixelExtent(handle))
    val extent = size.asStateFlow()
    private val notifications = MutableSharedFlow<DawnWindowEvent>(replay = 1, extraBufferCapacity = 64)
    val events = notifications.asSharedFlow()
    private var closeRequested = false

    fun event(event: DawnWindowEvent) {
        if (closeRequested) return
        if (event is DawnWindowEvent.Resized) size.value = event.extent
        if (event == DawnWindowEvent.CloseRequested) closeRequested = true
        notifications.tryEmit(event)
    }
}

internal class NativeWindowLease(
    private val backend: GlfwBackend,
    val handle: Long,
    private val runtime: GlfwRuntime.Lease,
) {
    val state = WindowSurfaceState(backend, handle)
    private val destroyed = atomic(false)
    private var retired = false
    private val mutex = Mutex()

    fun requireLive() { check(!destroyed.value) { "window has been destroyed" } }

    suspend fun retire() = mutex.withLock {
        if (!retired) {
            if (!destroyed.value) {
                backend.awaitTargetRetirement(handle)
                backend.destroyWindow(handle)
                destroyed.value = true
            }
            runtime.retire()
            retired = true
        }
    }
}

suspend fun openDawnWindow(
    windowOptions: DawnWindowOptions = DawnWindowOptions(),
    sessionOptions: DawnSessionOptions = DawnSessionOptions(),
    presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
    mainThread: DawnMainThreadLoop,
): DawnWindow = openDawnWindow(windowOptions, sessionOptions, presentationOptions, mainThread, ::openDawnSurface)

internal suspend fun openDawnWindow(
    windowOptions: DawnWindowOptions,
    sessionOptions: DawnSessionOptions,
    presentationOptions: DawnPresentationOptions,
    mainThread: DawnMainThreadLoop,
    openSurface: suspend (DawnSurfaceTarget, DawnSessionOptions, DawnPresentationOptions) -> DawnPresentedSession,
): DawnWindow {
    var lease: NativeWindowLease? = null
    var runtime: GlfwRuntime.Lease? = null
    var presented: DawnPresentedSession? = null
    var allocatedHandle = 0L
    var unleasedWindowDestroyed = false
    try {
        mainThread.owner.onOwner {
            runtime = mainThread.runtime.acquire(windowOptions.runtimeOwnership)
            val handle = mainThread.backend.createWindow(windowOptions)
            allocatedHandle = handle
            check(handle != 0L) { "GLFW returned no window" }
            lease = NativeWindowLease(mainThread.backend, handle, checkNotNull(runtime))
        }
        val native = checkNotNull(lease)
        val target = mainThread.owner.onOwner { mainThread.backend.surfaceTarget(native.handle) }
        presented = openSurface(target, sessionOptions, presentationOptions)
        val window = DawnWindow(mainThread, native, PresentedWindowSession(presented))
        mainThread.owner.onOwner {
            mainThread.backend.setEventSink(native.handle, native.state::event)
            mainThread.register(window)
        }
        return window
    } catch (failure: Throwable) {
        withContext(NonCancellable) {
            mainThread.retireFailedStartup {
                presented?.gpu?.manualProgress?.stop()
                presented?.shutdown()
                val acquired = lease
                if (acquired != null) acquired.retire() else {
                    if (allocatedHandle != 0L && !unleasedWindowDestroyed) {
                        mainThread.backend.awaitTargetRetirement(allocatedHandle)
                        mainThread.backend.destroyWindow(allocatedHandle)
                        unleasedWindowDestroyed = true
                    }
                    runtime?.retire()
                }
            }
        }
        throw failure
    }
}

suspend fun <T> withDawnWindow(
    windowOptions: DawnWindowOptions = DawnWindowOptions(),
    sessionOptions: DawnSessionOptions = DawnSessionOptions(),
    presentationOptions: DawnPresentationOptions = DawnPresentationOptions(),
    block: suspend CoroutineScope.(DawnSession, DawnWindow, DawnPresentation) -> T,
): T = DawnMainThreadLoop.run { main ->
    val window = openDawnWindow(windowOptions, sessionOptions, presentationOptions, main)
    var failure: Throwable? = null
    try { window.gpu.run { block(it, window, window.presentation) } }
    catch (caught: Throwable) { failure = caught; throw caught }
    finally {
        window.gpu.manualProgress?.stop()
        try { window.shutdown() }
        catch (cleanup: Throwable) { if (failure == null) throw cleanup else failure.addSuppressed(cleanup) }
    }
}
