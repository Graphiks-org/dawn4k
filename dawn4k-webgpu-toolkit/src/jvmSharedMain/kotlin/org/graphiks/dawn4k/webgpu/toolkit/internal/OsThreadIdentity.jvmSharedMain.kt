package org.graphiks.dawn4k.webgpu.toolkit.internal

@Suppress("DEPRECATION")
internal actual fun currentOsThreadId(): ULong = Thread.currentThread().id.toULong()
