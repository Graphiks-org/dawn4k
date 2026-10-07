package org.graphiks.dawn4k.demo

import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WaylandSurfaceHostTest {
    @Test fun opensConfiguresAndReleasesTheOwnerRepeatedly() {
        if (!waylandTestsEnabled()) return
        repeat(3) {
            val host = WaylandSurfaceHost.open(ParticleControls()) {}
            assertTrue(awaitWaylandSize(host).first > 0)
            host.close()
            host.close()
            assertFailsWith<IllegalStateException> { host.pixelSize() }
        }
        assertEquals(0, Thread.getAllStackTraces().keys.count {
            it.isAlive && it.name.startsWith("dawn-wayland-owner-")
        })
    }

    @Test fun closesBeforeConfigureWithoutLeavingAnOwner() {
        if (!waylandTestsEnabled()) return
        repeat(3) { WaylandSurfaceHost.open(ParticleControls()) {}.close() }
        assertEquals(0, Thread.getAllStackTraces().keys.count {
            it.isAlive && it.name.startsWith("dawn-wayland-owner-")
        })
    }
}

internal fun awaitWaylandSize(host: SurfaceHost, predicate: (Pair<Int, Int>) -> Boolean = { it.first > 0 && it.second > 0 }): Pair<Int, Int> {
    val deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10)
    while (System.nanoTime() < deadline) {
        val size = host.pixelSize()
        if (predicate(size)) return size
        Thread.sleep(10)
    }
    error("Wayland configure timed out")
}
