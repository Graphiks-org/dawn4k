package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import kotlinx.atomicfu.atomic
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.internal.*
import org.graphiks.webgpu.GPUDeviceLostInfo

/** Owns its context, adapter, device and queue. Exposed handles are borrowed. */
class DawnSession internal constructor(
    val context: DawnContext,
    val adapter: DawnAdapter,
    val device: DawnDevice,
    internal val owner: DawnExecutionOwner,
    private val ownsOwner: Boolean,
    private val options: DawnSessionOptions,
    private val progress: EventProgress,
    private val initialization: DawnPreparedInitialization?,
) {
    val queue: DawnQueue get() = device.queue as DawnQueue
    /** Borrowed owner for platform integrations with their own admission protocol; never close it while the session lives. */
    val executionOwner: DawnExecutionOwner get() = owner
    private val admission = SessionAdmission()
    private val lifecycle = SessionLifecycle()
    val state = lifecycle.state
    val manualProgress: DawnManualProgress? = if (options.execution is DawnExecutionMode.Manual)
        DawnManualProgress(owner, progress::processEvents) else null
    private val monitoring = CoroutineScope(SupervisorJob() + owner.dispatcher)
    private val transferred = atomic(false)
    private val errorObservation: AutoCloseable
    private val shutdownDriver = SessionShutdown(
        owner, ownsOwner, options.shutdownTimeout, options.progressInterval, lifecycle,
        object : SessionShutdownDriver {
            override fun processEvents() = progress.processEvents()
            override fun hasPendingOperations() = context.hasPendingOperations()
            override suspend fun awaitApplicationWork() { admission.run { } }
            override suspend fun retirePresentation() { initialization?.retire() }
            override fun closeDevice() { errorObservation.close(); device.close() }
            override fun closeAdapter() = adapter.close()
            override fun closeContext() { context.close(); monitoring.cancel(); progress.finish() }
            override suspend fun stopPolling() = progress.pause()
            override fun resumePolling() = progress.start()
        },
    )

    private fun scheduleCleanup() {
        if (!transferred.value) return
        manualProgress?.stop()
        shutdownDriver.requestCleanup()
    }

    init {
        errorObservation = device.observeUncapturedErrors {
            lifecycle.uncapturedError(it)
            if (state.value is DawnSessionState.Failed) scheduleCleanup()
        }
        progress.onFailure = { lifecycle.fail(it); scheduleCleanup() }
    }

    /** Called without another suspension after the last cancellable dispatcher return. */
    internal fun activate() {
        transferred.value = true
        if (state.value != DawnSessionState.Ready) scheduleCleanup()
        monitoring.launch {
            val loss = device.awaitLost()
            if (state.value == DawnSessionState.Ready) {
                loss.fold(lifecycle::lost, lifecycle::fail)
                scheduleCleanup()
            }
        }
    }

    internal fun abandonBootstrap() {
        monitoring.cancel()
        manualProgress?.stop()
        errorObservation.close()
    }

    suspend fun <T> run(block: suspend CoroutineScope.(DawnSession) -> T): T {
        val nested = admission.isAdmitted()
        if (!nested) lifecycle.requireReady()
        return owner.run {
            admission.run {
                if (nested) block(this@DawnSession)
                else lifecycle.track { block(this@DawnSession) }
            }
        }
    }

    suspend fun awaitLost(): Result<GPUDeviceLostInfo> = device.awaitLost()

    suspend fun shutdown() {
        check(!admission.isAdmitted()) { "shutdown cannot run inside session.run" }
        check(manualProgress == null || manualProgress.isStopped) { "stop the manual driver before shutdown" }
        shutdownDriver.shutdown()
    }
}

suspend fun openDawnSession(
    options: DawnSessionOptions = DawnSessionOptions(),
    initialization: DawnSessionInitialization? = null,
): DawnSession = bootstrapSession(options, initialization)

suspend fun <T> withDawnSession(
    options: DawnSessionOptions = DawnSessionOptions(),
    block: suspend CoroutineScope.(DawnSession) -> T,
): T {
    val session = openDawnSession(options)
    var failure: Throwable? = null
    try { return session.run(block) }
    catch (caught: Throwable) { failure = caught; throw caught }
    finally {
        session.manualProgress?.stop()
        try { session.shutdown() }
        catch (cleanup: Throwable) { if (failure == null) throw cleanup else failure.addSuppressed(cleanup) }
    }
}
