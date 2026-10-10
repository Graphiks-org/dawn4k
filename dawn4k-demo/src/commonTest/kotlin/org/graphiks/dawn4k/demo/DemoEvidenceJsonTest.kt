package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertTrue

class DemoEvidenceJsonTest {
    @Test fun nativeDiagnosticsRetainPresentedFramesAndEscapeErrors() {
        val json = DemoEvidenceSnapshot(frameCount = 3, backend = "Vulkan",
            firstError = "bad \"device\"\n", buttons = mapOf("Pause" to LogicalViewport(1f, 2f, 48f, 48f))).toJson()
        assertTrue(json.contains("\"frameCount\":3"))
        assertTrue(json.contains("\"backend\":\"Vulkan\""))
        assertTrue(json.contains("bad \\\"device\\\"\\n"))
        assertTrue(json.contains("\"Pause\":{\"x\":1.0,\"y\":2.0,\"width\":48.0,\"height\":48.0}"))
    }
}
