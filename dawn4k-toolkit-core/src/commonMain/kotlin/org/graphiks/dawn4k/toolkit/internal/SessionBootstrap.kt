package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.*
import kotlinx.atomicfu.atomic
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*
import kotlin.time.Duration.Companion.milliseconds

internal suspend fun bootstrapSession(
    options: DawnSessionOptions,
    initialization: DawnSessionInitialization?,
): DawnSession {
    val supplied = options.execution.owner
    val owner = supplied ?: DawnExecutionOwner.dedicated()
    val transaction = BootstrapTransaction()
    val events = atomic<EventProgress?>(null)
    // Keep acquisitions reachable even when platform retention or a native call cannot finish at the deadline.
    val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    val worker = scope.async {
        owner.run {
            val context = DawnContext.create(options.config)
            val progress = EventProgress(context, owner, options.progressInterval)
            events.value = progress
            val bootstrapJob = currentCoroutineContext()[Job]!!
            progress.onFailure = { bootstrapJob.cancel(CancellationException("Dawn bootstrap progression failed", it)) }
            transaction.onRollback { progress.drain(); progress.stop(); context.close() }
            progress.start()
            val prepared = initialization?.prepare(context)
            if (prepared != null) transaction.onRollback { progress.drain(); prepared.retire() }
            prepared?.acquire()
            val adapter = (prepared?.compatibleSurface?.let {
                context.requestAdapter(options.adapterOptions, it)
            } ?: context.requestAdapter(options.adapterOptions)).getOrThrow() as DawnAdapter
            transaction.onRollback { progress.drain(); adapter.close() }
            val device = adapter.requestDevice(options.deviceDescriptor).getOrThrow() as DawnDevice
            transaction.onRollback { progress.drain(); device.close(); progress.drain() }
            DawnSession(context, adapter, device, owner, supplied == null, options, progress, prepared).also {
                transaction.onRollback { it.abandonBootstrap() }
                if (prepared != null) it.run { session -> prepared.initialize(session) }
                if (options.execution is DawnExecutionMode.Manual) progress.pause()
            }
        }
    }
    try {
        // The deadline uses the real GPU clock, including when the caller uses a virtual test dispatcher.
        val session = withContext(Dispatchers.Default) { withTimeout(options.startupTimeout) { worker.await() } }
        transaction.commit()
        session.activate()
        scope.cancel()
        return session
    } catch (caught: Throwable) {
        val failure = if (worker.isCompleted) events.value?.failure ?: caught else caught
        worker.cancel()
        val retirement = scope.async {
            worker.join()
            retireWithDeadline(owner, transaction, failure, supplied == null, options)
        }
        retirement.invokeOnCompletion { scope.cancel() }
        withContext(NonCancellable + Dispatchers.Default) {
            try { withTimeout(options.shutdownTimeout) { retirement.await() } }
            catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
        }
        throw failure
    }
}

/**
 * Unwinds [transaction] after a failed bootstrap with the same bounding as the
 * [SessionShutdown] stages: one rollback attempt never outlives
 * [DawnSessionOptions.shutdownTimeout], and a timed-out attempt counts as
 * unclean — the loop retries after the progress delay instead of blocking
 * retirement on a hung drain.
 */
internal suspend fun retireWithDeadline(
    owner: DawnExecutionOwner,
    transaction: BootstrapTransaction,
    failure: Throwable,
    ownsOwner: Boolean,
    options: DawnSessionOptions,
) {
    var primary: Throwable? = failure
    while (true) {
        val cleaned = try {
            transaction.isEmpty || withTimeout(options.shutdownTimeout) { owner.run { transaction.rollback(primary) } }
        } catch (deadline: TimeoutCancellationException) {
            // A hung rollback (e.g. an unbounded drain) is an unclean attempt, not this loop's end.
            false
        } catch (cleanup: Throwable) { primary?.addSuppressed(cleanup); false }
        primary = null
        if (cleaned) {
            if (ownsOwner) owner.close()
            break
        }
        delay(maxOf(options.progressInterval, 100.milliseconds))
    }
}
