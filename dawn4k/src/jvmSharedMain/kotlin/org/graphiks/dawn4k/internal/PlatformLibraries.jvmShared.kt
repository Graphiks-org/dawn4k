package org.graphiks.dawn4k.internal

import java.io.File

internal actual fun preparePlatformLibraries() {
    if (!System.getProperty("os.name").startsWith("Windows")) return
    // Dawn 8077 searches beside its own DLL and java.exe. Its final attempt
    // to load a bare filename uses LOAD_LIBRARY_SEARCH_DLL_LOAD_DIR, which
    // requires an absolute path. Preload the OS shader compiler explicitly;
    // Windows can then reuse the loaded module for Dawn's FXC request.
    val windows = System.getenv("SystemRoot") ?: error("SystemRoot is missing on Windows")
    System.load(File(windows, "System32/d3dcompiler_47.dll").absolutePath)
}
