package org.graphiks.dawn4k.toolkit.testing

import org.graphiks.dawn4k.DawnBackend

internal actual fun toolkitGpuEnv(name: String): String? = System.getenv(name)
internal actual val toolkitGpuTarget: String get() = "jvm-${System.getProperty("os.name")}-${System.getProperty("os.arch")}"
internal actual val toolkitGpuBackend: DawnBackend get() = when {
    "mac" in System.getProperty("os.name").lowercase() -> DawnBackend.Metal
    "windows" in System.getProperty("os.name").lowercase() -> DawnBackend.D3D12
    else -> DawnBackend.Vulkan
}
