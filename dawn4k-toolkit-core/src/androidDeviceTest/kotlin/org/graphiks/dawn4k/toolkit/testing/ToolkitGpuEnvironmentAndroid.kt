package org.graphiks.dawn4k.toolkit.testing

import android.os.Build
import androidx.test.platform.app.InstrumentationRegistry
import org.graphiks.dawn4k.DawnBackend

internal actual fun toolkitGpuEnv(name: String): String? = InstrumentationRegistry.getArguments().getString(name) ?: System.getenv(name)
internal actual val toolkitGpuTarget: String get() = "android-${Build.SUPPORTED_ABIS.first()}-${Build.MODEL}"
internal actual val toolkitGpuBackend: DawnBackend get() = DawnBackend.Vulkan
