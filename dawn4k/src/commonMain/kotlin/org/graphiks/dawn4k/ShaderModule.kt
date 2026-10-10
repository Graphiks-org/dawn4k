package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.PendingOperation
import org.graphiks.dawn4k.mapper.allocateShaderModuleDescriptor
import org.graphiks.dawn4k.mapper.readCompilationInfo
import org.graphiks.dawn4k.native.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.native.WGPUCompilationInfoCallback
import org.graphiks.dawn4k.native.WGPUCompilationInfoCallbackInfo
import org.graphiks.dawn4k.native.WGPUCompilationInfoRequestStatus_Success
import org.graphiks.dawn4k.native.WGPUShaderModule
import org.graphiks.dawn4k.native.allocate
import org.graphiks.dawn4k.native.register
import org.graphiks.dawn4k.native.wgpuDeviceCreateShaderModule
import org.graphiks.dawn4k.native.wgpuShaderModuleGetCompilationInfo
import org.graphiks.dawn4k.native.wgpuShaderModuleRelease
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.memoryScope
import org.graphiks.webgpu.GPUCompilationInfo
import org.graphiks.webgpu.GPUShaderModule
import org.graphiks.webgpu.GPUShaderModuleDescriptor

/**
 * A raw [GPUShaderModule] backed by a Dawn `WGPUShaderModule`. Refcount-only:
 * [close] releases the reference immediately.
 */
class DawnShaderModule internal constructor(
    internal val session: DeviceSession,
    internal val handle: WGPUShaderModule,
    label: String,
) : GPUShaderModule {

    override var label: String = label

    init {
        session.resources.own(key = this, destroy = null, release = { wgpuShaderModuleRelease(handle) })
    }

    override suspend fun getCompilationInfo(): Result<GPUCompilationInfo> {
        val operation = session.runtime.pending<GPUCompilationInfo>(session) { }
        run {
            var registration: CallbackRegistration<WGPUCompilationInfoCallback>? = null
            session.runtime.beginSubdeviceOperation(
                operation = operation,
                issue = {
                    registration = WGPUCompilationInfoCallback.register(policy = CallbackPolicy.ONCE) { status, compilationInfo, _ ->
                        // Copy every message (and its positions) out of the borrowed
                        // pointer before returning to native code.
                        val result = if (status == WGPUCompilationInfoRequestStatus_Success && compilationInfo != null) {
                            Result.success(readCompilationInfo(compilationInfo))
                        } else {
                            Result.failure(
                                IllegalStateException("wgpuShaderModuleGetCompilationInfo failed (status=$status)"),
                            )
                        }
                        session.runtime.postCallback {
                            session.runtime.finishSubdeviceOperation(operation, registration!!, result)
                        }
                    }
                    memoryScope { allocator ->
                        val callbackInfo = WGPUCompilationInfoCallbackInfo.allocate(
                            allocator = allocator,
                            mode = WGPUCallbackMode_AllowProcessEvents,
                            registration = registration,
                        ).also { it.nextInChain = null }
                        wgpuShaderModuleGetCompilationInfo(allocator, handle, callbackInfo)
                    }
                },
                closeRegistration = { registration?.close() },
            )
        }
        return operation.await().also { operation.acceptOwnership() }
    }

    override fun close() {
        session.resources.release(this)
    }
}

/** Refuses a foreign or foreign-session shader module before its handle is read. */
internal fun GPUShaderModule.requireDawnShaderModule(owner: DeviceSession): DawnShaderModule {
    val dawn = this as? DawnShaderModule
        ?: throw IllegalArgumentException("the shader module does not belong to this Dawn backend: $this")
    require(dawn.session === owner) { "the shader module belongs to a different device session" }
    return dawn
}

/** Creates a [DawnShaderModule] on [this] session and registers its reference. */
internal fun DeviceSession.createShaderModule(descriptor: GPUShaderModuleDescriptor): DawnShaderModule =
    run {
        memoryScope { allocator ->
            val native = allocator.allocateShaderModuleDescriptor(descriptor)
            val handle = wgpuDeviceCreateShaderModule(this.handle, native)
                ?: throw IllegalStateException("wgpuDeviceCreateShaderModule returned no shader module")
            DawnShaderModule(this, handle, descriptor.label)
        }
    }
