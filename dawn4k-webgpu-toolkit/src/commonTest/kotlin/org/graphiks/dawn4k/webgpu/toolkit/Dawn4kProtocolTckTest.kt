package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.withTimeout
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.webgpu.toolkit.Dawn4kWebGpuToolkitExtension
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
 * TCK (Technology Compatibility Kit) tests for the Dawn4k WebGPU toolkit extension.
 *
 * This test suite runs the protocol contract kit against the real Dawn4k extension
 * to validate that it correctly implements the WebGPU toolkit protocol.
 *
 * Since Dawn4k is a real (non-simulated) extension, the following cases will be
 * skipped as they require scriptable fixtures:
 * - zero-extent suspension: requires SimulatedTarget to script extent changes
 * - stale-lease rollback: requires SimulatedTarget to hand out stale leases
 * - loss to Lost: requires SimulatedToolkitExtension to script device loss
 *
 * The remaining cases (lifecycle walk, idempotent shutdown, present-once) run
 * against the real extension: when a GPU adapter is available they must all pass;
 * on a GPU-less host they cannot acquire an adapter/device and their failures are
 * GPU-bound and documented (the test still passes there).
 */
class Dawn4kProtocolTckTest {

    /**
     * Expected skipped cases for a real (non-simulated) extension.
     * These cases require scriptable fixtures that only the simulation provides.
     */
    private val EXPECTED_SKIPPED_CASES = listOf(
        "zero-extent suspension",
        "stale-lease rollback",
        "loss to Lost"
    )

    @Test
    fun `ProtocolTck runAll against Dawn4kWebGpuToolkitExtension`() = runTest {
        val extension = Dawn4kWebGpuToolkitExtension()

        // Create a Dawn4k surface target for testing
        // Note: This uses a null surfaceReference which means we won't get actual frames
        // from the GPU. This is expected on a GPU-less host.
        val context = DawnContext.create()
        val target = Dawn4kSurfaceTarget(
            context = context,
            surfaceReference = null,
            platformType = PlatformSurfaceType.MetalLayer,
            initialExtent = Extent2D(width = 640u, height = 480u)
        )

        try {
            // Skip-aware: probe whether this host can actually provide a GPU adapter,
            // so GPU-less hosts still pass while adapter-available hosts must be clean.
            val adapterAvailable = probeAdapterAvailable(extension)

            val report: TckReport = ProtocolTck.runAll(extension, target)

            // Verify that the expected cases are skipped (require scriptable fixtures)
            assertTrue(
                report.skipped.containsAll(EXPECTED_SKIPPED_CASES),
                "Expected skipped cases: $EXPECTED_SKIPPED_CASES, but got: ${report.skipped}"
            )

            // The skipped list should only contain the expected cases
            assertEquals(
                EXPECTED_SKIPPED_CASES.sorted(),
                report.skipped.sorted(),
                "Unexpected skipped cases. Expected only: $EXPECTED_SKIPPED_CASES, but got: ${report.skipped}"
            )

            // Document the actual report
            println("\nTCK Report (adapterAvailable=$adapterAvailable):")
            println("  Skipped: ${report.skipped}")
            println("  Failures: ${report.failures}")

            if (adapterAvailable) {
                // With a real adapter, every non-skipped case must pass
                assertTrue(
                    report.failures.isEmpty(),
                    "TCK failures on an adapter-available host: ${report.failures}"
                )
            } else {
                // GPU-less host: the non-skipped cases cannot acquire a real
                // GPU adapter/device, so their failures are GPU-bound and expected.
                assertTrue(
                    report.failures.isNotEmpty(),
                    "Expected GPU-bound failures on a GPU-less host, but the report was clean"
                )
            }
        } finally {
            target.close()
            context.close()
        }
    }

    /**
     * Probes whether this host can provide a GPU adapter through the extension,
     * bounded by a timeout so a stuck native request cannot hang the test.
     */
    private suspend fun probeAdapterAvailable(extension: Dawn4kWebGpuToolkitExtension): Boolean {
        val instance = extension.open(SessionConfig())
        return try {
            withTimeout(10.seconds) {
                instance.requestAdapter(AdapterHints()).isSuccess
            }
        } catch (_: Throwable) {
            false
        } finally {
            runCatching { instance.close() }
        }
    }
}
