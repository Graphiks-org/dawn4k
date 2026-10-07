package org.graphiks.dawn4k.demo

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_BYTE
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentLinkedQueue
import kotlinx.coroutines.CancellationException

/** Must be called before AWT opens its first display on Linux. */
internal fun initializeLinuxXlibThreading() = Xlib.initializeThreads()

/** LP64 Xlib ABI, verified by docker/xlib-layout.c; loaded only on Linux paths. */
internal object Xlib {
    private val linker = Linker.nativeLinker()
    private val library = SymbolLookup.libraryLookup("libX11.so.6", Arena.global())
    private fun bind(name: String, result: MemoryLayout, vararg arguments: MemoryLayout): MethodHandle =
        linker.downcallHandle(library.find(name).orElseThrow(), FunctionDescriptor.of(result, *arguments))

    private val initThreads = bind("XInitThreads", JAVA_INT)
    private val open = bind("XOpenDisplay", ADDRESS, ADDRESS)
    private val close = bind("XCloseDisplay", JAVA_INT, ADDRESS)
    private val create = bind("XCreateSimpleWindow", JAVA_LONG, ADDRESS, JAVA_LONG,
        JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_LONG)
    private val select = bind("XSelectInput", JAVA_INT, ADDRESS, JAVA_LONG, JAVA_LONG)
    private val map = bind("XMapRaised", JAVA_INT, ADDRESS, JAVA_LONG)
    private val unmap = bind("XUnmapWindow", JAVA_INT, ADDRESS, JAVA_LONG)
    private val move = bind("XMoveResizeWindow", JAVA_INT, ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    private val destroy = bind("XDestroyWindow", JAVA_INT, ADDRESS, JAVA_LONG)
    private val pending = bind("XPending", JAVA_INT, ADDRESS)
    private val next = bind("XNextEvent", JAVA_INT, ADDRESS, ADDRESS)
    private val synchronize = bind("XSync", JAVA_INT, ADDRESS, JAVA_INT)
    private val getGeometry = bind("XGetGeometry", JAVA_INT, ADDRESS, JAVA_LONG,
        ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    private val setErrorHandler = bind("XSetErrorHandler", ADDRESS, ADDRESS)

    private data class Error(val code: Int, val request: Int, val resource: Long)
    private val errors = ConcurrentHashMap<Long, ConcurrentLinkedQueue<Error>>()
    private var previousHandler: MethodHandle? = null
    private var handlerInstalled = false
    private var threadingInitialized = false
    // Process-global callback must outlive every display and never throw through C.
    private val errorCallback by lazy {
        val target = MethodHandles.lookup().findVirtual(Xlib::class.java, "handleError",
            MethodType.methodType(Integer.TYPE, MemorySegment::class.java, MemorySegment::class.java)).bindTo(this)
        linker.upcallStub(target, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS), Arena.global())
    }

    @Synchronized fun initializeThreads() {
        if (!threadingInitialized) {
            check(ADDRESS.byteSize() == 8L) { "the Linux viewport requires the LP64 Xlib ABI" }
            check(initThreads.invokeWithArguments() as Int != 0) { "XInitThreads failed" }
            threadingInitialized = true
        }
    }

    @Synchronized private fun register(display: MemorySegment) {
        errors[display.address()] = ConcurrentLinkedQueue()
        if (!handlerInstalled) {
            val previous = setErrorHandler.invokeWithArguments(errorCallback) as MemorySegment
            if (previous != MemorySegment.NULL) {
                previousHandler = linker.downcallHandle(previous, FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS))
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
            val event = pointer.reinterpret(40)
            queue.add(Error(event.get(JAVA_BYTE, 32).toInt() and 255,
                event.get(JAVA_BYTE, 33).toInt() and 255, event.get(JAVA_LONG, 16)))
            0
        }
    } catch (failure: Throwable) {
        System.err.println("[X11] error handler failure: $failure")
        0
    }

    fun openDisplay(): MemorySegment {
        val display = open.invokeWithArguments(MemorySegment.NULL) as MemorySegment
        check(display != MemorySegment.NULL) { "XOpenDisplay failed for DISPLAY=${System.getenv("DISPLAY")}" }
        register(display)
        return display
    }

    fun closeDisplay(display: MemorySegment) {
        try { close.invokeWithArguments(display) } finally { errors.remove(display.address()) }
    }

    fun sync(display: MemorySegment) {
        synchronize.invokeWithArguments(display, 0)
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
        select.invokeWithArguments(display, window, 1L shl 17)
    }

    fun createChild(display: MemorySegment, parent: Long): Long =
        create.invokeWithArguments(display, parent, 0, 0, 1, 1, 0, 0L, 0L) as Long

    fun moveResize(display: MemorySegment, window: Long, x: Int, y: Int, width: Int, height: Int) {
        move.invokeWithArguments(display, window, x, y, width, height)
    }

    fun map(display: MemorySegment, window: Long) { map.invokeWithArguments(display, window) }
    fun unmap(display: MemorySegment, window: Long) { unmap.invokeWithArguments(display, window) }
    fun destroy(display: MemorySegment, window: Long) { destroy.invokeWithArguments(display, window) }

    /** A bounded nonblocking drain; parent/child destruction means presentation ended. */
    fun wasDestroyed(display: MemorySegment): Boolean = Arena.ofConfined().use { arena ->
        val event = arena.allocate(192, 8)
        var destroyed = false
        repeat(256) {
            if (pending.invokeWithArguments(display) as Int == 0) {
                checkErrors(display)
                return@use destroyed
            }
            next.invokeWithArguments(display, event)
            if (event.get(JAVA_INT, 0) == 17) destroyed = true // DestroyNotify
        }
        checkErrors(display)
        destroyed
    }

    fun size(display: MemorySegment, window: Long): Pair<Int, Int> = Arena.ofConfined().use { arena ->
        val values = arena.allocate(32, 8)
        val status = getGeometry.invokeWithArguments(display, window, values,
            values.asSlice(8), values.asSlice(12), values.asSlice(16), values.asSlice(20),
            values.asSlice(24), values.asSlice(28)) as Int
        sync(display)
        check(status != 0) { "XGetGeometry failed" }
        values.get(JAVA_INT, 16) to values.get(JAVA_INT, 20)
    }
}
