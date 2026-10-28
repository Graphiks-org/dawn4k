package org.graphiks.dawn4k.toolkit.surface

import kotlin.test.*

class PixelExtentTest {
    @Test fun physicalExtentChangesWithDpi() {
        assertEquals(PixelExtent(300, 150), PixelExtent.fromLogical(100.0, 50.0, 3.0))
        assertEquals(PixelExtent(150, 75), PixelExtent.fromLogical(100.0, 50.0, 1.5))
        assertEquals(PixelExtent(0, 0), PixelExtent.fromLogical(0.0, 0.0, 2.0))
    }
    @Test fun invalidScaleAndOverflowAreRejected() {
        assertFailsWith<IllegalArgumentException> { PixelExtent.fromLogical(1.0, 1.0, Double.NaN) }
        assertFailsWith<IllegalArgumentException> { PixelExtent.fromLogical(1.0, 1.0, 0.0) }
        assertFailsWith<IllegalArgumentException> { PixelExtent.fromLogical(Double.MAX_VALUE, 1.0, 2.0) }
    }
}
