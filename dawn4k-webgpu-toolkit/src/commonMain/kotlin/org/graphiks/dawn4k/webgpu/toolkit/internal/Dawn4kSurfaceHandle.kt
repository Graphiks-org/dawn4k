package org.graphiks.dawn4k.webgpu.toolkit.internal

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import org.graphiks.dawn4k.toolkit.DawnSessionOptions
import org.graphiks.dawn4k.toolkit.surface.DawnAlphaMode
import org.graphiks.dawn4k.toolkit.surface.DawnFrame
import org.graphiks.dawn4k.toolkit.surface.DawnPresentMode
import org.graphiks.dawn4k.toolkit.surface.DawnPresentedSession
import org.graphiks.dawn4k.toolkit.surface.DawnPresentationOptions
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.openDawnSurface
import org.graphiks.dawn4k.webgpu.toolkit.target.Dawn4kSurfaceTarget
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.AcquiredFrame
import org.graphiks.webgpu.toolkit.AlphaMode
import org.graphiks.webgpu.toolkit.Extent2D
import org.graphiks.webgpu.toolkit.PresentationConfiguration
import org.graphiks.webgpu.toolkit.PresentMode
import org.graphiks.webgpu.toolkit.SurfaceCapabilities
import org.graphiks.webgpu.toolkit.ToolkitSurfaceHandle

/**
 * Dawn4k implementation of [ToolkitSurfaceHandle] over a real dawn4k presentation.
 *
 * The handle owns a [DawnPresentedSession]: its own Dawn context, adapter, device and
 * [DawnPresentation] negotiated against the native surface target of the lease. Frames come
 * from [DawnPresentation.withFrame], so [acquireFrame] returns the swap-chain texture view and
 * [present] hands the frame to the compositor. The session is opened on the device the toolkit
 * configured, because a dawn4k surface reference belongs to the context that created it.
 *
 * @param surfaceTarget The native surface target of the lease this handle presents to
 * @param deviceDescriptor The descriptor the session asked its device with; the presentation
 *   opens its own device, so it asks for the same features and limits
 * @param startupTimeout How long opening the presentation may take
 * @param shutdownTimeout How long closing it may take
 */
class Dawn4kSurfaceHandle(
    private val surfaceTarget: Dawn4kSurfaceTarget,
    internal val deviceDescriptor: GPUDeviceDescriptor? = null,
    internal val startupTimeout: Duration = 10.seconds,
    internal val shutdownTimeout: Duration = 10.seconds,
) : ToolkitSurfaceHandle {

    private val mutex = Mutex()
    private var configuration: PresentationConfiguration? = null
    private var presented: DawnPresentedSession? = null
    private var isClosed = false

    /** The frame currently handed out, with the dawn4k frame that backs its view. */
    private var currentFrame: AcquiredFrame? = null
    private var currentNativeFrame: DawnFrame? = null
    private var framePresented = false

    override val capabilities: SurfaceCapabilities = SurfaceCapabilities(
        preferredFormat = GPUTextureFormat.BGRA8Unorm,
        formats = listOf(
            GPUTextureFormat.BGRA8Unorm,
            GPUTextureFormat.RGBA8Unorm,
            GPUTextureFormat.RGBA16Float,
        ),
        alphaModes = listOf(
            AlphaMode.Opaque,
            AlphaMode.Premultiplied,
            AlphaMode.PostMultiplied,
        ),
        // Every present mode dawn4k's swap chain can be configured with, led by the one that
        // never tears; the target's own capabilities narrow this at negotiation time.
        presentModes = listOf(
            PresentMode.Fifo,
            PresentMode.FifoRelaxed,
            PresentMode.Mailbox,
            PresentMode.Immediate,
        ),
    )

    override fun configure(configuration: PresentationConfiguration) {
        check(!isClosed) { "Cannot configure a closed surface handle" }
        check(this.configuration == null) { "Surface is already configured" }
        require(configuration.format in capabilities.formats) {
            "Format ${configuration.format} is not supported by this surface"
        }
        require(configuration.alphaMode in capabilities.alphaModes) {
            "Alpha mode ${configuration.alphaMode} is not supported by this surface"
        }
        require(configuration.presentMode in capabilities.presentModes) {
            "Present mode ${configuration.presentMode} is not supported by this surface"
        }
        this.configuration = configuration
    }

    override suspend fun acquireFrame(): AcquiredFrame = mutex.withLock {
        check(!isClosed) { "Cannot acquire frame from a closed surface handle" }
        val configured = checkNotNull(configuration) {
            "Surface must be configured before acquiring frames"
        }
        check(currentFrame == null) { "Previous frame must be released before acquiring a new one" }

        val presentation = presented ?: openPresentation(configured).also { presented = it }
        val frame = checkNotNull(presentation.acquireFrame()) {
            "The presentation target is unavailable, no frame was acquired"
        }
        currentNativeFrame = frame
        return AcquiredFrame(view = frame.view, extent = frame.extent.toExtent2D()).also {
            currentFrame = it
            framePresented = false
        }
    }

    override fun present(frame: AcquiredFrame) {
        check(!isClosed) { "Cannot present on a closed surface handle" }
        check(configuration != null) { "Surface must be configured before presenting" }
        check(frame === currentFrame) { "Can only present the currently acquired frame" }
        check(!framePresented) { "Frame has already been presented" }

        currentNativeFrame?.present()
        framePresented = true
    }

    override fun releaseFrame(frame: AcquiredFrame) {
        // Idempotent: no-op if frame is not the current one or already released.
        if (frame !== currentFrame) {
            return
        }
        // Closing the dawn4k frame releases its view and texture reference; presenting first
        // keeps the frame visible when the caller rendered without an explicit present.
        currentNativeFrame?.close()
        currentNativeFrame = null
        currentFrame = null
        framePresented = false
    }

    override fun close() {
        if (isClosed) return
        isClosed = true
        currentNativeFrame?.close()
        currentNativeFrame = null
        currentFrame = null
    }

    /**
     * Opens the dawn4k presentation for the configured device. The surface reference is created
     * by the lease against that same context, which keeps the reference and its requests valid.
     *
     * The policy the toolkit negotiated is applied to the presentation, so the format, alpha
     * mode and present mode the caller sees in [PresentationConfiguration] are the ones the swap
     * chain is configured with. dawn4k's own choice is checked against it: a target that cannot
     * honour the published configuration fails here instead of presenting with another one.
     */
    private suspend fun openPresentation(configuration: PresentationConfiguration): DawnPresentedSession {
        val target: DawnSurfaceTarget = checkNotNull(surfaceTarget.nativeTarget) {
            "This surface target has no native dawn4k target to present to"
        }
        val opened = openDawnSurface(
            target = target,
            sessionOptions = DawnSessionOptions(
                deviceDescriptor = deviceDescriptor,
                startupTimeout = startupTimeout,
                shutdownTimeout = shutdownTimeout,
            ),
            presentationOptions = surfaceTarget.presentationOptions.pinnedTo(configuration),
        )
        val negotiated = opened.presentation.configuration.value
        check(negotiated == null || negotiated.format == configuration.format) {
            "dawn4k configured the surface with ${negotiated?.format} instead of ${configuration.format}"
        }
        return opened
    }
}

/**
 * Narrows this policy to exactly [configuration], so dawn4k negotiates the values the toolkit
 * already published instead of a second, possibly different choice.
 */
private fun DawnPresentationOptions.pinnedTo(
    configuration: PresentationConfiguration,
): DawnPresentationOptions = copy(
    requiredFormat = configuration.format,
    requiredAlphaMode = configuration.alphaMode.toDawnAlphaMode(),
    requiredPresentMode = configuration.presentMode.toDawnPresentMode(),
)

/**
 * Translates a toolkit present mode to its dawn4k equivalent; both name when a presented frame
 * becomes visible relative to the display refresh.
 */
internal fun PresentMode.toDawnPresentMode(): DawnPresentMode = when (this) {
    PresentMode.Fifo -> DawnPresentMode.Fifo
    PresentMode.FifoRelaxed -> DawnPresentMode.FifoRelaxed
    PresentMode.Immediate -> DawnPresentMode.Immediate
    PresentMode.Mailbox -> DawnPresentMode.Mailbox
}

/**
 * Translates a toolkit alpha mode to its dawn4k equivalent; both describe how the compositor
 * blends presented frames with the content behind the target.
 */
internal fun AlphaMode.toDawnAlphaMode(): DawnAlphaMode = when (this) {
    AlphaMode.Opaque -> DawnAlphaMode.Opaque
    AlphaMode.Premultiplied -> DawnAlphaMode.Premultiplied
    AlphaMode.PostMultiplied -> DawnAlphaMode.Unpremultiplied
}

private fun org.graphiks.dawn4k.toolkit.surface.PixelExtent.toExtent2D(): Extent2D =
    Extent2D(width.toUInt(), height.toUInt())
