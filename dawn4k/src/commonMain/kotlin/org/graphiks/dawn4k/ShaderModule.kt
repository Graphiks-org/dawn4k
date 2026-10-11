package org.graphiks.dawn4k

import org.graphiks.dawn4k.internal.DeviceSession
import org.graphiks.dawn4k.internal.OwnedReference
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
    handle: WGPUShaderModule,
    label: String,
) : GPUShaderModule {

    override var label: String = label

    private val reference = OwnedReference(handle, release = { wgpuShaderModuleRelease(it) })
    internal val handle: WGPUShaderModule
        get() { session.requireOpen(); return reference.handle }

    override suspend fun getCompilationInfo(): Result<GPUCompilationInfo> {
        val nativeHandle = handle
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
                        wgpuShaderModuleGetCompilationInfo(allocator, nativeHandle, callbackInfo)
                    }
                },
                closeRegistration = { registration?.close() },
            )
        }
        return operation.await().also { operation.acceptOwnership() }
    }

    override fun close() {
        reference.close()
    }
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
