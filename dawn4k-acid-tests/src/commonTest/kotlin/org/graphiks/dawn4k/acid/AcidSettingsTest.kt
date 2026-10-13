package org.graphiks.dawn4k.acid

import org.graphiks.dawn4k.DawnBackend
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class AcidSettingsTest {
    @Test
    fun defaultsToDefaultBackendAndNonStrictMode() {
        assertEquals(AcidSettings(null, false), AcidSettings.parse(null, null))
        assertEquals(false, AcidSettings.parse(null, "0").requireAdapter)
    }

    @Test
    fun selectsBackendAndStrictMode() {
        assertEquals(AcidSettings(DawnBackend.Vulkan, true), AcidSettings.parse("Vulkan", "1"))
    }

    @Test
    fun rejectsInvalidEnvironmentValues() {
        assertFailsWith<IllegalArgumentException> { AcidSettings.parse(null, "yes") }
        assertFailsWith<IllegalArgumentException> { AcidSettings.parse("Typo", "0") }
    }
}
