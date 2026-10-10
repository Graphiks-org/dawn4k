@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.testing

import kotlinx.cinterop.toKString
import platform.posix.getenv

internal actual fun gpuTestEnv(name: String): String? = getenv(name)?.toKString()

// Kotlin/Native has no supported dynamic assumption API. The named warning is
// the evidence of non-execution; the standard runner may report a passed invocation.
internal actual fun gpuTestNotExecuted(warning: String) = Unit
