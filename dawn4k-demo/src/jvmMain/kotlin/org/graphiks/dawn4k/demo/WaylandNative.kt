package org.graphiks.dawn4k.demo

import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemoryLayout
import java.lang.foreign.MemorySegment
import java.lang.foreign.SymbolLookup
import java.lang.foreign.ValueLayout.ADDRESS
import java.lang.foreign.ValueLayout.JAVA_INT
import java.lang.invoke.MethodHandle
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.attribute.PosixFilePermissions

internal data class WaylandEvent(val type: Int, val width: Int, val height: Int,
    val scale: Int, val serial: UInt, val code: Int)

/** Loaded only by the native Wayland entry point; the C bridge owns all protocol callbacks. */
internal object WaylandNative {
    val eventLayout: MemoryLayout = MemoryLayout.structLayout(
        JAVA_INT.withName("type"), JAVA_INT.withName("width"), JAVA_INT.withName("height"),
        JAVA_INT.withName("scale"), JAVA_INT.withName("serial"), JAVA_INT.withName("code"),
    )
    val libraryPath: Path = extractLibrary()
    private val library = try {
        SymbolLookup.libraryLookup(libraryPath, Arena.global())
    } catch (failure: Throwable) {
        throw IllegalStateException("cannot load Wayland bridge $libraryPath; install libwayland-client and libxkbcommon", failure)
    }
    private val linker = Linker.nativeLinker()
    private fun bind(name: String, result: MemoryLayout?, vararg arguments: MemoryLayout): MethodHandle {
        val descriptor = if (result == null) FunctionDescriptor.ofVoid(*arguments) else FunctionDescriptor.of(result, *arguments)
        return linker.downcallHandle(library.find(name).orElseThrow {
            IllegalStateException("missing Wayland bridge symbol $name in $libraryPath")
        }, descriptor)
    }
    private val openCall = bind("dawn_wl_open", ADDRESS, ADDRESS, JAVA_INT, JAVA_INT, ADDRESS, JAVA_INT)
    private val displayCall = bind("dawn_wl_display", ADDRESS, ADDRESS)
    private val surfaceCall = bind("dawn_wl_surface", ADDRESS, ADDRESS)
    private val eventCall = bind("dawn_wl_next_event", JAVA_INT, ADDRESS, ADDRESS, JAVA_INT)
    private val scaleCall = bind("dawn_wl_set_scale", JAVA_INT, ADDRESS, JAVA_INT)
    private val titleCall = bind("dawn_wl_set_title", JAVA_INT, ADDRESS, ADDRESS)
    private val wakeCall = bind("dawn_wl_wake", null, ADDRESS)
    private val closeCall = bind("dawn_wl_close", null, ADDRESS)

    private fun extractLibrary(): Path {
        check(System.getProperty("os.name").contains("Linux")) { "native Wayland requires Linux" }
        val arch = when (System.getProperty("os.arch")) {
            "arm64", "aarch64" -> "linux-aarch64"
            "amd64", "x86_64" -> "linux-x86-64"
            else -> error("unsupported Wayland bridge architecture: ${System.getProperty("os.arch")}")
        }
        val resource = "/$arch/libdawn4k_wayland.so"
        val stream = WaylandNative::class.java.getResourceAsStream(resource)
            ?: error("missing packaged Wayland bridge $resource; build dawn4k-demo on Linux for this architecture")
        return stream.use {
            val directory = Files.createTempDirectory("dawn4k-wayland-", PosixFilePermissions.asFileAttribute(
                PosixFilePermissions.fromString("rwx------")))
            directory.toFile().deleteOnExit()
            val path = directory.resolve("libdawn4k_wayland.so")
            Files.copy(it, path)
            path.toFile().deleteOnExit()
            path
        }
    }

    fun open(title: String, width: Int, height: Int): MemorySegment {
        require(width > 0 && height > 0) { "Wayland initial dimensions must be positive" }
        return Arena.ofConfined().use { arena ->
            val error = arena.allocate(512).fill(0)
            val pointer = openCall.invokeWithArguments(arena.allocateFrom(title), width, height, error, 512) as MemorySegment
            check(pointer != MemorySegment.NULL) { "${error.getString(0)}; WAYLAND_DISPLAY=${System.getenv("WAYLAND_DISPLAY")}" }
            pointer
        }
    }
    fun display(host: MemorySegment): MemorySegment = displayCall.invokeWithArguments(host) as MemorySegment
    fun surface(host: MemorySegment): MemorySegment = surfaceCall.invokeWithArguments(host) as MemorySegment
    fun nextEvent(host: MemorySegment, timeoutMillis: Int): WaylandEvent? = Arena.ofConfined().use { arena ->
        val event = arena.allocate(eventLayout).fill(0)
        val status = eventCall.invokeWithArguments(host, event, timeoutMillis) as Int
        check(status >= 0) { "Wayland connection/event failure (code=${event.get(JAVA_INT, 20)})" }
        if (status == 0) null else WaylandEvent(event.get(JAVA_INT, 0), event.get(JAVA_INT, 4),
            event.get(JAVA_INT, 8), event.get(JAVA_INT, 12), event.get(JAVA_INT, 16).toUInt(), event.get(JAVA_INT, 20))
    }
    fun scale(host: MemorySegment, scale: Int) {
        check(scaleCall.invokeWithArguments(host, scale) as Int == 0) { "Wayland buffer scale update failed" }
    }
    fun title(host: MemorySegment, title: String) = Arena.ofConfined().use { arena ->
        check(titleCall.invokeWithArguments(host, arena.allocateFrom(title)) as Int == 0) { "Wayland title update failed" }
    }
    fun wake(host: MemorySegment) { wakeCall.invokeWithArguments(host) }
    fun close(host: MemorySegment) { closeCall.invokeWithArguments(host) }
}
