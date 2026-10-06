package org.graphiks.dawn4k

import kotlinx.coroutines.CancellationException
import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.DawnPopErrorScopeException
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.internal.requireWgpuSuccess
import org.graphiks.dawn4k.internal.scopeError
import org.graphiks.dawn4k.mapper.allocateAdapterInfoSnapshot
import org.graphiks.dawn4k.mapper.allocateLimitsSnapshot
import org.graphiks.dawn4k.mapper.copyAdapterInfo
import org.graphiks.dawn4k.mapper.copySupportedFeatures
import org.graphiks.dawn4k.mapper.readLimits
import org.graphiks.dawn4k.mapper.toNativeErrorFilter
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUErrorType
import org.graphiks.dawn4k.native.WGPUErrorType_NoError
import org.graphiks.dawn4k.native.WGPUPopErrorScopeCallback
import org.graphiks.dawn4k.native.WGPUPopErrorScopeCallbackInfo
import org.graphiks.dawn4k.native.WGPUPopErrorScopeStatus
import org.graphiks.dawn4k.native.WGPUPopErrorScopeStatus_Success
import org.graphiks.dawn4k.native.WGPUSupportedFeatures
import org.graphiks.dawn4k.native.wgpuAdapterGetInfo
import org.graphiks.dawn4k.native.wgpuAdapterInfoFreeMembers
import org.graphiks.dawn4k.native.wgpuDeviceGetFeatures
import org.graphiks.dawn4k.native.wgpuDeviceGetLimits
import org.graphiks.dawn4k.native.wgpuDevicePopErrorScope
import org.graphiks.dawn4k.native.wgpuDevicePushErrorScope
import org.graphiks.dawn4k.native.wgpuSupportedFeaturesFreeMembers
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUAddressMode
import org.graphiks.webgpu.GPUBindGroup
import org.graphiks.webgpu.GPUBindGroupDescriptor
import org.graphiks.webgpu.GPUBindGroupLayout
import org.graphiks.webgpu.GPUBindGroupLayoutDescriptor
import org.graphiks.webgpu.GPUBuffer
import org.graphiks.webgpu.GPUBufferDescriptor
import org.graphiks.webgpu.GPUCommandEncoder
import org.graphiks.webgpu.GPUCommandEncoderDescriptor
import org.graphiks.webgpu.GPUComputePipeline
import org.graphiks.webgpu.GPUComputePipelineDescriptor
import org.graphiks.webgpu.GPUCompareFunction
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUDeviceDescriptor
import org.graphiks.webgpu.GPUError
import org.graphiks.webgpu.GPUErrorFilter
import org.graphiks.webgpu.GPUFilterMode
import org.graphiks.webgpu.GPUMipmapFilterMode
import org.graphiks.webgpu.GPUPipelineLayout
import org.graphiks.webgpu.GPUPipelineLayoutDescriptor
import org.graphiks.webgpu.GPUQuerySet
import org.graphiks.webgpu.GPUQuerySetDescriptor
import org.graphiks.webgpu.GPUQueue
import org.graphiks.webgpu.GPURenderBundleEncoder
import org.graphiks.webgpu.GPURenderBundleEncoderDescriptor
import org.graphiks.webgpu.GPURenderPipeline
import org.graphiks.webgpu.GPURenderPipelineDescriptor
import org.graphiks.webgpu.GPUSampler
import org.graphiks.webgpu.GPUSamplerDescriptor
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUShaderModuleDescriptor
import org.graphiks.webgpu.GPUSupportedFeatures
import org.graphiks.webgpu.GPUSupportedLimits
import org.graphiks.webgpu.GPUTexture
import org.graphiks.webgpu.GPUTextureDescriptor
import org.graphiks.webgpu.GPUAdapterInfo

/**
 * A public [GPUDevice] backed by one raw [DeviceSession]: every factory is
 * wired to the session's wrappers, the error scopes run on the session's
 * device, and the descriptor's uncaptured-error callback is routed through the
 * session's callback choreography. The session's ownership model is unchanged:
 * [close] closes the session, and the queue is the session's owned reference
 * wrapped once (so its label is stable across [queue] accesses).
 *
 * The capability snapshots ([features], [limits], [adapterInfo]) copy their
 * borrowed native data — freeing the feature list and adapter-info members
 * after the copy — and refuse undefined limits instead of inventing them.
 */
class DawnDevice internal constructor(
    internal val session: DeviceSession,
    descriptor: GPUDeviceDescriptor?,
    private val fallbackAdapter: Boolean,
) : GPUDevice {

    /** The descriptor's label; labels are Kotlin-side metadata in this backend. */
    override var label: String = descriptor?.label ?: ""

    /** The session's owned queue, wrapped once so the label survives accesses. */
    private val ownedQueue: DawnQueue = session.queue

    init {
        ownedQueue.label = descriptor?.defaultQueue?.label ?: ""
        descriptor?.onUncapturedError?.let { callback ->
            session.callbacks.uncapturedErrorSink = { error ->
                try {
                    callback.onUncapturedError(error)
                } catch (failure: Throwable) {
                    // The runtime has already recorded the error; a user
                    // callback failure must not take down the dispatch loop.
                }
            }
        }
    }

    override val features: GPUSupportedFeatures
        get() = session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val supported = WGPUSupportedFeatures.allocate(allocator)
                supported.featureCount = 0uL
                supported.features = null
                wgpuDeviceGetFeatures(session.handle, supported)
                try {
                    // Copy before the members are freed; the borrowed struct is untouched.
                    copySupportedFeatures(supported)
                } finally {
                    wgpuSupportedFeaturesFreeMembers(supported)
                }
            }
        }

    override val limits: GPUSupportedLimits
        get() = session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val native = allocator.allocateLimitsSnapshot()
                requireWgpuSuccess(wgpuDeviceGetLimits(session.handle, native), "wgpuDeviceGetLimits")
                readLimits(native)
            }
        }

    override val adapterInfo: GPUAdapterInfo
        get() = session.runtime.dispatcher.call {
            memoryScope { allocator ->
                val native = allocator.allocateAdapterInfoSnapshot()
                requireWgpuSuccess(wgpuAdapterGetInfo(session.adapter, native), "wgpuAdapterGetInfo")
                try {
                    // Copy before the members are freed; the borrowed views are Kotlin-owned after this.
                    copyAdapterInfo(native, fallbackAdapter)
                } finally {
                    wgpuAdapterInfoFreeMembers(native)
                }
            }
        }

    override val queue: GPUQueue
        get() = ownedQueue

    /**
     * The raw `WGPUDevice` pointer of this device, for platform integrators
     * (surface bridges). Not part of the WebGPU contract: the handle is valid
     * only while this device is open, and native calls must go through the
     * context's [NativeBridge.call].
     */
    fun nativeHandle(): Long = session.handle.handler.rawValue

    override fun createBuffer(descriptor: GPUBufferDescriptor): GPUBuffer = session.createBuffer(descriptor)

    override fun createTexture(descriptor: GPUTextureDescriptor): GPUTexture = session.createTexture(descriptor)

    override fun createSampler(descriptor: GPUSamplerDescriptor?): GPUSampler =
        session.createSampler(descriptor ?: DEFAULT_SAMPLER_DESCRIPTOR)

    override fun createBindGroupLayout(descriptor: GPUBindGroupLayoutDescriptor): GPUBindGroupLayout =
        session.createBindGroupLayout(descriptor)

    override fun createPipelineLayout(descriptor: GPUPipelineLayoutDescriptor): GPUPipelineLayout =
        session.createPipelineLayout(descriptor)

    override fun createBindGroup(descriptor: GPUBindGroupDescriptor): GPUBindGroup = session.createBindGroup(descriptor)

    override fun createShaderModule(descriptor: GPUShaderModuleDescriptor): GPUShaderModule =
        session.createShaderModule(descriptor)

    override fun createComputePipeline(descriptor: GPUComputePipelineDescriptor): GPUComputePipeline =
        session.createComputePipeline(descriptor)

    override fun createRenderPipeline(descriptor: GPURenderPipelineDescriptor): GPURenderPipeline =
        session.createRenderPipeline(descriptor)

    override suspend fun createComputePipelineAsync(descriptor: GPUComputePipelineDescriptor): Result<GPUComputePipeline> =
        session.createComputePipelineAsync(descriptor)

    override suspend fun createRenderPipelineAsync(descriptor: GPURenderPipelineDescriptor): Result<GPURenderPipeline> =
        session.createRenderPipelineAsync(descriptor)

    override fun createCommandEncoder(descriptor: GPUCommandEncoderDescriptor?): GPUCommandEncoder =
        session.createCommandEncoder(descriptor)

    override fun createRenderBundleEncoder(descriptor: GPURenderBundleEncoderDescriptor): GPURenderBundleEncoder =
        session.createRenderBundleEncoder(descriptor)

    override fun createQuerySet(descriptor: GPUQuerySetDescriptor): GPUQuerySet = session.createQuerySet(descriptor)

    override fun pushErrorScope(filter: GPUErrorFilter) {
        session.runtime.dispatcher.call {
            wgpuDevicePushErrorScope(session.handle, filter.toNativeErrorFilter())
        }
    }

    /**
     * Pops the innermost error scope: the native callback's message is copied
     * inside the callback, `Result.success(null)` is returned only when C
     * reports no error, and a reported error arrives as the mapped [GPUError].
     * A pop that itself fails — or a type with no WebGPU interface — is a
     * [Result.failure]; a cancellation stays a cancellation.
     */
    override suspend fun popErrorScope(): Result<GPUError?> = try {
        popErrorScopeOnSession()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (failure: Throwable) {
        Result.failure(failure)
    }

    override fun close() = session.close()

    /** The popped scope's copied outcome; owns nothing native. */
    private class ErrorScopeOutcome(
        val status: WGPUPopErrorScopeStatus,
        val type: WGPUErrorType,
        val message: String,
    )

    /** Issues the native pop and maps its settled outcome onto the contract. */
    private suspend fun popErrorScopeOnSession(): Result<GPUError?> {
        val runtime = session.runtime
        val operation = PendingOperation<ErrorScopeOutcome> { }
        runtime.dispatcher.call {
            var registration: CallbackRegistration<WGPUPopErrorScopeCallback>? = null
            runtime.beginSubdeviceOperation(
                operation = operation,
                issue = {
                    registration = WGPUPopErrorScopeCallback.register(
                        policy = CallbackPolicy.ONCE,
                        callback = { status, type, message, _ ->
                            // Callback thread: copy the borrowed message before returning.
                            val outcome = ErrorScopeOutcome(status, type, message.copyToString())
                            runtime.dispatcher.post {
                                runtime.finishSubdeviceOperation(operation, registration!!, Result.success(outcome))
                            }
                        },
                    )
                    memoryScope { allocator ->
                        val callbackInfo = WGPUPopErrorScopeCallbackInfo.allocate(
                            allocator = allocator,
                            mode = WGPUCallbackMode_AllowProcessEvents,
                            registration = registration,
                        ).also { it.nextInChain = null }
                        wgpuDevicePopErrorScope(allocator, session.handle, callbackInfo)
                    }
                },
                closeRegistration = { registration?.close() },
            )
        }
        val outcome = operation.await().getOrThrow()
        if (outcome.status != WGPUPopErrorScopeStatus_Success) {
            return Result.failure(DawnPopErrorScopeException(outcome.status, outcome.message))
        }
        if (outcome.type == WGPUErrorType_NoError) {
            return Result.success(null)
        }
        // C reported an error: null would be a lie. An untypeable one is a failure.
        val error = scopeError(outcome.type, outcome.message)
            ?: return Result.failure(
                DawnPopErrorScopeException(
                    outcome.status,
                    "an error of unknown type ${outcome.type} was reported: ${outcome.message}",
                ),
            )
        return Result.success(error)
    }

    private companion object {
        /**
         * The WebGPU default sampler request, for the public factory called
         * without a descriptor: edge-clamped addressing, nearest filtering,
         * LOD 0–32, no comparison, anisotropy 1 — exactly the published
         * descriptor module's defaults, restated here because the descriptors
         * module is a test-only dependency of this backend.
         */
        val DEFAULT_SAMPLER_DESCRIPTOR: GPUSamplerDescriptor = object : GPUSamplerDescriptor {
            override val addressModeU = GPUAddressMode.ClampToEdge
            override val addressModeV = GPUAddressMode.ClampToEdge
            override val addressModeW = GPUAddressMode.ClampToEdge
            override val magFilter = GPUFilterMode.Nearest
            override val minFilter = GPUFilterMode.Nearest
            override val mipmapFilter = GPUMipmapFilterMode.Nearest
            override val lodMinClamp = 0f
            override val lodMaxClamp = 32f
            override val compare: GPUCompareFunction? = null
            override val maxAnisotropy = 1u.toUShort()
            override val label = ""
        }
    }
}
