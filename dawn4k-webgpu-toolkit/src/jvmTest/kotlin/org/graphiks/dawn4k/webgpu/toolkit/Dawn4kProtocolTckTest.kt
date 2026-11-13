package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.target.PlatformSurfaceType
import org.graphiks.webgpu.toolkit.AdapterHints
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.tck.ProtocolTck
import org.graphiks.webgpu.toolkit.tck.TckReport
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * Runs the portable toolkit contract kit against the real dawn4k extension.
 *
 * The kit opens real Dawn contexts, so this suite belongs to the JVM target only: the
 * native targets would link the published `webgpu-api` klib a second time through the
 * toolkit artifacts.
 */
class Dawn4kProtocolTckTest {

    /**
     * Cases the kit skips for every foreign extension: they need the scriptable
     * simulated fixtures. These cases require scriptable fixtures that only the
     * simulation provides.
     */
    private val expectedSkippedCases = listOf(
        "zero-extent suspension",
        "stale-lease rollback",
        "loss to Lost"
    )

    @Test
    fun `ProtocolTck runAll against Dawn4kWebGpuToolkitExtension`() = runTest {
        val extension = Dawn4kWebGpuToolkitExtension()

        // A null surface reference means no real frames: expected on a GPU-less host.
        val target = Dawn4kSurfaceTarget(
            surfaceReference = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = Extent2D(width = 640u, height = 480u)
        )

        // The kit opens real Dawn contexts. On a host without the native runtime (no GPU
        // stack, no native library) there is nothing to validate: the GPU-backed suite owns it.
        if (!nativeRuntimeAvailable()) {
            println("TCK skipped: the Dawn native runtime is not available on this host")
            target.close()
            return@runTest
        }

        try {
            // Skip-aware: probe whether this host can actually provide a GPU adapter,
            // so GPU-less hosts still pass while adapter-available hosts must be clean.
            val adapterAvailable = probeAdapterAvailable(extension)

            val report: TckReport = ProtocolTck.runAll(extension, target)

            assertTrue(
                report.skipped.containsAll(expectedSkippedCases),
                "Expected skipped cases: $expectedSkippedCases, but got: ${report.skipped}"
            )
            assertEquals(
                expectedSkippedCases.sorted(),
                report.skipped.sorted(),
                "Unexpected skipped cases. Expected only: $expectedSkippedCases, but got: ${report.skipped}"
            )

            println("TCK Report (adapterAvailable=$adapterAvailable): skipped=${report.skipped} failures=${report.failures}")

            if (adapterAvailable) {
                assertTrue(
                    report.failures.isEmpty(),
                    "TCK failures on an adapter-available host: ${report.failures}"
                )
            } else {
                // GPU-less host: the non-skipped cases cannot acquire a real adapter/device.
                assertTrue(
                    report.failures.isNotEmpty(),
                    "Expected GPU-bound failures on a GPU-less host, but the report was clean"
                )
            }
        } finally {
            target.close()
        }
    }

    /** Whether the Dawn native runtime loads on this host. */
    private fun nativeRuntimeAvailable(): Boolean =
        try {
            DawnContext.create().close()
            true
        } catch (failure: Throwable) {
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            false
        }

    /**
     * Probes whether this host can provide a GPU adapter through the extension,
     * bounded by a timeout so a stuck native request cannot hang the test.
     */
    private suspend fun probeAdapterAvailable(extension: Dawn4kWebGpuToolkitExtension): Boolean =
        try {
            withTimeout(10.seconds) {
                val instance = extension.open(SessionConfig())
                try {
                    instance.requestAdapter(AdapterHints()).isSuccess
                } finally {
                    instance.close()
                }
            }
        } catch (failure: Throwable) {
            // A timeout or a native failure means no usable adapter on this host.
            if (failure is kotlinx.coroutines.CancellationException) throw failure
            false
        }
}
