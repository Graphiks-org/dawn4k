package org.graphiks.dawn4k.toolkit.glfw.internal

import org.graphiks.dawn4k.toolkit.glfw.*
import org.graphiks.dawn4k.toolkit.surface.*
import org.lwjgl.glfw.Callbacks
import org.lwjgl.glfw.GLFW.*
import org.lwjgl.system.MemoryStack
import org.lwjgl.system.MemoryUtil

internal actual fun createGlfwBackend(main: DawnMainThreadLoop): GlfwBackend = LwjglGlfwBackend(main)

internal class LwjglGlfwBackend(private val main: DawnMainThreadLoop) : GlfwBackend {
    private val windows = mutableMapOf<Long, JvmGlfwTarget>()

    override fun initialize() {
        requireGlfwMainThread()
        check(glfwInit()) { "GLFW initialization failed: ${errorMessage()}" }
    }
    override fun requireInitialized() {
        requireGlfwMainThread()
        check(glfwGetPlatform() != 0) { "Borrowed requires an initialized caller-owned GLFW runtime: ${errorMessage()}" }
    }
    override fun terminate() { requireGlfwMainThread(); glfwTerminate() }

    override fun createWindow(options: DawnWindowOptions): Long {
        requireGlfwMainThread()
        glfwDefaultWindowHints()
        glfwWindowHint(GLFW_CLIENT_API, GLFW_NO_API)
        glfwWindowHint(GLFW_VISIBLE, if (options.visible) GLFW_TRUE else GLFW_FALSE)
        glfwWindowHint(GLFW_RESIZABLE, if (options.resizable) GLFW_TRUE else GLFW_FALSE)
        val handle = glfwCreateWindow(options.width, options.height, options.title, 0, 0)
        check(handle != 0L) { "GLFW window creation failed: ${errorMessage()}" }
        try {
            val target = JvmGlfwTarget(handle, main, pixelExtent(handle))
            windows[handle] = target
            glfwSetFramebufferSizeCallback(handle) { _, width, height ->
                val extent = PixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0))
                target.resize(extent)
                target.event(DawnWindowEvent.Resized(extent))
            }
            glfwSetWindowCloseCallback(handle) { _ -> target.invalidate(); target.event(DawnWindowEvent.CloseRequested) }
            glfwSetKeyCallback(handle) { _, key, scan, action, modifiers ->
                target.event(DawnWindowEvent.Key(key, scan, action, modifiers))
            }
            glfwSetCursorPosCallback(handle) { _, x, y -> target.event(DawnWindowEvent.PointerMoved(x, y)) }
            glfwSetMouseButtonCallback(handle) { _, button, action, modifiers ->
                target.event(DawnWindowEvent.PointerButton(button, action, modifiers))
            }
            glfwSetScrollCallback(handle) { _, x, y -> target.event(DawnWindowEvent.Scroll(x, y)) }
            return handle
        } catch (failure: Throwable) {
            windows.remove(handle)
            Callbacks.glfwFreeCallbacks(handle)
            glfwDestroyWindow(handle)
            throw failure
        }
    }

    override fun destroyWindow(handle: Long) {
        requireGlfwMainThread()
        windows.remove(handle)?.invalidate()
        Callbacks.glfwFreeCallbacks(handle)
        glfwDestroyWindow(handle)
    }
    override fun resizeWindow(handle: Long, width: Int, height: Int) {
        requireGlfwMainThread()
        glfwSetWindowSize(handle, width, height)
    }
    override fun pollEvents() { requireGlfwMainThread(); glfwPollEvents() }

    override fun pixelExtent(handle: Long): PixelExtent = MemoryStack.stackPush().use { stack ->
        val width = stack.mallocInt(1); val height = stack.mallocInt(1)
        glfwGetFramebufferSize(handle, width, height)
        PixelExtent(width[0].coerceAtLeast(0), height[0].coerceAtLeast(0))
    }
    override fun surfaceTarget(handle: Long): DawnSurfaceTarget = checkNotNull(windows[handle]).target
    override fun setEventSink(handle: Long, sink: (DawnWindowEvent) -> Unit) { checkNotNull(windows[handle]).sink = sink }
    override fun requestClose(handle: Long) {
        val target = checkNotNull(windows[handle])
        target.invalidate()
        glfwSetWindowShouldClose(handle, true)
        target.event(DawnWindowEvent.CloseRequested)
    }
    override suspend fun awaitTargetRetirement(handle: Long) { windows[handle]?.awaitRetirement() }

    private fun errorMessage(): String = MemoryStack.stackPush().use { stack ->
        val description = stack.mallocPointer(1)
        val code = glfwGetError(description)
        val message = description[0].takeIf { it != 0L }?.let(MemoryUtil::memUTF8) ?: "no native diagnostic"
        "$code: $message"
    }
}
