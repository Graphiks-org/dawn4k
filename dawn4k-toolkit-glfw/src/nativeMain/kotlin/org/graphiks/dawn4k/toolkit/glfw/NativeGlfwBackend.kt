@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.glfw.internal

import cnames.structs.GLFWwindow
import kotlinx.cinterop.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.glfw.c.*
import org.graphiks.dawn4k.toolkit.surface.*
import platform.posix.fflush
import platform.posix.fwrite
import platform.posix.stderr

internal actual fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend = NativeGlfwBackend(main)

internal actual fun writeGlfwStandardError(message: String) {
    val bytes = message.encodeToByteArray()
    if (bytes.isNotEmpty()) bytes.usePinned {
        fwrite(it.addressOf(0), 1u, bytes.size.toULong(), stderr)
        fflush(stderr)
    }
}

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
                callbackTarget(pointer)?.onResized(width, height)
            })
            glfwSetWindowCloseCallback(window, staticCFunction { pointer ->
                callbackTarget(pointer)?.onClose()
            })
            glfwSetKeyCallback(window, staticCFunction { pointer, key, scan, action, modifiers ->
                callbackTarget(pointer)?.onKey(key, scan, action, modifiers)
            })
            glfwSetCursorPosCallback(window, staticCFunction { pointer, x, y ->
                callbackTarget(pointer)?.onPointerMoved(x, y)
            })
            glfwSetMouseButtonCallback(window, staticCFunction { pointer, button, action, modifiers ->
                callbackTarget(pointer)?.onPointerButton(button, action, modifiers)
            })
            glfwSetScrollCallback(window, staticCFunction { pointer, x, y ->
                callbackTarget(pointer)?.onScroll(x, y)
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
        target.invalidate()
        glfwSetWindowShouldClose(pointer(handle), GLFW_TRUE)
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

/** Adds the native platform surface-target construction to the shared [GlfwWindowTarget] machinery. */
private class NativeGlfwTarget(
    window: CPointer<GLFWwindow>,
    main: DawnMainThreadLoop,
    extent: PixelExtent,
) : GlfwWindowTarget(extent) {
    val platform = glfwPlatform(dawn_glfw_platform())
    val target: DawnSurfaceTarget by lazy { nativeSurfaceTarget(window, platform, main, states, retention) }
}

internal expect fun nativeSurfaceTarget(
    window: CPointer<GLFWwindow>, platform: GlfwPlatform, main: DawnMainThreadLoop,
    states: MutableStateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget
