package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import kotlin.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class ViewRetirementTest {
    @Test fun hostScopeWaitsForNativeRetirementAcknowledgment() = runTest {
        val host = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val entered = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val retirement = host.launchViewRetirement(null) { entered.complete(Unit); release.await() }
        try {
            runCurrent()
            entered.await()
            host.cancel()
            val joined = backgroundScope.async { host.coroutineContext.job.join() }
            runCurrent()
            assertFalse(joined.isCompleted, "the host scope must retain cleanup until native acknowledgment")
        } finally {
            release.complete(Unit)
            retirement.join()
            host.cancel()
        }
    }

    @Test fun viewRetirementJoinsRendererCleanupFirst() = runTest {
        val host = CoroutineScope(SupervisorJob() + StandardTestDispatcher(testScheduler))
        val rendererRelease = CompletableDeferred<Unit>()
        var rendererRetired = false
        var viewRetired = false
        val renderer = host.launch(start = CoroutineStart.UNDISPATCHED) {
            try { awaitCancellation() }
            finally { withContext(NonCancellable) { rendererRelease.await(); rendererRetired = true } }
        }
        val retirement = host.launchViewRetirement(renderer) {
            assertTrue(rendererRetired)
            viewRetired = true
        }
        try {
            runCurrent()
            assertFalse(viewRetired)
            rendererRelease.complete(Unit)
            retirement.join()
            assertTrue(viewRetired)
        } finally { rendererRelease.complete(Unit); retirement.join(); host.cancel() }
    }
}
