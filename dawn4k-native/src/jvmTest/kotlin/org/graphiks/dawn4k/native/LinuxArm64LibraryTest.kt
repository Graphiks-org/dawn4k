package org.graphiks.dawn4k.native

import kotlin.test.Test
import kotlin.test.assertNotNull

class LinuxArm64LibraryTest {
    @Test
    fun bundledLibraryCreatesAnInstance() {
        if (!System.getProperty("os.name").contains("Linux") ||
            System.getProperty("os.arch") !in listOf("aarch64", "arm64")) return
        val instance = assertNotNull(wgpuCreateInstance(null))
        wgpuInstanceRelease(instance)
    }
}
