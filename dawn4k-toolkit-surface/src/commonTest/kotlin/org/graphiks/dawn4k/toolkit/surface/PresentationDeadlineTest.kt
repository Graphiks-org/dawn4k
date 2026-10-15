package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*
import kotlin.time.Duration.Companion.milliseconds

class PresentationDeadlineTest {
    @Test fun startupDeadlineIncludesUnreturnedTargetRetention() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finishRetirement = CompletableDeferred<Unit>()
        val retired = CompletableDeferred<Unit>()
        val target = object : DawnSurfaceTarget {
            override val state = MutableStateFlow(DawnTargetState(1, PixelExtent(4, 4), true))
            override suspend fun retain(): DawnTargetLease {
                entered.complete(Unit)
                try { awaitCancellation() }
                finally { withContext(NonCancellable) { finishRetirement.await(); retired.complete(Unit) } }
            }
        }
        val opening = async(Dispatchers.Default) {
            runCatching { openDawnSurface(target, DawnSessionOptions(startupTimeout = 100.milliseconds,
                shutdownTimeout = 100.milliseconds)) }
        }
        try {
            withTimeout(2_000) { entered.await() }
            val result = withTimeout(500) { opening.await() }
            assertIs<TimeoutCancellationException>(result.exceptionOrNull())
            assertFalse(retired.isCompleted, "partial retention must remain alive until its acknowledgment")
        } finally {
            finishRetirement.complete(Unit)
            opening.cancelAndJoin()
            withTimeout(2_000) { retired.await() }
        }
    }

    @Test fun startupDeadlineIncludesCapabilityNegotiation() = runBlocking {
        val entered = CompletableDeferred<Unit>()
        val finishCapabilities = CompletableDeferred<Unit>()
        val retired = CompletableDeferred<Unit>()
        val target = CountingTarget(1).apply { onRetire = { retired.complete(Unit) } }
        val native = CountingSurface().apply {
            onCapabilities = { runBlocking { entered.complete(Unit); finishCapabilities.await() } }
        }
        val factory = SurfaceFactory { lease, _ -> SurfaceAttachment(lease, null, { native }, native::close) }
        val opening = async(Dispatchers.Default) {
            runCatching { openDawnSurface(target, DawnSessionOptions(startupTimeout = 250.milliseconds,
                shutdownTimeout = 100.milliseconds), DawnPresentationOptions(), factory) }
        }
        try {
            withTimeout(2_000) { entered.await() }
            val result = withTimeout(500) { opening.await() }
            assertIs<TimeoutCancellationException>(result.exceptionOrNull())
            assertEquals(0, native.closes)
        } finally {
            finishCapabilities.complete(Unit)
            opening.cancelAndJoin()
            withTimeout(2_000) { retired.await() }
        }
        assertEquals(1, native.closes)
        assertEquals(1, target.retirements)
    }
}
