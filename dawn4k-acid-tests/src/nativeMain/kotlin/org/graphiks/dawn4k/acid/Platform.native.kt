@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class, kotlin.experimental.ExperimentalNativeApi::class)

package org.graphiks.dawn4k.acid

import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fwrite
import platform.posix.getenv

internal actual fun environment(name: String): String? = getenv(name)?.toKString()
internal actual fun targetName(): String = when (kotlin.native.Platform.osFamily) {
    kotlin.native.OsFamily.MACOSX -> "macosArm64"
    kotlin.native.OsFamily.LINUX -> "linuxX64"
    else -> error("Unsupported acid-test target")
}

internal actual fun writeReportFile(path: String, json: String) {
    val file = checkNotNull(fopen(path, "wb")) { "Cannot open report: $path" }
    var original: Throwable? = null
    try {
        val bytes = json.encodeToByteArray()
        if (bytes.isNotEmpty()) bytes.usePinned {
            check(fwrite(it.addressOf(0), 1.convert(), bytes.size.convert(), file).toLong() == bytes.size.toLong()) {
                "Incomplete report write: $path"
            }
        }
    } catch (failure: Throwable) {
        original = failure
        throw failure
    } finally {
        if (fclose(file) != 0) preserveFailure(original, IllegalStateException("Cannot close report: $path"))
    }
}
