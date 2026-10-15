package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.*

internal suspend fun bootstrapSession(
    options: DawnSessionOptions,
    initialization: DawnSessionInitialization?,
): DawnSession {
    val supplied = when (val execution = options.execution) {
        is DawnExecutionMode.Managed -> execution.owner
        is DawnExecutionMode.Manual -> execution.owner
    }
    val owner = supplied ?: DawnExecutionOwner.dedicated()
    val transaction = BootstrapTransaction()
    var events: EventProgress? = null
    try {
        val session = owner.run {
            withTimeout(options.startupTimeout) {
                val context = DawnContext.create(options.config)
                val progress = EventProgress(context, owner, options.progressInterval)
                events = progress
                val bootstrapJob = currentCoroutineContext()[Job]!!
                progress.onFailure = { bootstrapJob.cancel(CancellationException("Dawn bootstrap progression failed", it)) }
                transaction.onRollback { progress.drain(); progress.stop(); context.close() }
                progress.start()
                val prepared = initialization?.prepare(context)
                if (prepared != null) transaction.onRollback { progress.drain(); prepared.retire() }
                val adapter = (prepared?.compatibleSurface?.let {
                    context.requestAdapter(options.adapterOptions, it)
                } ?: context.requestAdapter(options.adapterOptions)).getOrThrow() as DawnAdapter
                transaction.onRollback { progress.drain(); adapter.close() }
                val device = adapter.requestDevice(options.deviceDescriptor).getOrThrow() as DawnDevice
                transaction.onRollback { progress.drain(); device.close(); progress.drain() }
                if (options.execution is DawnExecutionMode.Manual) progress.pause()
                DawnSession(context, adapter, device, owner, supplied == null, options, progress, prepared).also {
                    transaction.onRollback { it.abandonBootstrap() }
                }
            }
        }
        transaction.commit()
        session.activate()
        return session
    } catch (caught: Throwable) {
        val failure = events?.failure ?: caught
        var cleaned = false
        withContext(NonCancellable) {
            try {
                owner.run { withTimeout(options.shutdownTimeout) { cleaned = transaction.rollback(failure) } }
            } catch (cleanup: Throwable) { failure.addSuppressed(cleanup) }
        }
        if (cleaned) {
            if (supplied == null) owner.close()
        } else {
            // Retain acquisitions until native callbacks become quiescent.
            CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
                while (!owner.run { transaction.rollback() }) delay(options.progressInterval)
                if (supplied == null) owner.close()
            }
        }
        throw failure
    }
}
