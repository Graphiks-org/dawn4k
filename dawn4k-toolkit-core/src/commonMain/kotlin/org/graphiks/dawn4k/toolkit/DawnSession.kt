package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.*
import org.graphiks.dawn4k.*
import org.graphiks.dawn4k.toolkit.internal.*

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
    private val admission = SessionAdmission()
    private var closed = false

    suspend fun <T> run(block: suspend CoroutineScope.(DawnSession) -> T): T = owner.run {
        admission.run {
            check(!closed) { "session is closed" }
            block(this@DawnSession)
        }
    }

    suspend fun shutdown() {
        check(!admission.isAdmitted()) { "shutdown cannot run inside session.run" }
        withContext(NonCancellable) {
            owner.run {
                admission.run {
                    if (!closed) {
                        withTimeout(options.shutdownTimeout) {
                            progress.drain()
                            device.close()
                            progress.drain()
                            adapter.close()
                            initialization?.retire()
                            progress.drain()
                            progress.stop()
                            context.close()
                            closed = true
                        }
                    }
                }
            }
            if (ownsOwner) owner.close()
        }
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
        try { session.shutdown() }
        catch (cleanup: Throwable) { if (failure == null) throw cleanup else failure.addSuppressed(cleanup) }
    }
}
