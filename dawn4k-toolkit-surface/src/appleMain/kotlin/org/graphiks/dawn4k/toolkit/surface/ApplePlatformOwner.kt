package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.Dispatchers
import platform.Foundation.NSThread

internal fun requireAppleMainThread() {
    check(NSThread.isMainThread) { "Apple view operations require the platform main thread" }
}

internal fun appleMainOwner(): DawnPlatformOwner {
    val delegate = DawnPlatformOwner.fromDispatcher(Dispatchers.Main.immediate)
    return object : DawnPlatformOwner {
        override suspend fun <T> onOwner(block: suspend () -> T): T = delegate.onOwner {
            requireAppleMainThread()
            block()
        }
    }
}
