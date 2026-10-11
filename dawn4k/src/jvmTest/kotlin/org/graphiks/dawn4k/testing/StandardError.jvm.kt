package org.graphiks.dawn4k.testing

import java.io.ByteArrayOutputStream
import java.io.PrintStream

internal actual fun captureStandardError(block: () -> Unit): String {
    val previous = System.err
    val bytes = ByteArrayOutputStream()
    PrintStream(bytes, true, Charsets.UTF_8).use { output ->
        System.setErr(output)
        try { block() } finally { System.setErr(previous) }
    }
    return bytes.toString(Charsets.UTF_8)
}
