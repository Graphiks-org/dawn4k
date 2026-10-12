@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.testing

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import platform.posix.SEEK_SET
import platform.posix.close
import platform.posix.dup
import platform.posix.dup2
import platform.posix.fclose
import platform.posix.fflush
import platform.posix.fileno
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell
import platform.posix.stderr
import platform.posix.tmpfile

internal actual fun captureStandardError(block: () -> Unit): String {
    val output = checkNotNull(tmpfile())
    val descriptor = fileno(stderr)
    fflush(stderr)
    val previous = dup(descriptor)
    try {
        check(previous >= 0)
        check(dup2(fileno(output), descriptor) >= 0)
        try { block() } finally {
            fflush(stderr)
            check(dup2(previous, descriptor) >= 0)
        }
        val size = ftell(output)
        check(size in 0..Int.MAX_VALUE.toLong())
        check(fseek(output, 0, SEEK_SET) == 0)
        val bytes = ByteArray(size.toInt())
        if (bytes.isNotEmpty()) bytes.usePinned {
            check(fread(it.addressOf(0), 1u, bytes.size.toULong(), output) == bytes.size.toULong())
        }
        return bytes.decodeToString()
    } finally {
        if (previous >= 0) close(previous)
        fclose(output)
    }
}
