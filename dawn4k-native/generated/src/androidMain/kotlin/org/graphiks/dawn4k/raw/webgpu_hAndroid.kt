package org.graphiks.dawn4k.raw

import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.engine.NativeEngine
import org.graphiks.kffi.engine.UpcallEngine
import org.graphiks.kffi.CallbackExceptionHandler
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.CallbackRegistration
import org.graphiks.kffi.CallbackRuntime
import org.graphiks.kffi.CallbackRuntimeApi
import org.graphiks.kffi.PreparedCallbackRegistration
import org.graphiks.kffi.UnsafeCallbackRearmApi
import org.graphiks.kffi.CString
import org.graphiks.kffi.ArrayHolder
import org.graphiks.kffi.MemoryAllocator
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.toAddress
import kotlin.OptIn
import kotlin.UnsupportedOperationException
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

private object KextractAndroidBootstrap {
    init {
        java.lang.System.loadLibrary("webgpu_dawn")
    }
    
    private val libraryHandle: kotlin.Long by lazy { NativeEngine.loadNativeLibrary(java.lang.System.mapLibraryName("webgpu_dawn")).takeIf { it != 0L } ?: error("Unable to load native library: webgpu_dawn") }
    fun resolve(name: kotlin.String): kotlin.Long = NativeEngine.resolveSymbolIn(libraryHandle, name)
}

actual interface WGPUStringView {
    actual var data: CString?
    actual var length: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStringView = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStringView = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStringView) -> Unit): ArrayHolder<WGPUStringView> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUStringView>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUStringView {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var data: CString?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let(::CString)
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var length: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUStringView {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var data: CString?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let(::CString)
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var length: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

@kotlin.jvm.JvmInline
actual value class WGPUAdapter actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUBindGroup actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUBindGroupLayout actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUBuffer actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUCommandBuffer actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUCommandEncoder actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUComputePassEncoder actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUComputePipeline actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUDevice actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUExternalTexture actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUInstance actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUPipelineLayout actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUQuerySet actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUQueue actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPURenderBundle actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPURenderBundleEncoder actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPURenderPassEncoder actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPURenderPipeline actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUResourceTable actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUSampler actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUShaderModule actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUSharedBufferMemory actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUSharedFence actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUSharedTextureMemory actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUSurface actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUTexelBufferView actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUTexture actual constructor(actual val handler: NativeAddress)

@kotlin.jvm.JvmInline
actual value class WGPUTextureView actual constructor(actual val handler: NativeAddress)

actual interface WGPUChainedStruct {
    actual var next: WGPUChainedStruct?
    actual var sType: WGPUSType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUChainedStruct = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUChainedStruct = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUChainedStruct) -> Unit): ArrayHolder<WGPUChainedStruct> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUChainedStruct>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUChainedStruct {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var next: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var sType: WGPUSType
            get() = mem.readUInt(8uL) as WGPUSType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUChainedStruct {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var next: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var sType: WGPUSType
            get() = mem.readUInt(8uL) as WGPUSType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBufferMapCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferMapCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferMapCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferMapCallbackInfo) -> Unit): ArrayHolder<WGPUBufferMapCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUBufferMapCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferMapCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferMapCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCompilationInfoCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCompilationInfoCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationInfoCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfoCallbackInfo) -> Unit): ArrayHolder<WGPUCompilationInfoCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUCompilationInfoCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationInfoCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationInfoCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCreateComputePipelineAsyncCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCreateComputePipelineAsyncCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCreateComputePipelineAsyncCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateComputePipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateComputePipelineAsyncCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUCreateComputePipelineAsyncCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCreateComputePipelineAsyncCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCreateComputePipelineAsyncCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCreateRenderPipelineAsyncCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCreateRenderPipelineAsyncCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCreateRenderPipelineAsyncCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateRenderPipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateRenderPipelineAsyncCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUCreateRenderPipelineAsyncCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCreateRenderPipelineAsyncCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCreateRenderPipelineAsyncCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnLoadCacheDataCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnLoadCacheDataCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnLoadCacheDataCallbackInfo = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnLoadCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnLoadCacheDataCallbackInfo> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnLoadCacheDataCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnLoadCacheDataCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnLoadCacheDataCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnStoreCacheDataCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnStoreCacheDataCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnStoreCacheDataCallbackInfo = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnStoreCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnStoreCacheDataCallbackInfo> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnStoreCacheDataCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnStoreCacheDataCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnStoreCacheDataCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDeviceLostCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDeviceLostCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDeviceLostCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceLostCallbackInfo) -> Unit): ArrayHolder<WGPUDeviceLostCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUDeviceLostCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDeviceLostCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDeviceLostCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDisposeCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDisposeCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDisposeCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDisposeCallbackInfo) -> Unit): ArrayHolder<WGPUDisposeCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUDisposeCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDisposeCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDisposeCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPULoggingCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPULoggingCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPULoggingCallbackInfo = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULoggingCallbackInfo) -> Unit): ArrayHolder<WGPULoggingCallbackInfo> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPULoggingCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPULoggingCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPULoggingCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPopErrorScopeCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPopErrorScopeCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPopErrorScopeCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPopErrorScopeCallbackInfo) -> Unit): ArrayHolder<WGPUPopErrorScopeCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUPopErrorScopeCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPopErrorScopeCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPopErrorScopeCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUQueueWorkDoneCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUQueueWorkDoneCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUQueueWorkDoneCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueWorkDoneCallbackInfo) -> Unit): ArrayHolder<WGPUQueueWorkDoneCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUQueueWorkDoneCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUQueueWorkDoneCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUQueueWorkDoneCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURequestAdapterCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterCallbackInfo) -> Unit): ArrayHolder<WGPURequestAdapterCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPURequestAdapterCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURequestDeviceCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var mode: WGPUCallbackMode
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestDeviceCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestDeviceCallbackInfo = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestDeviceCallbackInfo) -> Unit): ArrayHolder<WGPURequestDeviceCallbackInfo> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPURequestDeviceCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestDeviceCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestDeviceCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mode: WGPUCallbackMode
            get() = mem.readUInt(8uL) as WGPUCallbackMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUUncapturedErrorCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUUncapturedErrorCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUUncapturedErrorCallbackInfo = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUUncapturedErrorCallbackInfo) -> Unit): ArrayHolder<WGPUUncapturedErrorCallbackInfo> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUUncapturedErrorCallbackInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUUncapturedErrorCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUUncapturedErrorCallbackInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var callback: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override var userdata1: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var userdata2: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesD3D {
    actual var chain: WGPUChainedStruct
    actual var shaderModel: UInt
    actual var adapterLUIDLowPart: UInt
    actual var adapterLUIDHighPart: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesD3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesD3D = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesD3D) -> Unit): ArrayHolder<WGPUAdapterPropertiesD3D> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesD3D>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesD3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var shaderModel: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var adapterLUIDLowPart: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var adapterLUIDHighPart: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesD3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var shaderModel: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var adapterLUIDLowPart: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var adapterLUIDHighPart: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesDrm {
    actual var chain: WGPUChainedStruct
    actual var hasPrimary: UInt
    actual var hasRender: UInt
    actual var primaryMajor: ULong
    actual var primaryMinor: ULong
    actual var renderMajor: ULong
    actual var renderMinor: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesDrm = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesDrm = ByReference(allocator.allocateBuffer(56uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesDrm) -> Unit): ArrayHolder<WGPUAdapterPropertiesDrm> {
            val buffer = allocator.allocateBuffer(56uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesDrm>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 56L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesDrm {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hasPrimary: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var hasRender: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var primaryMajor: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var primaryMinor: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var renderMajor: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var renderMinor: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesDrm {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hasPrimary: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var hasRender: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var primaryMajor: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var primaryMinor: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var renderMajor: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var renderMinor: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesVk {
    actual var chain: WGPUChainedStruct
    actual var driverVersion: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesVk = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesVk = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesVk) -> Unit): ArrayHolder<WGPUAdapterPropertiesVk> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesVk>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesVk {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var driverVersion: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesVk {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var driverVersion: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesWGPU {
    actual var chain: WGPUChainedStruct
    actual var backendType: WGPUBackendType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesWGPU = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesWGPU = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesWGPU) -> Unit): ArrayHolder<WGPUAdapterPropertiesWGPU> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesWGPU>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesWGPU {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(16uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesWGPU {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(16uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBindingResource {
    actual var nextInChain: WGPUChainedStruct?
    actual var buffer: WGPUBuffer?
    actual var offset: ULong
    actual var size: ULong
    actual var sampler: WGPUSampler?
    actual var textureView: WGPUTextureView?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindingResource = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindingResource = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindingResource) -> Unit): ArrayHolder<WGPUBindingResource> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUBindingResource>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindingResource {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var offset: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var textureView: WGPUTextureView?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindingResource {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var offset: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var textureView: WGPUTextureView?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBlendComponent {
    actual var operation: WGPUBlendOperation
    actual var srcFactor: WGPUBlendFactor
    actual var dstFactor: WGPUBlendFactor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBlendComponent = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBlendComponent = ByReference(allocator.allocateBuffer(12uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendComponent) -> Unit): ArrayHolder<WGPUBlendComponent> {
            val buffer = allocator.allocateBuffer(12uL * size)
            val result = ArrayHolder<WGPUBlendComponent>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 12L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBlendComponent {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var operation: WGPUBlendOperation
            get() = mem.readUInt(0uL) as WGPUBlendOperation
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var srcFactor: WGPUBlendFactor
            get() = mem.readUInt(4uL) as WGPUBlendFactor
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var dstFactor: WGPUBlendFactor
            get() = mem.readUInt(8uL) as WGPUBlendFactor
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBlendComponent {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var operation: WGPUBlendOperation
            get() = mem.readUInt(0uL) as WGPUBlendOperation
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var srcFactor: WGPUBlendFactor
            get() = mem.readUInt(4uL) as WGPUBlendFactor
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var dstFactor: WGPUBlendFactor
            get() = mem.readUInt(8uL) as WGPUBlendFactor
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBufferBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUBufferBindingType
    actual var hasDynamicOffset: UInt
    actual var minBindingSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferBindingLayout = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferBindingLayout) -> Unit): ArrayHolder<WGPUBufferBindingLayout> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUBufferBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUBufferBindingType
            get() = mem.readUInt(8uL) as WGPUBufferBindingType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var hasDynamicOffset: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var minBindingSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUBufferBindingType
            get() = mem.readUInt(8uL) as WGPUBufferBindingType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var hasDynamicOffset: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var minBindingSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBufferHostMappedPointer {
    actual var chain: WGPUChainedStruct
    actual var pointer: NativeAddress?
    actual var disposeCallback: NativeAddress?
    actual var userdata: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferHostMappedPointer = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferHostMappedPointer = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferHostMappedPointer) -> Unit): ArrayHolder<WGPUBufferHostMappedPointer> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUBufferHostMappedPointer>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferHostMappedPointer {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var pointer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var disposeCallback: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferHostMappedPointer {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var pointer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var disposeCallback: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var userdata: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUColor {
    actual var r: Double
    actual var g: Double
    actual var b: Double
    actual var a: Double
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColor) -> Unit): ArrayHolder<WGPUColor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUColor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUColor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var r: Double
            get() = mem.readDouble(0uL)
            set(value) { mem.writeDouble(value, 0uL) }
        override var g: Double
            get() = mem.readDouble(8uL)
            set(value) { mem.writeDouble(value, 8uL) }
        override var b: Double
            get() = mem.readDouble(16uL)
            set(value) { mem.writeDouble(value, 16uL) }
        override var a: Double
            get() = mem.readDouble(24uL)
            set(value) { mem.writeDouble(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUColor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var r: Double
            get() = mem.readDouble(0uL)
            set(value) { mem.writeDouble(value, 0uL) }
        override var g: Double
            get() = mem.readDouble(8uL)
            set(value) { mem.writeDouble(value, 8uL) }
        override var b: Double
            get() = mem.readDouble(16uL)
            set(value) { mem.writeDouble(value, 16uL) }
        override var a: Double
            get() = mem.readDouble(24uL)
            set(value) { mem.writeDouble(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUColorSpaceDawn {
    actual var nextInChain: WGPUChainedStruct?
    actual var primaries: WGPUColorSpacePrimariesDawn
    actual var transfer: WGPUColorSpaceTransferDawn
    actual var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
    actual var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
    actual var hdrReferenceWhiteLuminance: Float
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColorSpaceDawn = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColorSpaceDawn = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorSpaceDawn) -> Unit): ArrayHolder<WGPUColorSpaceDawn> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUColorSpaceDawn>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorSpaceDawn {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var primaries: WGPUColorSpacePrimariesDawn
            get() = mem.readUInt(8uL) as WGPUColorSpacePrimariesDawn
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var transfer: WGPUColorSpaceTransferDawn
            get() = mem.readUInt(12uL) as WGPUColorSpaceTransferDawn
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
            get() = mem.readUInt(16uL) as WGPUColorSpaceYCbCrRangeDawn
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
            get() = mem.readUInt(20uL) as WGPUColorSpaceYCbCrMatrixDawn
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var hdrReferenceWhiteLuminance: Float
            get() = mem.readFloat(24uL)
            set(value) { mem.writeFloat(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorSpaceDawn {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var primaries: WGPUColorSpacePrimariesDawn
            get() = mem.readUInt(8uL) as WGPUColorSpacePrimariesDawn
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var transfer: WGPUColorSpaceTransferDawn
            get() = mem.readUInt(12uL) as WGPUColorSpaceTransferDawn
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
            get() = mem.readUInt(16uL) as WGPUColorSpaceYCbCrRangeDawn
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
            get() = mem.readUInt(20uL) as WGPUColorSpaceYCbCrMatrixDawn
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var hdrReferenceWhiteLuminance: Float
            get() = mem.readFloat(24uL)
            set(value) { mem.writeFloat(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUColorTargetStateExpandResolveTextureDawn {
    actual var chain: WGPUChainedStruct
    actual var enabled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColorTargetStateExpandResolveTextureDawn = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColorTargetStateExpandResolveTextureDawn = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetStateExpandResolveTextureDawn) -> Unit): ArrayHolder<WGPUColorTargetStateExpandResolveTextureDawn> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUColorTargetStateExpandResolveTextureDawn>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorTargetStateExpandResolveTextureDawn {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enabled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorTargetStateExpandResolveTextureDawn {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enabled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCommandBufferDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCommandBufferDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCommandBufferDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandBufferDescriptor) -> Unit): ArrayHolder<WGPUCommandBufferDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUCommandBufferDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCommandBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCommandBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCompatibilityModeLimits {
    actual var chain: WGPUChainedStruct
    actual var maxStorageBuffersInVertexStage: UInt
    actual var maxStorageTexturesInVertexStage: UInt
    actual var maxStorageBuffersInFragmentStage: UInt
    actual var maxStorageTexturesInFragmentStage: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCompatibilityModeLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCompatibilityModeLimits = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompatibilityModeLimits) -> Unit): ArrayHolder<WGPUCompatibilityModeLimits> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUCompatibilityModeLimits>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompatibilityModeLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var maxStorageBuffersInVertexStage: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var maxStorageTexturesInVertexStage: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxStorageBuffersInFragmentStage: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var maxStorageTexturesInFragmentStage: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompatibilityModeLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var maxStorageBuffersInVertexStage: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var maxStorageTexturesInVertexStage: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxStorageBuffersInFragmentStage: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var maxStorageTexturesInFragmentStage: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUConstantEntry {
    actual var nextInChain: WGPUChainedStruct?
    actual var key: WGPUStringView
    actual var value: Double
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUConstantEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUConstantEntry = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUConstantEntry) -> Unit): ArrayHolder<WGPUConstantEntry> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUConstantEntry>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUConstantEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var key: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var value: Double
            get() = mem.readDouble(24uL)
            set(value) { mem.writeDouble(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUConstantEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var key: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var value: Double
            get() = mem.readDouble(24uL)
            set(value) { mem.writeDouble(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCopyTextureForBrowserOptions {
    actual var nextInChain: WGPUChainedStruct?
    actual var flipY: UInt
    actual var needsColorSpaceConversion: UInt
    actual var srcAlphaMode: WGPUAlphaMode
    actual var srcTransferFunctionParameters: NativeAddress?
    actual var conversionMatrix: NativeAddress?
    actual var dstTransferFunctionParameters: NativeAddress?
    actual var dstAlphaMode: WGPUAlphaMode
    actual var internalUsage: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCopyTextureForBrowserOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCopyTextureForBrowserOptions = ByReference(allocator.allocateBuffer(56uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCopyTextureForBrowserOptions) -> Unit): ArrayHolder<WGPUCopyTextureForBrowserOptions> {
            val buffer = allocator.allocateBuffer(56uL * size)
            val result = ArrayHolder<WGPUCopyTextureForBrowserOptions>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 56L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCopyTextureForBrowserOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var flipY: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var needsColorSpaceConversion: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var srcAlphaMode: WGPUAlphaMode
            get() = mem.readUInt(16uL) as WGPUAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var conversionMatrix: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override var dstAlphaMode: WGPUAlphaMode
            get() = mem.readUInt(48uL) as WGPUAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var internalUsage: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCopyTextureForBrowserOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var flipY: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var needsColorSpaceConversion: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var srcAlphaMode: WGPUAlphaMode
            get() = mem.readUInt(16uL) as WGPUAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var conversionMatrix: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override var dstAlphaMode: WGPUAlphaMode
            get() = mem.readUInt(48uL) as WGPUAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var internalUsage: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnAdapterPropertiesPowerPreference {
    actual var chain: WGPUChainedStruct
    actual var powerPreference: WGPUPowerPreference
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnAdapterPropertiesPowerPreference = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnAdapterPropertiesPowerPreference = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnAdapterPropertiesPowerPreference) -> Unit): ArrayHolder<WGPUDawnAdapterPropertiesPowerPreference> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnAdapterPropertiesPowerPreference>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnAdapterPropertiesPowerPreference {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var powerPreference: WGPUPowerPreference
            get() = mem.readUInt(16uL) as WGPUPowerPreference
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnAdapterPropertiesPowerPreference {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var powerPreference: WGPUPowerPreference
            get() = mem.readUInt(16uL) as WGPUPowerPreference
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnBufferDescriptorErrorInfoFromWireClient {
    actual var chain: WGPUChainedStruct
    actual var outOfMemory: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnBufferDescriptorErrorInfoFromWireClient = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnBufferDescriptorErrorInfoFromWireClient = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnBufferDescriptorErrorInfoFromWireClient) -> Unit): ArrayHolder<WGPUDawnBufferDescriptorErrorInfoFromWireClient> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnBufferDescriptorErrorInfoFromWireClient>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnBufferDescriptorErrorInfoFromWireClient {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var outOfMemory: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnBufferDescriptorErrorInfoFromWireClient {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var outOfMemory: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnCacheDeviceDescriptor {
    actual var chain: WGPUChainedStruct
    actual var isolationKey: WGPUStringView
    actual var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
    actual var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnCacheDeviceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnCacheDeviceDescriptor = ByReference(allocator.allocateBuffer(96uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCacheDeviceDescriptor) -> Unit): ArrayHolder<WGPUDawnCacheDeviceDescriptor> {
            val buffer = allocator.allocateBuffer(96uL * size)
            val result = ArrayHolder<WGPUDawnCacheDeviceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 96L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnCacheDeviceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 96uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var isolationKey: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
            get() = WGPUDawnLoadCacheDataCallbackInfo.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 32uL, 32uL)
            }
        override var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
            get() = WGPUDawnStoreCacheDataCallbackInfo.ByValue(NativeAddress(handle.rawValue + 64L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 64uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnCacheDeviceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 96uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var isolationKey: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
            get() = WGPUDawnLoadCacheDataCallbackInfo.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 32uL, 32uL)
            }
        override var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
            get() = WGPUDawnStoreCacheDataCallbackInfo.ByValue(NativeAddress(handle.rawValue + 64L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 64uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnCompilationMessageUtf16 {
    actual var chain: WGPUChainedStruct
    actual var linePos: ULong
    actual var offset: ULong
    actual var length: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnCompilationMessageUtf16 = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnCompilationMessageUtf16 = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCompilationMessageUtf16) -> Unit): ArrayHolder<WGPUDawnCompilationMessageUtf16> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUDawnCompilationMessageUtf16>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnCompilationMessageUtf16 {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var linePos: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var offset: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var length: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnCompilationMessageUtf16 {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var linePos: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var offset: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var length: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnConsumeAdapterDescriptor {
    actual var chain: WGPUChainedStruct
    actual var consumeAdapter: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnConsumeAdapterDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnConsumeAdapterDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnConsumeAdapterDescriptor) -> Unit): ArrayHolder<WGPUDawnConsumeAdapterDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnConsumeAdapterDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnConsumeAdapterDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var consumeAdapter: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnConsumeAdapterDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var consumeAdapter: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnDeviceAllocatorControl {
    actual var chain: WGPUChainedStruct
    actual var allocatorHeapBlockSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDeviceAllocatorControl = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDeviceAllocatorControl = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDeviceAllocatorControl) -> Unit): ArrayHolder<WGPUDawnDeviceAllocatorControl> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnDeviceAllocatorControl>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDeviceAllocatorControl {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var allocatorHeapBlockSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDeviceAllocatorControl {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var allocatorHeapBlockSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnDrmFormatProperties {
    actual var modifier: ULong
    actual var modifierPlaneCount: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatProperties = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatProperties) -> Unit): ArrayHolder<WGPUDawnDrmFormatProperties> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUDawnDrmFormatProperties>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDrmFormatProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var modifier: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var modifierPlaneCount: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDrmFormatProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var modifier: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var modifierPlaneCount: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnEncoderInternalUsageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var useInternalUsages: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnEncoderInternalUsageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnEncoderInternalUsageDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnEncoderInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnEncoderInternalUsageDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnEncoderInternalUsageDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnEncoderInternalUsageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var useInternalUsages: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnEncoderInternalUsageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var useInternalUsages: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnFakeBufferOOMForTesting {
    actual var chain: WGPUChainedStruct
    actual var fakeOOMAtWireClientMap: UInt
    actual var fakeOOMAtNativeMap: UInt
    actual var fakeOOMAtDevice: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFakeBufferOOMForTesting = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFakeBufferOOMForTesting = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeBufferOOMForTesting) -> Unit): ArrayHolder<WGPUDawnFakeBufferOOMForTesting> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnFakeBufferOOMForTesting>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFakeBufferOOMForTesting {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var fakeOOMAtWireClientMap: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var fakeOOMAtNativeMap: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var fakeOOMAtDevice: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFakeBufferOOMForTesting {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var fakeOOMAtWireClientMap: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var fakeOOMAtNativeMap: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var fakeOOMAtDevice: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnFakeDeviceInitializeErrorForTesting {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFakeDeviceInitializeErrorForTesting = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFakeDeviceInitializeErrorForTesting = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeDeviceInitializeErrorForTesting) -> Unit): ArrayHolder<WGPUDawnFakeDeviceInitializeErrorForTesting> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUDawnFakeDeviceInitializeErrorForTesting>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFakeDeviceInitializeErrorForTesting {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFakeDeviceInitializeErrorForTesting {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnHostMappedPointerLimits {
    actual var chain: WGPUChainedStruct
    actual var hostMappedPointerAlignment: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnHostMappedPointerLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnHostMappedPointerLimits = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnHostMappedPointerLimits) -> Unit): ArrayHolder<WGPUDawnHostMappedPointerLimits> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnHostMappedPointerLimits>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnHostMappedPointerLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hostMappedPointerAlignment: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnHostMappedPointerLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hostMappedPointerAlignment: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnInjectedInvalidSType {
    actual var chain: WGPUChainedStruct
    actual var invalidSType: WGPUSType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnInjectedInvalidSType = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnInjectedInvalidSType = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnInjectedInvalidSType) -> Unit): ArrayHolder<WGPUDawnInjectedInvalidSType> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnInjectedInvalidSType>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnInjectedInvalidSType {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var invalidSType: WGPUSType
            get() = mem.readUInt(16uL) as WGPUSType
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnInjectedInvalidSType {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var invalidSType: WGPUSType
            get() = mem.readUInt(16uL) as WGPUSType
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnRenderPassSampleCount {
    actual var chain: WGPUChainedStruct
    actual var sampleCount: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnRenderPassSampleCount = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnRenderPassSampleCount = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnRenderPassSampleCount) -> Unit): ArrayHolder<WGPUDawnRenderPassSampleCount> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnRenderPassSampleCount>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnRenderPassSampleCount {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sampleCount: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnRenderPassSampleCount {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sampleCount: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnShaderModuleSPIRVOptionsDescriptor {
    actual var chain: WGPUChainedStruct
    actual var allowNonUniformDerivatives: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnShaderModuleSPIRVOptionsDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnShaderModuleSPIRVOptionsDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderModuleSPIRVOptionsDescriptor) -> Unit): ArrayHolder<WGPUDawnShaderModuleSPIRVOptionsDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnShaderModuleSPIRVOptionsDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnShaderModuleSPIRVOptionsDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var allowNonUniformDerivatives: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnShaderModuleSPIRVOptionsDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var allowNonUniformDerivatives: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnShaderSourceSPIRV {
    actual var chain: WGPUChainedStruct
    actual var codeSize: ULong
    actual var code: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnShaderSourceSPIRV = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnShaderSourceSPIRV = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUDawnShaderSourceSPIRV> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnShaderSourceSPIRV>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnShaderSourceSPIRV {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var codeSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var code: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnShaderSourceSPIRV {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var codeSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var code: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnTexelCopyBufferRowAlignmentLimits {
    actual var chain: WGPUChainedStruct
    actual var minTexelCopyBufferRowAlignment: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnTexelCopyBufferRowAlignmentLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTexelCopyBufferRowAlignmentLimits = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTexelCopyBufferRowAlignmentLimits) -> Unit): ArrayHolder<WGPUDawnTexelCopyBufferRowAlignmentLimits> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnTexelCopyBufferRowAlignmentLimits>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTexelCopyBufferRowAlignmentLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var minTexelCopyBufferRowAlignment: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTexelCopyBufferRowAlignmentLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var minTexelCopyBufferRowAlignment: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnTextureInternalUsageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var internalUsage: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnTextureInternalUsageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTextureInternalUsageDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTextureInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnTextureInternalUsageDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUDawnTextureInternalUsageDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTextureInternalUsageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var internalUsage: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTextureInternalUsageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var internalUsage: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnTogglesDescriptor {
    actual var chain: WGPUChainedStruct
    actual var enabledToggleCount: ULong
    actual var enabledToggles: NativeAddress?
    actual var disabledToggleCount: ULong
    actual var disabledToggles: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnTogglesDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTogglesDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTogglesDescriptor) -> Unit): ArrayHolder<WGPUDawnTogglesDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUDawnTogglesDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTogglesDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enabledToggleCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var enabledToggles: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var disabledToggleCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var disabledToggles: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnTogglesDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enabledToggleCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var enabledToggles: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var disabledToggleCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var disabledToggles: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnWGSLBlocklist {
    actual var chain: WGPUChainedStruct
    actual var blocklistedFeatureCount: ULong
    actual var blocklistedFeatures: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnWGSLBlocklist = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnWGSLBlocklist = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWGSLBlocklist) -> Unit): ArrayHolder<WGPUDawnWGSLBlocklist> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnWGSLBlocklist>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnWGSLBlocklist {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var blocklistedFeatureCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var blocklistedFeatures: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnWGSLBlocklist {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var blocklistedFeatureCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var blocklistedFeatures: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnWireWGSLControl {
    actual var chain: WGPUChainedStruct
    actual var enableExperimental: UInt
    actual var enableUnsafe: UInt
    actual var enableTesting: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnWireWGSLControl = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnWireWGSLControl = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWireWGSLControl) -> Unit): ArrayHolder<WGPUDawnWireWGSLControl> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnWireWGSLControl>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnWireWGSLControl {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enableExperimental: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var enableUnsafe: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var enableTesting: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnWireWGSLControl {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var enableExperimental: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var enableUnsafe: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var enableTesting: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
    actual var chain: WGPUChainedStruct
    actual var selector: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUEmscriptenSurfaceSourceCanvasHTMLSelector) -> Unit): ArrayHolder<WGPUEmscriptenSurfaceSourceCanvasHTMLSelector> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var selector: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var selector: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUExtent2D {
    actual var width: UInt
    actual var height: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExtent2D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExtent2D = ByReference(allocator.allocateBuffer(8uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent2D) -> Unit): ArrayHolder<WGPUExtent2D> {
            val buffer = allocator.allocateBuffer(8uL * size)
            val result = ArrayHolder<WGPUExtent2D>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 8L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUExtent2D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var width: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var height: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUExtent2D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var width: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var height: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUExtent3D {
    actual var width: UInt
    actual var height: UInt
    actual var depthOrArrayLayers: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExtent3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExtent3D = ByReference(allocator.allocateBuffer(12uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent3D) -> Unit): ArrayHolder<WGPUExtent3D> {
            val buffer = allocator.allocateBuffer(12uL * size)
            val result = ArrayHolder<WGPUExtent3D>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 12L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUExtent3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var width: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var height: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override var depthOrArrayLayers: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUExtent3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var width: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var height: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override var depthOrArrayLayers: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUExternalTextureBindingEntry {
    actual var chain: WGPUChainedStruct
    actual var externalTexture: WGPUExternalTexture?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingEntry = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingEntry) -> Unit): ArrayHolder<WGPUExternalTextureBindingEntry> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUExternalTextureBindingEntry>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureBindingEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var externalTexture: WGPUExternalTexture?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUExternalTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureBindingEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var externalTexture: WGPUExternalTexture?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUExternalTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUExternalTextureBindingLayout {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingLayout = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingLayout) -> Unit): ArrayHolder<WGPUExternalTextureBindingLayout> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUExternalTextureBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUFuture {
    actual var id: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUFuture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUFuture = ByReference(allocator.allocateBuffer(8uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFuture) -> Unit): ArrayHolder<WGPUFuture> {
            val buffer = allocator.allocateBuffer(8uL * size)
            val result = ArrayHolder<WGPUFuture>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 8L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUFuture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var id: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUFuture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var id: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUInstanceLimits {
    actual var nextInChain: WGPUChainedStruct?
    actual var timedWaitAnyMaxCount: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUInstanceLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUInstanceLimits = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceLimits) -> Unit): ArrayHolder<WGPUInstanceLimits> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUInstanceLimits>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUInstanceLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var timedWaitAnyMaxCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUInstanceLimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var timedWaitAnyMaxCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
    actual var unused: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER = ByReference(allocator.allocateBuffer(4uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER) -> Unit): ArrayHolder<WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER> {
            val buffer = allocator.allocateBuffer(4uL * size)
            val result = ArrayHolder<WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 4L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 4uL) }
        override var unused: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 4uL) }
        override var unused: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUMemoryHeapInfo {
    actual var properties: ULong
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUMemoryHeapInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUMemoryHeapInfo = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMemoryHeapInfo) -> Unit): ArrayHolder<WGPUMemoryHeapInfo> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUMemoryHeapInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUMemoryHeapInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var properties: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var size: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUMemoryHeapInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var properties: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var size: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUMultisampleState {
    actual var nextInChain: WGPUChainedStruct?
    actual var count: UInt
    actual var mask: UInt
    actual var alphaToCoverageEnabled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUMultisampleState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUMultisampleState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMultisampleState) -> Unit): ArrayHolder<WGPUMultisampleState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUMultisampleState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUMultisampleState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var count: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var mask: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var alphaToCoverageEnabled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUMultisampleState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var count: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var mask: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var alphaToCoverageEnabled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUOrigin2D {
    actual var x: UInt
    actual var y: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUOrigin2D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUOrigin2D = ByReference(allocator.allocateBuffer(8uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin2D) -> Unit): ArrayHolder<WGPUOrigin2D> {
            val buffer = allocator.allocateBuffer(8uL * size)
            val result = ArrayHolder<WGPUOrigin2D>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 8L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUOrigin2D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var x: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var y: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUOrigin2D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var x: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var y: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUOrigin3D {
    actual var x: UInt
    actual var y: UInt
    actual var z: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUOrigin3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUOrigin3D = ByReference(allocator.allocateBuffer(12uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin3D) -> Unit): ArrayHolder<WGPUOrigin3D> {
            val buffer = allocator.allocateBuffer(12uL * size)
            val result = ArrayHolder<WGPUOrigin3D>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 12L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUOrigin3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var x: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var y: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override var z: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUOrigin3D {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 12uL) }
        override var x: UInt
            get() = mem.readUInt(0uL)
            set(value) { mem.writeUInt(value, 0uL) }
        override var y: UInt
            get() = mem.readUInt(4uL)
            set(value) { mem.writeUInt(value, 4uL) }
        override var z: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPassTimestampWrites {
    actual var nextInChain: WGPUChainedStruct?
    actual var querySet: WGPUQuerySet?
    actual var beginningOfPassWriteIndex: UInt
    actual var endOfPassWriteIndex: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPassTimestampWrites = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPassTimestampWrites = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPassTimestampWrites) -> Unit): ArrayHolder<WGPUPassTimestampWrites> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUPassTimestampWrites>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPassTimestampWrites {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var querySet: WGPUQuerySet?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUQuerySet(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var beginningOfPassWriteIndex: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var endOfPassWriteIndex: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPassTimestampWrites {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var querySet: WGPUQuerySet?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUQuerySet(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var beginningOfPassWriteIndex: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var endOfPassWriteIndex: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPipelineLayoutResourceTable {
    actual var chain: WGPUChainedStruct
    actual var usesResourceTable: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutResourceTable = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutResourceTable = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutResourceTable) -> Unit): ArrayHolder<WGPUPipelineLayoutResourceTable> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUPipelineLayoutResourceTable>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutResourceTable {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var usesResourceTable: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutResourceTable {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var usesResourceTable: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPipelineLayoutStorageAttachment {
    actual var nextInChain: WGPUChainedStruct?
    actual var offset: ULong
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutStorageAttachment = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutStorageAttachment = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutStorageAttachment) -> Unit): ArrayHolder<WGPUPipelineLayoutStorageAttachment> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUPipelineLayoutStorageAttachment>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutStorageAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(16uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutStorageAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(16uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPrimitiveState {
    actual var nextInChain: WGPUChainedStruct?
    actual var topology: WGPUPrimitiveTopology
    actual var stripIndexFormat: WGPUIndexFormat
    actual var frontFace: WGPUFrontFace
    actual var cullMode: WGPUCullMode
    actual var unclippedDepth: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPrimitiveState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPrimitiveState = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPrimitiveState) -> Unit): ArrayHolder<WGPUPrimitiveState> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUPrimitiveState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPrimitiveState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var topology: WGPUPrimitiveTopology
            get() = mem.readUInt(8uL) as WGPUPrimitiveTopology
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var stripIndexFormat: WGPUIndexFormat
            get() = mem.readUInt(12uL) as WGPUIndexFormat
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var frontFace: WGPUFrontFace
            get() = mem.readUInt(16uL) as WGPUFrontFace
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var cullMode: WGPUCullMode
            get() = mem.readUInt(20uL) as WGPUCullMode
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var unclippedDepth: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPrimitiveState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var topology: WGPUPrimitiveTopology
            get() = mem.readUInt(8uL) as WGPUPrimitiveTopology
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var stripIndexFormat: WGPUIndexFormat
            get() = mem.readUInt(12uL) as WGPUIndexFormat
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var frontFace: WGPUFrontFace
            get() = mem.readUInt(16uL) as WGPUFrontFace
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var cullMode: WGPUCullMode
            get() = mem.readUInt(20uL) as WGPUCullMode
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var unclippedDepth: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUQuerySetDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var type: WGPUQueryType
    actual var count: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUQuerySetDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUQuerySetDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQuerySetDescriptor) -> Unit): ArrayHolder<WGPUQuerySetDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUQuerySetDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUQuerySetDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var type: WGPUQueryType
            get() = mem.readUInt(24uL) as WGPUQueryType
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var count: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUQuerySetDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var type: WGPUQueryType
            get() = mem.readUInt(24uL) as WGPUQueryType
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var count: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUQueueDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUQueueDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUQueueDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueDescriptor) -> Unit): ArrayHolder<WGPUQueueDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUQueueDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUQueueDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUQueueDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderBundleDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderBundleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleDescriptor) -> Unit): ArrayHolder<WGPURenderBundleDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPURenderBundleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderBundleEncoderResourceTable {
    actual var chain: WGPUChainedStruct
    actual var usesResourceTable: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderBundleEncoderResourceTable = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderResourceTable = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderResourceTable) -> Unit): ArrayHolder<WGPURenderBundleEncoderResourceTable> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPURenderBundleEncoderResourceTable>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleEncoderResourceTable {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var usesResourceTable: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleEncoderResourceTable {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var usesResourceTable: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassDepthStencilAttachment {
    actual var nextInChain: WGPUChainedStruct?
    actual var view: WGPUTextureView?
    actual var depthLoadOp: WGPULoadOp
    actual var depthStoreOp: WGPUStoreOp
    actual var depthClearValue: Float
    actual var depthReadOnly: UInt
    actual var stencilLoadOp: WGPULoadOp
    actual var stencilStoreOp: WGPUStoreOp
    actual var stencilClearValue: UInt
    actual var stencilReadOnly: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassDepthStencilAttachment = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDepthStencilAttachment = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDepthStencilAttachment) -> Unit): ArrayHolder<WGPURenderPassDepthStencilAttachment> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPURenderPassDepthStencilAttachment>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDepthStencilAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var view: WGPUTextureView?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var depthLoadOp: WGPULoadOp
            get() = mem.readUInt(16uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var depthStoreOp: WGPUStoreOp
            get() = mem.readUInt(20uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var depthClearValue: Float
            get() = mem.readFloat(24uL)
            set(value) { mem.writeFloat(value, 24uL) }
        override var depthReadOnly: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var stencilLoadOp: WGPULoadOp
            get() = mem.readUInt(32uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var stencilStoreOp: WGPUStoreOp
            get() = mem.readUInt(36uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var stencilClearValue: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var stencilReadOnly: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDepthStencilAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var view: WGPUTextureView?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var depthLoadOp: WGPULoadOp
            get() = mem.readUInt(16uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var depthStoreOp: WGPUStoreOp
            get() = mem.readUInt(20uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var depthClearValue: Float
            get() = mem.readFloat(24uL)
            set(value) { mem.writeFloat(value, 24uL) }
        override var depthReadOnly: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var stencilLoadOp: WGPULoadOp
            get() = mem.readUInt(32uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var stencilStoreOp: WGPUStoreOp
            get() = mem.readUInt(36uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var stencilClearValue: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var stencilReadOnly: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassDescriptorResolveRect {
    actual var chain: WGPUChainedStruct
    actual var colorOffsetX: UInt
    actual var colorOffsetY: UInt
    actual var resolveOffsetX: UInt
    actual var resolveOffsetY: UInt
    actual var width: UInt
    actual var height: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassDescriptorResolveRect = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptorResolveRect = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptorResolveRect) -> Unit): ArrayHolder<WGPURenderPassDescriptorResolveRect> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPURenderPassDescriptorResolveRect>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDescriptorResolveRect {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var colorOffsetX: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var colorOffsetY: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var resolveOffsetX: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var resolveOffsetY: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var width: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var height: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDescriptorResolveRect {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var colorOffsetX: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var colorOffsetY: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var resolveOffsetX: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var resolveOffsetY: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var width: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var height: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassMaxDrawCount {
    actual var chain: WGPUChainedStruct
    actual var maxDrawCount: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassMaxDrawCount = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassMaxDrawCount = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassMaxDrawCount) -> Unit): ArrayHolder<WGPURenderPassMaxDrawCount> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPURenderPassMaxDrawCount>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassMaxDrawCount {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var maxDrawCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassMaxDrawCount {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var maxDrawCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURequestAdapterWebGPUBackendOptions {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterWebGPUBackendOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebGPUBackendOptions = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebGPUBackendOptions) -> Unit): ArrayHolder<WGPURequestAdapterWebGPUBackendOptions> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPURequestAdapterWebGPUBackendOptions>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterWebGPUBackendOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterWebGPUBackendOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURequestAdapterWebXROptions {
    actual var chain: WGPUChainedStruct
    actual var xrCompatible: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterWebXROptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebXROptions = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebXROptions) -> Unit): ArrayHolder<WGPURequestAdapterWebXROptions> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPURequestAdapterWebXROptions>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterWebXROptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var xrCompatible: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterWebXROptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var xrCompatible: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUResourceTableDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var size: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUResourceTableDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUResourceTableDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUResourceTableDescriptor) -> Unit): ArrayHolder<WGPUResourceTableDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUResourceTableDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUResourceTableDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var size: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUResourceTableDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var size: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSamplerBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUSamplerBindingType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSamplerBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSamplerBindingLayout = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerBindingLayout) -> Unit): ArrayHolder<WGPUSamplerBindingLayout> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUSamplerBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSamplerBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUSamplerBindingType
            get() = mem.readUInt(8uL) as WGPUSamplerBindingType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSamplerBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUSamplerBindingType
            get() = mem.readUInt(8uL) as WGPUSamplerBindingType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUShaderModuleCompilationOptions {
    actual var chain: WGPUChainedStruct
    actual var strictMath: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderModuleCompilationOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderModuleCompilationOptions = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleCompilationOptions) -> Unit): ArrayHolder<WGPUShaderModuleCompilationOptions> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUShaderModuleCompilationOptions>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderModuleCompilationOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var strictMath: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderModuleCompilationOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var strictMath: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUShaderSourceSPIRV {
    actual var chain: WGPUChainedStruct
    actual var codeSize: UInt
    actual var code: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderSourceSPIRV = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderSourceSPIRV = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUShaderSourceSPIRV> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUShaderSourceSPIRV>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderSourceSPIRV {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var codeSize: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var code: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderSourceSPIRV {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var codeSize: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var code: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUShaderSourceWGSL {
    actual var chain: WGPUChainedStruct
    actual var code: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderSourceWGSL = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderSourceWGSL = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceWGSL) -> Unit): ArrayHolder<WGPUShaderSourceWGSL> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUShaderSourceWGSL>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderSourceWGSL {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var code: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderSourceWGSL {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var code: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryBeginAccessDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var initialized: UInt
    actual var fenceCount: ULong
    actual var fences: NativeAddress?
    actual var signaledValueCount: ULong
    actual var signaledValues: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryBeginAccessDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryBeginAccessDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryBeginAccessDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryBeginAccessDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryBeginAccessDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryBeginAccessDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryEndAccessState {
    actual var nextInChain: WGPUChainedStruct?
    actual var initialized: UInt
    actual var fenceCount: ULong
    actual var fences: NativeAddress?
    actual var signaledValueCount: ULong
    actual var signaledValues: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryEndAccessState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryEndAccessState = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedBufferMemoryEndAccessState> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryEndAccessState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryFromWindowsHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryFromWindowsHandleDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryFromWindowsHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryFromWindowsHandleDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryFromWindowsHandleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryHostPointerDescriptor {
    actual var chain: WGPUChainedStruct
    actual var pointer: NativeAddress?
    actual var size: ULong
    actual var disposeCallbackInfo: WGPUDisposeCallbackInfo
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryHostPointerDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryHostPointerDescriptor = ByReference(allocator.allocateBuffer(72uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryHostPointerDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryHostPointerDescriptor> {
            val buffer = allocator.allocateBuffer(72uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryHostPointerDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 72L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryHostPointerDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var pointer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var disposeCallbackInfo: WGPUDisposeCallbackInfo
            get() = WGPUDisposeCallbackInfo.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(40)
                MemoryBuffer(value.handler, 40uL).readBytes(bytes, 0u, 0uL, 40uL)
                mem.writeBytes(bytes, 0u, 32uL, 40uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryHostPointerDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var pointer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var size: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var disposeCallbackInfo: WGPUDisposeCallbackInfo
            get() = WGPUDisposeCallbackInfo.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(40)
                MemoryBuffer(value.handler, 40uL).readBytes(bytes, 0u, 0uL, 40uL)
                mem.writeBytes(bytes, 0u, 32uL, 40uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryProperties {
    actual var nextInChain: WGPUChainedStruct?
    actual var usage: ULong
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryProperties = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryProperties) -> Unit): ArrayHolder<WGPUSharedBufferMemoryProperties> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryProperties>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usage: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var size: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usage: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var size: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceDXGISharedHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceDXGISharedHandleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDXGISharedHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDXGISharedHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceDXGISharedHandleExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceDXGISharedHandleExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDXGISharedHandleExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDXGISharedHandleExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceEGLSyncDescriptor {
    actual var chain: WGPUChainedStruct
    actual var sync: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceEGLSyncDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceEGLSyncDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sync: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceEGLSyncDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sync: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceEGLSyncExportInfo {
    actual var chain: WGPUChainedStruct
    actual var sync: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceEGLSyncExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceEGLSyncExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sync: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceEGLSyncExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sync: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceMTLSharedEventDescriptor {
    actual var chain: WGPUChainedStruct
    actual var sharedEvent: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceMTLSharedEventDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceMTLSharedEventDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sharedEvent: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceMTLSharedEventDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sharedEvent: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceMTLSharedEventExportInfo {
    actual var chain: WGPUChainedStruct
    actual var sharedEvent: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceMTLSharedEventExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceMTLSharedEventExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sharedEvent: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceMTLSharedEventExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sharedEvent: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceSyncFDDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceSyncFDDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceSyncFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceSyncFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceSyncFDExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceSyncFDExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceSyncFDExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceSyncFDExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleExportInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryAHardwareBufferDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryAHardwareBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryAHardwareBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryD3D11BeginState {
    actual var chain: WGPUChainedStruct
    actual var requiresEndAccessFence: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3D11BeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3D11BeginState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3D11BeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3D11BeginState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryD3D11BeginState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryD3D11BeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var requiresEndAccessFence: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryD3D11BeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var requiresEndAccessFence: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryD3DSwapchainBeginState {
    actual var chain: WGPUChainedStruct
    actual var isSwapchain: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3DSwapchainBeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3DSwapchainBeginState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3DSwapchainBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3DSwapchainBeginState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryD3DSwapchainBeginState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryD3DSwapchainBeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var isSwapchain: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryD3DSwapchainBeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var isSwapchain: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryDmaBufPlane {
    actual var fd: Int
    actual var offset: ULong
    actual var stride: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDmaBufPlane = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufPlane = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufPlane) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufPlane> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryDmaBufPlane>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDmaBufPlane {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var fd: Int
            get() = mem.readInt(0uL)
            set(value) { mem.writeInt(value, 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var stride: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDmaBufPlane {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var fd: Int
            get() = mem.readInt(0uL)
            set(value) { mem.writeInt(value, 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var stride: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual var useKeyedMutex: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDXGISharedHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDXGISharedHandleDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDXGISharedHandleDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryDXGISharedHandleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var useKeyedMutex: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var handle_2: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var useKeyedMutex: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryEGLImageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var image: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryEGLImageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEGLImageDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEGLImageDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEGLImageDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryEGLImageDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryEGLImageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var image: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryEGLImageDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var image: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryIOSurfaceDescriptor {
    actual var chain: WGPUChainedStruct
    actual var ioSurface: NativeAddress?
    actual var allowStorageBinding: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryIOSurfaceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryIOSurfaceDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryIOSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryIOSurfaceDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryIOSurfaceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryIOSurfaceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var ioSurface: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var allowStorageBinding: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryIOSurfaceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var ioSurface: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var allowStorageBinding: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryOpaqueFDDescriptor {
    actual var chain: WGPUChainedStruct
    actual var vkImageCreateInfo: NativeAddress?
    actual var memoryFD: Int
    actual var memoryTypeIndex: UInt
    actual var allocationSize: ULong
    actual var dedicatedAllocation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryOpaqueFDDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryOpaqueFDDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryOpaqueFDDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryOpaqueFDDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryOpaqueFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var vkImageCreateInfo: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var memoryFD: Int
            get() = mem.readInt(24uL)
            set(value) { mem.writeInt(value, 24uL) }
        override var memoryTypeIndex: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var allocationSize: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var dedicatedAllocation: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryOpaqueFDDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var vkImageCreateInfo: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var memoryFD: Int
            get() = mem.readInt(24uL)
            set(value) { mem.writeInt(value, 24uL) }
        override var memoryTypeIndex: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var allocationSize: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var dedicatedAllocation: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
    actual var chain: WGPUChainedStruct
    actual var dedicatedAllocation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var dedicatedAllocation: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var dedicatedAllocation: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryVkImageLayoutBeginState {
    actual var chain: WGPUChainedStruct
    actual var oldLayout: Int
    actual var newLayout: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutBeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutBeginState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutBeginState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutBeginState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkImageLayoutBeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var oldLayout: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override var newLayout: Int
            get() = mem.readInt(20uL)
            set(value) { mem.writeInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkImageLayoutBeginState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var oldLayout: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override var newLayout: Int
            get() = mem.readInt(20uL)
            set(value) { mem.writeInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryVkImageLayoutEndState {
    actual var chain: WGPUChainedStruct
    actual var oldLayout: Int
    actual var newLayout: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutEndState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutEndState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutEndState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutEndState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutEndState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkImageLayoutEndState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var oldLayout: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override var newLayout: Int
            get() = mem.readInt(20uL)
            set(value) { mem.writeInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryVkImageLayoutEndState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var oldLayout: Int
            get() = mem.readInt(16uL)
            set(value) { mem.writeInt(value, 16uL) }
        override var newLayout: Int
            get() = mem.readInt(20uL)
            set(value) { mem.writeInt(value, 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryZirconHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var memoryFD: UInt
    actual var allocationSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryZirconHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryZirconHandleDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryZirconHandleDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryZirconHandleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryZirconHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var memoryFD: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var allocationSize: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryZirconHandleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var memoryFD: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var allocationSize: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUStaticSamplerBindingLayout {
    actual var chain: WGPUChainedStruct
    actual var sampler: WGPUSampler?
    actual var sampledTextureBinding: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStaticSamplerBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStaticSamplerBindingLayout = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStaticSamplerBindingLayout) -> Unit): ArrayHolder<WGPUStaticSamplerBindingLayout> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUStaticSamplerBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUStaticSamplerBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var sampledTextureBinding: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUStaticSamplerBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var sampledTextureBinding: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUStencilFaceState {
    actual var compare: WGPUCompareFunction
    actual var failOp: WGPUStencilOperation
    actual var depthFailOp: WGPUStencilOperation
    actual var passOp: WGPUStencilOperation
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStencilFaceState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStencilFaceState = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStencilFaceState) -> Unit): ArrayHolder<WGPUStencilFaceState> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUStencilFaceState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUStencilFaceState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var compare: WGPUCompareFunction
            get() = mem.readUInt(0uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var failOp: WGPUStencilOperation
            get() = mem.readUInt(4uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var depthFailOp: WGPUStencilOperation
            get() = mem.readUInt(8uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var passOp: WGPUStencilOperation
            get() = mem.readUInt(12uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUStencilFaceState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var compare: WGPUCompareFunction
            get() = mem.readUInt(0uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var failOp: WGPUStencilOperation
            get() = mem.readUInt(4uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var depthFailOp: WGPUStencilOperation
            get() = mem.readUInt(8uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var passOp: WGPUStencilOperation
            get() = mem.readUInt(12uL) as WGPUStencilOperation
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUStorageTextureBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var access: WGPUStorageTextureAccess
    actual var format: WGPUTextureFormat
    actual var viewDimension: WGPUTextureViewDimension
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStorageTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStorageTextureBindingLayout = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStorageTextureBindingLayout) -> Unit): ArrayHolder<WGPUStorageTextureBindingLayout> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUStorageTextureBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUStorageTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var access: WGPUStorageTextureAccess
            get() = mem.readUInt(8uL) as WGPUStorageTextureAccess
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(12uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var viewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(16uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUStorageTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var access: WGPUStorageTextureAccess
            get() = mem.readUInt(8uL) as WGPUStorageTextureAccess
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(12uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var viewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(16uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSubgroupMatrixConfig {
    actual var componentType: WGPUSubgroupMatrixComponentType
    actual var resultComponentType: WGPUSubgroupMatrixComponentType
    actual var M: UInt
    actual var N: UInt
    actual var K: UInt
    actual var minSubgroupSize: UInt
    actual var maxSubgroupSize: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSubgroupMatrixConfig = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSubgroupMatrixConfig = ByReference(allocator.allocateBuffer(28uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSubgroupMatrixConfig) -> Unit): ArrayHolder<WGPUSubgroupMatrixConfig> {
            val buffer = allocator.allocateBuffer(28uL * size)
            val result = ArrayHolder<WGPUSubgroupMatrixConfig>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 28L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSubgroupMatrixConfig {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 28uL) }
        override var componentType: WGPUSubgroupMatrixComponentType
            get() = mem.readUInt(0uL) as WGPUSubgroupMatrixComponentType
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var resultComponentType: WGPUSubgroupMatrixComponentType
            get() = mem.readUInt(4uL) as WGPUSubgroupMatrixComponentType
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var M: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var N: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var K: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var minSubgroupSize: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxSubgroupSize: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSubgroupMatrixConfig {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 28uL) }
        override var componentType: WGPUSubgroupMatrixComponentType
            get() = mem.readUInt(0uL) as WGPUSubgroupMatrixComponentType
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var resultComponentType: WGPUSubgroupMatrixComponentType
            get() = mem.readUInt(4uL) as WGPUSubgroupMatrixComponentType
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var M: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var N: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var K: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var minSubgroupSize: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxSubgroupSize: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSupportedFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedFeatures = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedFeatures) -> Unit): ArrayHolder<WGPUSupportedFeatures> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUSupportedFeatures>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSupportedInstanceFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedInstanceFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedInstanceFeatures = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedInstanceFeatures) -> Unit): ArrayHolder<WGPUSupportedInstanceFeatures> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUSupportedInstanceFeatures>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedInstanceFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedInstanceFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSupportedWGSLLanguageFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedWGSLLanguageFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedWGSLLanguageFeatures = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedWGSLLanguageFeatures) -> Unit): ArrayHolder<WGPUSupportedWGSLLanguageFeatures> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUSupportedWGSLLanguageFeatures>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedWGSLLanguageFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSupportedWGSLLanguageFeatures {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var featureCount: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var features: NativeAddress?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceCapabilities {
    actual var nextInChain: WGPUChainedStruct?
    actual var usages: ULong
    actual var formatCount: ULong
    actual var formats: NativeAddress?
    actual var presentModeCount: ULong
    actual var presentModes: NativeAddress?
    actual var alphaModeCount: ULong
    actual var alphaModes: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceCapabilities = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceCapabilities = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceCapabilities) -> Unit): ArrayHolder<WGPUSurfaceCapabilities> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUSurfaceCapabilities>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usages: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var formatCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var formats: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var presentModeCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var presentModes: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override var alphaModeCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var alphaModes: NativeAddress?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usages: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var formatCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var formats: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var presentModeCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var presentModes: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override var alphaModeCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var alphaModes: NativeAddress?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceColorManagement {
    actual var chain: WGPUChainedStruct
    actual var colorSpace: WGPUPredefinedColorSpace
    actual var toneMappingMode: WGPUToneMappingMode
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceColorManagement = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceColorManagement = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceColorManagement) -> Unit): ArrayHolder<WGPUSurfaceColorManagement> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceColorManagement>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceColorManagement {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var colorSpace: WGPUPredefinedColorSpace
            get() = mem.readUInt(16uL) as WGPUPredefinedColorSpace
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var toneMappingMode: WGPUToneMappingMode
            get() = mem.readUInt(20uL) as WGPUToneMappingMode
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceColorManagement {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var colorSpace: WGPUPredefinedColorSpace
            get() = mem.readUInt(16uL) as WGPUPredefinedColorSpace
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var toneMappingMode: WGPUToneMappingMode
            get() = mem.readUInt(20uL) as WGPUToneMappingMode
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceConfiguration {
    actual var nextInChain: WGPUChainedStruct?
    actual var device: WGPUDevice?
    actual var format: WGPUTextureFormat
    actual var usage: ULong
    actual var width: UInt
    actual var height: UInt
    actual var viewFormatCount: ULong
    actual var viewFormats: NativeAddress?
    actual var alphaMode: WGPUCompositeAlphaMode
    actual var presentMode: WGPUPresentMode
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceConfiguration = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceConfiguration = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceConfiguration) -> Unit): ArrayHolder<WGPUSurfaceConfiguration> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUSurfaceConfiguration>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceConfiguration {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var device: WGPUDevice?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUDevice(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(16uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var width: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var height: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var viewFormatCount: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var viewFormats: NativeAddress?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 48uL) }
        override var alphaMode: WGPUCompositeAlphaMode
            get() = mem.readUInt(56uL) as WGPUCompositeAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 56uL) }
        override var presentMode: WGPUPresentMode
            get() = mem.readUInt(60uL) as WGPUPresentMode
            set(value) { mem.writeUInt(value.toUInt(), 60uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceConfiguration {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var device: WGPUDevice?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUDevice(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(16uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var width: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var height: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var viewFormatCount: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var viewFormats: NativeAddress?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 48uL) }
        override var alphaMode: WGPUCompositeAlphaMode
            get() = mem.readUInt(56uL) as WGPUCompositeAlphaMode
            set(value) { mem.writeUInt(value.toUInt(), 56uL) }
        override var presentMode: WGPUPresentMode
            get() = mem.readUInt(60uL) as WGPUPresentMode
            set(value) { mem.writeUInt(value.toUInt(), 60uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceDescriptorFromWindowsCoreWindow {
    actual var chain: WGPUChainedStruct
    actual var coreWindow: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsCoreWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsCoreWindow = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsCoreWindow) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsCoreWindow> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceDescriptorFromWindowsCoreWindow>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsCoreWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var coreWindow: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsCoreWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var coreWindow: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
    actual var chain: WGPUChainedStruct
    actual var swapChainPanel: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swapChainPanel: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swapChainPanel: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
    actual var chain: WGPUChainedStruct
    actual var swapChainPanel: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swapChainPanel: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swapChainPanel: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceAndroidNativeWindow {
    actual var chain: WGPUChainedStruct
    actual var window: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceAndroidNativeWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceAndroidNativeWindow = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceAndroidNativeWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceAndroidNativeWindow> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceAndroidNativeWindow>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceAndroidNativeWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var window: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceAndroidNativeWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var window: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceMetalLayer {
    actual var chain: WGPUChainedStruct
    actual var layer: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceMetalLayer = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceMetalLayer = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceMetalLayer) -> Unit): ArrayHolder<WGPUSurfaceSourceMetalLayer> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceMetalLayer>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceMetalLayer {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var layer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceMetalLayer {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var layer: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceWaylandSurface {
    actual var chain: WGPUChainedStruct
    actual var display: NativeAddress?
    actual var surface: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWaylandSurface = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWaylandSurface = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWaylandSurface) -> Unit): ArrayHolder<WGPUSurfaceSourceWaylandSurface> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceWaylandSurface>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceWaylandSurface {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var display: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var surface: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceWaylandSurface {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var display: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var surface: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceWindowsHWND {
    actual var chain: WGPUChainedStruct
    actual var hinstance: NativeAddress?
    actual var hwnd: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWindowsHWND = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWindowsHWND = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWindowsHWND) -> Unit): ArrayHolder<WGPUSurfaceSourceWindowsHWND> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceWindowsHWND>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceWindowsHWND {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hinstance: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var hwnd: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceWindowsHWND {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var hinstance: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var hwnd: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceXCBWindow {
    actual var chain: WGPUChainedStruct
    actual var connection: NativeAddress?
    actual var window: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXCBWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXCBWindow = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXCBWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXCBWindow> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceXCBWindow>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceXCBWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var connection: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var window: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceXCBWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var connection: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var window: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceSourceXlibWindow {
    actual var chain: WGPUChainedStruct
    actual var display: NativeAddress?
    actual var window: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXlibWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXlibWindow = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXlibWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXlibWindow> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSurfaceSourceXlibWindow>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceXlibWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var display: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var window: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceSourceXlibWindow {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var display: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var window: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceTexture {
    actual var nextInChain: WGPUChainedStruct?
    actual var texture: WGPUTexture?
    actual var status: WGPUSurfaceGetCurrentTextureStatus
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceTexture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceTexture = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceTexture) -> Unit): ArrayHolder<WGPUSurfaceTexture> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceTexture>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceTexture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var texture: WGPUTexture?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var status: WGPUSurfaceGetCurrentTextureStatus
            get() = mem.readUInt(16uL) as WGPUSurfaceGetCurrentTextureStatus
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceTexture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var texture: WGPUTexture?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var status: WGPUSurfaceGetCurrentTextureStatus
            get() = mem.readUInt(16uL) as WGPUSurfaceGetCurrentTextureStatus
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelBufferBindingEntry {
    actual var chain: WGPUChainedStruct
    actual var texelBufferView: WGPUTexelBufferView?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingEntry = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingEntry) -> Unit): ArrayHolder<WGPUTexelBufferBindingEntry> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUTexelBufferBindingEntry>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferBindingEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var texelBufferView: WGPUTexelBufferView?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUTexelBufferView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferBindingEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var texelBufferView: WGPUTexelBufferView?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUTexelBufferView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelBufferBindingLayout {
    actual var chain: WGPUChainedStruct
    actual var access: WGPUTexelBufferAccess
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingLayout = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingLayout) -> Unit): ArrayHolder<WGPUTexelBufferBindingLayout> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUTexelBufferBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var access: WGPUTexelBufferAccess
            get() = mem.readUInt(16uL) as WGPUTexelBufferAccess
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(20uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var access: WGPUTexelBufferAccess
            get() = mem.readUInt(16uL) as WGPUTexelBufferAccess
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(20uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelBufferViewDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var format: WGPUTextureFormat
    actual var offset: ULong
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelBufferViewDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferViewDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferViewDescriptor) -> Unit): ArrayHolder<WGPUTexelBufferViewDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUTexelBufferViewDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferViewDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(24uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var offset: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var size: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelBufferViewDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(24uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var offset: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var size: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelCopyBufferLayout {
    actual var offset: ULong
    actual var bytesPerRow: UInt
    actual var rowsPerImage: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferLayout = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferLayout) -> Unit): ArrayHolder<WGPUTexelCopyBufferLayout> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUTexelCopyBufferLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyBufferLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var offset: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var bytesPerRow: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var rowsPerImage: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyBufferLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var offset: ULong
            get() = mem.readULong(0uL)
            set(value) { mem.writeULong(value, 0uL) }
        override var bytesPerRow: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var rowsPerImage: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var sampleType: WGPUTextureSampleType
    actual var viewDimension: WGPUTextureViewDimension
    actual var multisampled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureBindingLayout = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingLayout) -> Unit): ArrayHolder<WGPUTextureBindingLayout> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUTextureBindingLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var sampleType: WGPUTextureSampleType
            get() = mem.readUInt(8uL) as WGPUTextureSampleType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var viewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(12uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var multisampled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureBindingLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var sampleType: WGPUTextureSampleType
            get() = mem.readUInt(8uL) as WGPUTextureSampleType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var viewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(12uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var multisampled: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureBindingViewDimension {
    actual var chain: WGPUChainedStruct
    actual var textureBindingViewDimension: WGPUTextureViewDimension
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureBindingViewDimension = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureBindingViewDimension = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingViewDimension) -> Unit): ArrayHolder<WGPUTextureBindingViewDimension> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUTextureBindingViewDimension>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureBindingViewDimension {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var textureBindingViewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(16uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureBindingViewDimension {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var textureBindingViewDimension: WGPUTextureViewDimension
            get() = mem.readUInt(16uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureComponentSwizzle {
    actual var r: WGPUComponentSwizzle
    actual var g: WGPUComponentSwizzle
    actual var b: WGPUComponentSwizzle
    actual var a: WGPUComponentSwizzle
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzle = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzle = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzle) -> Unit): ArrayHolder<WGPUTextureComponentSwizzle> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUTextureComponentSwizzle>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureComponentSwizzle {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var r: WGPUComponentSwizzle
            get() = mem.readUInt(0uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var g: WGPUComponentSwizzle
            get() = mem.readUInt(4uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var b: WGPUComponentSwizzle
            get() = mem.readUInt(8uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var a: WGPUComponentSwizzle
            get() = mem.readUInt(12uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureComponentSwizzle {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var r: WGPUComponentSwizzle
            get() = mem.readUInt(0uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 0uL) }
        override var g: WGPUComponentSwizzle
            get() = mem.readUInt(4uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 4uL) }
        override var b: WGPUComponentSwizzle
            get() = mem.readUInt(8uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var a: WGPUComponentSwizzle
            get() = mem.readUInt(12uL) as WGPUComponentSwizzle
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUVertexAttribute {
    actual var nextInChain: WGPUChainedStruct?
    actual var format: WGPUVertexFormat
    actual var offset: ULong
    actual var shaderLocation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUVertexAttribute = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexAttribute = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexAttribute) -> Unit): ArrayHolder<WGPUVertexAttribute> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUVertexAttribute>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexAttribute {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUVertexFormat
            get() = mem.readUInt(8uL) as WGPUVertexFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var offset: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var shaderLocation: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexAttribute {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUVertexFormat
            get() = mem.readUInt(8uL) as WGPUVertexFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var offset: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var shaderLocation: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUYCbCrVkDescriptor {
    actual var chain: WGPUChainedStruct
    actual var vkFormat: UInt
    actual var vkYCbCrModel: UInt
    actual var vkYCbCrRange: UInt
    actual var vkComponentSwizzleRed: UInt
    actual var vkComponentSwizzleGreen: UInt
    actual var vkComponentSwizzleBlue: UInt
    actual var vkComponentSwizzleAlpha: UInt
    actual var vkXChromaOffset: UInt
    actual var vkYChromaOffset: UInt
    actual var vkChromaFilter: WGPUFilterMode
    actual var forceExplicitReconstruction: UInt
    actual var externalFormat: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUYCbCrVkDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUYCbCrVkDescriptor = ByReference(allocator.allocateBuffer(72uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUYCbCrVkDescriptor) -> Unit): ArrayHolder<WGPUYCbCrVkDescriptor> {
            val buffer = allocator.allocateBuffer(72uL * size)
            val result = ArrayHolder<WGPUYCbCrVkDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 72L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUYCbCrVkDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var vkFormat: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var vkYCbCrModel: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var vkYCbCrRange: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var vkComponentSwizzleRed: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var vkComponentSwizzleGreen: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var vkComponentSwizzleBlue: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var vkComponentSwizzleAlpha: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var vkXChromaOffset: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var vkYChromaOffset: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var vkChromaFilter: WGPUFilterMode
            get() = mem.readUInt(52uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 52uL) }
        override var forceExplicitReconstruction: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var externalFormat: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUYCbCrVkDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var vkFormat: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var vkYCbCrModel: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var vkYCbCrRange: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var vkComponentSwizzleRed: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var vkComponentSwizzleGreen: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var vkComponentSwizzleBlue: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var vkComponentSwizzleAlpha: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var vkXChromaOffset: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var vkYChromaOffset: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var vkChromaFilter: WGPUFilterMode
            get() = mem.readUInt(52uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 52uL) }
        override var forceExplicitReconstruction: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var externalFormat: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesMemoryHeaps {
    actual var chain: WGPUChainedStruct
    actual var heapCount: ULong
    actual var heapInfo: WGPUMemoryHeapInfo?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesMemoryHeaps = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesMemoryHeaps = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesMemoryHeaps) -> Unit): ArrayHolder<WGPUAdapterPropertiesMemoryHeaps> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesMemoryHeaps>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesMemoryHeaps {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var heapCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var heapInfo: WGPUMemoryHeapInfo?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUMemoryHeapInfo(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesMemoryHeaps {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var heapCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var heapInfo: WGPUMemoryHeapInfo?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUMemoryHeapInfo(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterPropertiesSubgroupMatrixConfigs {
    actual var chain: WGPUChainedStruct
    actual var configCount: ULong
    actual var configs: WGPUSubgroupMatrixConfig?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesSubgroupMatrixConfigs = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesSubgroupMatrixConfigs = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesSubgroupMatrixConfigs) -> Unit): ArrayHolder<WGPUAdapterPropertiesSubgroupMatrixConfigs> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUAdapterPropertiesSubgroupMatrixConfigs>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesSubgroupMatrixConfigs {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var configCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var configs: WGPUSubgroupMatrixConfig?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUSubgroupMatrixConfig(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterPropertiesSubgroupMatrixConfigs {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var configCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var configs: WGPUSubgroupMatrixConfig?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUSubgroupMatrixConfig(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAHardwareBufferProperties {
    actual var yCbCrInfo: WGPUYCbCrVkDescriptor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAHardwareBufferProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAHardwareBufferProperties = ByReference(allocator.allocateBuffer(72uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUAHardwareBufferProperties> {
            val buffer = allocator.allocateBuffer(72uL * size)
            val result = ArrayHolder<WGPUAHardwareBufferProperties>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 72L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAHardwareBufferProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(72)
                MemoryBuffer(value.handler, 72uL).readBytes(bytes, 0u, 0uL, 72uL)
                mem.writeBytes(bytes, 0u, 0uL, 72uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAHardwareBufferProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(72)
                MemoryBuffer(value.handler, 72uL).readBytes(bytes, 0u, 0uL, 72uL)
                mem.writeBytes(bytes, 0u, 0uL, 72uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBindGroupEntry {
    actual var nextInChain: WGPUChainedStruct?
    actual var binding: UInt
    actual var buffer: WGPUBuffer?
    actual var offset: ULong
    actual var size: ULong
    actual var sampler: WGPUSampler?
    actual var textureView: WGPUTextureView?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindGroupEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupEntry = ByReference(allocator.allocateBuffer(56uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupEntry) -> Unit): ArrayHolder<WGPUBindGroupEntry> {
            val buffer = allocator.allocateBuffer(56uL * size)
            val result = ArrayHolder<WGPUBindGroupEntry>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 56L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var binding: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var offset: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var size: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var textureView: WGPUTextureView?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var binding: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var offset: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var size: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var sampler: WGPUSampler?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUSampler(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var textureView: WGPUTextureView?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBindGroupLayoutEntry {
    actual var nextInChain: WGPUChainedStruct?
    actual var binding: UInt
    actual var visibility: ULong
    actual var bindingArraySize: UInt
    actual var buffer: WGPUBufferBindingLayout
    actual var sampler: WGPUSamplerBindingLayout
    actual var texture: WGPUTextureBindingLayout
    actual var storageTexture: WGPUStorageTextureBindingLayout
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindGroupLayoutEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutEntry = ByReference(allocator.allocateBuffer(120uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutEntry) -> Unit): ArrayHolder<WGPUBindGroupLayoutEntry> {
            val buffer = allocator.allocateBuffer(120uL * size)
            val result = ArrayHolder<WGPUBindGroupLayoutEntry>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 120L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupLayoutEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 120uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var binding: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var visibility: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var bindingArraySize: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var buffer: WGPUBufferBindingLayout
            get() = WGPUBufferBindingLayout.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 32uL, 24uL)
            }
        override var sampler: WGPUSamplerBindingLayout
            get() = WGPUSamplerBindingLayout.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 56uL, 16uL)
            }
        override var texture: WGPUTextureBindingLayout
            get() = WGPUTextureBindingLayout.ByValue(NativeAddress(handle.rawValue + 72L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 72uL, 24uL)
            }
        override var storageTexture: WGPUStorageTextureBindingLayout
            get() = WGPUStorageTextureBindingLayout.ByValue(NativeAddress(handle.rawValue + 96L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 96uL, 24uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupLayoutEntry {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 120uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var binding: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var visibility: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var bindingArraySize: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var buffer: WGPUBufferBindingLayout
            get() = WGPUBufferBindingLayout.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 32uL, 24uL)
            }
        override var sampler: WGPUSamplerBindingLayout
            get() = WGPUSamplerBindingLayout.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 56uL, 16uL)
            }
        override var texture: WGPUTextureBindingLayout
            get() = WGPUTextureBindingLayout.ByValue(NativeAddress(handle.rawValue + 72L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 72uL, 24uL)
            }
        override var storageTexture: WGPUStorageTextureBindingLayout
            get() = WGPUStorageTextureBindingLayout.ByValue(NativeAddress(handle.rawValue + 96L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 96uL, 24uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBlendState {
    actual var color: WGPUBlendComponent
    actual var alpha: WGPUBlendComponent
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBlendState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBlendState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendState) -> Unit): ArrayHolder<WGPUBlendState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUBlendState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBlendState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var color: WGPUBlendComponent
            get() = WGPUBlendComponent.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 0uL, 12uL)
            }
        override var alpha: WGPUBlendComponent
            get() = WGPUBlendComponent.ByValue(NativeAddress(handle.rawValue + 12L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 12uL, 12uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBlendState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var color: WGPUBlendComponent
            get() = WGPUBlendComponent.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 0uL, 12uL)
            }
        override var alpha: WGPUBlendComponent
            get() = WGPUBlendComponent.ByValue(NativeAddress(handle.rawValue + 12L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 12uL, 12uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBufferDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var usage: ULong
    actual var size: ULong
    actual var mappedAtCreation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferDescriptor) -> Unit): ArrayHolder<WGPUBufferDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUBufferDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var size: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var mappedAtCreation: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBufferDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var size: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var mappedAtCreation: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCommandEncoderDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCommandEncoderDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCommandEncoderDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandEncoderDescriptor) -> Unit): ArrayHolder<WGPUCommandEncoderDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUCommandEncoderDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCommandEncoderDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCommandEncoderDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCompilationMessage {
    actual var nextInChain: WGPUChainedStruct?
    actual var message: WGPUStringView
    actual var type: WGPUCompilationMessageType
    actual var lineNum: ULong
    actual var linePos: ULong
    actual var offset: ULong
    actual var length: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCompilationMessage = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationMessage = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationMessage) -> Unit): ArrayHolder<WGPUCompilationMessage> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUCompilationMessage>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationMessage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var message: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var type: WGPUCompilationMessageType
            get() = mem.readUInt(24uL) as WGPUCompilationMessageType
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var lineNum: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var linePos: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var offset: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var length: ULong
            get() = mem.readULong(56uL)
            set(value) { mem.writeULong(value, 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationMessage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var message: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var type: WGPUCompilationMessageType
            get() = mem.readUInt(24uL) as WGPUCompilationMessageType
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var lineNum: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var linePos: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var offset: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var length: ULong
            get() = mem.readULong(56uL)
            set(value) { mem.writeULong(value, 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUComputePassDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var timestampWrites: WGPUPassTimestampWrites?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUComputePassDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUComputePassDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePassDescriptor) -> Unit): ArrayHolder<WGPUComputePassDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUComputePassDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputePassDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPassTimestampWrites(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputePassDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPassTimestampWrites(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUComputeState {
    actual var nextInChain: WGPUChainedStruct?
    actual var module: WGPUShaderModule?
    actual var entryPoint: WGPUStringView
    actual var constantCount: ULong
    actual var constants: WGPUConstantEntry?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUComputeState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUComputeState = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputeState) -> Unit): ArrayHolder<WGPUComputeState> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUComputeState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputeState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputeState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnDrmFormatCapabilities {
    actual var chain: WGPUChainedStruct
    actual var propertiesCount: ULong
    actual var properties: WGPUDawnDrmFormatProperties?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatCapabilities = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatCapabilities = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnDrmFormatCapabilities> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUDawnDrmFormatCapabilities>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDrmFormatCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var propertiesCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var properties: WGPUDawnDrmFormatProperties?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUDawnDrmFormatProperties(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnDrmFormatCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var propertiesCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var properties: WGPUDawnDrmFormatProperties?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUDawnDrmFormatProperties(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDepthStencilState {
    actual var nextInChain: WGPUChainedStruct?
    actual var format: WGPUTextureFormat
    actual var depthWriteEnabled: WGPUOptionalBool
    actual var depthCompare: WGPUCompareFunction
    actual var stencilFront: WGPUStencilFaceState
    actual var stencilBack: WGPUStencilFaceState
    actual var stencilReadMask: UInt
    actual var stencilWriteMask: UInt
    actual var depthBias: Int
    actual var depthBiasSlopeScale: Float
    actual var depthBiasClamp: Float
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDepthStencilState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDepthStencilState = ByReference(allocator.allocateBuffer(72uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDepthStencilState) -> Unit): ArrayHolder<WGPUDepthStencilState> {
            val buffer = allocator.allocateBuffer(72uL * size)
            val result = ArrayHolder<WGPUDepthStencilState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 72L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDepthStencilState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(8uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var depthWriteEnabled: WGPUOptionalBool
            get() = mem.readUInt(12uL) as WGPUOptionalBool
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var depthCompare: WGPUCompareFunction
            get() = mem.readUInt(16uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var stencilFront: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByValue(NativeAddress(handle.rawValue + 20L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 20uL, 16uL)
            }
        override var stencilBack: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByValue(NativeAddress(handle.rawValue + 36L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 36uL, 16uL)
            }
        override var stencilReadMask: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var stencilWriteMask: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var depthBias: Int
            get() = mem.readInt(60uL)
            set(value) { mem.writeInt(value, 60uL) }
        override var depthBiasSlopeScale: Float
            get() = mem.readFloat(64uL)
            set(value) { mem.writeFloat(value, 64uL) }
        override var depthBiasClamp: Float
            get() = mem.readFloat(68uL)
            set(value) { mem.writeFloat(value, 68uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDepthStencilState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(8uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var depthWriteEnabled: WGPUOptionalBool
            get() = mem.readUInt(12uL) as WGPUOptionalBool
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var depthCompare: WGPUCompareFunction
            get() = mem.readUInt(16uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 16uL) }
        override var stencilFront: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByValue(NativeAddress(handle.rawValue + 20L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 20uL, 16uL)
            }
        override var stencilBack: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByValue(NativeAddress(handle.rawValue + 36L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 36uL, 16uL)
            }
        override var stencilReadMask: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var stencilWriteMask: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var depthBias: Int
            get() = mem.readInt(60uL)
            set(value) { mem.writeInt(value, 60uL) }
        override var depthBiasSlopeScale: Float
            get() = mem.readFloat(64uL)
            set(value) { mem.writeFloat(value, 64uL) }
        override var depthBiasClamp: Float
            get() = mem.readFloat(68uL)
            set(value) { mem.writeFloat(value, 68uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUExternalTextureDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var plane0: WGPUTextureView?
    actual var plane1: WGPUTextureView?
    actual var cropOrigin: WGPUOrigin2D
    actual var cropSize: WGPUExtent2D
    actual var apparentSize: WGPUExtent2D
    actual var doYuvToRgbConversionOnly: UInt
    actual var yuvToRgbConversionMatrix: NativeAddress?
    actual var srcTransferFunctionParameters: NativeAddress?
    actual var dstTransferFunctionParameters: NativeAddress?
    actual var gamutConversionMatrix: NativeAddress?
    actual var mirrored: UInt
    actual var rotation: WGPUExternalTextureRotation
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExternalTextureDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureDescriptor = ByReference(allocator.allocateBuffer(112uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureDescriptor) -> Unit): ArrayHolder<WGPUExternalTextureDescriptor> {
            val buffer = allocator.allocateBuffer(112uL * size)
            val result = ArrayHolder<WGPUExternalTextureDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 112L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 112uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var plane0: WGPUTextureView?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var plane1: WGPUTextureView?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var cropOrigin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 40uL, 8uL)
            }
        override var cropSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 48L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 48uL, 8uL)
            }
        override var apparentSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 56uL, 8uL)
            }
        override var doYuvToRgbConversionOnly: UInt
            get() = mem.readUInt(64uL)
            set(value) { mem.writeUInt(value, 64uL) }
        override var yuvToRgbConversionMatrix: NativeAddress?
            get() = mem.readPointer(72uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 72uL) }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(80uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 80uL) }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(88uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 88uL) }
        override var gamutConversionMatrix: NativeAddress?
            get() = mem.readPointer(96uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 96uL) }
        override var mirrored: UInt
            get() = mem.readUInt(104uL)
            set(value) { mem.writeUInt(value, 104uL) }
        override var rotation: WGPUExternalTextureRotation
            get() = mem.readUInt(108uL) as WGPUExternalTextureRotation
            set(value) { mem.writeUInt(value.toUInt(), 108uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUExternalTextureDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 112uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var plane0: WGPUTextureView?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var plane1: WGPUTextureView?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var cropOrigin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 40uL, 8uL)
            }
        override var cropSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 48L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 48uL, 8uL)
            }
        override var apparentSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 56uL, 8uL)
            }
        override var doYuvToRgbConversionOnly: UInt
            get() = mem.readUInt(64uL)
            set(value) { mem.writeUInt(value, 64uL) }
        override var yuvToRgbConversionMatrix: NativeAddress?
            get() = mem.readPointer(72uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 72uL) }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(80uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 80uL) }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = mem.readPointer(88uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 88uL) }
        override var gamutConversionMatrix: NativeAddress?
            get() = mem.readPointer(96uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 96uL) }
        override var mirrored: UInt
            get() = mem.readUInt(104uL)
            set(value) { mem.writeUInt(value, 104uL) }
        override var rotation: WGPUExternalTextureRotation
            get() = mem.readUInt(108uL) as WGPUExternalTextureRotation
            set(value) { mem.writeUInt(value.toUInt(), 108uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUFutureWaitInfo {
    actual var future: WGPUFuture
    actual var completed: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUFutureWaitInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUFutureWaitInfo = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFutureWaitInfo) -> Unit): ArrayHolder<WGPUFutureWaitInfo> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUFutureWaitInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUFutureWaitInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var future: WGPUFuture
            get() = WGPUFuture.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 0uL, 8uL)
            }
        override var completed: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUFutureWaitInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var future: WGPUFuture
            get() = WGPUFuture.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 0uL, 8uL)
            }
        override var completed: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUImageCopyExternalTexture {
    actual var nextInChain: WGPUChainedStruct?
    actual var externalTexture: WGPUExternalTexture?
    actual var origin: WGPUOrigin3D
    actual var naturalSize: WGPUExtent2D
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUImageCopyExternalTexture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUImageCopyExternalTexture = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUImageCopyExternalTexture) -> Unit): ArrayHolder<WGPUImageCopyExternalTexture> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUImageCopyExternalTexture>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUImageCopyExternalTexture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var externalTexture: WGPUExternalTexture?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUExternalTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var naturalSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 28L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 28uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUImageCopyExternalTexture {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var externalTexture: WGPUExternalTexture?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUExternalTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var naturalSize: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 28L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 28uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUInstanceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var requiredFeatureCount: ULong
    actual var requiredFeatures: NativeAddress?
    actual var requiredLimits: WGPUInstanceLimits?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUInstanceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUInstanceDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceDescriptor) -> Unit): ArrayHolder<WGPUInstanceDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUInstanceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUInstanceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var requiredFeatureCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var requiredFeatures: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var requiredLimits: WGPUInstanceLimits?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUInstanceLimits(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUInstanceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var requiredFeatureCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var requiredFeatures: NativeAddress?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 16uL) }
        override var requiredLimits: WGPUInstanceLimits?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUInstanceLimits(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPULimits {
    actual var nextInChain: WGPUChainedStruct?
    actual var maxTextureDimension1D: UInt
    actual var maxTextureDimension2D: UInt
    actual var maxTextureDimension3D: UInt
    actual var maxTextureArrayLayers: UInt
    actual var maxBindGroups: UInt
    actual var maxBindGroupsPlusVertexBuffers: UInt
    actual var maxBindingsPerBindGroup: UInt
    actual var maxDynamicUniformBuffersPerPipelineLayout: UInt
    actual var maxDynamicStorageBuffersPerPipelineLayout: UInt
    actual var maxSampledTexturesPerShaderStage: UInt
    actual var maxSamplersPerShaderStage: UInt
    actual var maxStorageBuffersPerShaderStage: UInt
    actual var maxStorageTexturesPerShaderStage: UInt
    actual var maxUniformBuffersPerShaderStage: UInt
    actual var maxUniformBufferBindingSize: ULong
    actual var maxStorageBufferBindingSize: ULong
    actual var minUniformBufferOffsetAlignment: UInt
    actual var minStorageBufferOffsetAlignment: UInt
    actual var maxVertexBuffers: UInt
    actual var maxBufferSize: ULong
    actual var maxVertexAttributes: UInt
    actual var maxVertexBufferArrayStride: UInt
    actual var maxInterStageShaderVariables: UInt
    actual var maxColorAttachments: UInt
    actual var maxColorAttachmentBytesPerSample: UInt
    actual var maxComputeWorkgroupStorageSize: UInt
    actual var maxComputeInvocationsPerWorkgroup: UInt
    actual var maxComputeWorkgroupSizeX: UInt
    actual var maxComputeWorkgroupSizeY: UInt
    actual var maxComputeWorkgroupSizeZ: UInt
    actual var maxComputeWorkgroupsPerDimension: UInt
    actual var maxImmediateSize: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPULimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPULimits = ByReference(allocator.allocateBuffer(152uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULimits) -> Unit): ArrayHolder<WGPULimits> {
            val buffer = allocator.allocateBuffer(152uL * size)
            val result = ArrayHolder<WGPULimits>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 152L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPULimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 152uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var maxTextureDimension1D: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var maxTextureDimension2D: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var maxTextureDimension3D: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var maxTextureArrayLayers: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxBindGroups: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var maxBindGroupsPlusVertexBuffers: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var maxBindingsPerBindGroup: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var maxDynamicUniformBuffersPerPipelineLayout: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var maxDynamicStorageBuffersPerPipelineLayout: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var maxSampledTexturesPerShaderStage: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var maxSamplersPerShaderStage: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var maxStorageBuffersPerShaderStage: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var maxStorageTexturesPerShaderStage: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var maxUniformBuffersPerShaderStage: UInt
            get() = mem.readUInt(60uL)
            set(value) { mem.writeUInt(value, 60uL) }
        override var maxUniformBufferBindingSize: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override var maxStorageBufferBindingSize: ULong
            get() = mem.readULong(72uL)
            set(value) { mem.writeULong(value, 72uL) }
        override var minUniformBufferOffsetAlignment: UInt
            get() = mem.readUInt(80uL)
            set(value) { mem.writeUInt(value, 80uL) }
        override var minStorageBufferOffsetAlignment: UInt
            get() = mem.readUInt(84uL)
            set(value) { mem.writeUInt(value, 84uL) }
        override var maxVertexBuffers: UInt
            get() = mem.readUInt(88uL)
            set(value) { mem.writeUInt(value, 88uL) }
        override var maxBufferSize: ULong
            get() = mem.readULong(96uL)
            set(value) { mem.writeULong(value, 96uL) }
        override var maxVertexAttributes: UInt
            get() = mem.readUInt(104uL)
            set(value) { mem.writeUInt(value, 104uL) }
        override var maxVertexBufferArrayStride: UInt
            get() = mem.readUInt(108uL)
            set(value) { mem.writeUInt(value, 108uL) }
        override var maxInterStageShaderVariables: UInt
            get() = mem.readUInt(112uL)
            set(value) { mem.writeUInt(value, 112uL) }
        override var maxColorAttachments: UInt
            get() = mem.readUInt(116uL)
            set(value) { mem.writeUInt(value, 116uL) }
        override var maxColorAttachmentBytesPerSample: UInt
            get() = mem.readUInt(120uL)
            set(value) { mem.writeUInt(value, 120uL) }
        override var maxComputeWorkgroupStorageSize: UInt
            get() = mem.readUInt(124uL)
            set(value) { mem.writeUInt(value, 124uL) }
        override var maxComputeInvocationsPerWorkgroup: UInt
            get() = mem.readUInt(128uL)
            set(value) { mem.writeUInt(value, 128uL) }
        override var maxComputeWorkgroupSizeX: UInt
            get() = mem.readUInt(132uL)
            set(value) { mem.writeUInt(value, 132uL) }
        override var maxComputeWorkgroupSizeY: UInt
            get() = mem.readUInt(136uL)
            set(value) { mem.writeUInt(value, 136uL) }
        override var maxComputeWorkgroupSizeZ: UInt
            get() = mem.readUInt(140uL)
            set(value) { mem.writeUInt(value, 140uL) }
        override var maxComputeWorkgroupsPerDimension: UInt
            get() = mem.readUInt(144uL)
            set(value) { mem.writeUInt(value, 144uL) }
        override var maxImmediateSize: UInt
            get() = mem.readUInt(148uL)
            set(value) { mem.writeUInt(value, 148uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPULimits {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 152uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var maxTextureDimension1D: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var maxTextureDimension2D: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var maxTextureDimension3D: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var maxTextureArrayLayers: UInt
            get() = mem.readUInt(20uL)
            set(value) { mem.writeUInt(value, 20uL) }
        override var maxBindGroups: UInt
            get() = mem.readUInt(24uL)
            set(value) { mem.writeUInt(value, 24uL) }
        override var maxBindGroupsPlusVertexBuffers: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var maxBindingsPerBindGroup: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var maxDynamicUniformBuffersPerPipelineLayout: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var maxDynamicStorageBuffersPerPipelineLayout: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var maxSampledTexturesPerShaderStage: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var maxSamplersPerShaderStage: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var maxStorageBuffersPerShaderStage: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var maxStorageTexturesPerShaderStage: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var maxUniformBuffersPerShaderStage: UInt
            get() = mem.readUInt(60uL)
            set(value) { mem.writeUInt(value, 60uL) }
        override var maxUniformBufferBindingSize: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override var maxStorageBufferBindingSize: ULong
            get() = mem.readULong(72uL)
            set(value) { mem.writeULong(value, 72uL) }
        override var minUniformBufferOffsetAlignment: UInt
            get() = mem.readUInt(80uL)
            set(value) { mem.writeUInt(value, 80uL) }
        override var minStorageBufferOffsetAlignment: UInt
            get() = mem.readUInt(84uL)
            set(value) { mem.writeUInt(value, 84uL) }
        override var maxVertexBuffers: UInt
            get() = mem.readUInt(88uL)
            set(value) { mem.writeUInt(value, 88uL) }
        override var maxBufferSize: ULong
            get() = mem.readULong(96uL)
            set(value) { mem.writeULong(value, 96uL) }
        override var maxVertexAttributes: UInt
            get() = mem.readUInt(104uL)
            set(value) { mem.writeUInt(value, 104uL) }
        override var maxVertexBufferArrayStride: UInt
            get() = mem.readUInt(108uL)
            set(value) { mem.writeUInt(value, 108uL) }
        override var maxInterStageShaderVariables: UInt
            get() = mem.readUInt(112uL)
            set(value) { mem.writeUInt(value, 112uL) }
        override var maxColorAttachments: UInt
            get() = mem.readUInt(116uL)
            set(value) { mem.writeUInt(value, 116uL) }
        override var maxColorAttachmentBytesPerSample: UInt
            get() = mem.readUInt(120uL)
            set(value) { mem.writeUInt(value, 120uL) }
        override var maxComputeWorkgroupStorageSize: UInt
            get() = mem.readUInt(124uL)
            set(value) { mem.writeUInt(value, 124uL) }
        override var maxComputeInvocationsPerWorkgroup: UInt
            get() = mem.readUInt(128uL)
            set(value) { mem.writeUInt(value, 128uL) }
        override var maxComputeWorkgroupSizeX: UInt
            get() = mem.readUInt(132uL)
            set(value) { mem.writeUInt(value, 132uL) }
        override var maxComputeWorkgroupSizeY: UInt
            get() = mem.readUInt(136uL)
            set(value) { mem.writeUInt(value, 136uL) }
        override var maxComputeWorkgroupSizeZ: UInt
            get() = mem.readUInt(140uL)
            set(value) { mem.writeUInt(value, 140uL) }
        override var maxComputeWorkgroupsPerDimension: UInt
            get() = mem.readUInt(144uL)
            set(value) { mem.writeUInt(value, 144uL) }
        override var maxImmediateSize: UInt
            get() = mem.readUInt(148uL)
            set(value) { mem.writeUInt(value, 148uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPipelineLayoutPixelLocalStorage {
    actual var chain: WGPUChainedStruct
    actual var totalPixelLocalStorageSize: ULong
    actual var storageAttachmentCount: ULong
    actual var storageAttachments: WGPUPipelineLayoutStorageAttachment?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutPixelLocalStorage = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutPixelLocalStorage = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutPixelLocalStorage) -> Unit): ArrayHolder<WGPUPipelineLayoutPixelLocalStorage> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUPipelineLayoutPixelLocalStorage>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutPixelLocalStorage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var totalPixelLocalStorageSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var storageAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var storageAttachments: WGPUPipelineLayoutStorageAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayoutStorageAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutPixelLocalStorage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var totalPixelLocalStorageSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var storageAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var storageAttachments: WGPUPipelineLayoutStorageAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayoutStorageAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderBundleEncoderDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var colorFormatCount: ULong
    actual var colorFormats: NativeAddress?
    actual var depthStencilFormat: WGPUTextureFormat
    actual var sampleCount: UInt
    actual var depthReadOnly: UInt
    actual var stencilReadOnly: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderBundleEncoderDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderDescriptor = ByReference(allocator.allocateBuffer(56uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderDescriptor) -> Unit): ArrayHolder<WGPURenderBundleEncoderDescriptor> {
            val buffer = allocator.allocateBuffer(56uL * size)
            val result = ArrayHolder<WGPURenderBundleEncoderDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 56L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleEncoderDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var colorFormatCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var colorFormats: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var depthStencilFormat: WGPUTextureFormat
            get() = mem.readUInt(40uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 40uL) }
        override var sampleCount: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var depthReadOnly: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var stencilReadOnly: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderBundleEncoderDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var colorFormatCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var colorFormats: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var depthStencilFormat: WGPUTextureFormat
            get() = mem.readUInt(40uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 40uL) }
        override var sampleCount: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var depthReadOnly: UInt
            get() = mem.readUInt(48uL)
            set(value) { mem.writeUInt(value, 48uL) }
        override var stencilReadOnly: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassColorAttachment {
    actual var nextInChain: WGPUChainedStruct?
    actual var view: WGPUTextureView?
    actual var depthSlice: UInt
    actual var resolveTarget: WGPUTextureView?
    actual var loadOp: WGPULoadOp
    actual var storeOp: WGPUStoreOp
    actual var clearValue: WGPUColor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassColorAttachment = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassColorAttachment = ByReference(allocator.allocateBuffer(72uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassColorAttachment) -> Unit): ArrayHolder<WGPURenderPassColorAttachment> {
            val buffer = allocator.allocateBuffer(72uL * size)
            val result = ArrayHolder<WGPURenderPassColorAttachment>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 72L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassColorAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var view: WGPUTextureView?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var depthSlice: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var resolveTarget: WGPUTextureView?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var loadOp: WGPULoadOp
            get() = mem.readUInt(32uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var storeOp: WGPUStoreOp
            get() = mem.readUInt(36uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 40uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassColorAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 72uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var view: WGPUTextureView?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var depthSlice: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var resolveTarget: WGPUTextureView?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var loadOp: WGPULoadOp
            get() = mem.readUInt(32uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var storeOp: WGPUStoreOp
            get() = mem.readUInt(36uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 40uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassRenderAreaRect {
    actual var chain: WGPUChainedStruct
    actual var origin: WGPUOrigin2D
    actual var size: WGPUExtent2D
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassRenderAreaRect = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassRenderAreaRect = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassRenderAreaRect) -> Unit): ArrayHolder<WGPURenderPassRenderAreaRect> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPURenderPassRenderAreaRect>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassRenderAreaRect {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var origin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 16uL, 8uL)
            }
        override var size: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 24L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 24uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassRenderAreaRect {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var origin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 16uL, 8uL)
            }
        override var size: WGPUExtent2D
            get() = WGPUExtent2D.ByValue(NativeAddress(handle.rawValue + 24L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 24uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassStorageAttachment {
    actual var nextInChain: WGPUChainedStruct?
    actual var offset: ULong
    actual var storage: WGPUTextureView?
    actual var loadOp: WGPULoadOp
    actual var storeOp: WGPUStoreOp
    actual var clearValue: WGPUColor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassStorageAttachment = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassStorageAttachment = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassStorageAttachment) -> Unit): ArrayHolder<WGPURenderPassStorageAttachment> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPURenderPassStorageAttachment>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassStorageAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var storage: WGPUTextureView?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var loadOp: WGPULoadOp
            get() = mem.readUInt(24uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var storeOp: WGPUStoreOp
            get() = mem.readUInt(28uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 32uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassStorageAttachment {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var offset: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var storage: WGPUTextureView?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUTextureView(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var loadOp: WGPULoadOp
            get() = mem.readUInt(24uL) as WGPULoadOp
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var storeOp: WGPUStoreOp
            get() = mem.readUInt(28uL) as WGPUStoreOp
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 32uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURequestAdapterOptions {
    actual var nextInChain: WGPUChainedStruct?
    actual var featureLevel: WGPUFeatureLevel
    actual var powerPreference: WGPUPowerPreference
    actual var forceFallbackAdapter: UInt
    actual var backendType: WGPUBackendType
    actual var compatibleSurface: WGPUSurface?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterOptions = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterOptions) -> Unit): ArrayHolder<WGPURequestAdapterOptions> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPURequestAdapterOptions>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var featureLevel: WGPUFeatureLevel
            get() = mem.readUInt(8uL) as WGPUFeatureLevel
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var powerPreference: WGPUPowerPreference
            get() = mem.readUInt(12uL) as WGPUPowerPreference
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var forceFallbackAdapter: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(20uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var compatibleSurface: WGPUSurface?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUSurface(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURequestAdapterOptions {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var featureLevel: WGPUFeatureLevel
            get() = mem.readUInt(8uL) as WGPUFeatureLevel
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var powerPreference: WGPUPowerPreference
            get() = mem.readUInt(12uL) as WGPUPowerPreference
            set(value) { mem.writeUInt(value.toUInt(), 12uL) }
        override var forceFallbackAdapter: UInt
            get() = mem.readUInt(16uL)
            set(value) { mem.writeUInt(value, 16uL) }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(20uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 20uL) }
        override var compatibleSurface: WGPUSurface?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUSurface(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSamplerDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var addressModeU: WGPUAddressMode
    actual var addressModeV: WGPUAddressMode
    actual var addressModeW: WGPUAddressMode
    actual var magFilter: WGPUFilterMode
    actual var minFilter: WGPUFilterMode
    actual var mipmapFilter: WGPUMipmapFilterMode
    actual var lodMinClamp: Float
    actual var lodMaxClamp: Float
    actual var compare: WGPUCompareFunction
    actual var maxAnisotropy: UShort
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSamplerDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSamplerDescriptor = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerDescriptor) -> Unit): ArrayHolder<WGPUSamplerDescriptor> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUSamplerDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSamplerDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var addressModeU: WGPUAddressMode
            get() = mem.readUInt(24uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var addressModeV: WGPUAddressMode
            get() = mem.readUInt(28uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var addressModeW: WGPUAddressMode
            get() = mem.readUInt(32uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var magFilter: WGPUFilterMode
            get() = mem.readUInt(36uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var minFilter: WGPUFilterMode
            get() = mem.readUInt(40uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 40uL) }
        override var mipmapFilter: WGPUMipmapFilterMode
            get() = mem.readUInt(44uL) as WGPUMipmapFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 44uL) }
        override var lodMinClamp: Float
            get() = mem.readFloat(48uL)
            set(value) { mem.writeFloat(value, 48uL) }
        override var lodMaxClamp: Float
            get() = mem.readFloat(52uL)
            set(value) { mem.writeFloat(value, 52uL) }
        override var compare: WGPUCompareFunction
            get() = mem.readUInt(56uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 56uL) }
        override var maxAnisotropy: UShort
            get() = mem.readUShort(60uL)
            set(value) { mem.writeUShort(value, 60uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSamplerDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var addressModeU: WGPUAddressMode
            get() = mem.readUInt(24uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var addressModeV: WGPUAddressMode
            get() = mem.readUInt(28uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var addressModeW: WGPUAddressMode
            get() = mem.readUInt(32uL) as WGPUAddressMode
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var magFilter: WGPUFilterMode
            get() = mem.readUInt(36uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 36uL) }
        override var minFilter: WGPUFilterMode
            get() = mem.readUInt(40uL) as WGPUFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 40uL) }
        override var mipmapFilter: WGPUMipmapFilterMode
            get() = mem.readUInt(44uL) as WGPUMipmapFilterMode
            set(value) { mem.writeUInt(value.toUInt(), 44uL) }
        override var lodMinClamp: Float
            get() = mem.readFloat(48uL)
            set(value) { mem.writeFloat(value, 48uL) }
        override var lodMaxClamp: Float
            get() = mem.readFloat(52uL)
            set(value) { mem.writeFloat(value, 52uL) }
        override var compare: WGPUCompareFunction
            get() = mem.readUInt(56uL) as WGPUCompareFunction
            set(value) { mem.writeUInt(value.toUInt(), 56uL) }
        override var maxAnisotropy: UShort
            get() = mem.readUShort(60uL)
            set(value) { mem.writeUShort(value, 60uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUShaderModuleDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderModuleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderModuleDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleDescriptor) -> Unit): ArrayHolder<WGPUShaderModuleDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUShaderModuleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderModuleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUShaderModuleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedBufferMemoryDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedBufferMemoryDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedBufferMemoryDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedFenceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedFenceExportInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUSharedFenceType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceExportInfo = ByReference(allocator.allocateBuffer(16uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceExportInfo> {
            val buffer = allocator.allocateBuffer(16uL * size)
            val result = ArrayHolder<WGPUSharedFenceExportInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 16L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUSharedFenceType
            get() = mem.readUInt(8uL) as WGPUSharedFenceType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedFenceExportInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 16uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var type: WGPUSharedFenceType
            get() = mem.readUInt(8uL) as WGPUSharedFenceType
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryAHardwareBufferProperties {
    actual var chain: WGPUChainedStruct
    actual var yCbCrInfo: WGPUYCbCrVkDescriptor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferProperties = ByReference(allocator.allocateBuffer(88uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferProperties> {
            val buffer = allocator.allocateBuffer(88uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferProperties>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 88L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryAHardwareBufferProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 88uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(72)
                MemoryBuffer(value.handler, 72uL).readBytes(bytes, 0u, 0uL, 72uL)
                mem.writeBytes(bytes, 0u, 16uL, 72uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryAHardwareBufferProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 88uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(72)
                MemoryBuffer(value.handler, 72uL).readBytes(bytes, 0u, 0uL, 72uL)
                mem.writeBytes(bytes, 0u, 16uL, 72uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryBeginAccessDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var concurrentRead: UInt
    actual var initialized: UInt
    actual var fenceCount: ULong
    actual var fences: NativeAddress?
    actual var signaledValueCount: ULong
    actual var signaledValues: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryBeginAccessDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryBeginAccessDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryBeginAccessDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryBeginAccessDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryBeginAccessDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var concurrentRead: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var initialized: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryBeginAccessDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var concurrentRead: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var initialized: UInt
            get() = mem.readUInt(12uL)
            set(value) { mem.writeUInt(value, 12uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryDmaBufDescriptor {
    actual var chain: WGPUChainedStruct
    actual var size: WGPUExtent3D
    actual var drmFormat: UInt
    actual var drmModifier: ULong
    actual var planeCount: ULong
    actual var planes: WGPUSharedTextureMemoryDmaBufPlane?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDmaBufDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufDescriptor = ByReference(allocator.allocateBuffer(56uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufDescriptor> {
            val buffer = allocator.allocateBuffer(56uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryDmaBufDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 56L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDmaBufDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var drmFormat: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var drmModifier: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var planeCount: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var planes: WGPUSharedTextureMemoryDmaBufPlane?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUSharedTextureMemoryDmaBufPlane(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDmaBufDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 56uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var drmFormat: UInt
            get() = mem.readUInt(28uL)
            set(value) { mem.writeUInt(value, 28uL) }
        override var drmModifier: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var planeCount: ULong
            get() = mem.readULong(40uL)
            set(value) { mem.writeULong(value, 40uL) }
        override var planes: WGPUSharedTextureMemoryDmaBufPlane?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUSharedTextureMemoryDmaBufPlane(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryMetalEndAccessState {
    actual var chain: WGPUChainedStruct
    actual var commandsScheduledFuture: WGPUFuture
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryMetalEndAccessState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryMetalEndAccessState = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryMetalEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryMetalEndAccessState> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryMetalEndAccessState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryMetalEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var commandsScheduledFuture: WGPUFuture
            get() = WGPUFuture.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 16uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryMetalEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var commandsScheduledFuture: WGPUFuture
            get() = WGPUFuture.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(8)
                MemoryBuffer(value.handler, 8uL).readBytes(bytes, 0u, 0uL, 8uL)
                mem.writeBytes(bytes, 0u, 16uL, 8uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSurfaceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSurfaceDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSurfaceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSurfaceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelCopyBufferInfo {
    actual var layout: WGPUTexelCopyBufferLayout
    actual var buffer: WGPUBuffer?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferInfo) -> Unit): ArrayHolder<WGPUTexelCopyBufferInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUTexelCopyBufferInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyBufferInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var layout: WGPUTexelCopyBufferLayout
            get() = WGPUTexelCopyBufferLayout.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyBufferInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var layout: WGPUTexelCopyBufferLayout
            get() = WGPUTexelCopyBufferLayout.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var buffer: WGPUBuffer?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBuffer(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTexelCopyTextureInfo {
    actual var texture: WGPUTexture?
    actual var mipLevel: UInt
    actual var origin: WGPUOrigin3D
    actual var aspect: WGPUTextureAspect
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyTextureInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyTextureInfo = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyTextureInfo) -> Unit): ArrayHolder<WGPUTexelCopyTextureInfo> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUTexelCopyTextureInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyTextureInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var texture: WGPUTexture?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mipLevel: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByValue(NativeAddress(handle.rawValue + 12L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 12uL, 12uL)
            }
        override var aspect: WGPUTextureAspect
            get() = mem.readUInt(24uL) as WGPUTextureAspect
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTexelCopyTextureInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var texture: WGPUTexture?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUTexture(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var mipLevel: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByValue(NativeAddress(handle.rawValue + 12L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 12uL, 12uL)
            }
        override var aspect: WGPUTextureAspect
            get() = mem.readUInt(24uL) as WGPUTextureAspect
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureComponentSwizzleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var swizzle: WGPUTextureComponentSwizzle
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzleDescriptor = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzleDescriptor) -> Unit): ArrayHolder<WGPUTextureComponentSwizzleDescriptor> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUTextureComponentSwizzleDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureComponentSwizzleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swizzle: WGPUTextureComponentSwizzle
            get() = WGPUTextureComponentSwizzle.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureComponentSwizzleDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var swizzle: WGPUTextureComponentSwizzle
            get() = WGPUTextureComponentSwizzle.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var usage: ULong
    actual var dimension: WGPUTextureDimension
    actual var size: WGPUExtent3D
    actual var format: WGPUTextureFormat
    actual var mipLevelCount: UInt
    actual var sampleCount: UInt
    actual var viewFormatCount: ULong
    actual var viewFormats: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureDescriptor = ByReference(allocator.allocateBuffer(80uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureDescriptor) -> Unit): ArrayHolder<WGPUTextureDescriptor> {
            val buffer = allocator.allocateBuffer(80uL * size)
            val result = ArrayHolder<WGPUTextureDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 80L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 80uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var dimension: WGPUTextureDimension
            get() = mem.readUInt(32uL) as WGPUTextureDimension
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 36L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 36uL, 12uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(48uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var mipLevelCount: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var sampleCount: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var viewFormatCount: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override var viewFormats: NativeAddress?
            get() = mem.readPointer(72uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 72uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 80uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var usage: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var dimension: WGPUTextureDimension
            get() = mem.readUInt(32uL) as WGPUTextureDimension
            set(value) { mem.writeUInt(value.toUInt(), 32uL) }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 36L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 36uL, 12uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(48uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var mipLevelCount: UInt
            get() = mem.readUInt(52uL)
            set(value) { mem.writeUInt(value, 52uL) }
        override var sampleCount: UInt
            get() = mem.readUInt(56uL)
            set(value) { mem.writeUInt(value, 56uL) }
        override var viewFormatCount: ULong
            get() = mem.readULong(64uL)
            set(value) { mem.writeULong(value, 64uL) }
        override var viewFormats: NativeAddress?
            get() = mem.readPointer(72uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 72uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUVertexBufferLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var stepMode: WGPUVertexStepMode
    actual var arrayStride: ULong
    actual var attributeCount: ULong
    actual var attributes: WGPUVertexAttribute?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUVertexBufferLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexBufferLayout = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexBufferLayout) -> Unit): ArrayHolder<WGPUVertexBufferLayout> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUVertexBufferLayout>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexBufferLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var stepMode: WGPUVertexStepMode
            get() = mem.readUInt(8uL) as WGPUVertexStepMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var arrayStride: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var attributeCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var attributes: WGPUVertexAttribute?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUVertexAttribute(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexBufferLayout {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var stepMode: WGPUVertexStepMode
            get() = mem.readUInt(8uL) as WGPUVertexStepMode
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var arrayStride: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var attributeCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var attributes: WGPUVertexAttribute?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUVertexAttribute(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUAdapterInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var vendor: WGPUStringView
    actual var architecture: WGPUStringView
    actual var device: WGPUStringView
    actual var description: WGPUStringView
    actual var backendType: WGPUBackendType
    actual var adapterType: WGPUAdapterType
    actual var vendorID: UInt
    actual var deviceID: UInt
    actual var subgroupMinSize: UInt
    actual var subgroupMaxSize: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterInfo = ByReference(allocator.allocateBuffer(96uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterInfo) -> Unit): ArrayHolder<WGPUAdapterInfo> {
            val buffer = allocator.allocateBuffer(96uL * size)
            val result = ArrayHolder<WGPUAdapterInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 96L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 96uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var vendor: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var architecture: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 24L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 24uL, 16uL)
            }
        override var device: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 40uL, 16uL)
            }
        override var description: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 56uL, 16uL)
            }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(72uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 72uL) }
        override var adapterType: WGPUAdapterType
            get() = mem.readUInt(76uL) as WGPUAdapterType
            set(value) { mem.writeUInt(value.toUInt(), 76uL) }
        override var vendorID: UInt
            get() = mem.readUInt(80uL)
            set(value) { mem.writeUInt(value, 80uL) }
        override var deviceID: UInt
            get() = mem.readUInt(84uL)
            set(value) { mem.writeUInt(value, 84uL) }
        override var subgroupMinSize: UInt
            get() = mem.readUInt(88uL)
            set(value) { mem.writeUInt(value, 88uL) }
        override var subgroupMaxSize: UInt
            get() = mem.readUInt(92uL)
            set(value) { mem.writeUInt(value, 92uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUAdapterInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 96uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var vendor: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var architecture: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 24L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 24uL, 16uL)
            }
        override var device: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 40L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 40uL, 16uL)
            }
        override var description: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 56L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 56uL, 16uL)
            }
        override var backendType: WGPUBackendType
            get() = mem.readUInt(72uL) as WGPUBackendType
            set(value) { mem.writeUInt(value.toUInt(), 72uL) }
        override var adapterType: WGPUAdapterType
            get() = mem.readUInt(76uL) as WGPUAdapterType
            set(value) { mem.writeUInt(value.toUInt(), 76uL) }
        override var vendorID: UInt
            get() = mem.readUInt(80uL)
            set(value) { mem.writeUInt(value, 80uL) }
        override var deviceID: UInt
            get() = mem.readUInt(84uL)
            set(value) { mem.writeUInt(value, 84uL) }
        override var subgroupMinSize: UInt
            get() = mem.readUInt(88uL)
            set(value) { mem.writeUInt(value, 88uL) }
        override var subgroupMaxSize: UInt
            get() = mem.readUInt(92uL)
            set(value) { mem.writeUInt(value, 92uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBindGroupDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var layout: WGPUBindGroupLayout?
    actual var entryCount: ULong
    actual var entries: WGPUBindGroupEntry?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindGroupDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupDescriptor) -> Unit): ArrayHolder<WGPUBindGroupDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUBindGroupDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUBindGroupLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var entryCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var entries: WGPUBindGroupEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUBindGroupLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var entryCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var entries: WGPUBindGroupEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUBindGroupLayoutDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var entryCount: ULong
    actual var entries: WGPUBindGroupLayoutEntry?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindGroupLayoutDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutDescriptor = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutDescriptor) -> Unit): ArrayHolder<WGPUBindGroupLayoutDescriptor> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPUBindGroupLayoutDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupLayoutDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var entryCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var entries: WGPUBindGroupLayoutEntry?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupLayoutEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUBindGroupLayoutDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var entryCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var entries: WGPUBindGroupLayoutEntry?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPUBindGroupLayoutEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUColorTargetState {
    actual var nextInChain: WGPUChainedStruct?
    actual var format: WGPUTextureFormat
    actual var blend: WGPUBlendState?
    actual var writeMask: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColorTargetState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColorTargetState = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetState) -> Unit): ArrayHolder<WGPUColorTargetState> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUColorTargetState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorTargetState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(8uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var blend: WGPUBlendState?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBlendState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var writeMask: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUColorTargetState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(8uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 8uL) }
        override var blend: WGPUBlendState?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUBlendState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override var writeMask: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUCompilationInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var messageCount: ULong
    actual var messages: WGPUCompilationMessage?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCompilationInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationInfo = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfo) -> Unit): ArrayHolder<WGPUCompilationInfo> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUCompilationInfo>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var messageCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var messages: WGPUCompilationMessage?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUCompilationMessage(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUCompilationInfo {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var messageCount: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var messages: WGPUCompilationMessage?
            get() = mem.readPointer(16uL).takeIf { it.rawValue != 0L }?.let { WGPUCompilationMessage(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 16uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUComputePipelineDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var layout: WGPUPipelineLayout?
    actual var compute: WGPUComputeState
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUComputePipelineDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUComputePipelineDescriptor = ByReference(allocator.allocateBuffer(80uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePipelineDescriptor) -> Unit): ArrayHolder<WGPUComputePipelineDescriptor> {
            val buffer = allocator.allocateBuffer(80uL * size)
            val result = ArrayHolder<WGPUComputePipelineDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 80L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputePipelineDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 80uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUPipelineLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var compute: WGPUComputeState
            get() = WGPUComputeState.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(48)
                MemoryBuffer(value.handler, 48uL).readBytes(bytes, 0u, 0uL, 48uL)
                mem.writeBytes(bytes, 0u, 32uL, 48uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUComputePipelineDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 80uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUPipelineLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var compute: WGPUComputeState
            get() = WGPUComputeState.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(48)
                MemoryBuffer(value.handler, 48uL).readBytes(bytes, 0u, 0uL, 48uL)
                mem.writeBytes(bytes, 0u, 32uL, 48uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDawnFormatCapabilities {
    actual var nextInChain: WGPUChainedStruct?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFormatCapabilities = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFormatCapabilities = ByReference(allocator.allocateBuffer(8uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnFormatCapabilities> {
            val buffer = allocator.allocateBuffer(8uL * size)
            val result = ArrayHolder<WGPUDawnFormatCapabilities>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 8L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFormatCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDawnFormatCapabilities {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 8uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUDeviceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var requiredFeatureCount: ULong
    actual var requiredFeatures: NativeAddress?
    actual var requiredLimits: WGPULimits?
    actual var defaultQueue: WGPUQueueDescriptor
    actual var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
    actual var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDeviceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDeviceDescriptor = ByReference(allocator.allocateBuffer(144uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceDescriptor) -> Unit): ArrayHolder<WGPUDeviceDescriptor> {
            val buffer = allocator.allocateBuffer(144uL * size)
            val result = ArrayHolder<WGPUDeviceDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 144L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUDeviceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 144uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var requiredFeatureCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var requiredFeatures: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var requiredLimits: WGPULimits?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPULimits(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var defaultQueue: WGPUQueueDescriptor
            get() = WGPUQueueDescriptor.ByValue(NativeAddress(handle.rawValue + 48L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 48uL, 24uL)
            }
        override var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
            get() = WGPUDeviceLostCallbackInfo.ByValue(NativeAddress(handle.rawValue + 72L))
            set(value) {
                val bytes = ByteArray(40)
                MemoryBuffer(value.handler, 40uL).readBytes(bytes, 0u, 0uL, 40uL)
                mem.writeBytes(bytes, 0u, 72uL, 40uL)
            }
        override var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
            get() = WGPUUncapturedErrorCallbackInfo.ByValue(NativeAddress(handle.rawValue + 112L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 112uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUDeviceDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 144uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var requiredFeatureCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var requiredFeatures: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var requiredLimits: WGPULimits?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPULimits(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var defaultQueue: WGPUQueueDescriptor
            get() = WGPUQueueDescriptor.ByValue(NativeAddress(handle.rawValue + 48L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 48uL, 24uL)
            }
        override var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
            get() = WGPUDeviceLostCallbackInfo.ByValue(NativeAddress(handle.rawValue + 72L))
            set(value) {
                val bytes = ByteArray(40)
                MemoryBuffer(value.handler, 40uL).readBytes(bytes, 0u, 0uL, 40uL)
                mem.writeBytes(bytes, 0u, 72uL, 40uL)
            }
        override var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
            get() = WGPUUncapturedErrorCallbackInfo.ByValue(NativeAddress(handle.rawValue + 112L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 112uL, 32uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUPipelineLayoutDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var bindGroupLayoutCount: ULong
    actual var bindGroupLayouts: NativeAddress?
    actual var immediateSize: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutDescriptor = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutDescriptor) -> Unit): ArrayHolder<WGPUPipelineLayoutDescriptor> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUPipelineLayoutDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var bindGroupLayoutCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var bindGroupLayouts: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var immediateSize: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUPipelineLayoutDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var bindGroupLayoutCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var bindGroupLayouts: NativeAddress?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 32uL) }
        override var immediateSize: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassPixelLocalStorage {
    actual var chain: WGPUChainedStruct
    actual var totalPixelLocalStorageSize: ULong
    actual var storageAttachmentCount: ULong
    actual var storageAttachments: WGPURenderPassStorageAttachment?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassPixelLocalStorage = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassPixelLocalStorage = ByReference(allocator.allocateBuffer(40uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassPixelLocalStorage) -> Unit): ArrayHolder<WGPURenderPassPixelLocalStorage> {
            val buffer = allocator.allocateBuffer(40uL * size)
            val result = ArrayHolder<WGPURenderPassPixelLocalStorage>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 40L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassPixelLocalStorage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var totalPixelLocalStorageSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var storageAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var storageAttachments: WGPURenderPassStorageAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassStorageAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassPixelLocalStorage {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 40uL) }
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByValue(NativeAddress(handle.rawValue + 0L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 0uL, 16uL)
            }
        override var totalPixelLocalStorageSize: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var storageAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var storageAttachments: WGPURenderPassStorageAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassStorageAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDescriptor = ByReference(allocator.allocateBuffer(24uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDescriptor> {
            val buffer = allocator.allocateBuffer(24uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 24L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 24uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryEndAccessState {
    actual var nextInChain: WGPUChainedStruct?
    actual var initialized: UInt
    actual var fenceCount: ULong
    actual var fences: NativeAddress?
    actual var signaledValueCount: ULong
    actual var signaledValues: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryEndAccessState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEndAccessState = ByReference(allocator.allocateBuffer(48uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEndAccessState> {
            val buffer = allocator.allocateBuffer(48uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryEndAccessState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 48L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryEndAccessState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 48uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var initialized: UInt
            get() = mem.readUInt(8uL)
            set(value) { mem.writeUInt(value, 8uL) }
        override var fenceCount: ULong
            get() = mem.readULong(16uL)
            set(value) { mem.writeULong(value, 16uL) }
        override var fences: NativeAddress?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 24uL) }
        override var signaledValueCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var signaledValues: NativeAddress?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }
            set(value) { mem.writePointer(value ?: NativeAddress(0L), 40uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUSharedTextureMemoryProperties {
    actual var nextInChain: WGPUChainedStruct?
    actual var usage: ULong
    actual var size: WGPUExtent3D
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryProperties = ByReference(allocator.allocateBuffer(32uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryProperties> {
            val buffer = allocator.allocateBuffer(32uL * size)
            val result = ArrayHolder<WGPUSharedTextureMemoryProperties>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 32L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usage: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(28uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUSharedTextureMemoryProperties {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 32uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var usage: ULong
            get() = mem.readULong(8uL)
            set(value) { mem.writeULong(value, 8uL) }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(12)
                MemoryBuffer(value.handler, 12uL).readBytes(bytes, 0u, 0uL, 12uL)
                mem.writeBytes(bytes, 0u, 16uL, 12uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(28uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUTextureViewDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var format: WGPUTextureFormat
    actual var dimension: WGPUTextureViewDimension
    actual var baseMipLevel: UInt
    actual var mipLevelCount: UInt
    actual var baseArrayLayer: UInt
    actual var arrayLayerCount: UInt
    actual var aspect: WGPUTextureAspect
    actual var usage: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureViewDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureViewDescriptor = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureViewDescriptor) -> Unit): ArrayHolder<WGPUTextureViewDescriptor> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUTextureViewDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureViewDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(24uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var dimension: WGPUTextureViewDimension
            get() = mem.readUInt(28uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var baseMipLevel: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var mipLevelCount: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var baseArrayLayer: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var arrayLayerCount: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var aspect: WGPUTextureAspect
            get() = mem.readUInt(48uL) as WGPUTextureAspect
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var usage: ULong
            get() = mem.readULong(56uL)
            set(value) { mem.writeULong(value, 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUTextureViewDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var format: WGPUTextureFormat
            get() = mem.readUInt(24uL) as WGPUTextureFormat
            set(value) { mem.writeUInt(value.toUInt(), 24uL) }
        override var dimension: WGPUTextureViewDimension
            get() = mem.readUInt(28uL) as WGPUTextureViewDimension
            set(value) { mem.writeUInt(value.toUInt(), 28uL) }
        override var baseMipLevel: UInt
            get() = mem.readUInt(32uL)
            set(value) { mem.writeUInt(value, 32uL) }
        override var mipLevelCount: UInt
            get() = mem.readUInt(36uL)
            set(value) { mem.writeUInt(value, 36uL) }
        override var baseArrayLayer: UInt
            get() = mem.readUInt(40uL)
            set(value) { mem.writeUInt(value, 40uL) }
        override var arrayLayerCount: UInt
            get() = mem.readUInt(44uL)
            set(value) { mem.writeUInt(value, 44uL) }
        override var aspect: WGPUTextureAspect
            get() = mem.readUInt(48uL) as WGPUTextureAspect
            set(value) { mem.writeUInt(value.toUInt(), 48uL) }
        override var usage: ULong
            get() = mem.readULong(56uL)
            set(value) { mem.writeULong(value, 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUVertexState {
    actual var nextInChain: WGPUChainedStruct?
    actual var module: WGPUShaderModule?
    actual var entryPoint: WGPUStringView
    actual var constantCount: ULong
    actual var constants: WGPUConstantEntry?
    actual var bufferCount: ULong
    actual var buffers: WGPUVertexBufferLayout?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUVertexState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexState = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexState) -> Unit): ArrayHolder<WGPUVertexState> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUVertexState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var bufferCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var buffers: WGPUVertexBufferLayout?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUVertexBufferLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUVertexState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var bufferCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var buffers: WGPUVertexBufferLayout?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUVertexBufferLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPUFragmentState {
    actual var nextInChain: WGPUChainedStruct?
    actual var module: WGPUShaderModule?
    actual var entryPoint: WGPUStringView
    actual var constantCount: ULong
    actual var constants: WGPUConstantEntry?
    actual var targetCount: ULong
    actual var targets: WGPUColorTargetState?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUFragmentState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUFragmentState = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFragmentState) -> Unit): ArrayHolder<WGPUFragmentState> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPUFragmentState>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPUFragmentState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var targetCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var targets: WGPUColorTargetState?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUColorTargetState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPUFragmentState {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var module: WGPUShaderModule?
            get() = mem.readPointer(8uL).takeIf { it.rawValue != 0L }?.let { WGPUShaderModule(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 8uL) }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 16L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 16uL, 16uL)
            }
        override var constantCount: ULong
            get() = mem.readULong(32uL)
            set(value) { mem.writeULong(value, 32uL) }
        override var constants: WGPUConstantEntry?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPUConstantEntry(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var targetCount: ULong
            get() = mem.readULong(48uL)
            set(value) { mem.writeULong(value, 48uL) }
        override var targets: WGPUColorTargetState?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUColorTargetState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPassDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var colorAttachmentCount: ULong
    actual var colorAttachments: WGPURenderPassColorAttachment?
    actual var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
    actual var occlusionQuerySet: WGPUQuerySet?
    actual var timestampWrites: WGPUPassTimestampWrites?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptor = ByReference(allocator.allocateBuffer(64uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptor) -> Unit): ArrayHolder<WGPURenderPassDescriptor> {
            val buffer = allocator.allocateBuffer(64uL * size)
            val result = ArrayHolder<WGPURenderPassDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 64L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var colorAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var colorAttachments: WGPURenderPassColorAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassColorAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassDepthStencilAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var occlusionQuerySet: WGPUQuerySet?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUQuerySet(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUPassTimestampWrites(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPassDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 64uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var colorAttachmentCount: ULong
            get() = mem.readULong(24uL)
            set(value) { mem.writeULong(value, 24uL) }
        override var colorAttachments: WGPURenderPassColorAttachment?
            get() = mem.readPointer(32uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassColorAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 32uL) }
        override var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
            get() = mem.readPointer(40uL).takeIf { it.rawValue != 0L }?.let { WGPURenderPassDepthStencilAttachment(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 40uL) }
        override var occlusionQuerySet: WGPUQuerySet?
            get() = mem.readPointer(48uL).takeIf { it.rawValue != 0L }?.let { WGPUQuerySet(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 48uL) }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = mem.readPointer(56uL).takeIf { it.rawValue != 0L }?.let { WGPUPassTimestampWrites(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 56uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

actual interface WGPURenderPipelineDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var layout: WGPUPipelineLayout?
    actual var vertex: WGPUVertexState
    actual var primitive: WGPUPrimitiveState
    actual var depthStencil: WGPUDepthStencilState?
    actual var multisample: WGPUMultisampleState
    actual var fragment: WGPUFragmentState?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPipelineDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPipelineDescriptor = ByReference(allocator.allocateBuffer(168uL).handler)
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPipelineDescriptor) -> Unit): ArrayHolder<WGPURenderPipelineDescriptor> {
            val buffer = allocator.allocateBuffer(168uL * size)
            val result = ArrayHolder<WGPURenderPipelineDescriptor>(buffer.handler)
            repeat(size.toInt()) { index ->
                provider(index.toUInt(), ByValue(NativeAddress(buffer.handler.rawValue + index.toLong() * 168L)))
            }
            return result
        }
    }
    
    class ByReference(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPipelineDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 168uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUPipelineLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var vertex: WGPUVertexState
            get() = WGPUVertexState.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(64)
                MemoryBuffer(value.handler, 64uL).readBytes(bytes, 0u, 0uL, 64uL)
                mem.writeBytes(bytes, 0u, 32uL, 64uL)
            }
        override var primitive: WGPUPrimitiveState
            get() = WGPUPrimitiveState.ByValue(NativeAddress(handle.rawValue + 96L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 96uL, 32uL)
            }
        override var depthStencil: WGPUDepthStencilState?
            get() = mem.readPointer(128uL).takeIf { it.rawValue != 0L }?.let { WGPUDepthStencilState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 128uL) }
        override var multisample: WGPUMultisampleState
            get() = WGPUMultisampleState.ByValue(NativeAddress(handle.rawValue + 136L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 136uL, 24uL)
            }
        override var fragment: WGPUFragmentState?
            get() = mem.readPointer(160uL).takeIf { it.rawValue != 0L }?.let { WGPUFragmentState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 160uL) }
        override val handler: NativeAddress
            get() = handle
    }
    
    class ByValue(val handle: NativeAddress = NativeAddress(0L)) : WGPURenderPipelineDescriptor {
        private val mem: MemoryBuffer by lazy { MemoryBuffer(handle, 168uL) }
        override var nextInChain: WGPUChainedStruct?
            get() = mem.readPointer(0uL).takeIf { it.rawValue != 0L }?.let { WGPUChainedStruct(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 0uL) }
        override var label: WGPUStringView
            get() = WGPUStringView.ByValue(NativeAddress(handle.rawValue + 8L))
            set(value) {
                val bytes = ByteArray(16)
                MemoryBuffer(value.handler, 16uL).readBytes(bytes, 0u, 0uL, 16uL)
                mem.writeBytes(bytes, 0u, 8uL, 16uL)
            }
        override var layout: WGPUPipelineLayout?
            get() = mem.readPointer(24uL).takeIf { it.rawValue != 0L }?.let { WGPUPipelineLayout(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 24uL) }
        override var vertex: WGPUVertexState
            get() = WGPUVertexState.ByValue(NativeAddress(handle.rawValue + 32L))
            set(value) {
                val bytes = ByteArray(64)
                MemoryBuffer(value.handler, 64uL).readBytes(bytes, 0u, 0uL, 64uL)
                mem.writeBytes(bytes, 0u, 32uL, 64uL)
            }
        override var primitive: WGPUPrimitiveState
            get() = WGPUPrimitiveState.ByValue(NativeAddress(handle.rawValue + 96L))
            set(value) {
                val bytes = ByteArray(32)
                MemoryBuffer(value.handler, 32uL).readBytes(bytes, 0u, 0uL, 32uL)
                mem.writeBytes(bytes, 0u, 96uL, 32uL)
            }
        override var depthStencil: WGPUDepthStencilState?
            get() = mem.readPointer(128uL).takeIf { it.rawValue != 0L }?.let { WGPUDepthStencilState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 128uL) }
        override var multisample: WGPUMultisampleState
            get() = WGPUMultisampleState.ByValue(NativeAddress(handle.rawValue + 136L))
            set(value) {
                val bytes = ByteArray(24)
                MemoryBuffer(value.handler, 24uL).readBytes(bytes, 0u, 0uL, 24uL)
                mem.writeBytes(bytes, 0u, 136uL, 24uL)
            }
        override var fragment: WGPUFragmentState?
            get() = mem.readPointer(160uL).takeIf { it.rawValue != 0L }?.let { WGPUFragmentState(it) }
            set(value) { mem.writePointer(value?.handler ?: NativeAddress(0L), 160uL) }
        override val handler: NativeAddress
            get() = handle
    }
}

private val wgpuCreateInstance_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCreateInstance") }
actual fun wgpuCreateInstance(descriptor: WGPUInstanceDescriptor?): WGPUInstance? {
    return NativeEngine.callP1P(wgpuCreateInstance_ADDR, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUInstance)
}

private val wgpuGetInstanceFeatures_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuGetInstanceFeatures") }
actual fun wgpuGetInstanceFeatures(features: WGPUSupportedInstanceFeatures?): Unit {
    NativeEngine.callV1P(wgpuGetInstanceFeatures_ADDR, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuGetInstanceLimits_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuGetInstanceLimits") }
actual fun wgpuGetInstanceLimits(limits: WGPUInstanceLimits?): WGPUStatus {
    return (NativeEngine.callI1P(wgpuGetInstanceLimits_ADDR, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuHasInstanceFeature_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuHasInstanceFeature") }
actual fun wgpuHasInstanceFeature(feature: WGPUInstanceFeatureName): UInt {
    return NativeEngine.callI1I(wgpuHasInstanceFeature_ADDR, feature.toInt()).toInt().toUInt()
}

private val wgpuGetProcAddress_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuGetProcAddress") }
actual fun wgpuGetProcAddress(procName: WGPUStringView): NativeAddress? {
    val args = MemoryAllocator().allocateBuffer(16uL)
    val procNameBytes = ByteArray(16)
    MemoryBuffer(procName.handler, 16uL).readBytes(procNameBytes, 0u, 0uL, 16uL)
    args.writeBytes(procNameBytes, 0u, 0uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuGetProcAddress_ADDR, 1, "p:s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
    return out.readLong(0uL).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuAdapterCreateDevice_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterCreateDevice") }
actual fun wgpuAdapterCreateDevice(adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?): WGPUDevice? {
    return NativeEngine.callP2PP(wgpuAdapterCreateDevice_ADDR, adapter?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUDevice)
}

private val wgpuAdapterGetFeatures_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterGetFeatures") }
actual fun wgpuAdapterGetFeatures(adapter: WGPUAdapter?, features: WGPUSupportedFeatures?): Unit {
    NativeEngine.callV2PP(wgpuAdapterGetFeatures_ADDR, adapter?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterGetFormatCapabilities_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterGetFormatCapabilities") }
actual fun wgpuAdapterGetFormatCapabilities(adapter: WGPUAdapter?, format: WGPUTextureFormat, capabilities: WGPUDawnFormatCapabilities?): WGPUStatus {
    return (NativeEngine.callI3PIP(wgpuAdapterGetFormatCapabilities_ADDR, adapter?.handler?.rawValue ?: 0L, format.toInt(), capabilities?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterGetInfo_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterGetInfo") }
actual fun wgpuAdapterGetInfo(adapter: WGPUAdapter?, info: WGPUAdapterInfo?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuAdapterGetInfo_ADDR, adapter?.handler?.rawValue ?: 0L, info?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterGetInstance_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterGetInstance") }
actual fun wgpuAdapterGetInstance(adapter: WGPUAdapter?): WGPUInstance? {
    return NativeEngine.callP1P(wgpuAdapterGetInstance_ADDR, adapter?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUInstance)
}

private val wgpuAdapterGetLimits_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterGetLimits") }
actual fun wgpuAdapterGetLimits(adapter: WGPUAdapter?, limits: WGPULimits?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuAdapterGetLimits_ADDR, adapter?.handler?.rawValue ?: 0L, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterHasFeature_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterHasFeature") }
actual fun wgpuAdapterHasFeature(adapter: WGPUAdapter?, feature: WGPUFeatureName): UInt {
    return NativeEngine.callI2PI(wgpuAdapterHasFeature_ADDR, adapter?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuAdapterRequestDevice_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterRequestDevice") }
actual fun wgpuAdapterRequestDevice(allocator: MemoryAllocator, adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?, callbackInfo: WGPURequestDeviceCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(56uL)
    args.writeLong(adapter?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(descriptor?.handler?.rawValue ?: 0L, 8uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 16uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuAdapterRequestDevice_ADDR, 3, "u64:p,p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuAdapterAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterAddRef") }
actual fun wgpuAdapterAddRef(adapter: WGPUAdapter?): Unit {
    NativeEngine.callV1P(wgpuAdapterAddRef_ADDR, adapter?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterRelease") }
actual fun wgpuAdapterRelease(adapter: WGPUAdapter?): Unit {
    NativeEngine.callV1P(wgpuAdapterRelease_ADDR, adapter?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterInfoFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterInfoFreeMembers") }
actual fun wgpuAdapterInfoFreeMembers(adapterInfo: WGPUAdapterInfo): Unit {
    val args = MemoryAllocator().allocateBuffer(96uL)
    val adapterInfoBytes = ByteArray(96)
    MemoryBuffer(adapterInfo.handler, 96uL).readBytes(adapterInfoBytes, 0u, 0uL, 96uL)
    args.writeBytes(adapterInfoBytes, 0u, 0uL, 96uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuAdapterInfoFreeMembers_ADDR, 1, "v:s96@8(p,s16@8(p,i64),s16@8(p,i64),s16@8(p,i64),s16@8(p,i64),u32,u32,i32,i32,i32,i32)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuAdapterPropertiesMemoryHeapsFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterPropertiesMemoryHeapsFreeMembers") }
actual fun wgpuAdapterPropertiesMemoryHeapsFreeMembers(adapterPropertiesMemoryHeaps: WGPUAdapterPropertiesMemoryHeaps): Unit {
    val args = MemoryAllocator().allocateBuffer(32uL)
    val adapterPropertiesMemoryHeapsBytes = ByteArray(32)
    MemoryBuffer(adapterPropertiesMemoryHeaps.handler, 32uL).readBytes(adapterPropertiesMemoryHeapsBytes, 0u, 0uL, 32uL)
    args.writeBytes(adapterPropertiesMemoryHeapsBytes, 0u, 0uL, 32uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuAdapterPropertiesMemoryHeapsFreeMembers_ADDR, 1, "v:s32@8(s16@8(p,u32),i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers") }
actual fun wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers(adapterPropertiesSubgroupMatrixConfigs: WGPUAdapterPropertiesSubgroupMatrixConfigs): Unit {
    val args = MemoryAllocator().allocateBuffer(32uL)
    val adapterPropertiesSubgroupMatrixConfigsBytes = ByteArray(32)
    MemoryBuffer(adapterPropertiesSubgroupMatrixConfigs.handler, 32uL).readBytes(adapterPropertiesSubgroupMatrixConfigsBytes, 0u, 0uL, 32uL)
    args.writeBytes(adapterPropertiesSubgroupMatrixConfigsBytes, 0u, 0uL, 32uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers_ADDR, 1, "v:s32@8(s16@8(p,u32),i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuBindGroupSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupSetLabel") }
actual fun wgpuBindGroupSetLabel(bindGroup: WGPUBindGroup?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(bindGroup?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuBindGroupSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuBindGroupAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupAddRef") }
actual fun wgpuBindGroupAddRef(bindGroup: WGPUBindGroup?): Unit {
    NativeEngine.callV1P(wgpuBindGroupAddRef_ADDR, bindGroup?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupRelease") }
actual fun wgpuBindGroupRelease(bindGroup: WGPUBindGroup?): Unit {
    NativeEngine.callV1P(wgpuBindGroupRelease_ADDR, bindGroup?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupLayoutSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupLayoutSetLabel") }
actual fun wgpuBindGroupLayoutSetLabel(bindGroupLayout: WGPUBindGroupLayout?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(bindGroupLayout?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuBindGroupLayoutSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuBindGroupLayoutAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupLayoutAddRef") }
actual fun wgpuBindGroupLayoutAddRef(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    NativeEngine.callV1P(wgpuBindGroupLayoutAddRef_ADDR, bindGroupLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupLayoutRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBindGroupLayoutRelease") }
actual fun wgpuBindGroupLayoutRelease(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    NativeEngine.callV1P(wgpuBindGroupLayoutRelease_ADDR, bindGroupLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferCreateTexelView_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferCreateTexelView") }
actual fun wgpuBufferCreateTexelView(buffer: WGPUBuffer?, descriptor: WGPUTexelBufferViewDescriptor?): WGPUTexelBufferView? {
    return NativeEngine.callP2PP(wgpuBufferCreateTexelView_ADDR, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexelBufferView)
}

private val wgpuBufferDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferDestroy") }
actual fun wgpuBufferDestroy(buffer: WGPUBuffer?): Unit {
    NativeEngine.callV1P(wgpuBufferDestroy_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferGetConstMappedRange_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferGetConstMappedRange") }
actual fun wgpuBufferGetConstMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return NativeEngine.callP3PLL(wgpuBufferGetConstMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong()).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuBufferGetMappedRange_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferGetMappedRange") }
actual fun wgpuBufferGetMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return NativeEngine.callP3PLL(wgpuBufferGetMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong()).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuBufferGetMapState_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferGetMapState") }
actual fun wgpuBufferGetMapState(buffer: WGPUBuffer?): WGPUBufferMapState {
    return (NativeEngine.callI1P(wgpuBufferGetMapState_ADDR, buffer?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuBufferGetSize_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferGetSize") }
actual fun wgpuBufferGetSize(buffer: WGPUBuffer?): ULong {
    return NativeEngine.callL1P(wgpuBufferGetSize_ADDR, buffer?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuBufferGetUsage_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferGetUsage") }
actual fun wgpuBufferGetUsage(buffer: WGPUBuffer?): ULong {
    return NativeEngine.callL1P(wgpuBufferGetUsage_ADDR, buffer?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuBufferMapAsync_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferMapAsync") }
actual fun wgpuBufferMapAsync(allocator: MemoryAllocator, buffer: WGPUBuffer?, mode: ULong, offset: ULong, size: ULong, callbackInfo: WGPUBufferMapCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(72uL)
    args.writeLong(buffer?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(mode.toLong(), 8uL)
    args.writeLong(offset.toLong(), 16uL)
    args.writeLong(size.toLong(), 24uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 32uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuBufferMapAsync_ADDR, 5, "u64:p,u64,u64,u64,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuBufferReadMappedRange_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferReadMappedRange") }
actual fun wgpuBufferReadMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return (NativeEngine.callI4PLPL(wgpuBufferReadMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), data.toAddress(), size.toLong()).toInt()).toUInt()
}

private val wgpuBufferSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferSetLabel") }
actual fun wgpuBufferSetLabel(buffer: WGPUBuffer?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(buffer?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuBufferSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuBufferUnmap_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferUnmap") }
actual fun wgpuBufferUnmap(buffer: WGPUBuffer?): Unit {
    NativeEngine.callV1P(wgpuBufferUnmap_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferWriteMappedRange_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferWriteMappedRange") }
actual fun wgpuBufferWriteMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return (NativeEngine.callI4PLPL(wgpuBufferWriteMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), data.toAddress(), size.toLong()).toInt()).toUInt()
}

private val wgpuBufferAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferAddRef") }
actual fun wgpuBufferAddRef(buffer: WGPUBuffer?): Unit {
    NativeEngine.callV1P(wgpuBufferAddRef_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuBufferRelease") }
actual fun wgpuBufferRelease(buffer: WGPUBuffer?): Unit {
    NativeEngine.callV1P(wgpuBufferRelease_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandBufferSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandBufferSetLabel") }
actual fun wgpuCommandBufferSetLabel(commandBuffer: WGPUCommandBuffer?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(commandBuffer?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuCommandBufferSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuCommandBufferAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandBufferAddRef") }
actual fun wgpuCommandBufferAddRef(commandBuffer: WGPUCommandBuffer?): Unit {
    NativeEngine.callV1P(wgpuCommandBufferAddRef_ADDR, commandBuffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandBufferRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandBufferRelease") }
actual fun wgpuCommandBufferRelease(commandBuffer: WGPUCommandBuffer?): Unit {
    NativeEngine.callV1P(wgpuCommandBufferRelease_ADDR, commandBuffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderBeginComputePass_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderBeginComputePass") }
actual fun wgpuCommandEncoderBeginComputePass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUComputePassDescriptor?): WGPUComputePassEncoder? {
    return NativeEngine.callP2PP(wgpuCommandEncoderBeginComputePass_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePassEncoder)
}

private val wgpuCommandEncoderBeginRenderPass_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderBeginRenderPass") }
actual fun wgpuCommandEncoderBeginRenderPass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPURenderPassDescriptor?): WGPURenderPassEncoder? {
    return NativeEngine.callP2PP(wgpuCommandEncoderBeginRenderPass_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPassEncoder)
}

private val wgpuCommandEncoderClearBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderClearBuffer") }
actual fun wgpuCommandEncoderClearBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    NativeEngine.callV4PPLL(wgpuCommandEncoderClearBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuCommandEncoderCopyBufferToBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderCopyBufferToBuffer") }
actual fun wgpuCommandEncoderCopyBufferToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUBuffer?, sourceOffset: ULong, destination: WGPUBuffer?, destinationOffset: ULong, size: ULong): Unit {
    NativeEngine.callV6PPLPLL(wgpuCommandEncoderCopyBufferToBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, sourceOffset.toLong(), destination?.handler?.rawValue ?: 0L, destinationOffset.toLong(), size.toLong())
    return
}

private val wgpuCommandEncoderCopyBufferToTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderCopyBufferToTexture") }
actual fun wgpuCommandEncoderCopyBufferToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyBufferInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    NativeEngine.callV4PPPP(wgpuCommandEncoderCopyBufferToTexture_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderCopyTextureToBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderCopyTextureToBuffer") }
actual fun wgpuCommandEncoderCopyTextureToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyBufferInfo?, copySize: WGPUExtent3D?): Unit {
    NativeEngine.callV4PPPP(wgpuCommandEncoderCopyTextureToBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderCopyTextureToTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderCopyTextureToTexture") }
actual fun wgpuCommandEncoderCopyTextureToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    NativeEngine.callV4PPPP(wgpuCommandEncoderCopyTextureToTexture_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderFinish_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderFinish") }
actual fun wgpuCommandEncoderFinish(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUCommandBufferDescriptor?): WGPUCommandBuffer? {
    return NativeEngine.callP2PP(wgpuCommandEncoderFinish_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUCommandBuffer)
}

private val wgpuCommandEncoderInjectValidationError_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderInjectValidationError") }
actual fun wgpuCommandEncoderInjectValidationError(commandEncoder: WGPUCommandEncoder?, message: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(commandEncoder?.handler?.rawValue ?: 0L, 0uL)
    val messageBytes = ByteArray(16)
    MemoryBuffer(message.handler, 16uL).readBytes(messageBytes, 0u, 0uL, 16uL)
    args.writeBytes(messageBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuCommandEncoderInjectValidationError_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuCommandEncoderInsertDebugMarker_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderInsertDebugMarker") }
actual fun wgpuCommandEncoderInsertDebugMarker(commandEncoder: WGPUCommandEncoder?, markerLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(commandEncoder?.handler?.rawValue ?: 0L, 0uL)
    val markerLabelBytes = ByteArray(16)
    MemoryBuffer(markerLabel.handler, 16uL).readBytes(markerLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(markerLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuCommandEncoderInsertDebugMarker_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuCommandEncoderPopDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderPopDebugGroup") }
actual fun wgpuCommandEncoderPopDebugGroup(commandEncoder: WGPUCommandEncoder?): Unit {
    NativeEngine.callV1P(wgpuCommandEncoderPopDebugGroup_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderPushDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderPushDebugGroup") }
actual fun wgpuCommandEncoderPushDebugGroup(commandEncoder: WGPUCommandEncoder?, groupLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(commandEncoder?.handler?.rawValue ?: 0L, 0uL)
    val groupLabelBytes = ByteArray(16)
    MemoryBuffer(groupLabel.handler, 16uL).readBytes(groupLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(groupLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuCommandEncoderPushDebugGroup_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuCommandEncoderResolveQuerySet_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderResolveQuerySet") }
actual fun wgpuCommandEncoderResolveQuerySet(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, firstQuery: UInt, queryCount: UInt, destination: WGPUBuffer?, destinationOffset: ULong): Unit {
    NativeEngine.callV6PPIIPL(wgpuCommandEncoderResolveQuerySet_ADDR, commandEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, firstQuery.toInt(), queryCount.toInt(), destination?.handler?.rawValue ?: 0L, destinationOffset.toLong())
    return
}

private val wgpuCommandEncoderSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderSetLabel") }
actual fun wgpuCommandEncoderSetLabel(commandEncoder: WGPUCommandEncoder?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(commandEncoder?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuCommandEncoderSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuCommandEncoderWriteBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderWriteBuffer") }
actual fun wgpuCommandEncoderWriteBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    NativeEngine.callV5PPLPL(wgpuCommandEncoderWriteBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, bufferOffset.toLong(), data.toAddress(), size.toLong())
    return
}

private val wgpuCommandEncoderWriteTimestamp_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderWriteTimestamp") }
actual fun wgpuCommandEncoderWriteTimestamp(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    NativeEngine.callV3PPI(wgpuCommandEncoderWriteTimestamp_ADDR, commandEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuCommandEncoderAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderAddRef") }
actual fun wgpuCommandEncoderAddRef(commandEncoder: WGPUCommandEncoder?): Unit {
    NativeEngine.callV1P(wgpuCommandEncoderAddRef_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuCommandEncoderRelease") }
actual fun wgpuCommandEncoderRelease(commandEncoder: WGPUCommandEncoder?): Unit {
    NativeEngine.callV1P(wgpuCommandEncoderRelease_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderDispatchWorkgroups_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderDispatchWorkgroups") }
actual fun wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder: WGPUComputePassEncoder?, workgroupCountX: UInt, workgroupCountY: UInt, workgroupCountZ: UInt): Unit {
    NativeEngine.callV4PIII(wgpuComputePassEncoderDispatchWorkgroups_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, workgroupCountX.toInt(), workgroupCountY.toInt(), workgroupCountZ.toInt())
    return
}

private val wgpuComputePassEncoderDispatchWorkgroupsIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderDispatchWorkgroupsIndirect") }
actual fun wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder: WGPUComputePassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    NativeEngine.callV3PPL(wgpuComputePassEncoderDispatchWorkgroupsIndirect_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuComputePassEncoderEnd_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderEnd") }
actual fun wgpuComputePassEncoderEnd(computePassEncoder: WGPUComputePassEncoder?): Unit {
    NativeEngine.callV1P(wgpuComputePassEncoderEnd_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderInsertDebugMarker_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderInsertDebugMarker") }
actual fun wgpuComputePassEncoderInsertDebugMarker(computePassEncoder: WGPUComputePassEncoder?, markerLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(computePassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val markerLabelBytes = ByteArray(16)
    MemoryBuffer(markerLabel.handler, 16uL).readBytes(markerLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(markerLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuComputePassEncoderInsertDebugMarker_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuComputePassEncoderPopDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderPopDebugGroup") }
actual fun wgpuComputePassEncoderPopDebugGroup(computePassEncoder: WGPUComputePassEncoder?): Unit {
    NativeEngine.callV1P(wgpuComputePassEncoderPopDebugGroup_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderPushDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderPushDebugGroup") }
actual fun wgpuComputePassEncoderPushDebugGroup(computePassEncoder: WGPUComputePassEncoder?, groupLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(computePassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val groupLabelBytes = ByteArray(16)
    MemoryBuffer(groupLabel.handler, 16uL).readBytes(groupLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(groupLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuComputePassEncoderPushDebugGroup_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuComputePassEncoderSetBindGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderSetBindGroup") }
actual fun wgpuComputePassEncoderSetBindGroup(computePassEncoder: WGPUComputePassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    NativeEngine.callV5PIPLP(wgpuComputePassEncoderSetBindGroup_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets.toAddress())
    return
}

private val wgpuComputePassEncoderSetImmediates_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderSetImmediates") }
actual fun wgpuComputePassEncoderSetImmediates(computePassEncoder: WGPUComputePassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    NativeEngine.callV4PIPL(wgpuComputePassEncoderSetImmediates_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data.toAddress(), size.toLong())
    return
}

private val wgpuComputePassEncoderSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderSetLabel") }
actual fun wgpuComputePassEncoderSetLabel(computePassEncoder: WGPUComputePassEncoder?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(computePassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuComputePassEncoderSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuComputePassEncoderSetPipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderSetPipeline") }
actual fun wgpuComputePassEncoderSetPipeline(computePassEncoder: WGPUComputePassEncoder?, pipeline: WGPUComputePipeline?): Unit {
    NativeEngine.callV2PP(wgpuComputePassEncoderSetPipeline_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderSetResourceTable_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderSetResourceTable") }
actual fun wgpuComputePassEncoderSetResourceTable(computePassEncoder: WGPUComputePassEncoder?, table: WGPUResourceTable?): Unit {
    NativeEngine.callV2PP(wgpuComputePassEncoderSetResourceTable_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, table?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderWriteTimestamp_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderWriteTimestamp") }
actual fun wgpuComputePassEncoderWriteTimestamp(computePassEncoder: WGPUComputePassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    NativeEngine.callV3PPI(wgpuComputePassEncoderWriteTimestamp_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuComputePassEncoderAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderAddRef") }
actual fun wgpuComputePassEncoderAddRef(computePassEncoder: WGPUComputePassEncoder?): Unit {
    NativeEngine.callV1P(wgpuComputePassEncoderAddRef_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePassEncoderRelease") }
actual fun wgpuComputePassEncoderRelease(computePassEncoder: WGPUComputePassEncoder?): Unit {
    NativeEngine.callV1P(wgpuComputePassEncoderRelease_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePipelineGetBindGroupLayout_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePipelineGetBindGroupLayout") }
actual fun wgpuComputePipelineGetBindGroupLayout(computePipeline: WGPUComputePipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return NativeEngine.callP2PI(wgpuComputePipelineGetBindGroupLayout_ADDR, computePipeline?.handler?.rawValue ?: 0L, groupIndex.toInt()).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuComputePipelineSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePipelineSetLabel") }
actual fun wgpuComputePipelineSetLabel(computePipeline: WGPUComputePipeline?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(computePipeline?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuComputePipelineSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuComputePipelineAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePipelineAddRef") }
actual fun wgpuComputePipelineAddRef(computePipeline: WGPUComputePipeline?): Unit {
    NativeEngine.callV1P(wgpuComputePipelineAddRef_ADDR, computePipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePipelineRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuComputePipelineRelease") }
actual fun wgpuComputePipelineRelease(computePipeline: WGPUComputePipeline?): Unit {
    NativeEngine.callV1P(wgpuComputePipelineRelease_ADDR, computePipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDawnDrmFormatCapabilitiesFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDawnDrmFormatCapabilitiesFreeMembers") }
actual fun wgpuDawnDrmFormatCapabilitiesFreeMembers(dawnDrmFormatCapabilities: WGPUDawnDrmFormatCapabilities): Unit {
    val args = MemoryAllocator().allocateBuffer(32uL)
    val dawnDrmFormatCapabilitiesBytes = ByteArray(32)
    MemoryBuffer(dawnDrmFormatCapabilities.handler, 32uL).readBytes(dawnDrmFormatCapabilitiesBytes, 0u, 0uL, 32uL)
    args.writeBytes(dawnDrmFormatCapabilitiesBytes, 0u, 0uL, 32uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDawnDrmFormatCapabilitiesFreeMembers_ADDR, 1, "v:s32@8(s16@8(p,u32),i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuDeviceCreateBindGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateBindGroup") }
actual fun wgpuDeviceCreateBindGroup(device: WGPUDevice?, descriptor: WGPUBindGroupDescriptor?): WGPUBindGroup? {
    return NativeEngine.callP2PP(wgpuDeviceCreateBindGroup_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroup)
}

private val wgpuDeviceCreateBindGroupLayout_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateBindGroupLayout") }
actual fun wgpuDeviceCreateBindGroupLayout(device: WGPUDevice?, descriptor: WGPUBindGroupLayoutDescriptor?): WGPUBindGroupLayout? {
    return NativeEngine.callP2PP(wgpuDeviceCreateBindGroupLayout_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuDeviceCreateBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateBuffer") }
actual fun wgpuDeviceCreateBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return NativeEngine.callP2PP(wgpuDeviceCreateBuffer_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuDeviceCreateCommandEncoder_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateCommandEncoder") }
actual fun wgpuDeviceCreateCommandEncoder(device: WGPUDevice?, descriptor: WGPUCommandEncoderDescriptor?): WGPUCommandEncoder? {
    return NativeEngine.callP2PP(wgpuDeviceCreateCommandEncoder_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUCommandEncoder)
}

private val wgpuDeviceCreateComputePipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateComputePipeline") }
actual fun wgpuDeviceCreateComputePipeline(device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?): WGPUComputePipeline? {
    return NativeEngine.callP2PP(wgpuDeviceCreateComputePipeline_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePipeline)
}

private val wgpuDeviceCreateComputePipelineAsync_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateComputePipelineAsync") }
actual fun wgpuDeviceCreateComputePipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?, callbackInfo: WGPUCreateComputePipelineAsyncCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(56uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(descriptor?.handler?.rawValue ?: 0L, 8uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 16uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceCreateComputePipelineAsync_ADDR, 3, "u64:p,p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuDeviceCreateErrorBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorBuffer") }
actual fun wgpuDeviceCreateErrorBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return NativeEngine.callP2PP(wgpuDeviceCreateErrorBuffer_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuDeviceCreateErrorComputePipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorComputePipeline") }
actual fun wgpuDeviceCreateErrorComputePipeline(device: WGPUDevice?, label: WGPUStringView): WGPUComputePipeline? {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceCreateErrorComputePipeline_ADDR, 2, "p:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
    return out.readLong(0uL).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePipeline)
}

private val wgpuDeviceCreateErrorExternalTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorExternalTexture") }
actual fun wgpuDeviceCreateErrorExternalTexture(device: WGPUDevice?): WGPUExternalTexture? {
    return NativeEngine.callP1P(wgpuDeviceCreateErrorExternalTexture_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUExternalTexture)
}

private val wgpuDeviceCreateErrorRenderPipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorRenderPipeline") }
actual fun wgpuDeviceCreateErrorRenderPipeline(device: WGPUDevice?, label: WGPUStringView): WGPURenderPipeline? {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceCreateErrorRenderPipeline_ADDR, 2, "p:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
    return out.readLong(0uL).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPipeline)
}

private val wgpuDeviceCreateErrorShaderModule_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorShaderModule") }
actual fun wgpuDeviceCreateErrorShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?, errorMessage: WGPUStringView): WGPUShaderModule? {
    val args = MemoryAllocator().allocateBuffer(32uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(descriptor?.handler?.rawValue ?: 0L, 8uL)
    val errorMessageBytes = ByteArray(16)
    MemoryBuffer(errorMessage.handler, 16uL).readBytes(errorMessageBytes, 0u, 0uL, 16uL)
    args.writeBytes(errorMessageBytes, 0u, 16uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceCreateErrorShaderModule_ADDR, 3, "p:p,p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
    return out.readLong(0uL).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUShaderModule)
}

private val wgpuDeviceCreateErrorTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateErrorTexture") }
actual fun wgpuDeviceCreateErrorTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return NativeEngine.callP2PP(wgpuDeviceCreateErrorTexture_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuDeviceCreateExternalTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateExternalTexture") }
actual fun wgpuDeviceCreateExternalTexture(device: WGPUDevice?, externalTextureDescriptor: WGPUExternalTextureDescriptor?): WGPUExternalTexture? {
    return NativeEngine.callP2PP(wgpuDeviceCreateExternalTexture_ADDR, device?.handler?.rawValue ?: 0L, externalTextureDescriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUExternalTexture)
}

private val wgpuDeviceCreatePipelineLayout_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreatePipelineLayout") }
actual fun wgpuDeviceCreatePipelineLayout(device: WGPUDevice?, descriptor: WGPUPipelineLayoutDescriptor?): WGPUPipelineLayout? {
    return NativeEngine.callP2PP(wgpuDeviceCreatePipelineLayout_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUPipelineLayout)
}

private val wgpuDeviceCreateQuerySet_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateQuerySet") }
actual fun wgpuDeviceCreateQuerySet(device: WGPUDevice?, descriptor: WGPUQuerySetDescriptor?): WGPUQuerySet? {
    return NativeEngine.callP2PP(wgpuDeviceCreateQuerySet_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUQuerySet)
}

private val wgpuDeviceCreateRenderBundleEncoder_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateRenderBundleEncoder") }
actual fun wgpuDeviceCreateRenderBundleEncoder(device: WGPUDevice?, descriptor: WGPURenderBundleEncoderDescriptor?): WGPURenderBundleEncoder? {
    return NativeEngine.callP2PP(wgpuDeviceCreateRenderBundleEncoder_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderBundleEncoder)
}

private val wgpuDeviceCreateRenderPipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateRenderPipeline") }
actual fun wgpuDeviceCreateRenderPipeline(device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?): WGPURenderPipeline? {
    return NativeEngine.callP2PP(wgpuDeviceCreateRenderPipeline_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPipeline)
}

private val wgpuDeviceCreateRenderPipelineAsync_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateRenderPipelineAsync") }
actual fun wgpuDeviceCreateRenderPipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?, callbackInfo: WGPUCreateRenderPipelineAsyncCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(56uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(descriptor?.handler?.rawValue ?: 0L, 8uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 16uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceCreateRenderPipelineAsync_ADDR, 3, "u64:p,p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuDeviceCreateResourceTable_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateResourceTable") }
actual fun wgpuDeviceCreateResourceTable(device: WGPUDevice?, descriptor: WGPUResourceTableDescriptor?): WGPUResourceTable? {
    return NativeEngine.callP2PP(wgpuDeviceCreateResourceTable_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUResourceTable)
}

private val wgpuDeviceCreateSampler_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateSampler") }
actual fun wgpuDeviceCreateSampler(device: WGPUDevice?, descriptor: WGPUSamplerDescriptor?): WGPUSampler? {
    return NativeEngine.callP2PP(wgpuDeviceCreateSampler_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSampler)
}

private val wgpuDeviceCreateShaderModule_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateShaderModule") }
actual fun wgpuDeviceCreateShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?): WGPUShaderModule? {
    return NativeEngine.callP2PP(wgpuDeviceCreateShaderModule_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUShaderModule)
}

private val wgpuDeviceCreateTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceCreateTexture") }
actual fun wgpuDeviceCreateTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return NativeEngine.callP2PP(wgpuDeviceCreateTexture_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuDeviceDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceDestroy") }
actual fun wgpuDeviceDestroy(device: WGPUDevice?): Unit {
    NativeEngine.callV1P(wgpuDeviceDestroy_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceForceLoss_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceForceLoss") }
actual fun wgpuDeviceForceLoss(device: WGPUDevice?, type: WGPUDeviceLostReason, message: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(32uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    args.writeInt(type.toInt(), 8uL)
    val messageBytes = ByteArray(16)
    MemoryBuffer(message.handler, 16uL).readBytes(messageBytes, 0u, 0uL, 16uL)
    args.writeBytes(messageBytes, 0u, 16uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceForceLoss_ADDR, 3, "v:p,u32,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuDeviceGetAdapter_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetAdapter") }
actual fun wgpuDeviceGetAdapter(device: WGPUDevice?): WGPUAdapter? {
    return NativeEngine.callP1P(wgpuDeviceGetAdapter_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUAdapter)
}

private val wgpuDeviceGetAdapterInfo_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetAdapterInfo") }
actual fun wgpuDeviceGetAdapterInfo(device: WGPUDevice?, adapterInfo: WGPUAdapterInfo?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuDeviceGetAdapterInfo_ADDR, device?.handler?.rawValue ?: 0L, adapterInfo?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetAHardwareBufferProperties_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetAHardwareBufferProperties") }
actual fun wgpuDeviceGetAHardwareBufferProperties(device: WGPUDevice?, handle: NativeAddress?, properties: WGPUAHardwareBufferProperties?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuDeviceGetAHardwareBufferProperties_ADDR, device?.handler?.rawValue ?: 0L, handle.toAddress(), properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetFeatures_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetFeatures") }
actual fun wgpuDeviceGetFeatures(device: WGPUDevice?, features: WGPUSupportedFeatures?): Unit {
    NativeEngine.callV2PP(wgpuDeviceGetFeatures_ADDR, device?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceGetLimits_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetLimits") }
actual fun wgpuDeviceGetLimits(device: WGPUDevice?, limits: WGPULimits?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuDeviceGetLimits_ADDR, device?.handler?.rawValue ?: 0L, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetLostFuture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetLostFuture") }
actual fun wgpuDeviceGetLostFuture(allocator: MemoryAllocator, device: WGPUDevice?): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(8uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceGetLostFuture_ADDR, 1, "u64:p", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuDeviceGetQueue_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceGetQueue") }
actual fun wgpuDeviceGetQueue(device: WGPUDevice?): WGPUQueue? {
    return NativeEngine.callP1P(wgpuDeviceGetQueue_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUQueue)
}

private val wgpuDeviceHasFeature_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceHasFeature") }
actual fun wgpuDeviceHasFeature(device: WGPUDevice?, feature: WGPUFeatureName): UInt {
    return NativeEngine.callI2PI(wgpuDeviceHasFeature_ADDR, device?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuDeviceImportSharedBufferMemory_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceImportSharedBufferMemory") }
actual fun wgpuDeviceImportSharedBufferMemory(device: WGPUDevice?, descriptor: WGPUSharedBufferMemoryDescriptor?): WGPUSharedBufferMemory? {
    return NativeEngine.callP2PP(wgpuDeviceImportSharedBufferMemory_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedBufferMemory)
}

private val wgpuDeviceImportSharedFence_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceImportSharedFence") }
actual fun wgpuDeviceImportSharedFence(device: WGPUDevice?, descriptor: WGPUSharedFenceDescriptor?): WGPUSharedFence? {
    return NativeEngine.callP2PP(wgpuDeviceImportSharedFence_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedFence)
}

private val wgpuDeviceImportSharedTextureMemory_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceImportSharedTextureMemory") }
actual fun wgpuDeviceImportSharedTextureMemory(device: WGPUDevice?, descriptor: WGPUSharedTextureMemoryDescriptor?): WGPUSharedTextureMemory? {
    return NativeEngine.callP2PP(wgpuDeviceImportSharedTextureMemory_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedTextureMemory)
}

private val wgpuDeviceInjectError_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceInjectError") }
actual fun wgpuDeviceInjectError(device: WGPUDevice?, type: WGPUErrorType, message: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(32uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    args.writeInt(type.toInt(), 8uL)
    val messageBytes = ByteArray(16)
    MemoryBuffer(message.handler, 16uL).readBytes(messageBytes, 0u, 0uL, 16uL)
    args.writeBytes(messageBytes, 0u, 16uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceInjectError_ADDR, 3, "v:p,u32,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuDevicePopErrorScope_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDevicePopErrorScope") }
actual fun wgpuDevicePopErrorScope(allocator: MemoryAllocator, device: WGPUDevice?, callbackInfo: WGPUPopErrorScopeCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(48uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 8uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDevicePopErrorScope_ADDR, 2, "u64:p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuDevicePushErrorScope_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDevicePushErrorScope") }
actual fun wgpuDevicePushErrorScope(device: WGPUDevice?, filter: WGPUErrorFilter): Unit {
    NativeEngine.callV2PI(wgpuDevicePushErrorScope_ADDR, device?.handler?.rawValue ?: 0L, filter.toInt())
    return
}

private val wgpuDeviceSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceSetLabel") }
actual fun wgpuDeviceSetLabel(device: WGPUDevice?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuDeviceSetLoggingCallback_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceSetLoggingCallback") }
actual fun wgpuDeviceSetLoggingCallback(device: WGPUDevice?, callbackInfo: WGPULoggingCallbackInfo): Unit {
    val args = MemoryAllocator().allocateBuffer(40uL)
    args.writeLong(device?.handler?.rawValue ?: 0L, 0uL)
    val callbackInfoBytes = ByteArray(32)
    MemoryBuffer(callbackInfo.handler, 32uL).readBytes(callbackInfoBytes, 0u, 0uL, 32uL)
    args.writeBytes(callbackInfoBytes, 0u, 8uL, 32uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuDeviceSetLoggingCallback_ADDR, 2, "v:p,s32@8(p,p,p,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuDeviceTick_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceTick") }
actual fun wgpuDeviceTick(device: WGPUDevice?): Unit {
    NativeEngine.callV1P(wgpuDeviceTick_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceValidateTextureDescriptor_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceValidateTextureDescriptor") }
actual fun wgpuDeviceValidateTextureDescriptor(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): Unit {
    NativeEngine.callV2PP(wgpuDeviceValidateTextureDescriptor_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceAddRef") }
actual fun wgpuDeviceAddRef(device: WGPUDevice?): Unit {
    NativeEngine.callV1P(wgpuDeviceAddRef_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuDeviceRelease") }
actual fun wgpuDeviceRelease(device: WGPUDevice?): Unit {
    NativeEngine.callV1P(wgpuDeviceRelease_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureDestroy") }
actual fun wgpuExternalTextureDestroy(externalTexture: WGPUExternalTexture?): Unit {
    NativeEngine.callV1P(wgpuExternalTextureDestroy_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureExpire_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureExpire") }
actual fun wgpuExternalTextureExpire(externalTexture: WGPUExternalTexture?): Unit {
    NativeEngine.callV1P(wgpuExternalTextureExpire_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureRefresh_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureRefresh") }
actual fun wgpuExternalTextureRefresh(externalTexture: WGPUExternalTexture?): Unit {
    NativeEngine.callV1P(wgpuExternalTextureRefresh_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureSetLabel") }
actual fun wgpuExternalTextureSetLabel(externalTexture: WGPUExternalTexture?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(externalTexture?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuExternalTextureSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuExternalTextureAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureAddRef") }
actual fun wgpuExternalTextureAddRef(externalTexture: WGPUExternalTexture?): Unit {
    NativeEngine.callV1P(wgpuExternalTextureAddRef_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuExternalTextureRelease") }
actual fun wgpuExternalTextureRelease(externalTexture: WGPUExternalTexture?): Unit {
    NativeEngine.callV1P(wgpuExternalTextureRelease_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceCreateSurface_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceCreateSurface") }
actual fun wgpuInstanceCreateSurface(instance: WGPUInstance?, descriptor: WGPUSurfaceDescriptor?): WGPUSurface? {
    return NativeEngine.callP2PP(wgpuInstanceCreateSurface_ADDR, instance?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSurface)
}

private val wgpuInstanceGetWGSLLanguageFeatures_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceGetWGSLLanguageFeatures") }
actual fun wgpuInstanceGetWGSLLanguageFeatures(instance: WGPUInstance?, features: WGPUSupportedWGSLLanguageFeatures?): Unit {
    NativeEngine.callV2PP(wgpuInstanceGetWGSLLanguageFeatures_ADDR, instance?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceHasWGSLLanguageFeature_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceHasWGSLLanguageFeature") }
actual fun wgpuInstanceHasWGSLLanguageFeature(instance: WGPUInstance?, feature: WGPUWGSLLanguageFeatureName): UInt {
    return NativeEngine.callI2PI(wgpuInstanceHasWGSLLanguageFeature_ADDR, instance?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuInstanceProcessEvents_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceProcessEvents") }
actual fun wgpuInstanceProcessEvents(instance: WGPUInstance?): Unit {
    NativeEngine.callV1P(wgpuInstanceProcessEvents_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceRequestAdapter_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceRequestAdapter") }
actual fun wgpuInstanceRequestAdapter(allocator: MemoryAllocator, instance: WGPUInstance?, options: WGPURequestAdapterOptions?, callbackInfo: WGPURequestAdapterCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(56uL)
    args.writeLong(instance?.handler?.rawValue ?: 0L, 0uL)
    args.writeLong(options?.handler?.rawValue ?: 0L, 8uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 16uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuInstanceRequestAdapter_ADDR, 3, "u64:p,p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuInstanceWaitAny_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceWaitAny") }
actual fun wgpuInstanceWaitAny(instance: WGPUInstance?, futureCount: ULong, futures: WGPUFutureWaitInfo?, timeoutNS: ULong): WGPUWaitStatus {
    return (NativeEngine.callI4PLPL(wgpuInstanceWaitAny_ADDR, instance?.handler?.rawValue ?: 0L, futureCount.toLong(), futures?.handler?.rawValue ?: 0L, timeoutNS.toLong()).toInt()).toUInt()
}

private val wgpuInstanceAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceAddRef") }
actual fun wgpuInstanceAddRef(instance: WGPUInstance?): Unit {
    NativeEngine.callV1P(wgpuInstanceAddRef_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuInstanceRelease") }
actual fun wgpuInstanceRelease(instance: WGPUInstance?): Unit {
    NativeEngine.callV1P(wgpuInstanceRelease_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuPipelineLayoutSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuPipelineLayoutSetLabel") }
actual fun wgpuPipelineLayoutSetLabel(pipelineLayout: WGPUPipelineLayout?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(pipelineLayout?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuPipelineLayoutSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuPipelineLayoutAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuPipelineLayoutAddRef") }
actual fun wgpuPipelineLayoutAddRef(pipelineLayout: WGPUPipelineLayout?): Unit {
    NativeEngine.callV1P(wgpuPipelineLayoutAddRef_ADDR, pipelineLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuPipelineLayoutRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuPipelineLayoutRelease") }
actual fun wgpuPipelineLayoutRelease(pipelineLayout: WGPUPipelineLayout?): Unit {
    NativeEngine.callV1P(wgpuPipelineLayoutRelease_ADDR, pipelineLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetDestroy") }
actual fun wgpuQuerySetDestroy(querySet: WGPUQuerySet?): Unit {
    NativeEngine.callV1P(wgpuQuerySetDestroy_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetGetCount_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetGetCount") }
actual fun wgpuQuerySetGetCount(querySet: WGPUQuerySet?): UInt {
    return NativeEngine.callI1P(wgpuQuerySetGetCount_ADDR, querySet?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuQuerySetGetType_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetGetType") }
actual fun wgpuQuerySetGetType(querySet: WGPUQuerySet?): WGPUQueryType {
    return (NativeEngine.callI1P(wgpuQuerySetGetType_ADDR, querySet?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuQuerySetSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetSetLabel") }
actual fun wgpuQuerySetSetLabel(querySet: WGPUQuerySet?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(querySet?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuQuerySetSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuQuerySetAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetAddRef") }
actual fun wgpuQuerySetAddRef(querySet: WGPUQuerySet?): Unit {
    NativeEngine.callV1P(wgpuQuerySetAddRef_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQuerySetRelease") }
actual fun wgpuQuerySetRelease(querySet: WGPUQuerySet?): Unit {
    NativeEngine.callV1P(wgpuQuerySetRelease_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueCopyExternalTextureForBrowser_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueCopyExternalTextureForBrowser") }
actual fun wgpuQueueCopyExternalTextureForBrowser(queue: WGPUQueue?, source: WGPUImageCopyExternalTexture?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    NativeEngine.callV5PPPPP(wgpuQueueCopyExternalTextureForBrowser_ADDR, queue?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L, options?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueCopyTextureForBrowser_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueCopyTextureForBrowser") }
actual fun wgpuQueueCopyTextureForBrowser(queue: WGPUQueue?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    NativeEngine.callV5PPPPP(wgpuQueueCopyTextureForBrowser_ADDR, queue?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L, options?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueOnSubmittedWorkDone_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueOnSubmittedWorkDone") }
actual fun wgpuQueueOnSubmittedWorkDone(allocator: MemoryAllocator, queue: WGPUQueue?, callbackInfo: WGPUQueueWorkDoneCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(48uL)
    args.writeLong(queue?.handler?.rawValue ?: 0L, 0uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 8uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuQueueOnSubmittedWorkDone_ADDR, 2, "u64:p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuQueueSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueSetLabel") }
actual fun wgpuQueueSetLabel(queue: WGPUQueue?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(queue?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuQueueSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuQueueSubmit_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueSubmit") }
actual fun wgpuQueueSubmit(queue: WGPUQueue?, commandCount: ULong, commands: NativeAddress?): Unit {
    NativeEngine.callV3PLP(wgpuQueueSubmit_ADDR, queue?.handler?.rawValue ?: 0L, commandCount.toLong(), commands.toAddress())
    return
}

private val wgpuQueueWriteBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueWriteBuffer") }
actual fun wgpuQueueWriteBuffer(queue: WGPUQueue?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    NativeEngine.callV5PPLPL(wgpuQueueWriteBuffer_ADDR, queue?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, bufferOffset.toLong(), data.toAddress(), size.toLong())
    return
}

private val wgpuQueueWriteTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueWriteTexture") }
actual fun wgpuQueueWriteTexture(queue: WGPUQueue?, destination: WGPUTexelCopyTextureInfo?, data: NativeAddress?, dataSize: ULong, dataLayout: WGPUTexelCopyBufferLayout?, writeSize: WGPUExtent3D?): Unit {
    NativeEngine.callV6PPPLPP(wgpuQueueWriteTexture_ADDR, queue?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, data.toAddress(), dataSize.toLong(), dataLayout?.handler?.rawValue ?: 0L, writeSize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueAddRef") }
actual fun wgpuQueueAddRef(queue: WGPUQueue?): Unit {
    NativeEngine.callV1P(wgpuQueueAddRef_ADDR, queue?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuQueueRelease") }
actual fun wgpuQueueRelease(queue: WGPUQueue?): Unit {
    NativeEngine.callV1P(wgpuQueueRelease_ADDR, queue?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleSetLabel") }
actual fun wgpuRenderBundleSetLabel(renderBundle: WGPURenderBundle?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderBundle?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderBundleSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderBundleAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleAddRef") }
actual fun wgpuRenderBundleAddRef(renderBundle: WGPURenderBundle?): Unit {
    NativeEngine.callV1P(wgpuRenderBundleAddRef_ADDR, renderBundle?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleRelease") }
actual fun wgpuRenderBundleRelease(renderBundle: WGPURenderBundle?): Unit {
    NativeEngine.callV1P(wgpuRenderBundleRelease_ADDR, renderBundle?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderDraw_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderDraw") }
actual fun wgpuRenderBundleEncoderDraw(renderBundleEncoder: WGPURenderBundleEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    NativeEngine.callV5PIIII(wgpuRenderBundleEncoderDraw_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, vertexCount.toInt(), instanceCount.toInt(), firstVertex.toInt(), firstInstance.toInt())
    return
}

private val wgpuRenderBundleEncoderDrawIndexed_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderDrawIndexed") }
actual fun wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder: WGPURenderBundleEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    NativeEngine.callV6PIIIII(wgpuRenderBundleEncoderDrawIndexed_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indexCount.toInt(), instanceCount.toInt(), firstIndex.toInt(), baseVertex, firstInstance.toInt())
    return
}

private val wgpuRenderBundleEncoderDrawIndexedIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderDrawIndexedIndirect") }
actual fun wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    NativeEngine.callV3PPL(wgpuRenderBundleEncoderDrawIndexedIndirect_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderBundleEncoderDrawIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderDrawIndirect") }
actual fun wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    NativeEngine.callV3PPL(wgpuRenderBundleEncoderDrawIndirect_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderBundleEncoderFinish_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderFinish") }
actual fun wgpuRenderBundleEncoderFinish(renderBundleEncoder: WGPURenderBundleEncoder?, descriptor: WGPURenderBundleDescriptor?): WGPURenderBundle? {
    return NativeEngine.callP2PP(wgpuRenderBundleEncoderFinish_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderBundle)
}

private val wgpuRenderBundleEncoderInsertDebugMarker_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderInsertDebugMarker") }
actual fun wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder: WGPURenderBundleEncoder?, markerLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderBundleEncoder?.handler?.rawValue ?: 0L, 0uL)
    val markerLabelBytes = ByteArray(16)
    MemoryBuffer(markerLabel.handler, 16uL).readBytes(markerLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(markerLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderBundleEncoderInsertDebugMarker_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderBundleEncoderPopDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderPopDebugGroup") }
actual fun wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderBundleEncoderPopDebugGroup_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderPushDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderPushDebugGroup") }
actual fun wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderBundleEncoder?.handler?.rawValue ?: 0L, 0uL)
    val groupLabelBytes = ByteArray(16)
    MemoryBuffer(groupLabel.handler, 16uL).readBytes(groupLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(groupLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderBundleEncoderPushDebugGroup_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderBundleEncoderSetBindGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetBindGroup") }
actual fun wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    NativeEngine.callV5PIPLP(wgpuRenderBundleEncoderSetBindGroup_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets.toAddress())
    return
}

private val wgpuRenderBundleEncoderSetImmediates_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetImmediates") }
actual fun wgpuRenderBundleEncoderSetImmediates(renderBundleEncoder: WGPURenderBundleEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    NativeEngine.callV4PIPL(wgpuRenderBundleEncoderSetImmediates_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data.toAddress(), size.toLong())
    return
}

private val wgpuRenderBundleEncoderSetIndexBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetIndexBuffer") }
actual fun wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    NativeEngine.callV5PPILL(wgpuRenderBundleEncoderSetIndexBuffer_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, format.toInt(), offset.toLong(), size.toLong())
    return
}

private val wgpuRenderBundleEncoderSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetLabel") }
actual fun wgpuRenderBundleEncoderSetLabel(renderBundleEncoder: WGPURenderBundleEncoder?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderBundleEncoder?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderBundleEncoderSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderBundleEncoderSetPipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetPipeline") }
actual fun wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder: WGPURenderBundleEncoder?, pipeline: WGPURenderPipeline?): Unit {
    NativeEngine.callV2PP(wgpuRenderBundleEncoderSetPipeline_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderSetVertexBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderSetVertexBuffer") }
actual fun wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    NativeEngine.callV5PIPLL(wgpuRenderBundleEncoderSetVertexBuffer_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, slot.toInt(), buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuRenderBundleEncoderAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderAddRef") }
actual fun wgpuRenderBundleEncoderAddRef(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderBundleEncoderAddRef_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderBundleEncoderRelease") }
actual fun wgpuRenderBundleEncoderRelease(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderBundleEncoderRelease_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderBeginOcclusionQuery_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderBeginOcclusionQuery") }
actual fun wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?, queryIndex: UInt): Unit {
    NativeEngine.callV2PI(wgpuRenderPassEncoderBeginOcclusionQuery_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuRenderPassEncoderDraw_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderDraw") }
actual fun wgpuRenderPassEncoderDraw(renderPassEncoder: WGPURenderPassEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    NativeEngine.callV5PIIII(wgpuRenderPassEncoderDraw_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, vertexCount.toInt(), instanceCount.toInt(), firstVertex.toInt(), firstInstance.toInt())
    return
}

private val wgpuRenderPassEncoderDrawIndexed_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderDrawIndexed") }
actual fun wgpuRenderPassEncoderDrawIndexed(renderPassEncoder: WGPURenderPassEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    NativeEngine.callV6PIIIII(wgpuRenderPassEncoderDrawIndexed_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indexCount.toInt(), instanceCount.toInt(), firstIndex.toInt(), baseVertex, firstInstance.toInt())
    return
}

private val wgpuRenderPassEncoderDrawIndexedIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderDrawIndexedIndirect") }
actual fun wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    NativeEngine.callV3PPL(wgpuRenderPassEncoderDrawIndexedIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderPassEncoderDrawIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderDrawIndirect") }
actual fun wgpuRenderPassEncoderDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    NativeEngine.callV3PPL(wgpuRenderPassEncoderDrawIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderPassEncoderEnd_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderEnd") }
actual fun wgpuRenderPassEncoderEnd(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderEnd_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderEndOcclusionQuery_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderEndOcclusionQuery") }
actual fun wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderEndOcclusionQuery_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderExecuteBundles_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderExecuteBundles") }
actual fun wgpuRenderPassEncoderExecuteBundles(renderPassEncoder: WGPURenderPassEncoder?, bundleCount: ULong, bundles: NativeAddress?): Unit {
    NativeEngine.callV3PLP(wgpuRenderPassEncoderExecuteBundles_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, bundleCount.toLong(), bundles.toAddress())
    return
}

private val wgpuRenderPassEncoderInsertDebugMarker_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderInsertDebugMarker") }
actual fun wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder: WGPURenderPassEncoder?, markerLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderPassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val markerLabelBytes = ByteArray(16)
    MemoryBuffer(markerLabel.handler, 16uL).readBytes(markerLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(markerLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderPassEncoderInsertDebugMarker_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderPassEncoderMultiDrawIndexedIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderMultiDrawIndexedIndirect") }
actual fun wgpuRenderPassEncoderMultiDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    NativeEngine.callV6PPLIPL(wgpuRenderPassEncoderMultiDrawIndexedIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong(), maxDrawCount.toInt(), drawCountBuffer?.handler?.rawValue ?: 0L, drawCountBufferOffset.toLong())
    return
}

private val wgpuRenderPassEncoderMultiDrawIndirect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderMultiDrawIndirect") }
actual fun wgpuRenderPassEncoderMultiDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    NativeEngine.callV6PPLIPL(wgpuRenderPassEncoderMultiDrawIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong(), maxDrawCount.toInt(), drawCountBuffer?.handler?.rawValue ?: 0L, drawCountBufferOffset.toLong())
    return
}

private val wgpuRenderPassEncoderPixelLocalStorageBarrier_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderPixelLocalStorageBarrier") }
actual fun wgpuRenderPassEncoderPixelLocalStorageBarrier(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderPixelLocalStorageBarrier_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderPopDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderPopDebugGroup") }
actual fun wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderPopDebugGroup_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderPushDebugGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderPushDebugGroup") }
actual fun wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder: WGPURenderPassEncoder?, groupLabel: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderPassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val groupLabelBytes = ByteArray(16)
    MemoryBuffer(groupLabel.handler, 16uL).readBytes(groupLabelBytes, 0u, 0uL, 16uL)
    args.writeBytes(groupLabelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderPassEncoderPushDebugGroup_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderPassEncoderSetBindGroup_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetBindGroup") }
actual fun wgpuRenderPassEncoderSetBindGroup(renderPassEncoder: WGPURenderPassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    NativeEngine.callV5PIPLP(wgpuRenderPassEncoderSetBindGroup_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets.toAddress())
    return
}

private val wgpuRenderPassEncoderSetBlendConstant_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetBlendConstant") }
actual fun wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder: WGPURenderPassEncoder?, color: WGPUColor?): Unit {
    NativeEngine.callV2PP(wgpuRenderPassEncoderSetBlendConstant_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, color?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetImmediates_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetImmediates") }
actual fun wgpuRenderPassEncoderSetImmediates(renderPassEncoder: WGPURenderPassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    NativeEngine.callV4PIPL(wgpuRenderPassEncoderSetImmediates_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data.toAddress(), size.toLong())
    return
}

private val wgpuRenderPassEncoderSetIndexBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetIndexBuffer") }
actual fun wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder: WGPURenderPassEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    NativeEngine.callV5PPILL(wgpuRenderPassEncoderSetIndexBuffer_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, format.toInt(), offset.toLong(), size.toLong())
    return
}

private val wgpuRenderPassEncoderSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetLabel") }
actual fun wgpuRenderPassEncoderSetLabel(renderPassEncoder: WGPURenderPassEncoder?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderPassEncoder?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderPassEncoderSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderPassEncoderSetPipeline_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetPipeline") }
actual fun wgpuRenderPassEncoderSetPipeline(renderPassEncoder: WGPURenderPassEncoder?, pipeline: WGPURenderPipeline?): Unit {
    NativeEngine.callV2PP(wgpuRenderPassEncoderSetPipeline_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetResourceTable_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetResourceTable") }
actual fun wgpuRenderPassEncoderSetResourceTable(renderPassEncoder: WGPURenderPassEncoder?, table: WGPUResourceTable?): Unit {
    NativeEngine.callV2PP(wgpuRenderPassEncoderSetResourceTable_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, table?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetScissorRect_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetScissorRect") }
actual fun wgpuRenderPassEncoderSetScissorRect(renderPassEncoder: WGPURenderPassEncoder?, x: UInt, y: UInt, width: UInt, height: UInt): Unit {
    NativeEngine.callV5PIIII(wgpuRenderPassEncoderSetScissorRect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, x.toInt(), y.toInt(), width.toInt(), height.toInt())
    return
}

private val wgpuRenderPassEncoderSetStencilReference_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetStencilReference") }
actual fun wgpuRenderPassEncoderSetStencilReference(renderPassEncoder: WGPURenderPassEncoder?, reference: UInt): Unit {
    NativeEngine.callV2PI(wgpuRenderPassEncoderSetStencilReference_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, reference.toInt())
    return
}

private val wgpuRenderPassEncoderSetVertexBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetVertexBuffer") }
actual fun wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder: WGPURenderPassEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    NativeEngine.callV5PIPLL(wgpuRenderPassEncoderSetVertexBuffer_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, slot.toInt(), buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuRenderPassEncoderSetViewport_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderSetViewport") }
actual fun wgpuRenderPassEncoderSetViewport(renderPassEncoder: WGPURenderPassEncoder?, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float): Unit {
    NativeEngine.callV7PFFFFFF(wgpuRenderPassEncoderSetViewport_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, x, y, width, height, minDepth, maxDepth)
    return
}

private val wgpuRenderPassEncoderWriteTimestamp_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderWriteTimestamp") }
actual fun wgpuRenderPassEncoderWriteTimestamp(renderPassEncoder: WGPURenderPassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    NativeEngine.callV3PPI(wgpuRenderPassEncoderWriteTimestamp_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuRenderPassEncoderAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderAddRef") }
actual fun wgpuRenderPassEncoderAddRef(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderAddRef_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPassEncoderRelease") }
actual fun wgpuRenderPassEncoderRelease(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    NativeEngine.callV1P(wgpuRenderPassEncoderRelease_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPipelineGetBindGroupLayout_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPipelineGetBindGroupLayout") }
actual fun wgpuRenderPipelineGetBindGroupLayout(renderPipeline: WGPURenderPipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return NativeEngine.callP2PI(wgpuRenderPipelineGetBindGroupLayout_ADDR, renderPipeline?.handler?.rawValue ?: 0L, groupIndex.toInt()).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuRenderPipelineSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPipelineSetLabel") }
actual fun wgpuRenderPipelineSetLabel(renderPipeline: WGPURenderPipeline?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(renderPipeline?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuRenderPipelineSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuRenderPipelineAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPipelineAddRef") }
actual fun wgpuRenderPipelineAddRef(renderPipeline: WGPURenderPipeline?): Unit {
    NativeEngine.callV1P(wgpuRenderPipelineAddRef_ADDR, renderPipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPipelineRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuRenderPipelineRelease") }
actual fun wgpuRenderPipelineRelease(renderPipeline: WGPURenderPipeline?): Unit {
    NativeEngine.callV1P(wgpuRenderPipelineRelease_ADDR, renderPipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableDestroy") }
actual fun wgpuResourceTableDestroy(resourceTable: WGPUResourceTable?): Unit {
    NativeEngine.callV1P(wgpuResourceTableDestroy_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableGetSize_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableGetSize") }
actual fun wgpuResourceTableGetSize(resourceTable: WGPUResourceTable?): UInt {
    return NativeEngine.callI1P(wgpuResourceTableGetSize_ADDR, resourceTable?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuResourceTableInsert_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableInsert") }
actual fun wgpuResourceTableInsert(resourceTable: WGPUResourceTable?, resource: WGPUBindingResource?): UInt {
    return NativeEngine.callI2PP(wgpuResourceTableInsert_ADDR, resourceTable?.handler?.rawValue ?: 0L, resource?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuResourceTableRemove_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableRemove") }
actual fun wgpuResourceTableRemove(resourceTable: WGPUResourceTable?, slot: UInt): WGPUStatus {
    return (NativeEngine.callI2PI(wgpuResourceTableRemove_ADDR, resourceTable?.handler?.rawValue ?: 0L, slot.toInt()).toInt()).toUInt()
}

private val wgpuResourceTableSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableSetLabel") }
actual fun wgpuResourceTableSetLabel(resourceTable: WGPUResourceTable?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(resourceTable?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuResourceTableSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuResourceTableUpdate_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableUpdate") }
actual fun wgpuResourceTableUpdate(resourceTable: WGPUResourceTable?, slot: UInt, resource: WGPUBindingResource?): WGPUStatus {
    return (NativeEngine.callI3PIP(wgpuResourceTableUpdate_ADDR, resourceTable?.handler?.rawValue ?: 0L, slot.toInt(), resource?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuResourceTableAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableAddRef") }
actual fun wgpuResourceTableAddRef(resourceTable: WGPUResourceTable?): Unit {
    NativeEngine.callV1P(wgpuResourceTableAddRef_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuResourceTableRelease") }
actual fun wgpuResourceTableRelease(resourceTable: WGPUResourceTable?): Unit {
    NativeEngine.callV1P(wgpuResourceTableRelease_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSamplerSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSamplerSetLabel") }
actual fun wgpuSamplerSetLabel(sampler: WGPUSampler?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(sampler?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSamplerSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSamplerAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSamplerAddRef") }
actual fun wgpuSamplerAddRef(sampler: WGPUSampler?): Unit {
    NativeEngine.callV1P(wgpuSamplerAddRef_ADDR, sampler?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSamplerRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSamplerRelease") }
actual fun wgpuSamplerRelease(sampler: WGPUSampler?): Unit {
    NativeEngine.callV1P(wgpuSamplerRelease_ADDR, sampler?.handler?.rawValue ?: 0L)
    return
}

private val wgpuShaderModuleGetCompilationInfo_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuShaderModuleGetCompilationInfo") }
actual fun wgpuShaderModuleGetCompilationInfo(allocator: MemoryAllocator, shaderModule: WGPUShaderModule?, callbackInfo: WGPUCompilationInfoCallbackInfo): WGPUFuture {
    val args = MemoryAllocator().allocateBuffer(48uL)
    args.writeLong(shaderModule?.handler?.rawValue ?: 0L, 0uL)
    val callbackInfoBytes = ByteArray(40)
    MemoryBuffer(callbackInfo.handler, 40uL).readBytes(callbackInfoBytes, 0u, 0uL, 40uL)
    args.writeBytes(callbackInfoBytes, 0u, 8uL, 40uL)
    val out = allocator.allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuShaderModuleGetCompilationInfo_ADDR, 2, "u64:p,s40@8(p,u32,p,p,p)", args.handler.rawValue, out.handler.rawValue)
    return WGPUFuture.ByValue(out.handler)
}

private val wgpuShaderModuleSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuShaderModuleSetLabel") }
actual fun wgpuShaderModuleSetLabel(shaderModule: WGPUShaderModule?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(shaderModule?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuShaderModuleSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuShaderModuleAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuShaderModuleAddRef") }
actual fun wgpuShaderModuleAddRef(shaderModule: WGPUShaderModule?): Unit {
    NativeEngine.callV1P(wgpuShaderModuleAddRef_ADDR, shaderModule?.handler?.rawValue ?: 0L)
    return
}

private val wgpuShaderModuleRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuShaderModuleRelease") }
actual fun wgpuShaderModuleRelease(shaderModule: WGPUShaderModule?): Unit {
    NativeEngine.callV1P(wgpuShaderModuleRelease_ADDR, shaderModule?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryBeginAccess_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryBeginAccess") }
actual fun wgpuSharedBufferMemoryBeginAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryBeginAccessDescriptor?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuSharedBufferMemoryBeginAccess_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryCreateBuffer_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryCreateBuffer") }
actual fun wgpuSharedBufferMemoryCreateBuffer(sharedBufferMemory: WGPUSharedBufferMemory?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return NativeEngine.callP2PP(wgpuSharedBufferMemoryCreateBuffer_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuSharedBufferMemoryEndAccess_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryEndAccess") }
actual fun wgpuSharedBufferMemoryEndAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryEndAccessState?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuSharedBufferMemoryEndAccess_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryGetProperties_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryGetProperties") }
actual fun wgpuSharedBufferMemoryGetProperties(sharedBufferMemory: WGPUSharedBufferMemory?, properties: WGPUSharedBufferMemoryProperties?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuSharedBufferMemoryGetProperties_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryIsDeviceLost_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryIsDeviceLost") }
actual fun wgpuSharedBufferMemoryIsDeviceLost(sharedBufferMemory: WGPUSharedBufferMemory?): UInt {
    return NativeEngine.callI1P(wgpuSharedBufferMemoryIsDeviceLost_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuSharedBufferMemorySetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemorySetLabel") }
actual fun wgpuSharedBufferMemorySetLabel(sharedBufferMemory: WGPUSharedBufferMemory?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(sharedBufferMemory?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSharedBufferMemorySetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSharedBufferMemoryAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryAddRef") }
actual fun wgpuSharedBufferMemoryAddRef(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    NativeEngine.callV1P(wgpuSharedBufferMemoryAddRef_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryRelease") }
actual fun wgpuSharedBufferMemoryRelease(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    NativeEngine.callV1P(wgpuSharedBufferMemoryRelease_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryEndAccessStateFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedBufferMemoryEndAccessStateFreeMembers") }
actual fun wgpuSharedBufferMemoryEndAccessStateFreeMembers(sharedBufferMemoryEndAccessState: WGPUSharedBufferMemoryEndAccessState): Unit {
    val args = MemoryAllocator().allocateBuffer(48uL)
    val sharedBufferMemoryEndAccessStateBytes = ByteArray(48)
    MemoryBuffer(sharedBufferMemoryEndAccessState.handler, 48uL).readBytes(sharedBufferMemoryEndAccessStateBytes, 0u, 0uL, 48uL)
    args.writeBytes(sharedBufferMemoryEndAccessStateBytes, 0u, 0uL, 48uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSharedBufferMemoryEndAccessStateFreeMembers_ADDR, 1, "v:s48@8(p,i32,i64,p,i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSharedFenceExportInfo_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedFenceExportInfo") }
actual fun wgpuSharedFenceExportInfo(sharedFence: WGPUSharedFence?, info: WGPUSharedFenceExportInfo?): Unit {
    NativeEngine.callV2PP(wgpuSharedFenceExportInfo_ADDR, sharedFence?.handler?.rawValue ?: 0L, info?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedFenceSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedFenceSetLabel") }
actual fun wgpuSharedFenceSetLabel(sharedFence: WGPUSharedFence?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(sharedFence?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSharedFenceSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSharedFenceAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedFenceAddRef") }
actual fun wgpuSharedFenceAddRef(sharedFence: WGPUSharedFence?): Unit {
    NativeEngine.callV1P(wgpuSharedFenceAddRef_ADDR, sharedFence?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedFenceRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedFenceRelease") }
actual fun wgpuSharedFenceRelease(sharedFence: WGPUSharedFence?): Unit {
    NativeEngine.callV1P(wgpuSharedFenceRelease_ADDR, sharedFence?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryBeginAccess_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryBeginAccess") }
actual fun wgpuSharedTextureMemoryBeginAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryBeginAccessDescriptor?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuSharedTextureMemoryBeginAccess_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryCreateTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryCreateTexture") }
actual fun wgpuSharedTextureMemoryCreateTexture(sharedTextureMemory: WGPUSharedTextureMemory?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return NativeEngine.callP2PP(wgpuSharedTextureMemoryCreateTexture_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuSharedTextureMemoryEndAccess_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryEndAccess") }
actual fun wgpuSharedTextureMemoryEndAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryEndAccessState?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuSharedTextureMemoryEndAccess_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryGetProperties_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryGetProperties") }
actual fun wgpuSharedTextureMemoryGetProperties(sharedTextureMemory: WGPUSharedTextureMemory?, properties: WGPUSharedTextureMemoryProperties?): WGPUStatus {
    return (NativeEngine.callI2PP(wgpuSharedTextureMemoryGetProperties_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryIsDeviceLost_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryIsDeviceLost") }
actual fun wgpuSharedTextureMemoryIsDeviceLost(sharedTextureMemory: WGPUSharedTextureMemory?): UInt {
    return NativeEngine.callI1P(wgpuSharedTextureMemoryIsDeviceLost_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuSharedTextureMemorySetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemorySetLabel") }
actual fun wgpuSharedTextureMemorySetLabel(sharedTextureMemory: WGPUSharedTextureMemory?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(sharedTextureMemory?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSharedTextureMemorySetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSharedTextureMemoryAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryAddRef") }
actual fun wgpuSharedTextureMemoryAddRef(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    NativeEngine.callV1P(wgpuSharedTextureMemoryAddRef_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryRelease") }
actual fun wgpuSharedTextureMemoryRelease(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    NativeEngine.callV1P(wgpuSharedTextureMemoryRelease_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryEndAccessStateFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSharedTextureMemoryEndAccessStateFreeMembers") }
actual fun wgpuSharedTextureMemoryEndAccessStateFreeMembers(sharedTextureMemoryEndAccessState: WGPUSharedTextureMemoryEndAccessState): Unit {
    val args = MemoryAllocator().allocateBuffer(48uL)
    val sharedTextureMemoryEndAccessStateBytes = ByteArray(48)
    MemoryBuffer(sharedTextureMemoryEndAccessState.handler, 48uL).readBytes(sharedTextureMemoryEndAccessStateBytes, 0u, 0uL, 48uL)
    args.writeBytes(sharedTextureMemoryEndAccessStateBytes, 0u, 0uL, 48uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSharedTextureMemoryEndAccessStateFreeMembers_ADDR, 1, "v:s48@8(p,i32,i64,p,i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSupportedFeaturesFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSupportedFeaturesFreeMembers") }
actual fun wgpuSupportedFeaturesFreeMembers(supportedFeatures: WGPUSupportedFeatures): Unit {
    val args = MemoryAllocator().allocateBuffer(16uL)
    val supportedFeaturesBytes = ByteArray(16)
    MemoryBuffer(supportedFeatures.handler, 16uL).readBytes(supportedFeaturesBytes, 0u, 0uL, 16uL)
    args.writeBytes(supportedFeaturesBytes, 0u, 0uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSupportedFeaturesFreeMembers_ADDR, 1, "v:s16@8(i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSupportedInstanceFeaturesFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSupportedInstanceFeaturesFreeMembers") }
actual fun wgpuSupportedInstanceFeaturesFreeMembers(supportedInstanceFeatures: WGPUSupportedInstanceFeatures): Unit {
    val args = MemoryAllocator().allocateBuffer(16uL)
    val supportedInstanceFeaturesBytes = ByteArray(16)
    MemoryBuffer(supportedInstanceFeatures.handler, 16uL).readBytes(supportedInstanceFeaturesBytes, 0u, 0uL, 16uL)
    args.writeBytes(supportedInstanceFeaturesBytes, 0u, 0uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSupportedInstanceFeaturesFreeMembers_ADDR, 1, "v:s16@8(i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSupportedWGSLLanguageFeaturesFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSupportedWGSLLanguageFeaturesFreeMembers") }
actual fun wgpuSupportedWGSLLanguageFeaturesFreeMembers(supportedWGSLLanguageFeatures: WGPUSupportedWGSLLanguageFeatures): Unit {
    val args = MemoryAllocator().allocateBuffer(16uL)
    val supportedWGSLLanguageFeaturesBytes = ByteArray(16)
    MemoryBuffer(supportedWGSLLanguageFeatures.handler, 16uL).readBytes(supportedWGSLLanguageFeaturesBytes, 0u, 0uL, 16uL)
    args.writeBytes(supportedWGSLLanguageFeaturesBytes, 0u, 0uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSupportedWGSLLanguageFeaturesFreeMembers_ADDR, 1, "v:s16@8(i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSurfaceConfigure_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceConfigure") }
actual fun wgpuSurfaceConfigure(surface: WGPUSurface?, config: WGPUSurfaceConfiguration?): Unit {
    NativeEngine.callV2PP(wgpuSurfaceConfigure_ADDR, surface?.handler?.rawValue ?: 0L, config?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceGetCapabilities_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceGetCapabilities") }
actual fun wgpuSurfaceGetCapabilities(surface: WGPUSurface?, adapter: WGPUAdapter?, capabilities: WGPUSurfaceCapabilities?): WGPUStatus {
    return (NativeEngine.callI3PPP(wgpuSurfaceGetCapabilities_ADDR, surface?.handler?.rawValue ?: 0L, adapter?.handler?.rawValue ?: 0L, capabilities?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSurfaceGetCurrentTexture_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceGetCurrentTexture") }
actual fun wgpuSurfaceGetCurrentTexture(surface: WGPUSurface?, surfaceTexture: WGPUSurfaceTexture?): Unit {
    NativeEngine.callV2PP(wgpuSurfaceGetCurrentTexture_ADDR, surface?.handler?.rawValue ?: 0L, surfaceTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfacePresent_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfacePresent") }
actual fun wgpuSurfacePresent(surface: WGPUSurface?): WGPUStatus {
    return (NativeEngine.callI1P(wgpuSurfacePresent_ADDR, surface?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSurfaceSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceSetLabel") }
actual fun wgpuSurfaceSetLabel(surface: WGPUSurface?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(surface?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSurfaceSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuSurfaceUnconfigure_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceUnconfigure") }
actual fun wgpuSurfaceUnconfigure(surface: WGPUSurface?): Unit {
    NativeEngine.callV1P(wgpuSurfaceUnconfigure_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceAddRef") }
actual fun wgpuSurfaceAddRef(surface: WGPUSurface?): Unit {
    NativeEngine.callV1P(wgpuSurfaceAddRef_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceRelease") }
actual fun wgpuSurfaceRelease(surface: WGPUSurface?): Unit {
    NativeEngine.callV1P(wgpuSurfaceRelease_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceCapabilitiesFreeMembers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuSurfaceCapabilitiesFreeMembers") }
actual fun wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities: WGPUSurfaceCapabilities): Unit {
    val args = MemoryAllocator().allocateBuffer(64uL)
    val surfaceCapabilitiesBytes = ByteArray(64)
    MemoryBuffer(surfaceCapabilities.handler, 64uL).readBytes(surfaceCapabilitiesBytes, 0u, 0uL, 64uL)
    args.writeBytes(surfaceCapabilitiesBytes, 0u, 0uL, 64uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuSurfaceCapabilitiesFreeMembers_ADDR, 1, "v:s64@8(p,i64,i64,p,i64,p,i64,p)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuTexelBufferViewSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTexelBufferViewSetLabel") }
actual fun wgpuTexelBufferViewSetLabel(texelBufferView: WGPUTexelBufferView?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(texelBufferView?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuTexelBufferViewSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuTexelBufferViewAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTexelBufferViewAddRef") }
actual fun wgpuTexelBufferViewAddRef(texelBufferView: WGPUTexelBufferView?): Unit {
    NativeEngine.callV1P(wgpuTexelBufferViewAddRef_ADDR, texelBufferView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTexelBufferViewRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTexelBufferViewRelease") }
actual fun wgpuTexelBufferViewRelease(texelBufferView: WGPUTexelBufferView?): Unit {
    NativeEngine.callV1P(wgpuTexelBufferViewRelease_ADDR, texelBufferView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureCreateErrorView_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureCreateErrorView") }
actual fun wgpuTextureCreateErrorView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return NativeEngine.callP2PP(wgpuTextureCreateErrorView_ADDR, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTextureView)
}

private val wgpuTextureCreateView_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureCreateView") }
actual fun wgpuTextureCreateView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return NativeEngine.callP2PP(wgpuTextureCreateView_ADDR, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTextureView)
}

private val wgpuTextureDestroy_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureDestroy") }
actual fun wgpuTextureDestroy(texture: WGPUTexture?): Unit {
    NativeEngine.callV1P(wgpuTextureDestroy_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureGetDepthOrArrayLayers_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetDepthOrArrayLayers") }
actual fun wgpuTextureGetDepthOrArrayLayers(texture: WGPUTexture?): UInt {
    return NativeEngine.callI1P(wgpuTextureGetDepthOrArrayLayers_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetDimension_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetDimension") }
actual fun wgpuTextureGetDimension(texture: WGPUTexture?): WGPUTextureDimension {
    return (NativeEngine.callI1P(wgpuTextureGetDimension_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetFormat_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetFormat") }
actual fun wgpuTextureGetFormat(texture: WGPUTexture?): WGPUTextureFormat {
    return (NativeEngine.callI1P(wgpuTextureGetFormat_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetHeight_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetHeight") }
actual fun wgpuTextureGetHeight(texture: WGPUTexture?): UInt {
    return NativeEngine.callI1P(wgpuTextureGetHeight_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetMipLevelCount_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetMipLevelCount") }
actual fun wgpuTextureGetMipLevelCount(texture: WGPUTexture?): UInt {
    return NativeEngine.callI1P(wgpuTextureGetMipLevelCount_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetSampleCount_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetSampleCount") }
actual fun wgpuTextureGetSampleCount(texture: WGPUTexture?): UInt {
    return NativeEngine.callI1P(wgpuTextureGetSampleCount_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetTextureBindingViewDimension_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetTextureBindingViewDimension") }
actual fun wgpuTextureGetTextureBindingViewDimension(texture: WGPUTexture?): WGPUTextureViewDimension {
    return (NativeEngine.callI1P(wgpuTextureGetTextureBindingViewDimension_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetUsage_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetUsage") }
actual fun wgpuTextureGetUsage(texture: WGPUTexture?): ULong {
    return NativeEngine.callL1P(wgpuTextureGetUsage_ADDR, texture?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuTextureGetWidth_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureGetWidth") }
actual fun wgpuTextureGetWidth(texture: WGPUTexture?): UInt {
    return NativeEngine.callI1P(wgpuTextureGetWidth_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureSetLabel") }
actual fun wgpuTextureSetLabel(texture: WGPUTexture?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(texture?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuTextureSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuTextureSetOwnershipForMemoryDump_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureSetOwnershipForMemoryDump") }
actual fun wgpuTextureSetOwnershipForMemoryDump(texture: WGPUTexture?, ownerGuid: ULong): Unit {
    NativeEngine.callV2PL(wgpuTextureSetOwnershipForMemoryDump_ADDR, texture?.handler?.rawValue ?: 0L, ownerGuid.toLong())
    return
}

private val wgpuTextureAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureAddRef") }
actual fun wgpuTextureAddRef(texture: WGPUTexture?): Unit {
    NativeEngine.callV1P(wgpuTextureAddRef_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureRelease") }
actual fun wgpuTextureRelease(texture: WGPUTexture?): Unit {
    NativeEngine.callV1P(wgpuTextureRelease_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureViewSetLabel_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureViewSetLabel") }
actual fun wgpuTextureViewSetLabel(textureView: WGPUTextureView?, label: WGPUStringView): Unit {
    val args = MemoryAllocator().allocateBuffer(24uL)
    args.writeLong(textureView?.handler?.rawValue ?: 0L, 0uL)
    val labelBytes = ByteArray(16)
    MemoryBuffer(label.handler, 16uL).readBytes(labelBytes, 0u, 0uL, 16uL)
    args.writeBytes(labelBytes, 0u, 8uL, 16uL)
    val out = MemoryAllocator().allocateBuffer(8uL)
    NativeEngine.callGeneric(wgpuTextureViewSetLabel_ADDR, 2, "v:p,s16@8(p,i64)", args.handler.rawValue, out.handler.rawValue)
}

private val wgpuTextureViewAddRef_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureViewAddRef") }
actual fun wgpuTextureViewAddRef(textureView: WGPUTextureView?): Unit {
    NativeEngine.callV1P(wgpuTextureViewAddRef_ADDR, textureView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureViewRelease_ADDR: Long by lazy { KextractAndroidBootstrap.resolve("wgpuTextureViewRelease") }
actual fun wgpuTextureViewRelease(textureView: WGPUTextureView?): Unit {
    NativeEngine.callV1P(wgpuTextureViewRelease_ADDR, textureView?.handler?.rawValue ?: 0L)
    return
}

@OptIn(CallbackRuntimeApi::class)
private object WGPUCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(J)V",
            dispatchAbiSignature = "v(ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke()
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCallback,
): CallbackRegistration<WGPUCallback> = CallbackRuntime.register(
    type = WGPUCallbackType,
    trampoline = WGPUCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCallback,
): PreparedCallbackRegistration<WGPUCallback> = CallbackRuntime.prepare(
    type = WGPUCallbackType,
    trampoline = WGPUCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUDawnStoreCacheDataFunctionTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDawnStoreCacheDataFunctionTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JJJJJ)V",
            dispatchAbiSignature = "v(ptr,u64,ptr,u64,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, key: Long, keySize: Long, value: Long, valueSize: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDawnStoreCacheDataFunctionType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    key.takeIf { it != 0L }?.let { NativeAddress(it) },
                    keySize.toULong(),
                    value.takeIf { it != 0L }?.let { NativeAddress(it) },
                    valueSize.toULong(),
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDawnStoreCacheDataFunction.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataFunction,
): CallbackRegistration<WGPUDawnStoreCacheDataFunction> = CallbackRuntime.register(
    type = WGPUDawnStoreCacheDataFunctionType,
    trampoline = WGPUDawnStoreCacheDataFunctionTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUDawnStoreCacheDataFunction.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataFunction,
): PreparedCallbackRegistration<WGPUDawnStoreCacheDataFunction> = CallbackRuntime.prepare(
    type = WGPUDawnStoreCacheDataFunctionType,
    trampoline = WGPUDawnStoreCacheDataFunctionTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUProcTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUProcTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "()V",
            dispatchAbiSignature = "v()",
        ))
    }
    
    @JvmStatic
    fun dispatch() {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUProcType,
                userdata = null,
            ) { callback ->
                callback.invoke()
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUProc.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUProc,
): CallbackRegistration<WGPUProc> = CallbackRuntime.register(
    type = WGPUProcType,
    trampoline = WGPUProcTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUProc.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUProc,
): PreparedCallbackRegistration<WGPUProc> = CallbackRuntime.prepare(
    type = WGPUProcType,
    trampoline = WGPUProcTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@UnsafeCallbackRearmApi
@OptIn(CallbackRuntimeApi::class)
actual fun WGPUProc.Companion.rearmAfterNativeQuiescence(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUProc,
): CallbackRegistration<WGPUProc> = CallbackRuntime.rearmAfterNativeQuiescence(
    type = WGPUProcType,
    trampoline = WGPUProcTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUBufferMapCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUBufferMapCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJ)V",
            dispatchAbiSignature = "v(u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUBufferMapCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUMapAsyncStatus,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUBufferMapCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUBufferMapCallback,
): CallbackRegistration<WGPUBufferMapCallback> = CallbackRuntime.register(
    type = WGPUBufferMapCallbackType,
    trampoline = WGPUBufferMapCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUBufferMapCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUBufferMapCallback,
): PreparedCallbackRegistration<WGPUBufferMapCallback> = CallbackRuntime.prepare(
    type = WGPUBufferMapCallbackType,
    trampoline = WGPUBufferMapCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUCompilationInfoCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCompilationInfoCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJ)V",
            dispatchAbiSignature = "v(u32,ptr,ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, compilationInfo: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCompilationInfoCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCompilationInfoRequestStatus,
                    compilationInfo.takeIf { it != 0L }?.let { NativeAddress(it) },
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCompilationInfoCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCompilationInfoCallback,
): CallbackRegistration<WGPUCompilationInfoCallback> = CallbackRuntime.register(
    type = WGPUCompilationInfoCallbackType,
    trampoline = WGPUCompilationInfoCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUCompilationInfoCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCompilationInfoCallback,
): PreparedCallbackRegistration<WGPUCompilationInfoCallback> = CallbackRuntime.prepare(
    type = WGPUCompilationInfoCallbackType,
    trampoline = WGPUCompilationInfoCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUCreateComputePipelineAsyncCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCreateComputePipelineAsyncCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJJ)V",
            dispatchAbiSignature = "v(u32,ptr,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, pipeline: Long, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCreateComputePipelineAsyncCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCreatePipelineAsyncStatus,
                    pipeline.takeIf { it != 0L }?.let { NativeAddress(it) }?.let { WGPUComputePipeline(it) },
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCreateComputePipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateComputePipelineAsyncCallback,
): CallbackRegistration<WGPUCreateComputePipelineAsyncCallback> = CallbackRuntime.register(
    type = WGPUCreateComputePipelineAsyncCallbackType,
    trampoline = WGPUCreateComputePipelineAsyncCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUCreateComputePipelineAsyncCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateComputePipelineAsyncCallback,
): PreparedCallbackRegistration<WGPUCreateComputePipelineAsyncCallback> = CallbackRuntime.prepare(
    type = WGPUCreateComputePipelineAsyncCallbackType,
    trampoline = WGPUCreateComputePipelineAsyncCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUCreateRenderPipelineAsyncCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCreateRenderPipelineAsyncCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJJ)V",
            dispatchAbiSignature = "v(u32,ptr,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, pipeline: Long, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCreateRenderPipelineAsyncCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCreatePipelineAsyncStatus,
                    pipeline.takeIf { it != 0L }?.let { NativeAddress(it) }?.let { WGPURenderPipeline(it) },
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCreateRenderPipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateRenderPipelineAsyncCallback,
): CallbackRegistration<WGPUCreateRenderPipelineAsyncCallback> = CallbackRuntime.register(
    type = WGPUCreateRenderPipelineAsyncCallbackType,
    trampoline = WGPUCreateRenderPipelineAsyncCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUCreateRenderPipelineAsyncCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateRenderPipelineAsyncCallback,
): PreparedCallbackRegistration<WGPUCreateRenderPipelineAsyncCallback> = CallbackRuntime.prepare(
    type = WGPUCreateRenderPipelineAsyncCallbackType,
    trampoline = WGPUCreateRenderPipelineAsyncCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUDawnStoreCacheDataCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDawnStoreCacheDataCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JJJJJJ)V",
            dispatchAbiSignature = "v(u64,ptr,u64,ptr,ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, keySize: Long, key: Long, valueSize: Long, value: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDawnStoreCacheDataCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    keySize.toULong(),
                    key.takeIf { it != 0L }?.let { NativeAddress(it) },
                    valueSize.toULong(),
                    value.takeIf { it != 0L }?.let { NativeAddress(it) },
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDawnStoreCacheDataCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataCallback,
): CallbackRegistration<WGPUDawnStoreCacheDataCallback> = CallbackRuntime.register(
    type = WGPUDawnStoreCacheDataCallbackType,
    trampoline = WGPUDawnStoreCacheDataCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUDawnStoreCacheDataCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataCallback,
): PreparedCallbackRegistration<WGPUDawnStoreCacheDataCallback> = CallbackRuntime.prepare(
    type = WGPUDawnStoreCacheDataCallbackType,
    trampoline = WGPUDawnStoreCacheDataCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUDeviceLostCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDeviceLostCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JJIJJ)V",
            dispatchAbiSignature = "v(ptr,u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, device: Long, reason: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDeviceLostCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    device.takeIf { it != 0L }?.let { NativeAddress(it) },
                    reason.toUInt() as WGPUDeviceLostReason,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDeviceLostCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDeviceLostCallback,
): CallbackRegistration<WGPUDeviceLostCallback> = CallbackRuntime.register(
    type = WGPUDeviceLostCallbackType,
    trampoline = WGPUDeviceLostCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUDeviceLostCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDeviceLostCallback,
): PreparedCallbackRegistration<WGPUDeviceLostCallback> = CallbackRuntime.prepare(
    type = WGPUDeviceLostCallbackType,
    trampoline = WGPUDeviceLostCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUDisposeCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDisposeCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJ)V",
            dispatchAbiSignature = "v(u32,ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDisposeCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCallbackStatus,
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDisposeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDisposeCallback,
): CallbackRegistration<WGPUDisposeCallback> = CallbackRuntime.register(
    type = WGPUDisposeCallbackType,
    trampoline = WGPUDisposeCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUDisposeCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDisposeCallback,
): PreparedCallbackRegistration<WGPUDisposeCallback> = CallbackRuntime.prepare(
    type = WGPUDisposeCallbackType,
    trampoline = WGPUDisposeCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPULoggingCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPULoggingCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJ)V",
            dispatchAbiSignature = "v(u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, type: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPULoggingCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    type.toUInt() as WGPULoggingType,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPULoggingCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPULoggingCallback,
): CallbackRegistration<WGPULoggingCallback> = CallbackRuntime.register(
    type = WGPULoggingCallbackType,
    trampoline = WGPULoggingCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPULoggingCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPULoggingCallback,
): PreparedCallbackRegistration<WGPULoggingCallback> = CallbackRuntime.prepare(
    type = WGPULoggingCallbackType,
    trampoline = WGPULoggingCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUPopErrorScopeCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUPopErrorScopeCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIIJJ)V",
            dispatchAbiSignature = "v(u32,u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, type: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUPopErrorScopeCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUPopErrorScopeStatus,
                    type.toUInt() as WGPUErrorType,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUPopErrorScopeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUPopErrorScopeCallback,
): CallbackRegistration<WGPUPopErrorScopeCallback> = CallbackRuntime.register(
    type = WGPUPopErrorScopeCallbackType,
    trampoline = WGPUPopErrorScopeCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUPopErrorScopeCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUPopErrorScopeCallback,
): PreparedCallbackRegistration<WGPUPopErrorScopeCallback> = CallbackRuntime.prepare(
    type = WGPUPopErrorScopeCallbackType,
    trampoline = WGPUPopErrorScopeCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUQueueWorkDoneCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUQueueWorkDoneCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJ)V",
            dispatchAbiSignature = "v(u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUQueueWorkDoneCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUQueueWorkDoneStatus,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUQueueWorkDoneCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUQueueWorkDoneCallback,
): CallbackRegistration<WGPUQueueWorkDoneCallback> = CallbackRuntime.register(
    type = WGPUQueueWorkDoneCallbackType,
    trampoline = WGPUQueueWorkDoneCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUQueueWorkDoneCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUQueueWorkDoneCallback,
): PreparedCallbackRegistration<WGPUQueueWorkDoneCallback> = CallbackRuntime.prepare(
    type = WGPUQueueWorkDoneCallbackType,
    trampoline = WGPUQueueWorkDoneCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPURequestAdapterCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPURequestAdapterCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJJ)V",
            dispatchAbiSignature = "v(u32,ptr,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, adapter: Long, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPURequestAdapterCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPURequestAdapterStatus,
                    adapter.takeIf { it != 0L }?.let { NativeAddress(it) }?.let { WGPUAdapter(it) },
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPURequestAdapterCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestAdapterCallback,
): CallbackRegistration<WGPURequestAdapterCallback> = CallbackRuntime.register(
    type = WGPURequestAdapterCallbackType,
    trampoline = WGPURequestAdapterCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPURequestAdapterCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestAdapterCallback,
): PreparedCallbackRegistration<WGPURequestAdapterCallback> = CallbackRuntime.prepare(
    type = WGPURequestAdapterCallbackType,
    trampoline = WGPURequestAdapterCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPURequestDeviceCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPURequestDeviceCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JIJJJ)V",
            dispatchAbiSignature = "v(u32,ptr,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, status: Int, device: Long, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPURequestDeviceCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPURequestDeviceStatus,
                    device.takeIf { it != 0L }?.let { NativeAddress(it) }?.let { WGPUDevice(it) },
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPURequestDeviceCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestDeviceCallback,
): CallbackRegistration<WGPURequestDeviceCallback> = CallbackRuntime.register(
    type = WGPURequestDeviceCallbackType,
    trampoline = WGPURequestDeviceCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPURequestDeviceCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestDeviceCallback,
): PreparedCallbackRegistration<WGPURequestDeviceCallback> = CallbackRuntime.prepare(
    type = WGPURequestDeviceCallbackType,
    trampoline = WGPURequestDeviceCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private object WGPUUncapturedErrorCallbackTrampoline {
    val address: NativeAddress by lazy {
        NativeAddress(UpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUUncapturedErrorCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchJvmSignature = "(JJIJJ)V",
            dispatchAbiSignature = "v(ptr,u32,struct(ptr,u64),ptr,ptr)",
        ))
    }
    
    @JvmStatic
    fun dispatch(token: Long, device: Long, type: Int, message: Long, userdata1: Long) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUUncapturedErrorCallbackType,
                userdata = NativeAddress(token),
            ) { callback ->
                callback.invoke(
                    device.takeIf { it != 0L }?.let { NativeAddress(it) },
                    type.toUInt() as WGPUErrorType,
                    WGPUStringView.ByValue(NativeAddress(message)),
                    userdata1.takeIf { it != 0L }?.let { NativeAddress(it) },
                )
            }
        } catch (failure: Throwable) {
            CallbackRuntime.reportUnroutedFailure(failure)
        }
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUUncapturedErrorCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUUncapturedErrorCallback,
): CallbackRegistration<WGPUUncapturedErrorCallback> = CallbackRuntime.register(
    type = WGPUUncapturedErrorCallbackType,
    trampoline = WGPUUncapturedErrorCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
internal actual fun WGPUUncapturedErrorCallback.Companion.prepare(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUUncapturedErrorCallback,
): PreparedCallbackRegistration<WGPUUncapturedErrorCallback> = CallbackRuntime.prepare(
    type = WGPUUncapturedErrorCallbackType,
    trampoline = WGPUUncapturedErrorCallbackTrampoline.address,
    policy = policy,
    onError = onError,
    callback = callback,
)

