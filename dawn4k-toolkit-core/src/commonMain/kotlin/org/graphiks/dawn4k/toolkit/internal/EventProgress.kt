package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.*
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner
import kotlin.time.Duration

internal class EventProgress(
    private val context: DawnContext,
    private val owner: DawnExecutionOwner,
    private val interval: Duration,
) {
    private val scope = CoroutineScope(SupervisorJob() + owner.dispatcher)
    private var job: Job? = null

    fun start() {
        check(job == null)
        job = scope.launch {
            while (isActive) {
                owner.checkThread()
                context.processEvents()
                delay(interval)
            }
        }
    }
    suspend fun drain() {
        while (true) {
            owner.checkThread()
            context.processEvents()
            if (!context.hasPendingOperations()) return
            delay(interval)
        }
    }
    suspend fun stop() {
        job?.cancelAndJoin()
        job = null
        scope.cancel()
    }
}
