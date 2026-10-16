package org.graphiks.dawn4k.toolkit.internal

import platform.posix.pthread_self

internal actual fun currentOsThreadId(): ULong = pthread_self()
