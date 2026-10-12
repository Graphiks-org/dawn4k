package org.graphiks.dawn4k.demo

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CancellationException
import org.graphiks.kffi.x11.generated.DestroyNotify
import org.graphiks.kffi.x11.generated.IsViewable
import org.graphiks.kffi.x11.generated.StructureNotifyMask
import org.graphiks.kffi.x11.generated.XCloseDisplay
import org.graphiks.kffi.x11.generated.XCreateSimpleWindow
import org.graphiks.kffi.x11.generated.XDestroyWindow
import org.graphiks.kffi.x11.generated.XErrorEvent
import org.graphiks.kffi.x11.generated.XEventStorage
import org.graphiks.kffi.x11.generated.XGetGeometry
import org.graphiks.kffi.x11.generated.XGetWindowAttributes
import org.graphiks.kffi.x11.generated.XInitThreads
import org.graphiks.kffi.x11.generated.XMapRaised
import org.graphiks.kffi.x11.generated.XMoveResizeWindow
import org.graphiks.kffi.x11.generated.XNextEvent
import org.graphiks.kffi.x11.generated.XOpenDisplay
import org.graphiks.kffi.x11.generated.XPending
import org.graphiks.kffi.x11.generated.XSelectInput
import org.graphiks.kffi.x11.generated.XSetErrorHandler
import org.graphiks.kffi.x11.generated.XSync
import org.graphiks.kffi.x11.generated.XUnmapWindow
import org.graphiks.kffi.x11.generated.XWindowAttributes

/** Must be called before AWT opens its first display on Linux. */
internal fun initializeLinuxXlibThreading() = Xlib.initializeThreads()

/** Demo lifecycle/error policy over kffi's generated LP64 Xlib bindings; Linux-only. */
internal object Xlib {
    private val linker = Linker.nativeLinker()
    private val errorHandlerDescriptor = FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    private val errorFields = XErrorEvent()
    private val eventFields = XEventStorage()
    private val attributeFields = XWindowAttributes()

    private data class Error(val code: Int, val request: Int, val resource: Long)
    private val errors = ConcurrentHashMap<Long, ConcurrentLinkedQueue<Error>>()
    private var previousHandler: MethodHandle? = null
    private var handlerInstalled = false
    private var threadingInitialized = false
    // Process-global callback must outlive every display and never throw through C.
    private val errorCallback by lazy {
        val target = MethodHandles.lookup().findVirtual(Xlib::class.java, "handleError",
            MethodType.methodType(Integer.TYPE, MemorySegment::class.java, MemorySegment::class.java)).bindTo(this)
        linker.upcallStub(target, errorHandlerDescriptor, Arena.global())
    }

    @Synchronized fun initializeThreads() {
        if (!threadingInitialized) {
            check(ADDRESS.byteSize() == 8L) { "the Linux viewport requires the LP64 Xlib ABI" }
            check(XInitThreads() != 0) { "XInitThreads failed" }
            threadingInitialized = true
        }
    }

    @Synchronized private fun register(display: MemorySegment) {
        errors[display.address()] = ConcurrentLinkedQueue()
        if (!handlerInstalled) {
            val previous = XSetErrorHandler(errorCallback)
            if (previous != MemorySegment.NULL) {
                previousHandler = linker.downcallHandle(previous, errorHandlerDescriptor)
            }
            handlerInstalled = true
        }
    }

    @Suppress("unused") // Java FFM upcall target, reached by MethodHandle.
    private fun handleError(display: MemorySegment, pointer: MemorySegment): Int = try {
        val queue = errors[display.address()]
        if (queue == null) {
            previousHandler?.invokeWithArguments(display, pointer) as? Int ?: 0
        } else {
            val event = XErrorEvent.reinterpret(pointer)
            queue.add(Error(errorFields.error_code(event).toInt() and 255,
                errorFields.request_code(event).toInt() and 255, errorFields.resourceid(event)))
            0
        }
    } catch (failure: Throwable) {
        System.err.println("[X11] error handler failure: $failure")
        0
    }

    fun openDisplay(): MemorySegment {
        val display = XOpenDisplay(MemorySegment.NULL)
        check(display != MemorySegment.NULL) { "XOpenDisplay failed for DISPLAY=${System.getenv("DISPLAY")}" }
        register(display)
        return display
    }

    fun closeDisplay(display: MemorySegment) {
        try { XCloseDisplay(display) } finally { errors.remove(display.address()) }
    }

    fun sync(display: MemorySegment) {
        XSync(display, 0)
        checkErrors(display)
    }

    private fun checkErrors(display: MemorySegment) {
        val queue = errors[display.address()] ?: return
        val first = queue.poll() ?: return
        while (queue.poll() != null) { /* drain errors for this request batch */ }
        val message = "X11 error=${first.code}, request=${first.request}, resource=0x${first.resource.toString(16)}"
        if (first.code == 3 || first.code == 9) throw CancellationException("the X11 window was closed ($message)")
        error(message)
    }

    fun selectStructureEvents(display: MemorySegment, window: Long) {
        XSelectInput(display, window, StructureNotifyMask())
    }

    fun createChild(display: MemorySegment, parent: Long): Long =
        XCreateSimpleWindow(display, parent, 0, 0, 1, 1, 0, 0L, 0L)

    fun moveResize(display: MemorySegment, window: Long, x: Int, y: Int, width: Int, height: Int) {
        XMoveResizeWindow(display, window, x, y, width, height)
    }

    fun map(display: MemorySegment, window: Long) { XMapRaised(display, window) }
    fun unmap(display: MemorySegment, window: Long) { XUnmapWindow(display, window) }
    fun destroy(display: MemorySegment, window: Long) { XDestroyWindow(display, window) }

    /** A bounded nonblocking drain; parent/child destruction means presentation ended. */
    fun wasDestroyed(display: MemorySegment): Boolean = Arena.ofConfined().use { arena ->
        val event = XEventStorage.allocate(arena)
        var destroyed = false
        repeat(256) {
            if (XPending(display) == 0) {
                checkErrors(display)
                return@use destroyed
            }
            XNextEvent(display, event)
            if (eventFields.type(event) == DestroyNotify()) destroyed = true
        }
        checkErrors(display)
        destroyed
    }

    fun size(display: MemorySegment, window: Long): Pair<Int, Int> = Arena.ofConfined().use { arena ->
        val root = arena.allocate(JAVA_LONG)
        val x = arena.allocate(JAVA_INT)
        val y = arena.allocate(JAVA_INT)
        val width = arena.allocate(JAVA_INT)
        val height = arena.allocate(JAVA_INT)
        val border = arena.allocate(JAVA_INT)
        val depth = arena.allocate(JAVA_INT)
        val status = XGetGeometry(display, window, root, x, y, width, height, border, depth)
        sync(display)
        check(status != 0) { "XGetGeometry failed" }
        width.get(JAVA_INT, 0) to height.get(JAVA_INT, 0)
    }

    /** IsViewable includes ancestor map state, unlike positive drawable geometry. */
    fun isViewable(display: MemorySegment, window: Long): Boolean = Arena.ofConfined().use { arena ->
        val attributes = XWindowAttributes.allocate(arena)
        val status = XGetWindowAttributes(display, window, attributes)
        sync(display)
        check(status != 0) { "XGetWindowAttributes failed" }
        attributeFields.map_state(attributes) == IsViewable()
    }
}
