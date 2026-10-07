package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class WaylandHostStateTest {
    @Test fun initialConfigureAndScaleDeterminePixels() {
        val state = WaylandHostState(640, 480)
        assertEquals(0 to 0, state.pixelSize())
        state.configure(0, 0, 2)
        assertEquals(1280 to 960, state.pixelSize())
        state.configure(900, 600, 1)
        assertEquals(900 to 600, state.pixelSize())
        state.configure(0, 700, 2)
        assertEquals(1800 to 1400, state.pixelSize())
        state.close()
        assertFailsWith<IllegalStateException> { state.pixelSize() }
    }

    @Test fun invalidConfigureDoesNotPublishInvalidOrOverflowingDimensions() {
        val state = WaylandHostState(640, 480)
        state.configure(640, 480, 1)
        for ((width, height, scale) in listOf(Triple(-1, 480, 1), Triple(640, -1, 1),
            Triple(640, 480, 0), Triple(Int.MAX_VALUE, 480, 2))) {
            assertFailsWith<IllegalArgumentException> { state.configure(width, height, scale) }
            assertEquals(640 to 480, state.pixelSize())
        }
    }

    @Test fun closeBeforeConfigureIsIdempotentAndPreventsFurtherUpdates() {
        assertFailsWith<IllegalArgumentException> { WaylandHostState(0, 480) }
        val state = WaylandHostState(640, 480)
        state.close()
        state.close()
        assertFailsWith<IllegalStateException> { state.configure(640, 480, 1) }
    }
}
