@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.internal

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.fflush
import platform.posix.fwrite
import platform.posix.stderr

internal actual fun writeStandardError(message: String) {
    val bytes = message.encodeToByteArray()
    if (bytes.isNotEmpty()) bytes.usePinned {
        fwrite(it.addressOf(0), 1u, bytes.size.toULong(), stderr)
        fflush(stderr)
    }
}
