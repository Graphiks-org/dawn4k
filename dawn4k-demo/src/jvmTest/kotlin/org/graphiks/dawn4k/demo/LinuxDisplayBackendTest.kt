package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull
import kotlin.test.assertTrue

class LinuxDisplayBackendTest {
    @Test fun selectsEachEndpointAndPrefersWaylandWhenBothExist() {
        assertEquals(LinuxDisplayBackend.Wayland, selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to "wayland-1")))
        assertEquals(LinuxDisplayBackend.Wayland, selectLinuxDisplayBackend(mapOf("WAYLAND_SOCKET" to "7")))
        assertEquals(LinuxDisplayBackend.X11, selectLinuxDisplayBackend(mapOf("DISPLAY" to ":3")))
        assertEquals(LinuxDisplayBackend.Wayland,
            selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to "stale", "DISPLAY" to ":0")))
        assertEquals(LinuxDisplayBackend.X11,
            selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to " ", "WAYLAND_SOCKET" to "", "DISPLAY" to ":0")))
    }

    @Test fun reportsMissingEndpointsWithoutLoadingNativeLibraries() {
        val failure = assertFailsWith<IllegalStateException> {
            selectLinuxDisplayBackend(mapOf("DISPLAY" to " ", "WAYLAND_DISPLAY" to "", "WAYLAND_SOCKET" to " "))
        }
        assertTrue(failure.message.orEmpty().contains("DISPLAY"))
        assertTrue(failure.message.orEmpty().contains("WAYLAND"))
        assertTrue(failure.message.orEmpty().contains("/opt/demo/demo.sh"))
    }

    @Test fun forcingRequiresTheSelectedEndpointAndNeverFallsBack() {
        val both = mapOf("WAYLAND_DISPLAY" to "wayland-1", "DISPLAY" to ":0")
        assertEquals(LinuxDisplayBackend.X11, selectLinuxDisplayBackend(both, "x11"))
        assertEquals(LinuxDisplayBackend.Wayland, selectLinuxDisplayBackend(both, "wayland"))
        assertFailsWith<IllegalStateException> { selectLinuxDisplayBackend(mapOf("DISPLAY" to ":0"), "wayland") }
        assertFailsWith<IllegalStateException> { selectLinuxDisplayBackend(mapOf("WAYLAND_DISPLAY" to "wayland-1"), "x11") }
        assertFailsWith<IllegalArgumentException> { selectLinuxDisplayBackend(both, "auto") }
    }

    @Test fun parsesOptionalOverrideAndRejectsMalformedArguments() {
        assertNull(parseLinuxBackendOverride(emptyArray(), DemoPlatform.Linux))
        assertEquals("wayland", parseLinuxBackendOverride(arrayOf("--platform=wayland"), DemoPlatform.Linux))
        assertEquals("x11", parseLinuxBackendOverride(arrayOf("--platform=x11"), DemoPlatform.Linux))
        for (args in listOf(arrayOf("--platform"), arrayOf("--platform="), arrayOf("--platform=auto"),
            arrayOf("--unknown"), arrayOf("--platform=x11", "--platform=wayland"))) {
            assertFailsWith<IllegalArgumentException> { parseLinuxBackendOverride(args, DemoPlatform.Linux) }
        }
        for (platform in listOf(DemoPlatform.MacOS, DemoPlatform.Windows)) {
            assertNull(parseLinuxBackendOverride(emptyArray(), platform))
            assertFailsWith<IllegalArgumentException> { parseLinuxBackendOverride(arrayOf("--platform=x11"), platform) }
        }
    }
}
