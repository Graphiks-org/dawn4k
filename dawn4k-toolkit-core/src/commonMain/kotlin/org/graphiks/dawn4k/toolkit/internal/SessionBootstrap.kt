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
    try {
        val session = owner.run {
            withTimeout(options.startupTimeout) {
                val context = DawnContext.create(options.config)
                val events = EventProgress(context, owner, options.progressInterval)
                transaction.onRollback { events.drain(); events.stop(); context.close() }
                if (options.execution is DawnExecutionMode.Managed) events.start()
                val prepared = initialization?.prepare(context)
                if (prepared != null) transaction.onRollback { prepared.retire() }
                val adapter = (prepared?.compatibleSurface?.let {
                    context.requestAdapter(options.adapterOptions, it)
                } ?: context.requestAdapter(options.adapterOptions)).getOrThrow() as DawnAdapter
                transaction.onRollback { events.drain(); adapter.close() }
                val device = adapter.requestDevice(options.deviceDescriptor).getOrThrow() as DawnDevice
                transaction.onRollback { events.drain(); device.close(); events.drain() }
                DawnSession(context, adapter, device, owner, supplied == null, options, events, prepared)
            }
        }
        transaction.commit()
        return session
    } catch (failure: Throwable) {
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
            CoroutineScope(SupervisorJob() + owner.dispatcher).launch {
                while (!transaction.rollback()) delay(options.progressInterval)
                if (supplied == null) owner.close()
            }
        }
        throw failure
    }
}
