package org.graphiks.dawn4k.webgpu.toolkit.target

/**
 * Platform-specific surface types for native window system integration.
 */
enum class PlatformSurfaceType {
    /** Metal layer for macOS/iOS platforms */
    MetalLayer,
    /** Win32 window surface for Windows */
    Win32,
    /** X11 window surface for Linux/X11 */
    X11,
    /** Wayland surface for Linux/Wayland */
    Wayland,
}
