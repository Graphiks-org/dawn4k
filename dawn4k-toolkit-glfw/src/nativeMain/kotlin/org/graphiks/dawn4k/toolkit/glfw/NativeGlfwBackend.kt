@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw.internal

import cnames.structs.GLFWwindow
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.glfw.c.*
import org.graphiks.dawn4k.toolkit.surface.*

internal actual fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend = NativeGlfwBackend(main)

internal class NativeGlfwBackend(private val main: DawnMainThreadLoop) : GlfwBackend {
    private val windows = mutableMapOf<Long, StableRef<NativeGlfwTarget>>()
    val liveWindowCount: Int get() = windows.size
    fun targetPlatform(handle: Long): GlfwPlatform = checkNotNull(windows[handle]).get().platform

    override fun initialize() {
        requireGlfwMainThread()
        check(glfwInit() == GLFW_TRUE) { "GLFW initialization failed: ${errorMessage()}" }
    }
    override fun requireInitialized() {
        requireGlfwMainThread()
        check(dawn_glfw_platform() != 0) { "Borrowed requires an initialized caller-owned GLFW runtime: ${errorMessage()}" }
    }
    override fun terminate() { requireGlfwMainThread(); glfwTerminate() }
    override fun createWindow(options: DawnWindowOptions): Long {
        requireGlfwMainThread()
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_VISIBLE, if (options.visible) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, if (options.resizable) GLFW_TRUE else GLFW_FALSE)
        val window = checkNotNull(glfwCreateWindow(options.width, options.height, options.title, null, null)) {
            "GLFW window creation failed: ${errorMessage()}"
        }
        val handle = window.rawValue.toLong()
        var reference: StableRef<NativeGlfwTarget>? = null
        try {
            val target = NativeGlfwTarget(window, main, pixelExtent(handle))
            reference = StableRef.create(target)
            glfwSetWindowUserPointer(window, reference.asCPointer())
            windows[handle] = reference
            glfwSetFramebufferSizeCallback(window, staticCFunction { pointer, width, height ->
                callbackTarget(pointer)?.let { target ->
                    val extent = PixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0))
                    target.resize(extent); target.event(DawnWindowEvent.Resized(extent))
                }
            })
            glfwSetWindowCloseCallback(window, staticCFunction { pointer ->
                callbackTarget(pointer)?.let { it.invalidate(); it.event(DawnWindowEvent.CloseRequested) }
            })
            glfwSetKeyCallback(window, staticCFunction { pointer, key, scan, action, modifiers ->
                callbackTarget(pointer)?.event(DawnWindowEvent.Key(key, scan, action, modifiers))
            })
            glfwSetCursorPosCallback(window, staticCFunction { pointer, x, y ->
                callbackTarget(pointer)?.event(DawnWindowEvent.PointerMoved(x, y))
            })
            glfwSetMouseButtonCallback(window, staticCFunction { pointer, button, action, modifiers ->
                callbackTarget(pointer)?.event(DawnWindowEvent.PointerButton(button, action, modifiers))
            })
            glfwSetScrollCallback(window, staticCFunction { pointer, x, y ->
                callbackTarget(pointer)?.event(DawnWindowEvent.Scroll(x, y))
            })
            return handle
        } catch (failure: Throwable) {
            windows.remove(handle)
            clearCallbacks(window)
            glfwDestroyWindow(window)
            reference?.dispose()
            throw failure
        }
    }
    override fun destroyWindow(handle: Long) {
        requireGlfwMainThread()
        val reference = windows.remove(handle) ?: return
        reference.get().invalidate()
        val window = pointer(handle)
        clearCallbacks(window)
        glfwDestroyWindow(window)
        reference.dispose()
    }
    override fun resizeWindow(handle: Long, width: Int, height: Int) {
        requireGlfwMainThread()
        glfwSetWindowSize(pointer(handle), width, height)
    }
    override fun pollEvents() { requireGlfwMainThread(); glfwPollEvents() }
    override fun pixelExtent(handle: Long): PixelExtent = memScoped {
        val width = alloc<IntVar>(); val height = alloc<IntVar>()
        glfwGetFramebufferSize(pointer(handle), width.ptr, height.ptr)
        PixelExtent(width.value.coerceAtLeast(0), height.value.coerceAtLeast(0))
    }
    override fun surfaceTarget(handle: Long): DawnSurfaceTarget = checkNotNull(windows[handle]).get().target
    override fun setEventSink(handle: Long, sink: (DawnWindowEvent) -> Unit) { checkNotNull(windows[handle]).get().sink = sink }
    override fun requestClose(handle: Long) {
        val target = checkNotNull(windows[handle]).get()
        target.invalidate(); glfwSetWindowShouldClose(pointer(handle), GLFW_TRUE)
        target.event(DawnWindowEvent.CloseRequested)
    }
    override suspend fun awaitTargetRetirement(handle: Long) { windows[handle]?.get()?.awaitRetirement() }

    private fun errorMessage(): String = memScoped {
        val description = alloc<CPointerVar<ByteVar>>()
        val code = glfwGetError(description.ptr)
        "$code: ${description.value?.toKString() ?: "no native diagnostic"}"
    }
}

private fun pointer(handle: Long): CPointer<GLFWwindow> = checkNotNull(handle.toCPointer())
private fun callbackTarget(window: CPointer<GLFWwindow>?): NativeGlfwTarget? =
    glfwGetWindowUserPointer(window)?.asStableRef<NativeGlfwTarget>()?.get()

private fun clearCallbacks(window: CPointer<GLFWwindow>) {
    glfwSetFramebufferSizeCallback(window, null); glfwSetWindowCloseCallback(window, null)
    glfwSetKeyCallback(window, null); glfwSetCursorPosCallback(window, null)
    glfwSetMouseButtonCallback(window, null); glfwSetScrollCallback(window, null)
    glfwSetWindowUserPointer(window, null)
}

private class NativeGlfwTarget(window: CPointer<GLFWwindow>, main: DawnMainThreadLoop, extent: PixelExtent) {
    private val states = MutableStateFlow(DawnTargetState.fresh(extent, true))
    private val references = MutableStateFlow(0)
    val platform = glfwPlatform(dawn_glfw_platform())
    var sink: (DawnWindowEvent) -> Unit = {}
    fun event(event: DawnWindowEvent) = sink(event)
    fun resize(extent: PixelExtent) { states.value = states.value.copy(extent = extent) }
    fun invalidate() { states.value = states.value.copy(available = false) }
    suspend fun awaitRetirement() { invalidate(); references.first { it == 0 } }
    private val retention = object : DawnTargetRetention {
        override suspend fun retain() { references.value++ }
        override suspend fun retire() { check(references.value > 0); references.value-- }
    }
    val target: DawnSurfaceTarget by lazy { nativeSurfaceTarget(window, platform, main, states, retention) }
}

internal expect fun nativeSurfaceTarget(
    window: CPointer<GLFWwindow>, platform: GlfwPlatform, main: DawnMainThreadLoop,
    states: MutableStateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget
