@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import platform.CoreFoundation.CFRunLoopRunInMode
import platform.CoreFoundation.kCFRunLoopDefaultMode
import platform.Foundation.NSThread
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Native's command-line test runner needs to pump UIKit's main queue while the test suspends. */
internal fun appleMainTest(block: suspend CoroutineScope.() -> Unit) {
    check(NSThread.isMainThread)
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val task = scope.async { coroutineScope(block) }
    val started = TimeSource.Monotonic.markNow()
    try {
        while (!task.isCompleted) {
            if (started.elapsedNow() > 60.seconds) task.cancel("Apple main-thread test timed out")
            check(started.elapsedNow() < 75.seconds) { "Apple main-thread cleanup did not settle" }
            CFRunLoopRunInMode(kCFRunLoopDefaultMode, 0.01, true)
        }
        runBlocking { task.await() }
    } finally { scope.cancel() }
}
