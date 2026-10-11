package org.graphiks.dawn4k.demo

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val evidenceJson = Json {
    encodeDefaults = true
    explicitNulls = true
}

/** Stable private qualification format shared by both mobile launchers. */
internal fun DemoEvidenceSnapshot.toJson(): String = evidenceJson.encodeToString(this)
