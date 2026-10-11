package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.jsonObject

class DemoEvidenceJsonTest {
    @Test fun diagnosticsCanBeDecodedAsATypedSnapshot() {
        val snapshot = DemoEvidenceSnapshot(
            phase = DemoPhase.Running,
            firstError = "bad \"device\"\n\t\u0000\\é",
            viewport = LogicalViewport(1f, 2f, 300f, 200f),
            buttons = mapOf("Pause" to LogicalViewport(1f, 2f, 48f, 48f)),
            physicalExtent = PixelExtent(600, 400),
        )
        assertEquals(snapshot, Json.decodeFromString<DemoEvidenceSnapshot>(snapshot.toJson()))
    }

    @Test fun diagnosticsRetainDefaultFieldsAndExplicitNulls() {
        val fields = Json.parseToJsonElement(DemoEvidenceSnapshot().toJson()).jsonObject
        assertEquals(setOf("sessionId", "phase", "backend", "adapter", "count", "paused",
            "resetGeneration", "lifecycleActive", "firstError", "viewport", "buttons",
            "physicalExtent", "surfaceGeneration", "frameCount", "simulationFingerprint",
            "deviceCreations", "surfaceCreations"), fields.keys)
        assertEquals(JsonNull, fields["backend"])
        assertEquals(JsonNull, fields["viewport"])
        assertEquals(JsonNull, fields["simulationFingerprint"])
    }

    @Test fun nativeDiagnosticsRetainPresentedFramesAndEscapeErrors() {
        val json = DemoEvidenceSnapshot(frameCount = 3, backend = "Vulkan",
            firstError = "bad \"device\"\n", buttons = mapOf("Pause" to LogicalViewport(1f, 2f, 48f, 48f))).toJson()
        assertTrue(json.contains("\"frameCount\":3"))
        assertTrue(json.contains("\"backend\":\"Vulkan\""))
        assertTrue(json.contains("bad \\\"device\\\"\\n"))
        assertTrue(json.contains("\"Pause\":{\"x\":1.0,\"y\":2.0,\"width\":48.0,\"height\":48.0}"))
    }
}
