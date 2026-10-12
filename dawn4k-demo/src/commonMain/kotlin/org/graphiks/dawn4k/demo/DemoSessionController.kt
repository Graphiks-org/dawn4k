package org.graphiks.dawn4k.demo

import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.TimeSource

private val demoTimeOrigin = TimeSource.Monotonic.markNow()
internal fun demoMonotonicNanos(): Long = demoTimeOrigin.elapsedNow().inWholeNanoseconds

/** Commands serialize on one owner; the frame lock acknowledges native retirement. */
internal class DemoSessionController(
    scope: CoroutineScope,
    internal val nowNanos: () -> Long = ::demoMonotonicNanos,
    val controls: ParticleControls = ParticleControls(),
) {
    private val mutableState = MutableStateFlow(DemoSessionState())
    val state = mutableState.asStateFlow()
    private val mutableSurface = MutableStateFlow<DemoSurfaceLease?>(null)
    val surface = mutableSurface.asStateFlow()
    val evidence = DemoEvidence(this)
    private val frameLock = Mutex()
    internal val frameClock = ParticleFrameClock()
    // Accessed only under frameLock, by the frame owner or command owner.
    internal var releasePresentation: (suspend () -> Unit)? = null
    private val commands = Channel<suspend () -> Unit>(Channel.UNLIMITED)
    private var finalized = false
    private val sessionScope = CoroutineScope(scope.coroutineContext + SupervisorJob(scope.coroutineContext[Job]))
    private var renderer: Job? = null
    private val owner = sessionScope.launch {
        try {
            startRenderer()
            for (command in commands) command()
        } finally {
            withContext(NonCancellable) {
                finalized = true
                commands.close()
                stopOwned()
                // Settle acknowledged callers and close queued leases even when
                // the caller-owned scope cancelled the command owner first.
                for (command in commands) command()
            }
        }
    }

    private fun startRenderer() {
        val id = state.value.id
        renderer = sessionScope.launch(Dispatchers.Default) {
            try { runParticleDemo(this@DemoSessionController, id) }
            catch (cancelled: CancellationException) { throw cancelled }
            catch (failure: Throwable) { failed(id, failure.message ?: failure.toString()) }
        }
    }

    internal suspend fun <T> withSurface(block: suspend (DemoSurfaceLease?) -> T): T =
        frameLock.withLock { block(mutableSurface.value) }

    private suspend fun retireSurface() = frameLock.withLock {
        val lease = mutableSurface.value
        mutableSurface.value = null
        try { releasePresentation?.invoke() }
        finally {
            releasePresentation = null
            lease?.close()
        }
    }

    fun attach(lease: DemoSurfaceLease) {
        val sent = commands.trySend {
            if (finalized || state.value.phase !in listOf(DemoPhase.Initializing, DemoPhase.Running) || !lease.valid) {
                lease.close()
            } else if (surface.value !== lease) {
                retireSurface()
                frameLock.withLock { mutableSurface.value = lease }
            }
        }
        if (sent.isFailure) lease.close()
    }

    suspend fun detach(generation: Long) = acknowledged {
        if (surface.value?.generation == generation) retireSurface()
    }

    fun lifecycle(active: Boolean) {
        commands.trySend {
            if (!finalized) frameLock.withLock {
                mutableState.value = state.value.copy(lifecycleActive = active)
                if (!active) frameClock.suspend()
            }
        }
    }

    internal fun ready(id: Long) {
        commands.trySend {
            if (state.value.id == id && state.value.phase == DemoPhase.Initializing)
                mutableState.value = state.value.copy(phase = DemoPhase.Running)
        }
    }

    internal fun failed(id: Long, message: String) {
        commands.trySend {
            if (!finalized && state.value.id == id && state.value.error == null &&
                state.value.phase in listOf(DemoPhase.Initializing, DemoPhase.Running)) {
                controls.fail(message, id)
                mutableState.value = state.value.copy(phase = DemoPhase.Failed, error = message)
                renderer?.cancelAndJoin()
                retireSurface()
            }
        }
    }

    private suspend fun stopOwned() {
        if (state.value.phase == DemoPhase.Stopped) return
        controls.deactivate()
        mutableState.value = state.value.copy(phase = DemoPhase.Stopping)
        renderer?.cancelAndJoin()
        renderer = null
        retireSurface()
        mutableState.value = state.value.copy(phase = DemoPhase.Stopped)
    }

    suspend fun stop() = acknowledged { stopOwned() }

    suspend fun restart() = acknowledged {
        if (!finalized && state.value.phase in listOf(DemoPhase.Stopped, DemoPhase.Failed)) {
            stopOwned()
            val id = state.value.id + 1
            controls.beginSession(id)
            mutableState.value = DemoSessionState(id = id, lifecycleActive = state.value.lifecycleActive)
            startRenderer()
        }
    }

    suspend fun close() {
        acknowledged {
            if (!finalized) {
                finalized = true
                stopOwned()
                commands.close()
            }
        }
        owner.join()
        sessionScope.cancel()
    }

    private suspend fun acknowledged(command: suspend () -> Unit) {
        val answer = CompletableDeferred<Unit>()
        if (commands.trySend {
                try { command(); answer.complete(Unit) }
                catch (failure: Throwable) { answer.completeExceptionally(failure) }
            }.isSuccess) answer.await()
    }
}
