package org.graphiks.dawn4k.demo

import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.NativeBridge

/** Owns a Win32 child window alongside Compose's controls, never its rendering HWND. */
internal class WindowsSurfaceHost private constructor(
    private val hwnd: MemorySegment,
    private val hinstance: MemorySegment,
    private val viewport: AtomicReference<Rectangle>,
) : SurfaceHost {
    private var closed = false
    private var lastBounds: Rectangle? = null
    // AWT's native pump runs on AWT-Windows, not the EDT that created this HWND.
    // Keep processing its queue during GPU initialization, waits and rendering.
    private val messagePump = Timer(16) { pumpMessages() }.apply { start() }

    override val backend: DawnBackend get() = DawnBackend.D3D12

    companion object {
        fun attach(parentHwnd: Long, viewport: AtomicReference<Rectangle>): WindowsSurfaceHost = onWindowsEdt {
            require(parentHwnd != 0L) { "the Compose window handle is null" }
            val instance = Win32.getModuleHandle.invokeWithArguments(MemorySegment.NULL) as MemorySegment
            check(instance != MemorySegment.NULL) { "GetModuleHandleW failed" }
            val child = Arena.ofConfined().use { arena ->
                val className = arena.allocate(14L, 2L)
                "STATIC\u0000".forEachIndexed { index, char -> className.set(JAVA_CHAR, index * 2L, char) }
                // WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS. The AWT EDT owns the HWND.
                Win32.createWindow.invokeWithArguments(
                    0, className, MemorySegment.NULL, 0x54000000,
                    0, 0, 1, 1, MemorySegment.ofAddress(parentHwnd), MemorySegment.NULL,
                    instance, MemorySegment.NULL,
                ) as MemorySegment
            }
            check(child != MemorySegment.NULL) { "CreateWindowExW failed" }
            WindowsSurfaceHost(child, instance, viewport)
        }
    }

    override fun createSurface(bridge: NativeBridge, deviceHandle: Long): DawnSurface =
        DawnSurface.createWindows(bridge, deviceHandle, hwnd.address(), hinstance.address())

    /** Compose layout coordinates and GetClientRect are both physical pixels. */
    override fun pixelSize(): Pair<Int, Int> = onWindowsEdt {
        check(!closed) { "the Windows surface host is closed" }
        if (!Win32.isWindow(hwnd)) throw CancellationException("the Compose window was closed")
        val bounds = viewport.get()
        if (bounds.width <= 0 || bounds.height <= 0) return@onWindowsEdt 0 to 0
        if (bounds != lastBounds) {
            // HWND_TOP, SWP_NOACTIVATE. Keep the particle child above Skiko's canvas.
            val moved = Win32.setWindowPos.invokeWithArguments(
                hwnd, MemorySegment.NULL, bounds.x, bounds.y, bounds.width, bounds.height, 0x10,
            ) as Int
            check(moved != 0) { "SetWindowPos failed" }
            lastBounds = Rectangle(bounds)
        }
        Arena.ofConfined().use { arena ->
            val rect = arena.allocate(16L, 4L)
            val success = Win32.getClientRect.invokeWithArguments(hwnd, rect) as Int
            check(success != 0) { "GetClientRect failed" }
            (rect.get(JAVA_INT, 8L) - rect.get(JAVA_INT, 0L)) to
                (rect.get(JAVA_INT, 12L) - rect.get(JAVA_INT, 4L))
        }
    }

    override fun close() = onWindowsEdt {
        if (!closed) {
            closed = true
            messagePump.stop()
            // Windows may already have destroyed the child with its parent.
            if (Win32.isWindow(hwnd)) {
                check(Win32.destroyWindow.invokeWithArguments(hwnd) as Int != 0) { "DestroyWindow failed" }
            }
        }
    }

    private fun pumpMessages() {
        if (closed) return
        Arena.ofConfined().use { arena ->
            val message = arena.allocate(48L, 8L) // Win64 MSG
            // PM_REMOVE; bound work so a busy native queue cannot starve Compose.
            repeat(256) {
                val available = Win32.peekMessage.invokeWithArguments(
                    message, MemorySegment.NULL, 0, 0, 1,
                ) as Int
                if (available == 0) return@use
                Win32.translateMessage.invokeWithArguments(message)
                Win32.dispatchMessage.invokeWithArguments(message)
            }
        }
    }
}

private fun <T> onWindowsEdt(operation: () -> T): T {
    if (SwingUtilities.isEventDispatchThread()) return operation()
    var result: Result<T>? = null
    SwingUtilities.invokeAndWait { result = runCatching(operation) }
    return requireNotNull(result).getOrThrow()
}

/** Win64 ABI: BOOL/DWORD are 32 bits, HWND/HINSTANCE are pointer sized. */
private object Win32 {
    private val linker = Linker.nativeLinker()
    private val user32 = SymbolLookup.libraryLookup("user32.dll", Arena.global())
    private val kernel32 = SymbolLookup.libraryLookup("kernel32.dll", Arena.global())

    val getModuleHandle = linker.downcallHandle(
        kernel32.find("GetModuleHandleW").orElseThrow(), FunctionDescriptor.of(ADDRESS, ADDRESS)
    )
    val createWindow = linker.downcallHandle(
        user32.find("CreateWindowExW").orElseThrow(),
        FunctionDescriptor.of(ADDRESS, JAVA_INT, ADDRESS, ADDRESS, JAVA_INT,
            JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, ADDRESS, ADDRESS, ADDRESS, ADDRESS)
    )
    val setWindowPos = linker.downcallHandle(
        user32.find("SetWindowPos").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT)
    )
    val getClientRect = linker.downcallHandle(
        user32.find("GetClientRect").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS)
    )
    val destroyWindow = linker.downcallHandle(
        user32.find("DestroyWindow").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS)
    )
    val peekMessage = linker.downcallHandle(
        user32.find("PeekMessageW").orElseThrow(),
        FunctionDescriptor.of(JAVA_INT, ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, JAVA_INT)
    )
    val translateMessage = linker.downcallHandle(
        user32.find("TranslateMessage").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS)
    )
    val dispatchMessage = linker.downcallHandle(
        user32.find("DispatchMessageW").orElseThrow(), FunctionDescriptor.of(JAVA_LONG, ADDRESS)
    )
    private val isWindowHandle = linker.downcallHandle(
        user32.find("IsWindow").orElseThrow(), FunctionDescriptor.of(JAVA_INT, ADDRESS)
    )
    fun isWindow(hwnd: MemorySegment): Boolean = isWindowHandle.invokeWithArguments(hwnd) as Int != 0
}
