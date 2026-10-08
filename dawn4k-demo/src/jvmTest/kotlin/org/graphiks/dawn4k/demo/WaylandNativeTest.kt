package org.graphiks.dawn4k.demo

import java.lang.foreign.MemoryLayout.PathElement.groupElement
import java.lang.foreign.MemorySegment
import java.nio.file.Files
import java.nio.file.attribute.PosixFilePermission
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue

class WaylandNativeTest {
    @Test fun packagedLibraryAndFfmLayoutMatchCompiledOracle() {
        if (!waylandTestsEnabled()) return
        val target = if (System.getProperty("os.arch") in setOf("aarch64", "arm64")) "linuxArm64" else "linuxX64"
        val oracle = ProcessBuilder("build/wayland/$target/abi").redirectErrorStream(true).start()
        assertTrue(oracle.waitFor(5, TimeUnit.SECONDS))
        assertEquals(0, oracle.exitValue())
        val expected = oracle.inputStream.bufferedReader().readText().trim()
            .split(' ').associate { val pair = it.split('='); pair[0] to pair[1].toLong() }
        assertEquals(expected["size"], WaylandNative.eventLayout.byteSize())
        assertEquals(expected["align"], WaylandNative.eventLayout.byteAlignment())
        for (name in listOf("type", "width", "height", "scale", "serial", "code"))
            assertEquals(expected[name], WaylandNative.eventLayout.byteOffset(groupElement(name)))
        assertTrue(Files.isRegularFile(WaylandNative.libraryPath))
        assertTrue(!Files.getPosixFilePermissions(WaylandNative.libraryPath.parent).contains(PosixFilePermission.OTHERS_READ))
        assertTrue(!WaylandNative.libraryPath.toString().contains("build/wayland"))
        val host = WaylandNative.open("dawn4k ABI native test", 640, 480)
        try {
            assertTrue(WaylandNative.display(host) != MemorySegment.NULL)
            assertTrue(WaylandNative.surface(host) != MemorySegment.NULL)
            WaylandNative.title(host, "dawn4k ABI title")
            WaylandNative.scale(host, 1)
            WaylandNative.wake(host)
            WaylandNative.nextEvent(host, 16)
        } finally { WaylandNative.close(host) }
    }

    @Test fun invalidInitialDimensionsFailBeforeCallingProtocol() {
        if (!waylandTestsEnabled()) return
        assertFailsWith<IllegalArgumentException> { WaylandNative.open("invalid", 0, 480) }
    }
}

internal fun waylandTestsEnabled(): Boolean =
    System.getProperty("os.name").contains("Linux") && System.getenv("DAWN_WAYLAND_TESTS") == "1"
