package org.graphiks.dawn4k.toolkit.surface

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.kffi.x11.generated.XGetGeometry

fun DawnSurfaceTargets.x11(
    display: MemorySegment, window: ULong, owner: DawnPlatformOwner,
    states: StateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget = x11(display.address(), window, owner, states, retention)

/** The caller owns Xlib initialization and serializes this display on its platform owner. */
suspend fun DawnSurfaceTargets.x11PixelExtent(display: Long, window: ULong, owner: DawnPlatformOwner): PixelExtent {
    require(display != 0L && window != 0uL)
    check(surfacePlatform == SurfacePlatform.Linux)
    return owner.onOwner {
        Arena.ofConfined().use { arena ->
            val root = arena.allocate(JAVA_LONG)
            val x = arena.allocate(JAVA_INT); val y = arena.allocate(JAVA_INT)
            val width = arena.allocate(JAVA_INT); val height = arena.allocate(JAVA_INT)
            val border = arena.allocate(JAVA_INT); val depth = arena.allocate(JAVA_INT)
            check(XGetGeometry(MemorySegment.ofAddress(display), window.toLong(), root, x, y, width, height, border, depth) != 0)
            PixelExtent(width.get(JAVA_INT, 0L), height.get(JAVA_INT, 0L))
        }
    }
}
