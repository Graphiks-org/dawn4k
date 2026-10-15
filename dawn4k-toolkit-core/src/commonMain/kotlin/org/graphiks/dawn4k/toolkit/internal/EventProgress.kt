package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.*
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import kotlin.time.Duration

internal class EventProgress(
    private val owner: DawnExecutionOwner,
    private val interval: Duration,
    private val process: () -> Unit,
    private val pending: () -> Boolean,
) {
    constructor(context: DawnContext, owner: DawnExecutionOwner, interval: Duration) :
        this(owner, interval, context::processEvents, context::hasPendingOperations)

    private val scope = CoroutineScope(SupervisorJob() + owner.dispatcher)
    private var job: Job? = null
    var onFailure: (Throwable) -> Unit = {}
    var failure: Throwable? = null
        private set

    fun processEvents() { owner.checkThread(); process() }

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            try {
                while (isActive) { processEvents(); delay(interval) }
            } catch (cancelled: CancellationException) { throw cancelled }
            catch (caught: Throwable) { failure = caught; onFailure(caught) }
        }
    }
    suspend fun drain() {
        while (true) {
            processEvents()
            if (!pending()) return
            delay(interval)
        }
    }
    suspend fun pause() { job?.cancelAndJoin(); job = null }
    fun finish() { scope.cancel() }
    suspend fun stop() { pause(); finish() }
}
