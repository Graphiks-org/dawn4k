@file:OptIn(kotlin.experimental.ExperimentalNativeApi::class, kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k

import kotlin.coroutines.CoroutineContext
import kotlin.native.concurrent.Worker
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Runnable
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.testing.gpuTestEnvironment
import kotlin.test.Test

class MultithreadEncodingGpuTest {
    @Test fun independentEncodersShareOneDeviceAndImmutableSource() = runBlocking {
        if (!gpuTestEnvironment("MultithreadEncodingGpuTest.independentEncodersShareOneDeviceAndImmutableSource")) return@runBlocking
        withWorkers { checkParallelEncoding(it) }
    }
    @Test fun validationScopesBelongToTheirIssuingOsThread() = runBlocking {
        if (!gpuTestEnvironment("MultithreadEncodingGpuTest.validationScopesBelongToTheirIssuingOsThread")) return@runBlocking
        withWorkers { checkThreadLocalErrorScopes(it) }
    }
    private suspend fun withWorkers(block: suspend (List<CoroutineDispatcher>) -> Unit) {
        val workers = List(2) { Worker.start(name = "consumer-$it") }
        val dispatchers = workers.map { worker -> object : CoroutineDispatcher() {
            override fun dispatch(context: CoroutineContext, block: Runnable) {
                worker.executeAfter(0L) { block.run() }
            }
        } }
        try { block(dispatchers) } finally { workers.forEach { it.requestTermination().result } }
    }
}
