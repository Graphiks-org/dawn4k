package org.graphiks.dawn4k.demo

import java.awt.Rectangle
import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_CHAR
import java.lang.foreign.ValueLayout.JAVA_INT
import java.util.concurrent.atomic.AtomicReference
import javax.swing.SwingUtilities
import javax.swing.Timer
import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.toolkit.surface.*
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.kffi.win32.generated.CreateWindowExW
import org.graphiks.kffi.win32.generated.DestroyWindow
import org.graphiks.kffi.win32.generated.DispatchMessageW
import org.graphiks.kffi.win32.generated.GetClientRect
import org.graphiks.kffi.win32.generated.GetModuleHandleW
import org.graphiks.kffi.win32.generated.IsWindow
import org.graphiks.kffi.win32.generated.PeekMessageW
import org.graphiks.kffi.win32.generated.SetWindowPos
import org.graphiks.kffi.win32.generated.TranslateMessage
import org.graphiks.kffi.win32.generated.init

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
            init()
            val instance = GetModuleHandleW(MemorySegment.NULL)
            check(instance != MemorySegment.NULL) { "GetModuleHandleW failed" }
            val child = Arena.ofConfined().use { arena ->
                val className = arena.allocate(14L, 2L)
                "STATIC\u0000".forEachIndexed { index, char -> className.set(JAVA_CHAR, index * 2L, char) }
                // WS_CHILD | WS_VISIBLE | WS_CLIPSIBLINGS. The AWT EDT owns the HWND.
                CreateWindowExW(
                    0, className, MemorySegment.NULL, 0x54000000,
                    0, 0, 1, 1, MemorySegment.ofAddress(parentHwnd), MemorySegment.NULL,
                    instance, MemorySegment.NULL,
                )
            }
            check(child != MemorySegment.NULL) { "CreateWindowExW failed" }
            WindowsSurfaceHost(child, instance, viewport)
        }
    }

    override fun createTarget(states: StateFlow<DawnTargetState>): DawnSurfaceTarget =
        DawnSurfaceTargets.win32(hwnd, hinstance, DawnPlatformOwners.awt(), states, BorrowedDemoHostRetention)

    /** Compose layout coordinates and GetClientRect are both physical pixels. */
    override fun pixelSize(): Pair<Int, Int> = onWindowsEdt {
        check(!closed) { "the Windows surface host is closed" }
        if (IsWindow(hwnd) == 0) throw CancellationException("the Compose window was closed")
        val bounds = viewport.get()
        if (bounds.width <= 0 || bounds.height <= 0) return@onWindowsEdt 0 to 0
        if (bounds != lastBounds) {
            // HWND_TOP, SWP_NOACTIVATE. Keep the particle child above Skiko's canvas.
            val moved = SetWindowPos(
                hwnd, MemorySegment.NULL, bounds.x, bounds.y, bounds.width, bounds.height, 0x10,
            )
            check(moved != 0) { "SetWindowPos failed" }
            lastBounds = Rectangle(bounds)
        }
        Arena.ofConfined().use { arena ->
            val rect = arena.allocate(16L, 4L)
            val success = GetClientRect(hwnd, rect)
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
            if (IsWindow(hwnd) != 0) {
                check(DestroyWindow(hwnd) != 0) { "DestroyWindow failed" }
            }
        }
    }

    private fun pumpMessages() {
        if (closed) return
        Arena.ofConfined().use { arena ->
            val message = arena.allocate(48L, 8L) // Win64 MSG
            // PM_REMOVE; bound work so a busy native queue cannot starve Compose.
            repeat(256) {
                val available = PeekMessageW(message, MemorySegment.NULL, 0, 0, 1)
                if (available == 0) return@use
                TranslateMessage(message)
                DispatchMessageW(message)
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
