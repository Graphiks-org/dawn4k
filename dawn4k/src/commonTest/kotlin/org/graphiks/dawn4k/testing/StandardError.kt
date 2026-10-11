package org.graphiks.dawn4k.testing

/** Captures the real platform stderr; callers must not run captures concurrently. */
internal expect fun captureStandardError(block: () -> Unit): String
