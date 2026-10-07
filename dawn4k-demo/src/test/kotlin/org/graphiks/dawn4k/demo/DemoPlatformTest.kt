package org.graphiks.dawn4k.demo

import kotlin.test.Test
import kotlin.test.assertEquals

class DemoPlatformTest {
    @Test fun recognizesSupportedPlatformsWithoutLoadingNativeLibraries() {
        assertEquals(DemoPlatform.Linux, detectDemoPlatform("Linux"))
        assertEquals(DemoPlatform.Linux, detectDemoPlatform("LINUX"))
        assertEquals(DemoPlatform.MacOS, detectDemoPlatform("Mac OS X"))
        assertEquals(DemoPlatform.Windows, detectDemoPlatform("Windows 11"))
        assertEquals(null, detectDemoPlatform("FreeBSD"))
        assertEquals(null, detectDemoPlatform(""))
    }
}
