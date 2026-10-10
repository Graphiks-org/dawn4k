package org.graphiks.dawn4k

import java.util.concurrent.Executors
import kotlinx.coroutines.asCoroutineDispatcher
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
    private suspend fun withWorkers(block: suspend (List<kotlinx.coroutines.CoroutineDispatcher>) -> Unit) {
        val workers = List(2) { index -> Executors.newSingleThreadExecutor { runnable -> Thread(runnable, "consumer-$index") }.asCoroutineDispatcher() }
        try { block(workers) } finally { workers.forEach { it.close() } }
    }
}
