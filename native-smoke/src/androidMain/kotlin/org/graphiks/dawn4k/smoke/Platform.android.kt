package org.graphiks.dawn4k.smoke

actual fun smokeTargetName(): String =
    "android-${android.os.Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}"

actual fun smokeIsMacOs(): Boolean = false
