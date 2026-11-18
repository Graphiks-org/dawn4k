package org.graphiks.dawn4k.webgpu.toolkit.internal

import platform.posix.pthread_self

internal actual fun currentOsThreadId(): ULong = pthread_self()
