@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)
package org.graphiks.dawn4k.toolkit.testing

import kotlinx.cinterop.toKString
import kotlin.native.Platform
import kotlin.native.OsFamily
import org.graphiks.dawn4k.DawnBackend
import platform.posix.getenv

internal actual fun toolkitGpuEnv(name: String): String? = getenv(name)?.toKString()
internal actual val toolkitGpuTarget: String get() = "native-${Platform.osFamily}-${Platform.cpuArchitecture}"
internal actual val toolkitGpuBackend: DawnBackend get() = when (Platform.osFamily) {
    OsFamily.LINUX -> DawnBackend.Vulkan
    else -> DawnBackend.Metal
}
