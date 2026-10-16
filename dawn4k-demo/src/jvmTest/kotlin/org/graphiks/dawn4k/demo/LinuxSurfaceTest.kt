package org.graphiks.dawn4k.demo

import kotlin.time.Duration.Companion.seconds

import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import org.graphiks.dawn4k.NativeBridge
import org.graphiks.dawn4k.DawnAdapter
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.DawnDevice
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.foreign.ValueLayout.JAVA_LONG
import kotlinx.coroutines.*
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.*

class LinuxSurfaceTest {
    @Test fun rejectsNullDisplayOrWindowBeforeCallingNativeCode() {
        val owner = org.graphiks.dawn4k.toolkit.surface.DawnPlatformOwners.awt()
        val states = kotlinx.coroutines.flow.MutableStateFlow(org.graphiks.dawn4k.toolkit.surface.DawnTargetState(1,
            org.graphiks.dawn4k.toolkit.surface.PixelExtent(16, 16), true))
        assertFailsWith<IllegalArgumentException> { org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTargets.x11(0L, 1uL, owner, states, BorrowedDemoHostRetention) }
        assertFailsWith<IllegalArgumentException> { org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTargets.x11(1L, 0uL, owner, states, BorrowedDemoHostRetention) }
    }

    @Test fun linuxNegotiatesAndPresentsOnAnXlibSurface() = runBlocking {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_DESKTOP_TESTS") != "1") return@runBlocking
        assertTrue(!System.getenv("DISPLAY").isNullOrEmpty(), "desktop tests require DISPLAY")
        Arena.ofConfined().use { arena ->
            val linker = Linker.nativeLinker()
            val library = SymbolLookup.libraryLookup("libX11.so.6", arena)
            fun function(name: String, descriptor: FunctionDescriptor) =
                linker.downcallHandle(library.find(name).orElseThrow(), descriptor)
            val init = function("XInitThreads", FunctionDescriptor.of(JAVA_INT))
            assertTrue(init.invokeWithArguments() as Int != 0)
            val open = function("XOpenDisplay", FunctionDescriptor.of(ADDRESS, ADDRESS))
            val display = open.invokeWithArguments(MemorySegment.NULL) as MemorySegment
            assertTrue(display != MemorySegment.NULL)
            val close = function("XCloseDisplay", FunctionDescriptor.of(JAVA_INT, ADDRESS))
            val root = function("XDefaultRootWindow", FunctionDescriptor.of(JAVA_LONG, ADDRESS))
                .invokeWithArguments(display) as Long
            val create = function("XCreateSimpleWindow", FunctionDescriptor.of(JAVA_LONG,
                ADDRESS, JAVA_LONG, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_INT, JAVA_LONG, JAVA_LONG))
            val window = create.invokeWithArguments(display, root, 0, 0, 128, 96, 0, 0L, 0L) as Long
            val destroy = function("XDestroyWindow", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG))
            try {
                function("XMapWindow", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_LONG))
                    .invokeWithArguments(display, window)
                function("XSync", FunctionDescriptor.of(JAVA_INT, ADDRESS, JAVA_INT))
                    .invokeWithArguments(display, 0)
                java.util.concurrent.Executors.newSingleThreadExecutor().asCoroutineDispatcher().use { dispatcher ->
                    val states = kotlinx.coroutines.flow.MutableStateFlow(DawnTargetState(1,
                        org.graphiks.dawn4k.toolkit.surface.PixelExtent(128, 96), true))
                    val target = RetiringDemoTarget(DawnSurfaceTargets.x11(display.address(), window.toULong(),
                        DawnPlatformOwner.fromDispatcher(dispatcher), states, BorrowedDemoHostRetention))
                    try {
                        withDawnSurface(target, DawnSessionOptions(config = DawnConfig(DawnBackend.Vulkan), startupTimeout = 60.seconds)) { gpu, presentation ->
                            assertEquals(states.value.extent, presentation.configuration.value?.extent)
                            assertTrue(presentDemoTestFrame(gpu, presentation))
                        }
                    } finally { withContext(NonCancellable) { target.awaitRetirement() } }
                }
            } finally {
                destroy.invokeWithArguments(display, window)
                close.invokeWithArguments(display)
            }
        }
    }
}
