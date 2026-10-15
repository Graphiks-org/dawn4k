package org.graphiks.dawn4k.toolkit.internal

@Suppress("DEPRECATION")
internal actual fun currentOsThreadId(): ULong = Thread.currentThread().id.toULong()
