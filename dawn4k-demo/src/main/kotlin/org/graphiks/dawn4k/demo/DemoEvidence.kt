package org.graphiks.dawn4k.demo

import kotlinx.coroutines.flow.MutableStateFlow
import org.graphiks.webgpu.GPUBufferUsage
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUMapMode
import org.graphiks.webgpu.descriptors.BufferDescriptor
import org.graphiks.webgpu.suite.demos.particles.ParticleScene

internal data class DemoEvidenceSnapshot(
    val sessionId: Long = 1,
    val phase: DemoPhase = DemoPhase.Initializing,
    val backend: String? = null,
    val adapter: String? = null,
    val count: Int = 4096,
    val paused: Boolean = false,
    val resetGeneration: Long = 0,
    val lifecycleActive: Boolean = true,
    val firstError: String? = null,
    val viewport: LogicalViewport? = null,
    val buttons: Map<String, LogicalViewport> = emptyMap(),
    val physicalExtent: PixelExtent = PixelExtent(0, 0),
    val surfaceGeneration: Long = 0,
    val frameCount: Long = 0,
    val simulationFingerprint: String? = null,
    val deviceCreations: Long = 0,
    val surfaceCreations: Long = 0,
)

/** GPU readback is opt-in; snapshots are observations, never commands. */
internal class DemoEvidence(private val controller: DemoSessionController) {
    private val latest = MutableStateFlow(DemoEvidenceSnapshot())
    private val fingerprintEnabled = MutableStateFlow(false)
    internal val wantsFingerprint get() = fingerprintEnabled.value
    fun captureFingerprints(enabled: Boolean) { fingerprintEnabled.value = enabled }
    fun snapshot(): DemoEvidenceSnapshot = latest.value.copy(
        phase = controller.state.value.phase,
        lifecycleActive = controller.state.value.lifecycleActive,
        firstError = controller.state.value.error,
    )
    internal fun publish(value: DemoEvidenceSnapshot) { latest.value = value }
}

internal suspend fun particleFingerprint(device: GPUDevice, scene: ParticleScene): String {
    val source = scene.particleBuffer
    device.createBuffer(BufferDescriptor(size = source.size,
        usage = GPUBufferUsage.CopyDst or GPUBufferUsage.MapRead)).use { staging ->
        device.createCommandEncoder().use { encoder ->
            encoder.copyBufferToBuffer(source, 0uL, staging, 0uL, source.size)
            encoder.finish().use { device.queue.submit(listOf(it)) }
        }
        device.queue.onSubmittedWorkDone().getOrThrow()
        staging.mapAsync(GPUMapMode.Read).getOrThrow()
        try {
            var hash = 14695981039346656037uL
            for (byte in staging.getMappedRange().toByteArray())
                hash = (hash xor byte.toUByte().toULong()) * 1099511628211uL
            return hash.toString(16)
        } finally { staging.unmap() }
    }
}
