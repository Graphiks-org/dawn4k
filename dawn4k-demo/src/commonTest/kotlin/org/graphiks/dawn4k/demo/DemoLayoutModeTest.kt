package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoLayoutModeTest {
    @Test fun layoutThresholdDependsOnBothAvailableDimensions() {
        assertEquals(LayoutMode.Compact, demoLayoutMode(719f, 600f))
        assertEquals(LayoutMode.Compact, demoLayoutMode(720f, 359f))
        assertEquals(LayoutMode.Wide, demoLayoutMode(720f, 360f))
    }
}
