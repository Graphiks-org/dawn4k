package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.internal.DawnRequestAdapterException
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.native.WGPURequestAdapterStatus_Unavailable

internal fun isUnavailable(failure: Throwable): Boolean =
    failure is DawnRequestAdapterException && failure.status == WGPURequestAdapterStatus_Unavailable

internal data class GpuTestSettings(val backend: DawnBackend?, val requireAdapter: Boolean) {
    companion object {
        fun parse(backend: String?, requireAdapter: String?): GpuTestSettings = GpuTestSettings(
            backend = backend?.let { DawnBackend.valueOf(it) },
            requireAdapter = when (requireAdapter) {
                null, "0" -> false
                "1" -> true
                else -> throw IllegalArgumentException("DAWN_REQUIRE_ADAPTER must be 0 or 1")
            },
        )
    }
}

internal suspend fun checkGpuTestEnvironment(
    name: String,
    requireAdapter: Boolean,
    probe: suspend () -> Result<Unit>,
    report: (String) -> Unit,
): Boolean {
    val result = probe()
    val failure = result.exceptionOrNull()
    if (failure != null) {
        if (requireAdapter || !isUnavailable(failure)) throw failure
        report("WARNING: GPU_TEST_NOT_EXECUTED $name: ${failure.message}")
        return false
    }
    return true
}

private fun gpuTestSettings(): GpuTestSettings = GpuTestSettings.parse(
    gpuTestEnv("DAWN_TEST_BACKEND"), gpuTestEnv("DAWN_REQUIRE_ADAPTER"),
)

internal fun gpuTestConfig(): DawnConfig = DawnConfig(gpuTestSettings().backend)

/** Only adapter Unavailable permits not executing a GPU test; no device/body error is swallowed. */
internal suspend fun gpuTestEnvironment(name: String): Boolean {
    val settings = gpuTestSettings()
    return checkGpuTestEnvironment(name, settings.requireAdapter, probe = {
        DawnContext.create(DawnConfig(settings.backend)).use { context ->
            val result = context.requestAdapter()
            val failure = result.exceptionOrNull()
            if (failure != null) {
                Result.failure(failure)
            } else {
                result.getOrThrow().use { adapter ->
                    println("GPU_TEST_ADAPTER $name: backend=${settings.backend ?: "default"}, vendor=${adapter.info.vendor}")
                }
                Result.success(Unit)
            }
        }
    }, report = { warning ->
        println(warning)
        gpuTestNotExecuted(warning)
    })
}

internal expect fun gpuTestEnv(name: String): String?

/** JVM reports an assumption violation; Native returns so the caller can leave the test body. */
internal expect fun gpuTestNotExecuted(warning: String)
