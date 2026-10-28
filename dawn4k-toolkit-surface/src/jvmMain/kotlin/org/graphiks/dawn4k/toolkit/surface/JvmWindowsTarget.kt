package org.graphiks.dawn4k.toolkit.surface

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.kffi.win32.generated.GetClientRect
import org.graphiks.kffi.win32.generated.init

fun DawnSurfaceTargets.win32(
    hwnd: MemorySegment, hinstance: MemorySegment, owner: DawnPlatformOwner,
    states: StateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget = win32(hwnd.address(), hinstance.address(), owner, states, retention)

/** Win32 client rectangles already use physical pixels; do not multiply by DPI a second time. */
suspend fun DawnSurfaceTargets.win32PixelExtent(hwnd: Long, owner: DawnPlatformOwner): PixelExtent {
    require(hwnd != 0L)
    check(surfacePlatform == SurfacePlatform.Windows)
    return owner.onOwner {
        init()
        Arena.ofConfined().use { arena ->
            val rect = arena.allocate(16L, 4L)
            check(GetClientRect(MemorySegment.ofAddress(hwnd), rect) != 0) { "GetClientRect failed" }
            PixelExtent(rect.get(JAVA_INT, 8L) - rect.get(JAVA_INT, 0L), rect.get(JAVA_INT, 12L) - rect.get(JAVA_INT, 4L))
        }
    }
}
