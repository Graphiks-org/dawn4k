package org.graphiks.dawn4k.acid

import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.newSingleThreadContext
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.webgpu.GPUUncapturedErrorCallback
import org.graphiks.webgpu.descriptors.DeviceDescriptor
import org.graphiks.webgpu.suite.AcidCase
import org.graphiks.webgpu.suite.AcidContext
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** This consumer, not the binding, owns progression, isolation and deadlines. */
internal suspend fun runDawnCase(case: AcidCase, settings: AcidSettings): CaseResult =
    executeCase(case.id.id, 30.seconds) {
        val errors = mutableListOf<String>()
        val result = withDawnContext(settings) { context ->
            context.requestAdapter().getOrThrow().use { adapter ->
                val missing = (case.requiredFeatures - adapter.features).map { it.name }.sorted()
                if (missing.isNotEmpty()) {
                    CaseResult(case.id.id, "unsupported", "Missing optional features: ${missing.joinToString()}",
                        missingFeatures = missing, adapterDescription = adapter.info.description)
                } else {
                    adapter.requestDevice(DeviceDescriptor(
                        requiredFeatures = case.requiredFeatures.toList(),
                        onUncapturedError = GPUUncapturedErrorCallback { errors.add(it.message) },
                    )).getOrThrow().use { device ->
                        checkDeviceFeatures(case.requiredFeatures, device.features)
                        case.run(AcidContext(device, requestAdapter = { options -> context.requestAdapter(options) }))
                        device.queue.onSubmittedWorkDone().getOrThrow()
                        context.processEvents()
                        CaseResult(case.id.id, "passed", adapterDescription = adapter.info.description)
                    }
                }
            }
        }
        check(errors.isEmpty()) { "Unexpected uncaptured GPU errors:\n${errors.joinToString("\n")}" }
        result
    }

@OptIn(DelicateCoroutinesApi::class, ExperimentalCoroutinesApi::class)
private suspend fun <T> withDawnContext(settings: AcidSettings, body: suspend (DawnContext) -> T): T {
    // Dawn's error-scope stacks are thread-local. Sequential execution on a
    // thread pool is not enough: suspension must resume on this same thread.
    val outcome = newSingleThreadContext("dawn-acid-case").use { dispatcher ->
        withContext(dispatcher) {
            coroutineScope {
                val context = DawnContext.create(DawnConfig(settings.backend, implicitDeviceSynchronization = true))
                val events = launch { while (isActive) { context.processEvents(); delay(1) } }
                var result = runCatching { body(context) }
                try {
                    withContext(NonCancellable) { events.cancelAndJoin() }
                    val started = TimeSource.Monotonic.markNow()
                    // Also settle callbacks posted by resource teardown before checking errors.
                    context.processEvents()
                    while (context.hasPendingOperations()) {
                        check(started.elapsedNow() < 10.seconds) { "Dawn teardown did not settle within 10 seconds" }
                        context.processEvents()
                    }
                    context.close()
                } catch (cleanup: Throwable) {
                    val original = result.exceptionOrNull()
                    if (original != null) preserveFailure(original, cleanup) else result = Result.failure(cleanup)
                }
                result
            }
        }
    }
    return outcome.getOrThrow()
}
