@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.raw

import org.graphiks.kffi.NativeAddress
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
import org.graphiks.kffi.toCString
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.COpaquePointer
import kotlinx.cinterop.COpaquePointerVar
import kotlinx.cinterop.CValue
import kotlinx.cinterop.DoubleVar
import kotlinx.cinterop.FloatVar
import kotlinx.cinterop.IntVar
import kotlinx.cinterop.LongVar
import kotlinx.cinterop.ShortVar
import kotlinx.cinterop.UByteVar
import kotlinx.cinterop.UIntVar
import kotlinx.cinterop.ULongVar
import kotlinx.cinterop.UShortVar
import kotlinx.cinterop.cValue
import kotlinx.cinterop.get
import kotlinx.cinterop.pointed
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.sizeOf
import kotlinx.cinterop.staticCFunction
import kotlinx.cinterop.useContents
import kotlin.OptIn

actual interface WGPUStringView {
    actual var data: CString?
    actual var length: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStringView = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStringView =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUStringView>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStringView) -> Unit): ArrayHolder<WGPUStringView> {
            val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUStringView>) : WGPUStringView {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var data: CString?
            get() = handle.useContents { this.data?.let { CString(NativeAddress.fromPointer(it)) } }
            set(value) { error("Setters not supported on ByValue") }
        override var length: ULong
            get() = handle.useContents { this.length }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUStringView {
        private val struct: webgpu.native.WGPUStringView
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUStringView>().pointed
        
        override var data: CString?
            get() = struct.data?.let { CString(NativeAddress.fromPointer(it)) }
            set(value) { struct.data = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var length: ULong
            get() = struct.length
            set(value) { struct.length = value }
    }
}

fun WGPUStringView.toCValue(): CValue<webgpu.native.WGPUStringView> = cValue {
    this.data = this@toCValue.data?.handler?.pointer?.takeIf { this@toCValue.data?.handler?.rawValue != 0L }?.reinterpret()
    this.length = this@toCValue.length
}

actual value class WGPUAdapter actual constructor(actual val handler: NativeAddress)

actual value class WGPUBindGroup actual constructor(actual val handler: NativeAddress)

actual value class WGPUBindGroupLayout actual constructor(actual val handler: NativeAddress)

actual value class WGPUBuffer actual constructor(actual val handler: NativeAddress)

actual value class WGPUCommandBuffer actual constructor(actual val handler: NativeAddress)

actual value class WGPUCommandEncoder actual constructor(actual val handler: NativeAddress)

actual value class WGPUComputePassEncoder actual constructor(actual val handler: NativeAddress)

actual value class WGPUComputePipeline actual constructor(actual val handler: NativeAddress)

actual value class WGPUDevice actual constructor(actual val handler: NativeAddress)

actual value class WGPUExternalTexture actual constructor(actual val handler: NativeAddress)

actual value class WGPUInstance actual constructor(actual val handler: NativeAddress)

actual value class WGPUPipelineLayout actual constructor(actual val handler: NativeAddress)

actual value class WGPUQuerySet actual constructor(actual val handler: NativeAddress)

actual value class WGPUQueue actual constructor(actual val handler: NativeAddress)

actual value class WGPURenderBundle actual constructor(actual val handler: NativeAddress)

actual value class WGPURenderBundleEncoder actual constructor(actual val handler: NativeAddress)

actual value class WGPURenderPassEncoder actual constructor(actual val handler: NativeAddress)

actual value class WGPURenderPipeline actual constructor(actual val handler: NativeAddress)

actual value class WGPUResourceTable actual constructor(actual val handler: NativeAddress)

actual value class WGPUSampler actual constructor(actual val handler: NativeAddress)

actual value class WGPUShaderModule actual constructor(actual val handler: NativeAddress)

actual value class WGPUSharedBufferMemory actual constructor(actual val handler: NativeAddress)

actual value class WGPUSharedFence actual constructor(actual val handler: NativeAddress)

actual value class WGPUSharedTextureMemory actual constructor(actual val handler: NativeAddress)

actual value class WGPUSurface actual constructor(actual val handler: NativeAddress)

actual value class WGPUTexelBufferView actual constructor(actual val handler: NativeAddress)

actual value class WGPUTexture actual constructor(actual val handler: NativeAddress)

actual value class WGPUTextureView actual constructor(actual val handler: NativeAddress)

actual interface WGPUChainedStruct {
    actual var next: WGPUChainedStruct?
    actual var sType: WGPUSType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUChainedStruct = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUChainedStruct =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUChainedStruct>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUChainedStruct) -> Unit): ArrayHolder<WGPUChainedStruct> {
            val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUChainedStruct>) : WGPUChainedStruct {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var next: WGPUChainedStruct?
            get() = handle.useContents { this.next?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var sType: WGPUSType
            get() = handle.useContents { this.sType as WGPUSType }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUChainedStruct {
        private val struct: webgpu.native.WGPUChainedStruct
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUChainedStruct>().pointed
        
        override var next: WGPUChainedStruct?
            get() = struct.next?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.next = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var sType: WGPUSType
            get() = struct.sType as WGPUSType
            set(value) { struct.sType = value }
    }
}

fun WGPUChainedStruct.toCValue(): CValue<webgpu.native.WGPUChainedStruct> = cValue {
    this.next = this@toCValue.next?.handler?.pointer?.takeIf { this@toCValue.next?.handler?.rawValue != 0L }?.reinterpret()
    this.sType = this@toCValue.sType
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferMapCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBufferMapCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferMapCallbackInfo) -> Unit): ArrayHolder<WGPUBufferMapCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUBufferMapCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBufferMapCallbackInfo>) : WGPUBufferMapCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBufferMapCallbackInfo {
        private val struct: webgpu.native.WGPUBufferMapCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBufferMapCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBufferMapCallbackInfo.toCValue(): CValue<webgpu.native.WGPUBufferMapCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationInfoCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCompilationInfoCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfoCallbackInfo) -> Unit): ArrayHolder<WGPUCompilationInfoCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUCompilationInfoCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCompilationInfoCallbackInfo>) : WGPUCompilationInfoCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCompilationInfoCallbackInfo {
        private val struct: webgpu.native.WGPUCompilationInfoCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCompilationInfoCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUCompilationInfoCallbackInfo.toCValue(): CValue<webgpu.native.WGPUCompilationInfoCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCreateComputePipelineAsyncCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateComputePipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateComputePipelineAsyncCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo>) : WGPUCreateComputePipelineAsyncCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCreateComputePipelineAsyncCallbackInfo {
        private val struct: webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUCreateComputePipelineAsyncCallbackInfo.toCValue(): CValue<webgpu.native.WGPUCreateComputePipelineAsyncCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCreateRenderPipelineAsyncCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCreateRenderPipelineAsyncCallbackInfo) -> Unit): ArrayHolder<WGPUCreateRenderPipelineAsyncCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo>) : WGPUCreateRenderPipelineAsyncCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCreateRenderPipelineAsyncCallbackInfo {
        private val struct: webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUCreateRenderPipelineAsyncCallbackInfo.toCValue(): CValue<webgpu.native.WGPUCreateRenderPipelineAsyncCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnLoadCacheDataCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnLoadCacheDataCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnLoadCacheDataCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnLoadCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnLoadCacheDataCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>) : WGPUDawnLoadCacheDataCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnLoadCacheDataCallbackInfo {
        private val struct: webgpu.native.WGPUDawnLoadCacheDataCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnLoadCacheDataCallbackInfo.toCValue(): CValue<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnStoreCacheDataCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnStoreCacheDataCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnStoreCacheDataCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnStoreCacheDataCallbackInfo) -> Unit): ArrayHolder<WGPUDawnStoreCacheDataCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>) : WGPUDawnStoreCacheDataCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnStoreCacheDataCallbackInfo {
        private val struct: webgpu.native.WGPUDawnStoreCacheDataCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnStoreCacheDataCallbackInfo.toCValue(): CValue<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDeviceLostCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDeviceLostCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceLostCallbackInfo) -> Unit): ArrayHolder<WGPUDeviceLostCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUDeviceLostCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDeviceLostCallbackInfo>) : WGPUDeviceLostCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDeviceLostCallbackInfo {
        private val struct: webgpu.native.WGPUDeviceLostCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDeviceLostCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDeviceLostCallbackInfo.toCValue(): CValue<webgpu.native.WGPUDeviceLostCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDisposeCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDisposeCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDisposeCallbackInfo) -> Unit): ArrayHolder<WGPUDisposeCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUDisposeCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDisposeCallbackInfo>) : WGPUDisposeCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDisposeCallbackInfo {
        private val struct: webgpu.native.WGPUDisposeCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDisposeCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDisposeCallbackInfo.toCValue(): CValue<webgpu.native.WGPUDisposeCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPULoggingCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPULoggingCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPULoggingCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPULoggingCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULoggingCallbackInfo) -> Unit): ArrayHolder<WGPULoggingCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPULoggingCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPULoggingCallbackInfo>) : WGPULoggingCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPULoggingCallbackInfo {
        private val struct: webgpu.native.WGPULoggingCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPULoggingCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPULoggingCallbackInfo.toCValue(): CValue<webgpu.native.WGPULoggingCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUPopErrorScopeCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPopErrorScopeCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPopErrorScopeCallbackInfo) -> Unit): ArrayHolder<WGPUPopErrorScopeCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUPopErrorScopeCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPopErrorScopeCallbackInfo>) : WGPUPopErrorScopeCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPopErrorScopeCallbackInfo {
        private val struct: webgpu.native.WGPUPopErrorScopeCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPopErrorScopeCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUPopErrorScopeCallbackInfo.toCValue(): CValue<webgpu.native.WGPUPopErrorScopeCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUQueueWorkDoneCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUQueueWorkDoneCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueWorkDoneCallbackInfo) -> Unit): ArrayHolder<WGPUQueueWorkDoneCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUQueueWorkDoneCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUQueueWorkDoneCallbackInfo>) : WGPUQueueWorkDoneCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUQueueWorkDoneCallbackInfo {
        private val struct: webgpu.native.WGPUQueueWorkDoneCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUQueueWorkDoneCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUQueueWorkDoneCallbackInfo.toCValue(): CValue<webgpu.native.WGPUQueueWorkDoneCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURequestAdapterCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterCallbackInfo) -> Unit): ArrayHolder<WGPURequestAdapterCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPURequestAdapterCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURequestAdapterCallbackInfo>) : WGPURequestAdapterCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURequestAdapterCallbackInfo {
        private val struct: webgpu.native.WGPURequestAdapterCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURequestAdapterCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURequestAdapterCallbackInfo.toCValue(): CValue<webgpu.native.WGPURequestAdapterCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPURequestDeviceCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURequestDeviceCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestDeviceCallbackInfo) -> Unit): ArrayHolder<WGPURequestDeviceCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPURequestDeviceCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURequestDeviceCallbackInfo>) : WGPURequestDeviceCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mode: WGPUCallbackMode
            get() = handle.useContents { this.mode as WGPUCallbackMode }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURequestDeviceCallbackInfo {
        private val struct: webgpu.native.WGPURequestDeviceCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURequestDeviceCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mode: WGPUCallbackMode
            get() = struct.mode as WGPUCallbackMode
            set(value) { struct.mode = value }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURequestDeviceCallbackInfo.toCValue(): CValue<webgpu.native.WGPURequestDeviceCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.mode = this@toCValue.mode
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUUncapturedErrorCallbackInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var callback: NativeAddress?
    actual var userdata1: NativeAddress?
    actual var userdata2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUUncapturedErrorCallbackInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUUncapturedErrorCallbackInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUUncapturedErrorCallbackInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUUncapturedErrorCallbackInfo) -> Unit): ArrayHolder<WGPUUncapturedErrorCallbackInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUUncapturedErrorCallbackInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUUncapturedErrorCallbackInfo>) : WGPUUncapturedErrorCallbackInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var callback: NativeAddress?
            get() = handle.useContents { this.callback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata1: NativeAddress?
            get() = handle.useContents { this.userdata1?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata2: NativeAddress?
            get() = handle.useContents { this.userdata2?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUUncapturedErrorCallbackInfo {
        private val struct: webgpu.native.WGPUUncapturedErrorCallbackInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUUncapturedErrorCallbackInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var callback: NativeAddress?
            get() = struct.callback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.callback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata1: NativeAddress?
            get() = struct.userdata1?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata1 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata2: NativeAddress?
            get() = struct.userdata2?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata2 = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUUncapturedErrorCallbackInfo.toCValue(): CValue<webgpu.native.WGPUUncapturedErrorCallbackInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.callback = this@toCValue.callback?.pointer?.takeIf { this@toCValue.callback?.rawValue != 0L }?.reinterpret()
    this.userdata1 = this@toCValue.userdata1?.pointer?.takeIf { this@toCValue.userdata1?.rawValue != 0L }?.reinterpret()
    this.userdata2 = this@toCValue.userdata2?.pointer?.takeIf { this@toCValue.userdata2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUAdapterPropertiesD3D {
    actual var chain: WGPUChainedStruct
    actual var shaderModel: UInt
    actual var adapterLUIDLowPart: UInt
    actual var adapterLUIDHighPart: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesD3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesD3D =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesD3D>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesD3D) -> Unit): ArrayHolder<WGPUAdapterPropertiesD3D> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesD3D>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesD3D>) : WGPUAdapterPropertiesD3D {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var shaderModel: UInt
            get() = handle.useContents { this.shaderModel }
            set(value) { error("Setters not supported on ByValue") }
        override var adapterLUIDLowPart: UInt
            get() = handle.useContents { this.adapterLUIDLowPart }
            set(value) { error("Setters not supported on ByValue") }
        override var adapterLUIDHighPart: UInt
            get() = handle.useContents { this.adapterLUIDHighPart }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesD3D {
        private val struct: webgpu.native.WGPUAdapterPropertiesD3D
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesD3D>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var shaderModel: UInt
            get() = struct.shaderModel
            set(value) { struct.shaderModel = value }
        override var adapterLUIDLowPart: UInt
            get() = struct.adapterLUIDLowPart
            set(value) { struct.adapterLUIDLowPart = value }
        override var adapterLUIDHighPart: UInt
            get() = struct.adapterLUIDHighPart
            set(value) { struct.adapterLUIDHighPart = value }
    }
}

fun WGPUAdapterPropertiesD3D.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesD3D> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.shaderModel = this@toCValue.shaderModel
    this.adapterLUIDLowPart = this@toCValue.adapterLUIDLowPart
    this.adapterLUIDHighPart = this@toCValue.adapterLUIDHighPart
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
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesDrm =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesDrm>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesDrm) -> Unit): ArrayHolder<WGPUAdapterPropertiesDrm> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesDrm>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesDrm>) : WGPUAdapterPropertiesDrm {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var hasPrimary: UInt
            get() = handle.useContents { this.hasPrimary }
            set(value) { error("Setters not supported on ByValue") }
        override var hasRender: UInt
            get() = handle.useContents { this.hasRender }
            set(value) { error("Setters not supported on ByValue") }
        override var primaryMajor: ULong
            get() = handle.useContents { this.primaryMajor }
            set(value) { error("Setters not supported on ByValue") }
        override var primaryMinor: ULong
            get() = handle.useContents { this.primaryMinor }
            set(value) { error("Setters not supported on ByValue") }
        override var renderMajor: ULong
            get() = handle.useContents { this.renderMajor }
            set(value) { error("Setters not supported on ByValue") }
        override var renderMinor: ULong
            get() = handle.useContents { this.renderMinor }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesDrm {
        private val struct: webgpu.native.WGPUAdapterPropertiesDrm
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesDrm>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var hasPrimary: UInt
            get() = struct.hasPrimary
            set(value) { struct.hasPrimary = value }
        override var hasRender: UInt
            get() = struct.hasRender
            set(value) { struct.hasRender = value }
        override var primaryMajor: ULong
            get() = struct.primaryMajor
            set(value) { struct.primaryMajor = value }
        override var primaryMinor: ULong
            get() = struct.primaryMinor
            set(value) { struct.primaryMinor = value }
        override var renderMajor: ULong
            get() = struct.renderMajor
            set(value) { struct.renderMajor = value }
        override var renderMinor: ULong
            get() = struct.renderMinor
            set(value) { struct.renderMinor = value }
    }
}

fun WGPUAdapterPropertiesDrm.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesDrm> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.hasPrimary = this@toCValue.hasPrimary
    this.hasRender = this@toCValue.hasRender
    this.primaryMajor = this@toCValue.primaryMajor
    this.primaryMinor = this@toCValue.primaryMinor
    this.renderMajor = this@toCValue.renderMajor
    this.renderMinor = this@toCValue.renderMinor
}

actual interface WGPUAdapterPropertiesVk {
    actual var chain: WGPUChainedStruct
    actual var driverVersion: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesVk = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesVk =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesVk>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesVk) -> Unit): ArrayHolder<WGPUAdapterPropertiesVk> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesVk>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesVk>) : WGPUAdapterPropertiesVk {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var driverVersion: UInt
            get() = handle.useContents { this.driverVersion }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesVk {
        private val struct: webgpu.native.WGPUAdapterPropertiesVk
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesVk>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var driverVersion: UInt
            get() = struct.driverVersion
            set(value) { struct.driverVersion = value }
    }
}

fun WGPUAdapterPropertiesVk.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesVk> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.driverVersion = this@toCValue.driverVersion
}

actual interface WGPUAdapterPropertiesWGPU {
    actual var chain: WGPUChainedStruct
    actual var backendType: WGPUBackendType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesWGPU = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesWGPU =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesWGPU>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesWGPU) -> Unit): ArrayHolder<WGPUAdapterPropertiesWGPU> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesWGPU>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesWGPU>) : WGPUAdapterPropertiesWGPU {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var backendType: WGPUBackendType
            get() = handle.useContents { this.backendType as WGPUBackendType }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesWGPU {
        private val struct: webgpu.native.WGPUAdapterPropertiesWGPU
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesWGPU>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var backendType: WGPUBackendType
            get() = struct.backendType as WGPUBackendType
            set(value) { struct.backendType = value }
    }
}

fun WGPUAdapterPropertiesWGPU.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesWGPU> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.backendType = this@toCValue.backendType
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBindingResource =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBindingResource>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindingResource) -> Unit): ArrayHolder<WGPUBindingResource> {
            val byteSize = sizeOf<webgpu.native.WGPUBindingResource>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBindingResource>) : WGPUBindingResource {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var buffer: WGPUBuffer?
            get() = handle.useContents { this.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        override var sampler: WGPUSampler?
            get() = handle.useContents { this.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var textureView: WGPUTextureView?
            get() = handle.useContents { this.textureView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBindingResource {
        private val struct: webgpu.native.WGPUBindingResource
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBindingResource>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var buffer: WGPUBuffer?
            get() = struct.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
            set(value) { struct.buffer = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
        override var sampler: WGPUSampler?
            get() = struct.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) }
            set(value) { struct.sampler = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var textureView: WGPUTextureView?
            get() = struct.textureView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.textureView = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBindingResource.toCValue(): CValue<webgpu.native.WGPUBindingResource> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.buffer = this@toCValue.buffer?.handler?.pointer?.takeIf { this@toCValue.buffer?.handler?.rawValue != 0L }?.reinterpret()
    this.offset = this@toCValue.offset
    this.size = this@toCValue.size
    this.sampler = this@toCValue.sampler?.handler?.pointer?.takeIf { this@toCValue.sampler?.handler?.rawValue != 0L }?.reinterpret()
    this.textureView = this@toCValue.textureView?.handler?.pointer?.takeIf { this@toCValue.textureView?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUBlendComponent {
    actual var operation: WGPUBlendOperation
    actual var srcFactor: WGPUBlendFactor
    actual var dstFactor: WGPUBlendFactor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBlendComponent = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBlendComponent =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBlendComponent>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendComponent) -> Unit): ArrayHolder<WGPUBlendComponent> {
            val byteSize = sizeOf<webgpu.native.WGPUBlendComponent>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBlendComponent>) : WGPUBlendComponent {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var operation: WGPUBlendOperation
            get() = handle.useContents { this.operation as WGPUBlendOperation }
            set(value) { error("Setters not supported on ByValue") }
        override var srcFactor: WGPUBlendFactor
            get() = handle.useContents { this.srcFactor as WGPUBlendFactor }
            set(value) { error("Setters not supported on ByValue") }
        override var dstFactor: WGPUBlendFactor
            get() = handle.useContents { this.dstFactor as WGPUBlendFactor }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBlendComponent {
        private val struct: webgpu.native.WGPUBlendComponent
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBlendComponent>().pointed
        
        override var operation: WGPUBlendOperation
            get() = struct.operation as WGPUBlendOperation
            set(value) { struct.operation = value }
        override var srcFactor: WGPUBlendFactor
            get() = struct.srcFactor as WGPUBlendFactor
            set(value) { struct.srcFactor = value }
        override var dstFactor: WGPUBlendFactor
            get() = struct.dstFactor as WGPUBlendFactor
            set(value) { struct.dstFactor = value }
    }
}

fun WGPUBlendComponent.toCValue(): CValue<webgpu.native.WGPUBlendComponent> = cValue {
    this.operation = this@toCValue.operation
    this.srcFactor = this@toCValue.srcFactor
    this.dstFactor = this@toCValue.dstFactor
}

actual interface WGPUBufferBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUBufferBindingType
    actual var hasDynamicOffset: UInt
    actual var minBindingSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBufferBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferBindingLayout) -> Unit): ArrayHolder<WGPUBufferBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUBufferBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBufferBindingLayout>) : WGPUBufferBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var type: WGPUBufferBindingType
            get() = handle.useContents { this.type as WGPUBufferBindingType }
            set(value) { error("Setters not supported on ByValue") }
        override var hasDynamicOffset: UInt
            get() = handle.useContents { this.hasDynamicOffset }
            set(value) { error("Setters not supported on ByValue") }
        override var minBindingSize: ULong
            get() = handle.useContents { this.minBindingSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBufferBindingLayout {
        private val struct: webgpu.native.WGPUBufferBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBufferBindingLayout>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var type: WGPUBufferBindingType
            get() = struct.type as WGPUBufferBindingType
            set(value) { struct.type = value }
        override var hasDynamicOffset: UInt
            get() = struct.hasDynamicOffset
            set(value) { struct.hasDynamicOffset = value }
        override var minBindingSize: ULong
            get() = struct.minBindingSize
            set(value) { struct.minBindingSize = value }
    }
}

fun WGPUBufferBindingLayout.toCValue(): CValue<webgpu.native.WGPUBufferBindingLayout> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.type = this@toCValue.type
    this.hasDynamicOffset = this@toCValue.hasDynamicOffset
    this.minBindingSize = this@toCValue.minBindingSize
}

actual interface WGPUBufferHostMappedPointer {
    actual var chain: WGPUChainedStruct
    actual var pointer: NativeAddress?
    actual var disposeCallback: NativeAddress?
    actual var userdata: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBufferHostMappedPointer = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferHostMappedPointer =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBufferHostMappedPointer>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferHostMappedPointer) -> Unit): ArrayHolder<WGPUBufferHostMappedPointer> {
            val byteSize = sizeOf<webgpu.native.WGPUBufferHostMappedPointer>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBufferHostMappedPointer>) : WGPUBufferHostMappedPointer {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var pointer: NativeAddress?
            get() = handle.useContents { this.pointer?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var disposeCallback: NativeAddress?
            get() = handle.useContents { this.disposeCallback?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var userdata: NativeAddress?
            get() = handle.useContents { this.userdata?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBufferHostMappedPointer {
        private val struct: webgpu.native.WGPUBufferHostMappedPointer
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBufferHostMappedPointer>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var pointer: NativeAddress?
            get() = struct.pointer?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.pointer = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var disposeCallback: NativeAddress?
            get() = struct.disposeCallback?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.disposeCallback = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var userdata: NativeAddress?
            get() = struct.userdata?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.userdata = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBufferHostMappedPointer.toCValue(): CValue<webgpu.native.WGPUBufferHostMappedPointer> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.pointer = this@toCValue.pointer?.pointer?.takeIf { this@toCValue.pointer?.rawValue != 0L }?.reinterpret()
    this.disposeCallback = this@toCValue.disposeCallback?.pointer?.takeIf { this@toCValue.disposeCallback?.rawValue != 0L }?.reinterpret()
    this.userdata = this@toCValue.userdata?.pointer?.takeIf { this@toCValue.userdata?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUColor {
    actual var r: Double
    actual var g: Double
    actual var b: Double
    actual var a: Double
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUColor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColor) -> Unit): ArrayHolder<WGPUColor> {
            val byteSize = sizeOf<webgpu.native.WGPUColor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUColor>) : WGPUColor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var r: Double
            get() = handle.useContents { this.r }
            set(value) { error("Setters not supported on ByValue") }
        override var g: Double
            get() = handle.useContents { this.g }
            set(value) { error("Setters not supported on ByValue") }
        override var b: Double
            get() = handle.useContents { this.b }
            set(value) { error("Setters not supported on ByValue") }
        override var a: Double
            get() = handle.useContents { this.a }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUColor {
        private val struct: webgpu.native.WGPUColor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUColor>().pointed
        
        override var r: Double
            get() = struct.r
            set(value) { struct.r = value }
        override var g: Double
            get() = struct.g
            set(value) { struct.g = value }
        override var b: Double
            get() = struct.b
            set(value) { struct.b = value }
        override var a: Double
            get() = struct.a
            set(value) { struct.a = value }
    }
}

fun WGPUColor.toCValue(): CValue<webgpu.native.WGPUColor> = cValue {
    this.r = this@toCValue.r
    this.g = this@toCValue.g
    this.b = this@toCValue.b
    this.a = this@toCValue.a
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
        actual fun allocate(allocator: MemoryAllocator): WGPUColorSpaceDawn =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUColorSpaceDawn>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorSpaceDawn) -> Unit): ArrayHolder<WGPUColorSpaceDawn> {
            val byteSize = sizeOf<webgpu.native.WGPUColorSpaceDawn>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUColorSpaceDawn>) : WGPUColorSpaceDawn {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var primaries: WGPUColorSpacePrimariesDawn
            get() = handle.useContents { this.primaries as WGPUColorSpacePrimariesDawn }
            set(value) { error("Setters not supported on ByValue") }
        override var transfer: WGPUColorSpaceTransferDawn
            get() = handle.useContents { this.transfer as WGPUColorSpaceTransferDawn }
            set(value) { error("Setters not supported on ByValue") }
        override var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
            get() = handle.useContents { this.yCbCrRange as WGPUColorSpaceYCbCrRangeDawn }
            set(value) { error("Setters not supported on ByValue") }
        override var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
            get() = handle.useContents { this.yCbCrMatrix as WGPUColorSpaceYCbCrMatrixDawn }
            set(value) { error("Setters not supported on ByValue") }
        override var hdrReferenceWhiteLuminance: Float
            get() = handle.useContents { this.hdrReferenceWhiteLuminance }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUColorSpaceDawn {
        private val struct: webgpu.native.WGPUColorSpaceDawn
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUColorSpaceDawn>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var primaries: WGPUColorSpacePrimariesDawn
            get() = struct.primaries as WGPUColorSpacePrimariesDawn
            set(value) { struct.primaries = value }
        override var transfer: WGPUColorSpaceTransferDawn
            get() = struct.transfer as WGPUColorSpaceTransferDawn
            set(value) { struct.transfer = value }
        override var yCbCrRange: WGPUColorSpaceYCbCrRangeDawn
            get() = struct.yCbCrRange as WGPUColorSpaceYCbCrRangeDawn
            set(value) { struct.yCbCrRange = value }
        override var yCbCrMatrix: WGPUColorSpaceYCbCrMatrixDawn
            get() = struct.yCbCrMatrix as WGPUColorSpaceYCbCrMatrixDawn
            set(value) { struct.yCbCrMatrix = value }
        override var hdrReferenceWhiteLuminance: Float
            get() = struct.hdrReferenceWhiteLuminance
            set(value) { struct.hdrReferenceWhiteLuminance = value }
    }
}

fun WGPUColorSpaceDawn.toCValue(): CValue<webgpu.native.WGPUColorSpaceDawn> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.primaries = this@toCValue.primaries
    this.transfer = this@toCValue.transfer
    this.yCbCrRange = this@toCValue.yCbCrRange
    this.yCbCrMatrix = this@toCValue.yCbCrMatrix
    this.hdrReferenceWhiteLuminance = this@toCValue.hdrReferenceWhiteLuminance
}

actual interface WGPUColorTargetStateExpandResolveTextureDawn {
    actual var chain: WGPUChainedStruct
    actual var enabled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColorTargetStateExpandResolveTextureDawn = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColorTargetStateExpandResolveTextureDawn =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetStateExpandResolveTextureDawn) -> Unit): ArrayHolder<WGPUColorTargetStateExpandResolveTextureDawn> {
            val byteSize = sizeOf<webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn>) : WGPUColorTargetStateExpandResolveTextureDawn {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var enabled: UInt
            get() = handle.useContents { this.enabled }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUColorTargetStateExpandResolveTextureDawn {
        private val struct: webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var enabled: UInt
            get() = struct.enabled
            set(value) { struct.enabled = value }
    }
}

fun WGPUColorTargetStateExpandResolveTextureDawn.toCValue(): CValue<webgpu.native.WGPUColorTargetStateExpandResolveTextureDawn> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.enabled = this@toCValue.enabled
}

actual interface WGPUCommandBufferDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCommandBufferDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCommandBufferDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCommandBufferDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandBufferDescriptor) -> Unit): ArrayHolder<WGPUCommandBufferDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUCommandBufferDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCommandBufferDescriptor>) : WGPUCommandBufferDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCommandBufferDescriptor {
        private val struct: webgpu.native.WGPUCommandBufferDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCommandBufferDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUCommandBufferDescriptor.toCValue(): CValue<webgpu.native.WGPUCommandBufferDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCompatibilityModeLimits =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCompatibilityModeLimits>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompatibilityModeLimits) -> Unit): ArrayHolder<WGPUCompatibilityModeLimits> {
            val byteSize = sizeOf<webgpu.native.WGPUCompatibilityModeLimits>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCompatibilityModeLimits>) : WGPUCompatibilityModeLimits {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageBuffersInVertexStage: UInt
            get() = handle.useContents { this.maxStorageBuffersInVertexStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageTexturesInVertexStage: UInt
            get() = handle.useContents { this.maxStorageTexturesInVertexStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageBuffersInFragmentStage: UInt
            get() = handle.useContents { this.maxStorageBuffersInFragmentStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageTexturesInFragmentStage: UInt
            get() = handle.useContents { this.maxStorageTexturesInFragmentStage }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCompatibilityModeLimits {
        private val struct: webgpu.native.WGPUCompatibilityModeLimits
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCompatibilityModeLimits>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var maxStorageBuffersInVertexStage: UInt
            get() = struct.maxStorageBuffersInVertexStage
            set(value) { struct.maxStorageBuffersInVertexStage = value }
        override var maxStorageTexturesInVertexStage: UInt
            get() = struct.maxStorageTexturesInVertexStage
            set(value) { struct.maxStorageTexturesInVertexStage = value }
        override var maxStorageBuffersInFragmentStage: UInt
            get() = struct.maxStorageBuffersInFragmentStage
            set(value) { struct.maxStorageBuffersInFragmentStage = value }
        override var maxStorageTexturesInFragmentStage: UInt
            get() = struct.maxStorageTexturesInFragmentStage
            set(value) { struct.maxStorageTexturesInFragmentStage = value }
    }
}

fun WGPUCompatibilityModeLimits.toCValue(): CValue<webgpu.native.WGPUCompatibilityModeLimits> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.maxStorageBuffersInVertexStage = this@toCValue.maxStorageBuffersInVertexStage
    this.maxStorageTexturesInVertexStage = this@toCValue.maxStorageTexturesInVertexStage
    this.maxStorageBuffersInFragmentStage = this@toCValue.maxStorageBuffersInFragmentStage
    this.maxStorageTexturesInFragmentStage = this@toCValue.maxStorageTexturesInFragmentStage
}

actual interface WGPUConstantEntry {
    actual var nextInChain: WGPUChainedStruct?
    actual var key: WGPUStringView
    actual var value: Double
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUConstantEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUConstantEntry =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUConstantEntry>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUConstantEntry) -> Unit): ArrayHolder<WGPUConstantEntry> {
            val byteSize = sizeOf<webgpu.native.WGPUConstantEntry>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUConstantEntry>) : WGPUConstantEntry {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var key: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.key.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var value: Double
            get() = handle.useContents { this.value }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUConstantEntry {
        private val struct: webgpu.native.WGPUConstantEntry
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUConstantEntry>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var key: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.key.ptr))
            set(value) {
                val destBytes = struct.key.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var value: Double
            get() = struct.value
            set(value) { struct.value = value }
    }
}

fun WGPUConstantEntry.toCValue(): CValue<webgpu.native.WGPUConstantEntry> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_key = this.key.ptr.reinterpret<ByteVar>()
    val src_key = requireNotNull(this@toCValue.key.handler.pointer).reinterpret<ByteVar>()
    val size_key = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_key) {
        dest_key[i.toInt()] = src_key[i.toInt()]
    }
    this.value = this@toCValue.value
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCopyTextureForBrowserOptions =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCopyTextureForBrowserOptions>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCopyTextureForBrowserOptions) -> Unit): ArrayHolder<WGPUCopyTextureForBrowserOptions> {
            val byteSize = sizeOf<webgpu.native.WGPUCopyTextureForBrowserOptions>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCopyTextureForBrowserOptions>) : WGPUCopyTextureForBrowserOptions {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var flipY: UInt
            get() = handle.useContents { this.flipY }
            set(value) { error("Setters not supported on ByValue") }
        override var needsColorSpaceConversion: UInt
            get() = handle.useContents { this.needsColorSpaceConversion }
            set(value) { error("Setters not supported on ByValue") }
        override var srcAlphaMode: WGPUAlphaMode
            get() = handle.useContents { this.srcAlphaMode as WGPUAlphaMode }
            set(value) { error("Setters not supported on ByValue") }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = handle.useContents { this.srcTransferFunctionParameters?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var conversionMatrix: NativeAddress?
            get() = handle.useContents { this.conversionMatrix?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = handle.useContents { this.dstTransferFunctionParameters?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var dstAlphaMode: WGPUAlphaMode
            get() = handle.useContents { this.dstAlphaMode as WGPUAlphaMode }
            set(value) { error("Setters not supported on ByValue") }
        override var internalUsage: UInt
            get() = handle.useContents { this.internalUsage }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCopyTextureForBrowserOptions {
        private val struct: webgpu.native.WGPUCopyTextureForBrowserOptions
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCopyTextureForBrowserOptions>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var flipY: UInt
            get() = struct.flipY
            set(value) { struct.flipY = value }
        override var needsColorSpaceConversion: UInt
            get() = struct.needsColorSpaceConversion
            set(value) { struct.needsColorSpaceConversion = value }
        override var srcAlphaMode: WGPUAlphaMode
            get() = struct.srcAlphaMode as WGPUAlphaMode
            set(value) { struct.srcAlphaMode = value }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = struct.srcTransferFunctionParameters?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.srcTransferFunctionParameters = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var conversionMatrix: NativeAddress?
            get() = struct.conversionMatrix?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.conversionMatrix = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = struct.dstTransferFunctionParameters?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.dstTransferFunctionParameters = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var dstAlphaMode: WGPUAlphaMode
            get() = struct.dstAlphaMode as WGPUAlphaMode
            set(value) { struct.dstAlphaMode = value }
        override var internalUsage: UInt
            get() = struct.internalUsage
            set(value) { struct.internalUsage = value }
    }
}

fun WGPUCopyTextureForBrowserOptions.toCValue(): CValue<webgpu.native.WGPUCopyTextureForBrowserOptions> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.flipY = this@toCValue.flipY
    this.needsColorSpaceConversion = this@toCValue.needsColorSpaceConversion
    this.srcAlphaMode = this@toCValue.srcAlphaMode
    this.srcTransferFunctionParameters = this@toCValue.srcTransferFunctionParameters?.pointer?.takeIf { this@toCValue.srcTransferFunctionParameters?.rawValue != 0L }?.reinterpret()
    this.conversionMatrix = this@toCValue.conversionMatrix?.pointer?.takeIf { this@toCValue.conversionMatrix?.rawValue != 0L }?.reinterpret()
    this.dstTransferFunctionParameters = this@toCValue.dstTransferFunctionParameters?.pointer?.takeIf { this@toCValue.dstTransferFunctionParameters?.rawValue != 0L }?.reinterpret()
    this.dstAlphaMode = this@toCValue.dstAlphaMode
    this.internalUsage = this@toCValue.internalUsage
}

actual interface WGPUDawnAdapterPropertiesPowerPreference {
    actual var chain: WGPUChainedStruct
    actual var powerPreference: WGPUPowerPreference
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnAdapterPropertiesPowerPreference = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnAdapterPropertiesPowerPreference =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnAdapterPropertiesPowerPreference>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnAdapterPropertiesPowerPreference) -> Unit): ArrayHolder<WGPUDawnAdapterPropertiesPowerPreference> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnAdapterPropertiesPowerPreference>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnAdapterPropertiesPowerPreference>) : WGPUDawnAdapterPropertiesPowerPreference {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var powerPreference: WGPUPowerPreference
            get() = handle.useContents { this.powerPreference as WGPUPowerPreference }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnAdapterPropertiesPowerPreference {
        private val struct: webgpu.native.WGPUDawnAdapterPropertiesPowerPreference
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnAdapterPropertiesPowerPreference>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var powerPreference: WGPUPowerPreference
            get() = struct.powerPreference as WGPUPowerPreference
            set(value) { struct.powerPreference = value }
    }
}

fun WGPUDawnAdapterPropertiesPowerPreference.toCValue(): CValue<webgpu.native.WGPUDawnAdapterPropertiesPowerPreference> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.powerPreference = this@toCValue.powerPreference
}

actual interface WGPUDawnBufferDescriptorErrorInfoFromWireClient {
    actual var chain: WGPUChainedStruct
    actual var outOfMemory: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnBufferDescriptorErrorInfoFromWireClient = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnBufferDescriptorErrorInfoFromWireClient =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnBufferDescriptorErrorInfoFromWireClient) -> Unit): ArrayHolder<WGPUDawnBufferDescriptorErrorInfoFromWireClient> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient>) : WGPUDawnBufferDescriptorErrorInfoFromWireClient {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var outOfMemory: UInt
            get() = handle.useContents { this.outOfMemory }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnBufferDescriptorErrorInfoFromWireClient {
        private val struct: webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var outOfMemory: UInt
            get() = struct.outOfMemory
            set(value) { struct.outOfMemory = value }
    }
}

fun WGPUDawnBufferDescriptorErrorInfoFromWireClient.toCValue(): CValue<webgpu.native.WGPUDawnBufferDescriptorErrorInfoFromWireClient> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.outOfMemory = this@toCValue.outOfMemory
}

actual interface WGPUDawnCacheDeviceDescriptor {
    actual var chain: WGPUChainedStruct
    actual var isolationKey: WGPUStringView
    actual var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
    actual var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnCacheDeviceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnCacheDeviceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnCacheDeviceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCacheDeviceDescriptor) -> Unit): ArrayHolder<WGPUDawnCacheDeviceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnCacheDeviceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnCacheDeviceDescriptor>) : WGPUDawnCacheDeviceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var isolationKey: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.isolationKey.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
            get() = handle.useContents { WGPUDawnLoadCacheDataCallbackInfo.ByReference(NativeAddress.fromPointer(this.dawnLoadCacheDataCallbackInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
            get() = handle.useContents { WGPUDawnStoreCacheDataCallbackInfo.ByReference(NativeAddress.fromPointer(this.dawnStoreCacheDataCallbackInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnCacheDeviceDescriptor {
        private val struct: webgpu.native.WGPUDawnCacheDeviceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnCacheDeviceDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var isolationKey: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.isolationKey.ptr))
            set(value) {
                val destBytes = struct.isolationKey.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var dawnLoadCacheDataCallbackInfo: WGPUDawnLoadCacheDataCallbackInfo
            get() = WGPUDawnLoadCacheDataCallbackInfo.ByReference(NativeAddress.fromPointer(struct.dawnLoadCacheDataCallbackInfo.ptr))
            set(value) {
                val destBytes = struct.dawnLoadCacheDataCallbackInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var dawnStoreCacheDataCallbackInfo: WGPUDawnStoreCacheDataCallbackInfo
            get() = WGPUDawnStoreCacheDataCallbackInfo.ByReference(NativeAddress.fromPointer(struct.dawnStoreCacheDataCallbackInfo.ptr))
            set(value) {
                val destBytes = struct.dawnStoreCacheDataCallbackInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUDawnCacheDeviceDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnCacheDeviceDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_isolationKey = this.isolationKey.ptr.reinterpret<ByteVar>()
    val src_isolationKey = requireNotNull(this@toCValue.isolationKey.handler.pointer).reinterpret<ByteVar>()
    val size_isolationKey = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_isolationKey) {
        dest_isolationKey[i.toInt()] = src_isolationKey[i.toInt()]
    }
    val dest_dawnLoadCacheDataCallbackInfo = this.dawnLoadCacheDataCallbackInfo.ptr.reinterpret<ByteVar>()
    val src_dawnLoadCacheDataCallbackInfo = requireNotNull(this@toCValue.dawnLoadCacheDataCallbackInfo.handler.pointer).reinterpret<ByteVar>()
    val size_dawnLoadCacheDataCallbackInfo = sizeOf<webgpu.native.WGPUDawnLoadCacheDataCallbackInfo>().toLong()
    for (i in 0L until size_dawnLoadCacheDataCallbackInfo) {
        dest_dawnLoadCacheDataCallbackInfo[i.toInt()] = src_dawnLoadCacheDataCallbackInfo[i.toInt()]
    }
    val dest_dawnStoreCacheDataCallbackInfo = this.dawnStoreCacheDataCallbackInfo.ptr.reinterpret<ByteVar>()
    val src_dawnStoreCacheDataCallbackInfo = requireNotNull(this@toCValue.dawnStoreCacheDataCallbackInfo.handler.pointer).reinterpret<ByteVar>()
    val size_dawnStoreCacheDataCallbackInfo = sizeOf<webgpu.native.WGPUDawnStoreCacheDataCallbackInfo>().toLong()
    for (i in 0L until size_dawnStoreCacheDataCallbackInfo) {
        dest_dawnStoreCacheDataCallbackInfo[i.toInt()] = src_dawnStoreCacheDataCallbackInfo[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnCompilationMessageUtf16 =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnCompilationMessageUtf16>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnCompilationMessageUtf16) -> Unit): ArrayHolder<WGPUDawnCompilationMessageUtf16> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnCompilationMessageUtf16>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnCompilationMessageUtf16>) : WGPUDawnCompilationMessageUtf16 {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var linePos: ULong
            get() = handle.useContents { this.linePos }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var length: ULong
            get() = handle.useContents { this.length }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnCompilationMessageUtf16 {
        private val struct: webgpu.native.WGPUDawnCompilationMessageUtf16
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnCompilationMessageUtf16>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var linePos: ULong
            get() = struct.linePos
            set(value) { struct.linePos = value }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var length: ULong
            get() = struct.length
            set(value) { struct.length = value }
    }
}

fun WGPUDawnCompilationMessageUtf16.toCValue(): CValue<webgpu.native.WGPUDawnCompilationMessageUtf16> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.linePos = this@toCValue.linePos
    this.offset = this@toCValue.offset
    this.length = this@toCValue.length
}

actual interface WGPUDawnConsumeAdapterDescriptor {
    actual var chain: WGPUChainedStruct
    actual var consumeAdapter: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnConsumeAdapterDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnConsumeAdapterDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnConsumeAdapterDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnConsumeAdapterDescriptor) -> Unit): ArrayHolder<WGPUDawnConsumeAdapterDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnConsumeAdapterDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnConsumeAdapterDescriptor>) : WGPUDawnConsumeAdapterDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var consumeAdapter: UInt
            get() = handle.useContents { this.consumeAdapter }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnConsumeAdapterDescriptor {
        private val struct: webgpu.native.WGPUDawnConsumeAdapterDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnConsumeAdapterDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var consumeAdapter: UInt
            get() = struct.consumeAdapter
            set(value) { struct.consumeAdapter = value }
    }
}

fun WGPUDawnConsumeAdapterDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnConsumeAdapterDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.consumeAdapter = this@toCValue.consumeAdapter
}

actual interface WGPUDawnDeviceAllocatorControl {
    actual var chain: WGPUChainedStruct
    actual var allocatorHeapBlockSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDeviceAllocatorControl = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDeviceAllocatorControl =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnDeviceAllocatorControl>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDeviceAllocatorControl) -> Unit): ArrayHolder<WGPUDawnDeviceAllocatorControl> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnDeviceAllocatorControl>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnDeviceAllocatorControl>) : WGPUDawnDeviceAllocatorControl {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var allocatorHeapBlockSize: ULong
            get() = handle.useContents { this.allocatorHeapBlockSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnDeviceAllocatorControl {
        private val struct: webgpu.native.WGPUDawnDeviceAllocatorControl
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnDeviceAllocatorControl>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var allocatorHeapBlockSize: ULong
            get() = struct.allocatorHeapBlockSize
            set(value) { struct.allocatorHeapBlockSize = value }
    }
}

fun WGPUDawnDeviceAllocatorControl.toCValue(): CValue<webgpu.native.WGPUDawnDeviceAllocatorControl> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.allocatorHeapBlockSize = this@toCValue.allocatorHeapBlockSize
}

actual interface WGPUDawnDrmFormatProperties {
    actual var modifier: ULong
    actual var modifierPlaneCount: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatProperties =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnDrmFormatProperties>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatProperties) -> Unit): ArrayHolder<WGPUDawnDrmFormatProperties> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnDrmFormatProperties>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnDrmFormatProperties>) : WGPUDawnDrmFormatProperties {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var modifier: ULong
            get() = handle.useContents { this.modifier }
            set(value) { error("Setters not supported on ByValue") }
        override var modifierPlaneCount: UInt
            get() = handle.useContents { this.modifierPlaneCount }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnDrmFormatProperties {
        private val struct: webgpu.native.WGPUDawnDrmFormatProperties
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnDrmFormatProperties>().pointed
        
        override var modifier: ULong
            get() = struct.modifier
            set(value) { struct.modifier = value }
        override var modifierPlaneCount: UInt
            get() = struct.modifierPlaneCount
            set(value) { struct.modifierPlaneCount = value }
    }
}

fun WGPUDawnDrmFormatProperties.toCValue(): CValue<webgpu.native.WGPUDawnDrmFormatProperties> = cValue {
    this.modifier = this@toCValue.modifier
    this.modifierPlaneCount = this@toCValue.modifierPlaneCount
}

actual interface WGPUDawnEncoderInternalUsageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var useInternalUsages: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnEncoderInternalUsageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnEncoderInternalUsageDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnEncoderInternalUsageDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnEncoderInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnEncoderInternalUsageDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnEncoderInternalUsageDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnEncoderInternalUsageDescriptor>) : WGPUDawnEncoderInternalUsageDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var useInternalUsages: UInt
            get() = handle.useContents { this.useInternalUsages }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnEncoderInternalUsageDescriptor {
        private val struct: webgpu.native.WGPUDawnEncoderInternalUsageDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnEncoderInternalUsageDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var useInternalUsages: UInt
            get() = struct.useInternalUsages
            set(value) { struct.useInternalUsages = value }
    }
}

fun WGPUDawnEncoderInternalUsageDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnEncoderInternalUsageDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.useInternalUsages = this@toCValue.useInternalUsages
}

actual interface WGPUDawnFakeBufferOOMForTesting {
    actual var chain: WGPUChainedStruct
    actual var fakeOOMAtWireClientMap: UInt
    actual var fakeOOMAtNativeMap: UInt
    actual var fakeOOMAtDevice: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFakeBufferOOMForTesting = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFakeBufferOOMForTesting =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnFakeBufferOOMForTesting>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeBufferOOMForTesting) -> Unit): ArrayHolder<WGPUDawnFakeBufferOOMForTesting> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnFakeBufferOOMForTesting>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnFakeBufferOOMForTesting>) : WGPUDawnFakeBufferOOMForTesting {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var fakeOOMAtWireClientMap: UInt
            get() = handle.useContents { this.fakeOOMAtWireClientMap }
            set(value) { error("Setters not supported on ByValue") }
        override var fakeOOMAtNativeMap: UInt
            get() = handle.useContents { this.fakeOOMAtNativeMap }
            set(value) { error("Setters not supported on ByValue") }
        override var fakeOOMAtDevice: UInt
            get() = handle.useContents { this.fakeOOMAtDevice }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnFakeBufferOOMForTesting {
        private val struct: webgpu.native.WGPUDawnFakeBufferOOMForTesting
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnFakeBufferOOMForTesting>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var fakeOOMAtWireClientMap: UInt
            get() = struct.fakeOOMAtWireClientMap
            set(value) { struct.fakeOOMAtWireClientMap = value }
        override var fakeOOMAtNativeMap: UInt
            get() = struct.fakeOOMAtNativeMap
            set(value) { struct.fakeOOMAtNativeMap = value }
        override var fakeOOMAtDevice: UInt
            get() = struct.fakeOOMAtDevice
            set(value) { struct.fakeOOMAtDevice = value }
    }
}

fun WGPUDawnFakeBufferOOMForTesting.toCValue(): CValue<webgpu.native.WGPUDawnFakeBufferOOMForTesting> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.fakeOOMAtWireClientMap = this@toCValue.fakeOOMAtWireClientMap
    this.fakeOOMAtNativeMap = this@toCValue.fakeOOMAtNativeMap
    this.fakeOOMAtDevice = this@toCValue.fakeOOMAtDevice
}

actual interface WGPUDawnFakeDeviceInitializeErrorForTesting {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFakeDeviceInitializeErrorForTesting = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFakeDeviceInitializeErrorForTesting =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFakeDeviceInitializeErrorForTesting) -> Unit): ArrayHolder<WGPUDawnFakeDeviceInitializeErrorForTesting> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting>) : WGPUDawnFakeDeviceInitializeErrorForTesting {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnFakeDeviceInitializeErrorForTesting {
        private val struct: webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUDawnFakeDeviceInitializeErrorForTesting.toCValue(): CValue<webgpu.native.WGPUDawnFakeDeviceInitializeErrorForTesting> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
}

actual interface WGPUDawnHostMappedPointerLimits {
    actual var chain: WGPUChainedStruct
    actual var hostMappedPointerAlignment: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnHostMappedPointerLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnHostMappedPointerLimits =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnHostMappedPointerLimits>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnHostMappedPointerLimits) -> Unit): ArrayHolder<WGPUDawnHostMappedPointerLimits> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnHostMappedPointerLimits>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnHostMappedPointerLimits>) : WGPUDawnHostMappedPointerLimits {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var hostMappedPointerAlignment: UInt
            get() = handle.useContents { this.hostMappedPointerAlignment }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnHostMappedPointerLimits {
        private val struct: webgpu.native.WGPUDawnHostMappedPointerLimits
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnHostMappedPointerLimits>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var hostMappedPointerAlignment: UInt
            get() = struct.hostMappedPointerAlignment
            set(value) { struct.hostMappedPointerAlignment = value }
    }
}

fun WGPUDawnHostMappedPointerLimits.toCValue(): CValue<webgpu.native.WGPUDawnHostMappedPointerLimits> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.hostMappedPointerAlignment = this@toCValue.hostMappedPointerAlignment
}

actual interface WGPUDawnInjectedInvalidSType {
    actual var chain: WGPUChainedStruct
    actual var invalidSType: WGPUSType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnInjectedInvalidSType = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnInjectedInvalidSType =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnInjectedInvalidSType>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnInjectedInvalidSType) -> Unit): ArrayHolder<WGPUDawnInjectedInvalidSType> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnInjectedInvalidSType>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnInjectedInvalidSType>) : WGPUDawnInjectedInvalidSType {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var invalidSType: WGPUSType
            get() = handle.useContents { this.invalidSType as WGPUSType }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnInjectedInvalidSType {
        private val struct: webgpu.native.WGPUDawnInjectedInvalidSType
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnInjectedInvalidSType>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var invalidSType: WGPUSType
            get() = struct.invalidSType as WGPUSType
            set(value) { struct.invalidSType = value }
    }
}

fun WGPUDawnInjectedInvalidSType.toCValue(): CValue<webgpu.native.WGPUDawnInjectedInvalidSType> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.invalidSType = this@toCValue.invalidSType
}

actual interface WGPUDawnRenderPassSampleCount {
    actual var chain: WGPUChainedStruct
    actual var sampleCount: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnRenderPassSampleCount = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnRenderPassSampleCount =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnRenderPassSampleCount>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnRenderPassSampleCount) -> Unit): ArrayHolder<WGPUDawnRenderPassSampleCount> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnRenderPassSampleCount>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnRenderPassSampleCount>) : WGPUDawnRenderPassSampleCount {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sampleCount: UInt
            get() = handle.useContents { this.sampleCount }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnRenderPassSampleCount {
        private val struct: webgpu.native.WGPUDawnRenderPassSampleCount
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnRenderPassSampleCount>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sampleCount: UInt
            get() = struct.sampleCount
            set(value) { struct.sampleCount = value }
    }
}

fun WGPUDawnRenderPassSampleCount.toCValue(): CValue<webgpu.native.WGPUDawnRenderPassSampleCount> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sampleCount = this@toCValue.sampleCount
}

actual interface WGPUDawnShaderModuleSPIRVOptionsDescriptor {
    actual var chain: WGPUChainedStruct
    actual var allowNonUniformDerivatives: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnShaderModuleSPIRVOptionsDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnShaderModuleSPIRVOptionsDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderModuleSPIRVOptionsDescriptor) -> Unit): ArrayHolder<WGPUDawnShaderModuleSPIRVOptionsDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor>) : WGPUDawnShaderModuleSPIRVOptionsDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var allowNonUniformDerivatives: UInt
            get() = handle.useContents { this.allowNonUniformDerivatives }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnShaderModuleSPIRVOptionsDescriptor {
        private val struct: webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var allowNonUniformDerivatives: UInt
            get() = struct.allowNonUniformDerivatives
            set(value) { struct.allowNonUniformDerivatives = value }
    }
}

fun WGPUDawnShaderModuleSPIRVOptionsDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnShaderModuleSPIRVOptionsDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.allowNonUniformDerivatives = this@toCValue.allowNonUniformDerivatives
}

actual interface WGPUDawnShaderSourceSPIRV {
    actual var chain: WGPUChainedStruct
    actual var codeSize: ULong
    actual var code: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnShaderSourceSPIRV = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnShaderSourceSPIRV =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnShaderSourceSPIRV>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUDawnShaderSourceSPIRV> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnShaderSourceSPIRV>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnShaderSourceSPIRV>) : WGPUDawnShaderSourceSPIRV {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var codeSize: ULong
            get() = handle.useContents { this.codeSize }
            set(value) { error("Setters not supported on ByValue") }
        override var code: NativeAddress?
            get() = handle.useContents { this.code?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnShaderSourceSPIRV {
        private val struct: webgpu.native.WGPUDawnShaderSourceSPIRV
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnShaderSourceSPIRV>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var codeSize: ULong
            get() = struct.codeSize
            set(value) { struct.codeSize = value }
        override var code: NativeAddress?
            get() = struct.code?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.code = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnShaderSourceSPIRV.toCValue(): CValue<webgpu.native.WGPUDawnShaderSourceSPIRV> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.codeSize = this@toCValue.codeSize
    this.code = this@toCValue.code?.pointer?.takeIf { this@toCValue.code?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnTexelCopyBufferRowAlignmentLimits {
    actual var chain: WGPUChainedStruct
    actual var minTexelCopyBufferRowAlignment: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnTexelCopyBufferRowAlignmentLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTexelCopyBufferRowAlignmentLimits =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTexelCopyBufferRowAlignmentLimits) -> Unit): ArrayHolder<WGPUDawnTexelCopyBufferRowAlignmentLimits> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits>) : WGPUDawnTexelCopyBufferRowAlignmentLimits {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var minTexelCopyBufferRowAlignment: UInt
            get() = handle.useContents { this.minTexelCopyBufferRowAlignment }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnTexelCopyBufferRowAlignmentLimits {
        private val struct: webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var minTexelCopyBufferRowAlignment: UInt
            get() = struct.minTexelCopyBufferRowAlignment
            set(value) { struct.minTexelCopyBufferRowAlignment = value }
    }
}

fun WGPUDawnTexelCopyBufferRowAlignmentLimits.toCValue(): CValue<webgpu.native.WGPUDawnTexelCopyBufferRowAlignmentLimits> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.minTexelCopyBufferRowAlignment = this@toCValue.minTexelCopyBufferRowAlignment
}

actual interface WGPUDawnTextureInternalUsageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var internalUsage: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnTextureInternalUsageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTextureInternalUsageDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnTextureInternalUsageDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTextureInternalUsageDescriptor) -> Unit): ArrayHolder<WGPUDawnTextureInternalUsageDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnTextureInternalUsageDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnTextureInternalUsageDescriptor>) : WGPUDawnTextureInternalUsageDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var internalUsage: ULong
            get() = handle.useContents { this.internalUsage }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnTextureInternalUsageDescriptor {
        private val struct: webgpu.native.WGPUDawnTextureInternalUsageDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnTextureInternalUsageDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var internalUsage: ULong
            get() = struct.internalUsage
            set(value) { struct.internalUsage = value }
    }
}

fun WGPUDawnTextureInternalUsageDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnTextureInternalUsageDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.internalUsage = this@toCValue.internalUsage
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnTogglesDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnTogglesDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnTogglesDescriptor) -> Unit): ArrayHolder<WGPUDawnTogglesDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnTogglesDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnTogglesDescriptor>) : WGPUDawnTogglesDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var enabledToggleCount: ULong
            get() = handle.useContents { this.enabledToggleCount }
            set(value) { error("Setters not supported on ByValue") }
        override var enabledToggles: NativeAddress?
            get() = handle.useContents { this.enabledToggles?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var disabledToggleCount: ULong
            get() = handle.useContents { this.disabledToggleCount }
            set(value) { error("Setters not supported on ByValue") }
        override var disabledToggles: NativeAddress?
            get() = handle.useContents { this.disabledToggles?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnTogglesDescriptor {
        private val struct: webgpu.native.WGPUDawnTogglesDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnTogglesDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var enabledToggleCount: ULong
            get() = struct.enabledToggleCount
            set(value) { struct.enabledToggleCount = value }
        override var enabledToggles: NativeAddress?
            get() = struct.enabledToggles?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.enabledToggles = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var disabledToggleCount: ULong
            get() = struct.disabledToggleCount
            set(value) { struct.disabledToggleCount = value }
        override var disabledToggles: NativeAddress?
            get() = struct.disabledToggles?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.disabledToggles = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnTogglesDescriptor.toCValue(): CValue<webgpu.native.WGPUDawnTogglesDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.enabledToggleCount = this@toCValue.enabledToggleCount
    this.enabledToggles = this@toCValue.enabledToggles?.pointer?.takeIf { this@toCValue.enabledToggles?.rawValue != 0L }?.reinterpret()
    this.disabledToggleCount = this@toCValue.disabledToggleCount
    this.disabledToggles = this@toCValue.disabledToggles?.pointer?.takeIf { this@toCValue.disabledToggles?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnWGSLBlocklist {
    actual var chain: WGPUChainedStruct
    actual var blocklistedFeatureCount: ULong
    actual var blocklistedFeatures: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnWGSLBlocklist = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnWGSLBlocklist =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnWGSLBlocklist>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWGSLBlocklist) -> Unit): ArrayHolder<WGPUDawnWGSLBlocklist> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnWGSLBlocklist>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnWGSLBlocklist>) : WGPUDawnWGSLBlocklist {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var blocklistedFeatureCount: ULong
            get() = handle.useContents { this.blocklistedFeatureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var blocklistedFeatures: NativeAddress?
            get() = handle.useContents { this.blocklistedFeatures?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnWGSLBlocklist {
        private val struct: webgpu.native.WGPUDawnWGSLBlocklist
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnWGSLBlocklist>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var blocklistedFeatureCount: ULong
            get() = struct.blocklistedFeatureCount
            set(value) { struct.blocklistedFeatureCount = value }
        override var blocklistedFeatures: NativeAddress?
            get() = struct.blocklistedFeatures?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.blocklistedFeatures = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnWGSLBlocklist.toCValue(): CValue<webgpu.native.WGPUDawnWGSLBlocklist> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.blocklistedFeatureCount = this@toCValue.blocklistedFeatureCount
    this.blocklistedFeatures = this@toCValue.blocklistedFeatures?.pointer?.takeIf { this@toCValue.blocklistedFeatures?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnWireWGSLControl {
    actual var chain: WGPUChainedStruct
    actual var enableExperimental: UInt
    actual var enableUnsafe: UInt
    actual var enableTesting: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnWireWGSLControl = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnWireWGSLControl =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnWireWGSLControl>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnWireWGSLControl) -> Unit): ArrayHolder<WGPUDawnWireWGSLControl> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnWireWGSLControl>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnWireWGSLControl>) : WGPUDawnWireWGSLControl {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var enableExperimental: UInt
            get() = handle.useContents { this.enableExperimental }
            set(value) { error("Setters not supported on ByValue") }
        override var enableUnsafe: UInt
            get() = handle.useContents { this.enableUnsafe }
            set(value) { error("Setters not supported on ByValue") }
        override var enableTesting: UInt
            get() = handle.useContents { this.enableTesting }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnWireWGSLControl {
        private val struct: webgpu.native.WGPUDawnWireWGSLControl
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnWireWGSLControl>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var enableExperimental: UInt
            get() = struct.enableExperimental
            set(value) { struct.enableExperimental = value }
        override var enableUnsafe: UInt
            get() = struct.enableUnsafe
            set(value) { struct.enableUnsafe = value }
        override var enableTesting: UInt
            get() = struct.enableTesting
            set(value) { struct.enableTesting = value }
    }
}

fun WGPUDawnWireWGSLControl.toCValue(): CValue<webgpu.native.WGPUDawnWireWGSLControl> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.enableExperimental = this@toCValue.enableExperimental
    this.enableUnsafe = this@toCValue.enableUnsafe
    this.enableTesting = this@toCValue.enableTesting
}

actual interface WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
    actual var chain: WGPUChainedStruct
    actual var selector: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUEmscriptenSurfaceSourceCanvasHTMLSelector =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUEmscriptenSurfaceSourceCanvasHTMLSelector) -> Unit): ArrayHolder<WGPUEmscriptenSurfaceSourceCanvasHTMLSelector> {
            val byteSize = sizeOf<webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>) : WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var selector: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.selector.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUEmscriptenSurfaceSourceCanvasHTMLSelector {
        private val struct: webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var selector: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.selector.ptr))
            set(value) {
                val destBytes = struct.selector.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUEmscriptenSurfaceSourceCanvasHTMLSelector.toCValue(): CValue<webgpu.native.WGPUEmscriptenSurfaceSourceCanvasHTMLSelector> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_selector = this.selector.ptr.reinterpret<ByteVar>()
    val src_selector = requireNotNull(this@toCValue.selector.handler.pointer).reinterpret<ByteVar>()
    val size_selector = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_selector) {
        dest_selector[i.toInt()] = src_selector[i.toInt()]
    }
}

actual interface WGPUExtent2D {
    actual var width: UInt
    actual var height: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExtent2D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExtent2D =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUExtent2D>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent2D) -> Unit): ArrayHolder<WGPUExtent2D> {
            val byteSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUExtent2D>) : WGPUExtent2D {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var width: UInt
            get() = handle.useContents { this.width }
            set(value) { error("Setters not supported on ByValue") }
        override var height: UInt
            get() = handle.useContents { this.height }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUExtent2D {
        private val struct: webgpu.native.WGPUExtent2D
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUExtent2D>().pointed
        
        override var width: UInt
            get() = struct.width
            set(value) { struct.width = value }
        override var height: UInt
            get() = struct.height
            set(value) { struct.height = value }
    }
}

fun WGPUExtent2D.toCValue(): CValue<webgpu.native.WGPUExtent2D> = cValue {
    this.width = this@toCValue.width
    this.height = this@toCValue.height
}

actual interface WGPUExtent3D {
    actual var width: UInt
    actual var height: UInt
    actual var depthOrArrayLayers: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExtent3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExtent3D =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUExtent3D>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExtent3D) -> Unit): ArrayHolder<WGPUExtent3D> {
            val byteSize = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUExtent3D>) : WGPUExtent3D {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var width: UInt
            get() = handle.useContents { this.width }
            set(value) { error("Setters not supported on ByValue") }
        override var height: UInt
            get() = handle.useContents { this.height }
            set(value) { error("Setters not supported on ByValue") }
        override var depthOrArrayLayers: UInt
            get() = handle.useContents { this.depthOrArrayLayers }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUExtent3D {
        private val struct: webgpu.native.WGPUExtent3D
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUExtent3D>().pointed
        
        override var width: UInt
            get() = struct.width
            set(value) { struct.width = value }
        override var height: UInt
            get() = struct.height
            set(value) { struct.height = value }
        override var depthOrArrayLayers: UInt
            get() = struct.depthOrArrayLayers
            set(value) { struct.depthOrArrayLayers = value }
    }
}

fun WGPUExtent3D.toCValue(): CValue<webgpu.native.WGPUExtent3D> = cValue {
    this.width = this@toCValue.width
    this.height = this@toCValue.height
    this.depthOrArrayLayers = this@toCValue.depthOrArrayLayers
}

actual interface WGPUExternalTextureBindingEntry {
    actual var chain: WGPUChainedStruct
    actual var externalTexture: WGPUExternalTexture?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingEntry =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUExternalTextureBindingEntry>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingEntry) -> Unit): ArrayHolder<WGPUExternalTextureBindingEntry> {
            val byteSize = sizeOf<webgpu.native.WGPUExternalTextureBindingEntry>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUExternalTextureBindingEntry>) : WGPUExternalTextureBindingEntry {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var externalTexture: WGPUExternalTexture?
            get() = handle.useContents { this.externalTexture?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUExternalTextureBindingEntry {
        private val struct: webgpu.native.WGPUExternalTextureBindingEntry
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUExternalTextureBindingEntry>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var externalTexture: WGPUExternalTexture?
            get() = struct.externalTexture?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) }
            set(value) { struct.externalTexture = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUExternalTextureBindingEntry.toCValue(): CValue<webgpu.native.WGPUExternalTextureBindingEntry> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.externalTexture = this@toCValue.externalTexture?.handler?.pointer?.takeIf { this@toCValue.externalTexture?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUExternalTextureBindingLayout {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUExternalTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUExternalTextureBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureBindingLayout) -> Unit): ArrayHolder<WGPUExternalTextureBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUExternalTextureBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUExternalTextureBindingLayout>) : WGPUExternalTextureBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUExternalTextureBindingLayout {
        private val struct: webgpu.native.WGPUExternalTextureBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUExternalTextureBindingLayout>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUExternalTextureBindingLayout.toCValue(): CValue<webgpu.native.WGPUExternalTextureBindingLayout> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
}

actual interface WGPUFuture {
    actual var id: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUFuture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUFuture =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUFuture>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFuture) -> Unit): ArrayHolder<WGPUFuture> {
            val byteSize = sizeOf<webgpu.native.WGPUFuture>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUFuture>) : WGPUFuture {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var id: ULong
            get() = handle.useContents { this.id }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUFuture {
        private val struct: webgpu.native.WGPUFuture
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUFuture>().pointed
        
        override var id: ULong
            get() = struct.id
            set(value) { struct.id = value }
    }
}

fun WGPUFuture.toCValue(): CValue<webgpu.native.WGPUFuture> = cValue {
    this.id = this@toCValue.id
}

actual interface WGPUInstanceLimits {
    actual var nextInChain: WGPUChainedStruct?
    actual var timedWaitAnyMaxCount: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUInstanceLimits = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUInstanceLimits =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUInstanceLimits>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceLimits) -> Unit): ArrayHolder<WGPUInstanceLimits> {
            val byteSize = sizeOf<webgpu.native.WGPUInstanceLimits>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUInstanceLimits>) : WGPUInstanceLimits {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var timedWaitAnyMaxCount: ULong
            get() = handle.useContents { this.timedWaitAnyMaxCount }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUInstanceLimits {
        private val struct: webgpu.native.WGPUInstanceLimits
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUInstanceLimits>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var timedWaitAnyMaxCount: ULong
            get() = struct.timedWaitAnyMaxCount
            set(value) { struct.timedWaitAnyMaxCount = value }
    }
}

fun WGPUInstanceLimits.toCValue(): CValue<webgpu.native.WGPUInstanceLimits> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.timedWaitAnyMaxCount = this@toCValue.timedWaitAnyMaxCount
}

actual interface WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
    actual var unused: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER) -> Unit): ArrayHolder<WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER> {
            val byteSize = sizeOf<webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>) : WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var unused: UInt
            get() = handle.useContents { this.unused }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER {
        private val struct: webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER>().pointed
        
        override var unused: UInt
            get() = struct.unused
            set(value) { struct.unused = value }
    }
}

fun WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER.toCValue(): CValue<webgpu.native.WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER> = cValue {
    this.unused = this@toCValue.unused
}

actual interface WGPUMemoryHeapInfo {
    actual var properties: ULong
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUMemoryHeapInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUMemoryHeapInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUMemoryHeapInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMemoryHeapInfo) -> Unit): ArrayHolder<WGPUMemoryHeapInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUMemoryHeapInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUMemoryHeapInfo>) : WGPUMemoryHeapInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var properties: ULong
            get() = handle.useContents { this.properties }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUMemoryHeapInfo {
        private val struct: webgpu.native.WGPUMemoryHeapInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUMemoryHeapInfo>().pointed
        
        override var properties: ULong
            get() = struct.properties
            set(value) { struct.properties = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
    }
}

fun WGPUMemoryHeapInfo.toCValue(): CValue<webgpu.native.WGPUMemoryHeapInfo> = cValue {
    this.properties = this@toCValue.properties
    this.size = this@toCValue.size
}

actual interface WGPUMultisampleState {
    actual var nextInChain: WGPUChainedStruct?
    actual var count: UInt
    actual var mask: UInt
    actual var alphaToCoverageEnabled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUMultisampleState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUMultisampleState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUMultisampleState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUMultisampleState) -> Unit): ArrayHolder<WGPUMultisampleState> {
            val byteSize = sizeOf<webgpu.native.WGPUMultisampleState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUMultisampleState>) : WGPUMultisampleState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var count: UInt
            get() = handle.useContents { this.count }
            set(value) { error("Setters not supported on ByValue") }
        override var mask: UInt
            get() = handle.useContents { this.mask }
            set(value) { error("Setters not supported on ByValue") }
        override var alphaToCoverageEnabled: UInt
            get() = handle.useContents { this.alphaToCoverageEnabled }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUMultisampleState {
        private val struct: webgpu.native.WGPUMultisampleState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUMultisampleState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var count: UInt
            get() = struct.count
            set(value) { struct.count = value }
        override var mask: UInt
            get() = struct.mask
            set(value) { struct.mask = value }
        override var alphaToCoverageEnabled: UInt
            get() = struct.alphaToCoverageEnabled
            set(value) { struct.alphaToCoverageEnabled = value }
    }
}

fun WGPUMultisampleState.toCValue(): CValue<webgpu.native.WGPUMultisampleState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.count = this@toCValue.count
    this.mask = this@toCValue.mask
    this.alphaToCoverageEnabled = this@toCValue.alphaToCoverageEnabled
}

actual interface WGPUOrigin2D {
    actual var x: UInt
    actual var y: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUOrigin2D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUOrigin2D =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUOrigin2D>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin2D) -> Unit): ArrayHolder<WGPUOrigin2D> {
            val byteSize = sizeOf<webgpu.native.WGPUOrigin2D>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUOrigin2D>) : WGPUOrigin2D {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var x: UInt
            get() = handle.useContents { this.x }
            set(value) { error("Setters not supported on ByValue") }
        override var y: UInt
            get() = handle.useContents { this.y }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUOrigin2D {
        private val struct: webgpu.native.WGPUOrigin2D
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUOrigin2D>().pointed
        
        override var x: UInt
            get() = struct.x
            set(value) { struct.x = value }
        override var y: UInt
            get() = struct.y
            set(value) { struct.y = value }
    }
}

fun WGPUOrigin2D.toCValue(): CValue<webgpu.native.WGPUOrigin2D> = cValue {
    this.x = this@toCValue.x
    this.y = this@toCValue.y
}

actual interface WGPUOrigin3D {
    actual var x: UInt
    actual var y: UInt
    actual var z: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUOrigin3D = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUOrigin3D =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUOrigin3D>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUOrigin3D) -> Unit): ArrayHolder<WGPUOrigin3D> {
            val byteSize = sizeOf<webgpu.native.WGPUOrigin3D>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUOrigin3D>) : WGPUOrigin3D {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var x: UInt
            get() = handle.useContents { this.x }
            set(value) { error("Setters not supported on ByValue") }
        override var y: UInt
            get() = handle.useContents { this.y }
            set(value) { error("Setters not supported on ByValue") }
        override var z: UInt
            get() = handle.useContents { this.z }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUOrigin3D {
        private val struct: webgpu.native.WGPUOrigin3D
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUOrigin3D>().pointed
        
        override var x: UInt
            get() = struct.x
            set(value) { struct.x = value }
        override var y: UInt
            get() = struct.y
            set(value) { struct.y = value }
        override var z: UInt
            get() = struct.z
            set(value) { struct.z = value }
    }
}

fun WGPUOrigin3D.toCValue(): CValue<webgpu.native.WGPUOrigin3D> = cValue {
    this.x = this@toCValue.x
    this.y = this@toCValue.y
    this.z = this@toCValue.z
}

actual interface WGPUPassTimestampWrites {
    actual var nextInChain: WGPUChainedStruct?
    actual var querySet: WGPUQuerySet?
    actual var beginningOfPassWriteIndex: UInt
    actual var endOfPassWriteIndex: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPassTimestampWrites = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPassTimestampWrites =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPassTimestampWrites>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPassTimestampWrites) -> Unit): ArrayHolder<WGPUPassTimestampWrites> {
            val byteSize = sizeOf<webgpu.native.WGPUPassTimestampWrites>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPassTimestampWrites>) : WGPUPassTimestampWrites {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var querySet: WGPUQuerySet?
            get() = handle.useContents { this.querySet?.let { NativeAddress.fromPointer(it) }?.let { WGPUQuerySet(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var beginningOfPassWriteIndex: UInt
            get() = handle.useContents { this.beginningOfPassWriteIndex }
            set(value) { error("Setters not supported on ByValue") }
        override var endOfPassWriteIndex: UInt
            get() = handle.useContents { this.endOfPassWriteIndex }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPassTimestampWrites {
        private val struct: webgpu.native.WGPUPassTimestampWrites
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPassTimestampWrites>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var querySet: WGPUQuerySet?
            get() = struct.querySet?.let { NativeAddress.fromPointer(it) }?.let { WGPUQuerySet(it) }
            set(value) { struct.querySet = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var beginningOfPassWriteIndex: UInt
            get() = struct.beginningOfPassWriteIndex
            set(value) { struct.beginningOfPassWriteIndex = value }
        override var endOfPassWriteIndex: UInt
            get() = struct.endOfPassWriteIndex
            set(value) { struct.endOfPassWriteIndex = value }
    }
}

fun WGPUPassTimestampWrites.toCValue(): CValue<webgpu.native.WGPUPassTimestampWrites> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.querySet = this@toCValue.querySet?.handler?.pointer?.takeIf { this@toCValue.querySet?.handler?.rawValue != 0L }?.reinterpret()
    this.beginningOfPassWriteIndex = this@toCValue.beginningOfPassWriteIndex
    this.endOfPassWriteIndex = this@toCValue.endOfPassWriteIndex
}

actual interface WGPUPipelineLayoutResourceTable {
    actual var chain: WGPUChainedStruct
    actual var usesResourceTable: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutResourceTable = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutResourceTable =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPipelineLayoutResourceTable>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutResourceTable) -> Unit): ArrayHolder<WGPUPipelineLayoutResourceTable> {
            val byteSize = sizeOf<webgpu.native.WGPUPipelineLayoutResourceTable>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPipelineLayoutResourceTable>) : WGPUPipelineLayoutResourceTable {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var usesResourceTable: UInt
            get() = handle.useContents { this.usesResourceTable }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPipelineLayoutResourceTable {
        private val struct: webgpu.native.WGPUPipelineLayoutResourceTable
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPipelineLayoutResourceTable>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var usesResourceTable: UInt
            get() = struct.usesResourceTable
            set(value) { struct.usesResourceTable = value }
    }
}

fun WGPUPipelineLayoutResourceTable.toCValue(): CValue<webgpu.native.WGPUPipelineLayoutResourceTable> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.usesResourceTable = this@toCValue.usesResourceTable
}

actual interface WGPUPipelineLayoutStorageAttachment {
    actual var nextInChain: WGPUChainedStruct?
    actual var offset: ULong
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutStorageAttachment = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutStorageAttachment =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPipelineLayoutStorageAttachment>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutStorageAttachment) -> Unit): ArrayHolder<WGPUPipelineLayoutStorageAttachment> {
            val byteSize = sizeOf<webgpu.native.WGPUPipelineLayoutStorageAttachment>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPipelineLayoutStorageAttachment>) : WGPUPipelineLayoutStorageAttachment {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPipelineLayoutStorageAttachment {
        private val struct: webgpu.native.WGPUPipelineLayoutStorageAttachment
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPipelineLayoutStorageAttachment>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
    }
}

fun WGPUPipelineLayoutStorageAttachment.toCValue(): CValue<webgpu.native.WGPUPipelineLayoutStorageAttachment> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.offset = this@toCValue.offset
    this.format = this@toCValue.format
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
        actual fun allocate(allocator: MemoryAllocator): WGPUPrimitiveState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPrimitiveState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPrimitiveState) -> Unit): ArrayHolder<WGPUPrimitiveState> {
            val byteSize = sizeOf<webgpu.native.WGPUPrimitiveState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPrimitiveState>) : WGPUPrimitiveState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var topology: WGPUPrimitiveTopology
            get() = handle.useContents { this.topology as WGPUPrimitiveTopology }
            set(value) { error("Setters not supported on ByValue") }
        override var stripIndexFormat: WGPUIndexFormat
            get() = handle.useContents { this.stripIndexFormat as WGPUIndexFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var frontFace: WGPUFrontFace
            get() = handle.useContents { this.frontFace as WGPUFrontFace }
            set(value) { error("Setters not supported on ByValue") }
        override var cullMode: WGPUCullMode
            get() = handle.useContents { this.cullMode as WGPUCullMode }
            set(value) { error("Setters not supported on ByValue") }
        override var unclippedDepth: UInt
            get() = handle.useContents { this.unclippedDepth }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPrimitiveState {
        private val struct: webgpu.native.WGPUPrimitiveState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPrimitiveState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var topology: WGPUPrimitiveTopology
            get() = struct.topology as WGPUPrimitiveTopology
            set(value) { struct.topology = value }
        override var stripIndexFormat: WGPUIndexFormat
            get() = struct.stripIndexFormat as WGPUIndexFormat
            set(value) { struct.stripIndexFormat = value }
        override var frontFace: WGPUFrontFace
            get() = struct.frontFace as WGPUFrontFace
            set(value) { struct.frontFace = value }
        override var cullMode: WGPUCullMode
            get() = struct.cullMode as WGPUCullMode
            set(value) { struct.cullMode = value }
        override var unclippedDepth: UInt
            get() = struct.unclippedDepth
            set(value) { struct.unclippedDepth = value }
    }
}

fun WGPUPrimitiveState.toCValue(): CValue<webgpu.native.WGPUPrimitiveState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.topology = this@toCValue.topology
    this.stripIndexFormat = this@toCValue.stripIndexFormat
    this.frontFace = this@toCValue.frontFace
    this.cullMode = this@toCValue.cullMode
    this.unclippedDepth = this@toCValue.unclippedDepth
}

actual interface WGPUQuerySetDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var type: WGPUQueryType
    actual var count: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUQuerySetDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUQuerySetDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUQuerySetDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQuerySetDescriptor) -> Unit): ArrayHolder<WGPUQuerySetDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUQuerySetDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUQuerySetDescriptor>) : WGPUQuerySetDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var type: WGPUQueryType
            get() = handle.useContents { this.type as WGPUQueryType }
            set(value) { error("Setters not supported on ByValue") }
        override var count: UInt
            get() = handle.useContents { this.count }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUQuerySetDescriptor {
        private val struct: webgpu.native.WGPUQuerySetDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUQuerySetDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var type: WGPUQueryType
            get() = struct.type as WGPUQueryType
            set(value) { struct.type = value }
        override var count: UInt
            get() = struct.count
            set(value) { struct.count = value }
    }
}

fun WGPUQuerySetDescriptor.toCValue(): CValue<webgpu.native.WGPUQuerySetDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.type = this@toCValue.type
    this.count = this@toCValue.count
}

actual interface WGPUQueueDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUQueueDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUQueueDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUQueueDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUQueueDescriptor) -> Unit): ArrayHolder<WGPUQueueDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUQueueDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUQueueDescriptor>) : WGPUQueueDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUQueueDescriptor {
        private val struct: webgpu.native.WGPUQueueDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUQueueDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUQueueDescriptor.toCValue(): CValue<webgpu.native.WGPUQueueDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPURenderBundleDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderBundleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderBundleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleDescriptor) -> Unit): ArrayHolder<WGPURenderBundleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPURenderBundleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderBundleDescriptor>) : WGPURenderBundleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderBundleDescriptor {
        private val struct: webgpu.native.WGPURenderBundleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderBundleDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPURenderBundleDescriptor.toCValue(): CValue<webgpu.native.WGPURenderBundleDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPURenderBundleEncoderResourceTable {
    actual var chain: WGPUChainedStruct
    actual var usesResourceTable: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderBundleEncoderResourceTable = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderResourceTable =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderBundleEncoderResourceTable>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderResourceTable) -> Unit): ArrayHolder<WGPURenderBundleEncoderResourceTable> {
            val byteSize = sizeOf<webgpu.native.WGPURenderBundleEncoderResourceTable>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderBundleEncoderResourceTable>) : WGPURenderBundleEncoderResourceTable {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var usesResourceTable: UInt
            get() = handle.useContents { this.usesResourceTable }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderBundleEncoderResourceTable {
        private val struct: webgpu.native.WGPURenderBundleEncoderResourceTable
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderBundleEncoderResourceTable>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var usesResourceTable: UInt
            get() = struct.usesResourceTable
            set(value) { struct.usesResourceTable = value }
    }
}

fun WGPURenderBundleEncoderResourceTable.toCValue(): CValue<webgpu.native.WGPURenderBundleEncoderResourceTable> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.usesResourceTable = this@toCValue.usesResourceTable
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDepthStencilAttachment =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassDepthStencilAttachment>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDepthStencilAttachment) -> Unit): ArrayHolder<WGPURenderPassDepthStencilAttachment> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassDepthStencilAttachment>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassDepthStencilAttachment>) : WGPURenderPassDepthStencilAttachment {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var view: WGPUTextureView?
            get() = handle.useContents { this.view?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var depthLoadOp: WGPULoadOp
            get() = handle.useContents { this.depthLoadOp as WGPULoadOp }
            set(value) { error("Setters not supported on ByValue") }
        override var depthStoreOp: WGPUStoreOp
            get() = handle.useContents { this.depthStoreOp as WGPUStoreOp }
            set(value) { error("Setters not supported on ByValue") }
        override var depthClearValue: Float
            get() = handle.useContents { this.depthClearValue }
            set(value) { error("Setters not supported on ByValue") }
        override var depthReadOnly: UInt
            get() = handle.useContents { this.depthReadOnly }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilLoadOp: WGPULoadOp
            get() = handle.useContents { this.stencilLoadOp as WGPULoadOp }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilStoreOp: WGPUStoreOp
            get() = handle.useContents { this.stencilStoreOp as WGPUStoreOp }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilClearValue: UInt
            get() = handle.useContents { this.stencilClearValue }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilReadOnly: UInt
            get() = handle.useContents { this.stencilReadOnly }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassDepthStencilAttachment {
        private val struct: webgpu.native.WGPURenderPassDepthStencilAttachment
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassDepthStencilAttachment>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var view: WGPUTextureView?
            get() = struct.view?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.view = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var depthLoadOp: WGPULoadOp
            get() = struct.depthLoadOp as WGPULoadOp
            set(value) { struct.depthLoadOp = value }
        override var depthStoreOp: WGPUStoreOp
            get() = struct.depthStoreOp as WGPUStoreOp
            set(value) { struct.depthStoreOp = value }
        override var depthClearValue: Float
            get() = struct.depthClearValue
            set(value) { struct.depthClearValue = value }
        override var depthReadOnly: UInt
            get() = struct.depthReadOnly
            set(value) { struct.depthReadOnly = value }
        override var stencilLoadOp: WGPULoadOp
            get() = struct.stencilLoadOp as WGPULoadOp
            set(value) { struct.stencilLoadOp = value }
        override var stencilStoreOp: WGPUStoreOp
            get() = struct.stencilStoreOp as WGPUStoreOp
            set(value) { struct.stencilStoreOp = value }
        override var stencilClearValue: UInt
            get() = struct.stencilClearValue
            set(value) { struct.stencilClearValue = value }
        override var stencilReadOnly: UInt
            get() = struct.stencilReadOnly
            set(value) { struct.stencilReadOnly = value }
    }
}

fun WGPURenderPassDepthStencilAttachment.toCValue(): CValue<webgpu.native.WGPURenderPassDepthStencilAttachment> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.view = this@toCValue.view?.handler?.pointer?.takeIf { this@toCValue.view?.handler?.rawValue != 0L }?.reinterpret()
    this.depthLoadOp = this@toCValue.depthLoadOp
    this.depthStoreOp = this@toCValue.depthStoreOp
    this.depthClearValue = this@toCValue.depthClearValue
    this.depthReadOnly = this@toCValue.depthReadOnly
    this.stencilLoadOp = this@toCValue.stencilLoadOp
    this.stencilStoreOp = this@toCValue.stencilStoreOp
    this.stencilClearValue = this@toCValue.stencilClearValue
    this.stencilReadOnly = this@toCValue.stencilReadOnly
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptorResolveRect =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassDescriptorResolveRect>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptorResolveRect) -> Unit): ArrayHolder<WGPURenderPassDescriptorResolveRect> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassDescriptorResolveRect>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassDescriptorResolveRect>) : WGPURenderPassDescriptorResolveRect {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var colorOffsetX: UInt
            get() = handle.useContents { this.colorOffsetX }
            set(value) { error("Setters not supported on ByValue") }
        override var colorOffsetY: UInt
            get() = handle.useContents { this.colorOffsetY }
            set(value) { error("Setters not supported on ByValue") }
        override var resolveOffsetX: UInt
            get() = handle.useContents { this.resolveOffsetX }
            set(value) { error("Setters not supported on ByValue") }
        override var resolveOffsetY: UInt
            get() = handle.useContents { this.resolveOffsetY }
            set(value) { error("Setters not supported on ByValue") }
        override var width: UInt
            get() = handle.useContents { this.width }
            set(value) { error("Setters not supported on ByValue") }
        override var height: UInt
            get() = handle.useContents { this.height }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassDescriptorResolveRect {
        private val struct: webgpu.native.WGPURenderPassDescriptorResolveRect
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassDescriptorResolveRect>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var colorOffsetX: UInt
            get() = struct.colorOffsetX
            set(value) { struct.colorOffsetX = value }
        override var colorOffsetY: UInt
            get() = struct.colorOffsetY
            set(value) { struct.colorOffsetY = value }
        override var resolveOffsetX: UInt
            get() = struct.resolveOffsetX
            set(value) { struct.resolveOffsetX = value }
        override var resolveOffsetY: UInt
            get() = struct.resolveOffsetY
            set(value) { struct.resolveOffsetY = value }
        override var width: UInt
            get() = struct.width
            set(value) { struct.width = value }
        override var height: UInt
            get() = struct.height
            set(value) { struct.height = value }
    }
}

fun WGPURenderPassDescriptorResolveRect.toCValue(): CValue<webgpu.native.WGPURenderPassDescriptorResolveRect> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.colorOffsetX = this@toCValue.colorOffsetX
    this.colorOffsetY = this@toCValue.colorOffsetY
    this.resolveOffsetX = this@toCValue.resolveOffsetX
    this.resolveOffsetY = this@toCValue.resolveOffsetY
    this.width = this@toCValue.width
    this.height = this@toCValue.height
}

actual interface WGPURenderPassMaxDrawCount {
    actual var chain: WGPUChainedStruct
    actual var maxDrawCount: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassMaxDrawCount = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassMaxDrawCount =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassMaxDrawCount>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassMaxDrawCount) -> Unit): ArrayHolder<WGPURenderPassMaxDrawCount> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassMaxDrawCount>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassMaxDrawCount>) : WGPURenderPassMaxDrawCount {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var maxDrawCount: ULong
            get() = handle.useContents { this.maxDrawCount }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassMaxDrawCount {
        private val struct: webgpu.native.WGPURenderPassMaxDrawCount
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassMaxDrawCount>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var maxDrawCount: ULong
            get() = struct.maxDrawCount
            set(value) { struct.maxDrawCount = value }
    }
}

fun WGPURenderPassMaxDrawCount.toCValue(): CValue<webgpu.native.WGPURenderPassMaxDrawCount> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.maxDrawCount = this@toCValue.maxDrawCount
}

actual interface WGPURequestAdapterWebGPUBackendOptions {
    actual var chain: WGPUChainedStruct
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterWebGPUBackendOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebGPUBackendOptions =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURequestAdapterWebGPUBackendOptions>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebGPUBackendOptions) -> Unit): ArrayHolder<WGPURequestAdapterWebGPUBackendOptions> {
            val byteSize = sizeOf<webgpu.native.WGPURequestAdapterWebGPUBackendOptions>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURequestAdapterWebGPUBackendOptions>) : WGPURequestAdapterWebGPUBackendOptions {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURequestAdapterWebGPUBackendOptions {
        private val struct: webgpu.native.WGPURequestAdapterWebGPUBackendOptions
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURequestAdapterWebGPUBackendOptions>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPURequestAdapterWebGPUBackendOptions.toCValue(): CValue<webgpu.native.WGPURequestAdapterWebGPUBackendOptions> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
}

actual interface WGPURequestAdapterWebXROptions {
    actual var chain: WGPUChainedStruct
    actual var xrCompatible: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURequestAdapterWebXROptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterWebXROptions =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURequestAdapterWebXROptions>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterWebXROptions) -> Unit): ArrayHolder<WGPURequestAdapterWebXROptions> {
            val byteSize = sizeOf<webgpu.native.WGPURequestAdapterWebXROptions>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURequestAdapterWebXROptions>) : WGPURequestAdapterWebXROptions {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var xrCompatible: UInt
            get() = handle.useContents { this.xrCompatible }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURequestAdapterWebXROptions {
        private val struct: webgpu.native.WGPURequestAdapterWebXROptions
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURequestAdapterWebXROptions>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var xrCompatible: UInt
            get() = struct.xrCompatible
            set(value) { struct.xrCompatible = value }
    }
}

fun WGPURequestAdapterWebXROptions.toCValue(): CValue<webgpu.native.WGPURequestAdapterWebXROptions> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.xrCompatible = this@toCValue.xrCompatible
}

actual interface WGPUResourceTableDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var size: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUResourceTableDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUResourceTableDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUResourceTableDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUResourceTableDescriptor) -> Unit): ArrayHolder<WGPUResourceTableDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUResourceTableDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUResourceTableDescriptor>) : WGPUResourceTableDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var size: UInt
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUResourceTableDescriptor {
        private val struct: webgpu.native.WGPUResourceTableDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUResourceTableDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var size: UInt
            get() = struct.size
            set(value) { struct.size = value }
    }
}

fun WGPUResourceTableDescriptor.toCValue(): CValue<webgpu.native.WGPUResourceTableDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.size = this@toCValue.size
}

actual interface WGPUSamplerBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUSamplerBindingType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSamplerBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSamplerBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSamplerBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerBindingLayout) -> Unit): ArrayHolder<WGPUSamplerBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUSamplerBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSamplerBindingLayout>) : WGPUSamplerBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var type: WGPUSamplerBindingType
            get() = handle.useContents { this.type as WGPUSamplerBindingType }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSamplerBindingLayout {
        private val struct: webgpu.native.WGPUSamplerBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSamplerBindingLayout>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var type: WGPUSamplerBindingType
            get() = struct.type as WGPUSamplerBindingType
            set(value) { struct.type = value }
    }
}

fun WGPUSamplerBindingLayout.toCValue(): CValue<webgpu.native.WGPUSamplerBindingLayout> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.type = this@toCValue.type
}

actual interface WGPUShaderModuleCompilationOptions {
    actual var chain: WGPUChainedStruct
    actual var strictMath: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderModuleCompilationOptions = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderModuleCompilationOptions =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUShaderModuleCompilationOptions>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleCompilationOptions) -> Unit): ArrayHolder<WGPUShaderModuleCompilationOptions> {
            val byteSize = sizeOf<webgpu.native.WGPUShaderModuleCompilationOptions>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUShaderModuleCompilationOptions>) : WGPUShaderModuleCompilationOptions {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var strictMath: UInt
            get() = handle.useContents { this.strictMath }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUShaderModuleCompilationOptions {
        private val struct: webgpu.native.WGPUShaderModuleCompilationOptions
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUShaderModuleCompilationOptions>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var strictMath: UInt
            get() = struct.strictMath
            set(value) { struct.strictMath = value }
    }
}

fun WGPUShaderModuleCompilationOptions.toCValue(): CValue<webgpu.native.WGPUShaderModuleCompilationOptions> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.strictMath = this@toCValue.strictMath
}

actual interface WGPUShaderSourceSPIRV {
    actual var chain: WGPUChainedStruct
    actual var codeSize: UInt
    actual var code: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderSourceSPIRV = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderSourceSPIRV =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUShaderSourceSPIRV>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceSPIRV) -> Unit): ArrayHolder<WGPUShaderSourceSPIRV> {
            val byteSize = sizeOf<webgpu.native.WGPUShaderSourceSPIRV>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUShaderSourceSPIRV>) : WGPUShaderSourceSPIRV {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var codeSize: UInt
            get() = handle.useContents { this.codeSize }
            set(value) { error("Setters not supported on ByValue") }
        override var code: NativeAddress?
            get() = handle.useContents { this.code?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUShaderSourceSPIRV {
        private val struct: webgpu.native.WGPUShaderSourceSPIRV
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUShaderSourceSPIRV>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var codeSize: UInt
            get() = struct.codeSize
            set(value) { struct.codeSize = value }
        override var code: NativeAddress?
            get() = struct.code?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.code = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUShaderSourceSPIRV.toCValue(): CValue<webgpu.native.WGPUShaderSourceSPIRV> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.codeSize = this@toCValue.codeSize
    this.code = this@toCValue.code?.pointer?.takeIf { this@toCValue.code?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUShaderSourceWGSL {
    actual var chain: WGPUChainedStruct
    actual var code: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderSourceWGSL = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderSourceWGSL =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUShaderSourceWGSL>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderSourceWGSL) -> Unit): ArrayHolder<WGPUShaderSourceWGSL> {
            val byteSize = sizeOf<webgpu.native.WGPUShaderSourceWGSL>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUShaderSourceWGSL>) : WGPUShaderSourceWGSL {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var code: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.code.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUShaderSourceWGSL {
        private val struct: webgpu.native.WGPUShaderSourceWGSL
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUShaderSourceWGSL>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var code: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.code.ptr))
            set(value) {
                val destBytes = struct.code.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUShaderSourceWGSL.toCValue(): CValue<webgpu.native.WGPUShaderSourceWGSL> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_code = this.code.ptr.reinterpret<ByteVar>()
    val src_code = requireNotNull(this@toCValue.code.handler.pointer).reinterpret<ByteVar>()
    val size_code = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_code) {
        dest_code[i.toInt()] = src_code[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryBeginAccessDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryBeginAccessDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor>) : WGPUSharedBufferMemoryBeginAccessDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var initialized: UInt
            get() = handle.useContents { this.initialized }
            set(value) { error("Setters not supported on ByValue") }
        override var fenceCount: ULong
            get() = handle.useContents { this.fenceCount }
            set(value) { error("Setters not supported on ByValue") }
        override var fences: NativeAddress?
            get() = handle.useContents { this.fences?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValueCount: ULong
            get() = handle.useContents { this.signaledValueCount }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValues: NativeAddress?
            get() = handle.useContents { this.signaledValues?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryBeginAccessDescriptor {
        private val struct: webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var initialized: UInt
            get() = struct.initialized
            set(value) { struct.initialized = value }
        override var fenceCount: ULong
            get() = struct.fenceCount
            set(value) { struct.fenceCount = value }
        override var fences: NativeAddress?
            get() = struct.fences?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.fences = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var signaledValueCount: ULong
            get() = struct.signaledValueCount
            set(value) { struct.signaledValueCount = value }
        override var signaledValues: NativeAddress?
            get() = struct.signaledValues?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.signaledValues = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedBufferMemoryBeginAccessDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryBeginAccessDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.initialized = this@toCValue.initialized
    this.fenceCount = this@toCValue.fenceCount
    this.fences = this@toCValue.fences?.pointer?.takeIf { this@toCValue.fences?.rawValue != 0L }?.reinterpret()
    this.signaledValueCount = this@toCValue.signaledValueCount
    this.signaledValues = this@toCValue.signaledValues?.pointer?.takeIf { this@toCValue.signaledValues?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryEndAccessState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryEndAccessState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedBufferMemoryEndAccessState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryEndAccessState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryEndAccessState>) : WGPUSharedBufferMemoryEndAccessState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var initialized: UInt
            get() = handle.useContents { this.initialized }
            set(value) { error("Setters not supported on ByValue") }
        override var fenceCount: ULong
            get() = handle.useContents { this.fenceCount }
            set(value) { error("Setters not supported on ByValue") }
        override var fences: NativeAddress?
            get() = handle.useContents { this.fences?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValueCount: ULong
            get() = handle.useContents { this.signaledValueCount }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValues: NativeAddress?
            get() = handle.useContents { this.signaledValues?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryEndAccessState {
        private val struct: webgpu.native.WGPUSharedBufferMemoryEndAccessState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryEndAccessState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var initialized: UInt
            get() = struct.initialized
            set(value) { struct.initialized = value }
        override var fenceCount: ULong
            get() = struct.fenceCount
            set(value) { struct.fenceCount = value }
        override var fences: NativeAddress?
            get() = struct.fences?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.fences = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var signaledValueCount: ULong
            get() = struct.signaledValueCount
            set(value) { struct.signaledValueCount = value }
        override var signaledValues: NativeAddress?
            get() = struct.signaledValues?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.signaledValues = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedBufferMemoryEndAccessState.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryEndAccessState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.initialized = this@toCValue.initialized
    this.fenceCount = this@toCValue.fenceCount
    this.fences = this@toCValue.fences?.pointer?.takeIf { this@toCValue.fences?.rawValue != 0L }?.reinterpret()
    this.signaledValueCount = this@toCValue.signaledValueCount
    this.signaledValues = this@toCValue.signaledValues?.pointer?.takeIf { this@toCValue.signaledValues?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryFromWindowsHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryFromWindowsHandleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryFromWindowsHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryFromWindowsHandleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor>) : WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: NativeAddress?
            get() = handle.useContents { this.handle?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryFromWindowsHandleDescriptor {
        private val struct: webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: NativeAddress?
            get() = struct.handle?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.handle = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
    }
}

fun WGPUSharedBufferMemoryFromWindowsHandleDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryFromWindowsHandleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2?.pointer?.takeIf { this@toCValue.handle_2?.rawValue != 0L }?.reinterpret()
    this.size = this@toCValue.size
}

actual interface WGPUSharedBufferMemoryHostPointerDescriptor {
    actual var chain: WGPUChainedStruct
    actual var pointer: NativeAddress?
    actual var size: ULong
    actual var disposeCallbackInfo: WGPUDisposeCallbackInfo
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryHostPointerDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryHostPointerDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryHostPointerDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryHostPointerDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor>) : WGPUSharedBufferMemoryHostPointerDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var pointer: NativeAddress?
            get() = handle.useContents { this.pointer?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        override var disposeCallbackInfo: WGPUDisposeCallbackInfo
            get() = handle.useContents { WGPUDisposeCallbackInfo.ByReference(NativeAddress.fromPointer(this.disposeCallbackInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryHostPointerDescriptor {
        private val struct: webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var pointer: NativeAddress?
            get() = struct.pointer?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.pointer = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
        override var disposeCallbackInfo: WGPUDisposeCallbackInfo
            get() = WGPUDisposeCallbackInfo.ByReference(NativeAddress.fromPointer(struct.disposeCallbackInfo.ptr))
            set(value) {
                val destBytes = struct.disposeCallbackInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUDisposeCallbackInfo>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedBufferMemoryHostPointerDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryHostPointerDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.pointer = this@toCValue.pointer?.pointer?.takeIf { this@toCValue.pointer?.rawValue != 0L }?.reinterpret()
    this.size = this@toCValue.size
    val dest_disposeCallbackInfo = this.disposeCallbackInfo.ptr.reinterpret<ByteVar>()
    val src_disposeCallbackInfo = requireNotNull(this@toCValue.disposeCallbackInfo.handler.pointer).reinterpret<ByteVar>()
    val size_disposeCallbackInfo = sizeOf<webgpu.native.WGPUDisposeCallbackInfo>().toLong()
    for (i in 0L until size_disposeCallbackInfo) {
        dest_disposeCallbackInfo[i.toInt()] = src_disposeCallbackInfo[i.toInt()]
    }
}

actual interface WGPUSharedBufferMemoryProperties {
    actual var nextInChain: WGPUChainedStruct?
    actual var usage: ULong
    actual var size: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryProperties =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryProperties>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryProperties) -> Unit): ArrayHolder<WGPUSharedBufferMemoryProperties> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryProperties>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryProperties>) : WGPUSharedBufferMemoryProperties {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryProperties {
        private val struct: webgpu.native.WGPUSharedBufferMemoryProperties
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryProperties>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
    }
}

fun WGPUSharedBufferMemoryProperties.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryProperties> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.usage = this@toCValue.usage
    this.size = this@toCValue.size
}

actual interface WGPUSharedFenceDXGISharedHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor>) : WGPUSharedFenceDXGISharedHandleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: NativeAddress?
            get() = handle.useContents { this.handle?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceDXGISharedHandleDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: NativeAddress?
            get() = struct.handle?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.handle = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceDXGISharedHandleDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceDXGISharedHandleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2?.pointer?.takeIf { this@toCValue.handle_2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceDXGISharedHandleExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDXGISharedHandleExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDXGISharedHandleExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDXGISharedHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceDXGISharedHandleExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo>) : WGPUSharedFenceDXGISharedHandleExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: NativeAddress?
            get() = handle.useContents { this.handle?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceDXGISharedHandleExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: NativeAddress?
            get() = struct.handle?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.handle = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceDXGISharedHandleExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceDXGISharedHandleExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2?.pointer?.takeIf { this@toCValue.handle_2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceEGLSyncDescriptor {
    actual var chain: WGPUChainedStruct
    actual var sync: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceEGLSyncDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceEGLSyncDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceEGLSyncDescriptor>) : WGPUSharedFenceEGLSyncDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sync: NativeAddress?
            get() = handle.useContents { this.sync?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceEGLSyncDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceEGLSyncDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceEGLSyncDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sync: NativeAddress?
            get() = struct.sync?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.sync = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceEGLSyncDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceEGLSyncDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sync = this@toCValue.sync?.pointer?.takeIf { this@toCValue.sync?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceEGLSyncExportInfo {
    actual var chain: WGPUChainedStruct
    actual var sync: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceEGLSyncExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceEGLSyncExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceEGLSyncExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceEGLSyncExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceEGLSyncExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceEGLSyncExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceEGLSyncExportInfo>) : WGPUSharedFenceEGLSyncExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sync: NativeAddress?
            get() = handle.useContents { this.sync?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceEGLSyncExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceEGLSyncExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceEGLSyncExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sync: NativeAddress?
            get() = struct.sync?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.sync = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceEGLSyncExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceEGLSyncExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sync = this@toCValue.sync?.pointer?.takeIf { this@toCValue.sync?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceMTLSharedEventDescriptor {
    actual var chain: WGPUChainedStruct
    actual var sharedEvent: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor>) : WGPUSharedFenceMTLSharedEventDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sharedEvent: NativeAddress?
            get() = handle.useContents { this.sharedEvent?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceMTLSharedEventDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sharedEvent: NativeAddress?
            get() = struct.sharedEvent?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.sharedEvent = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceMTLSharedEventDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceMTLSharedEventDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sharedEvent = this@toCValue.sharedEvent?.pointer?.takeIf { this@toCValue.sharedEvent?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceMTLSharedEventExportInfo {
    actual var chain: WGPUChainedStruct
    actual var sharedEvent: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceMTLSharedEventExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceMTLSharedEventExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceMTLSharedEventExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceMTLSharedEventExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo>) : WGPUSharedFenceMTLSharedEventExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sharedEvent: NativeAddress?
            get() = handle.useContents { this.sharedEvent?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceMTLSharedEventExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sharedEvent: NativeAddress?
            get() = struct.sharedEvent?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.sharedEvent = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedFenceMTLSharedEventExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceMTLSharedEventExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sharedEvent = this@toCValue.sharedEvent?.pointer?.takeIf { this@toCValue.sharedEvent?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedFenceSyncFDDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceSyncFDDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceSyncFDDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceSyncFDDescriptor>) : WGPUSharedFenceSyncFDDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: Int
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceSyncFDDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceSyncFDDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceSyncFDDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: Int
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceSyncFDDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceSyncFDDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedFenceSyncFDExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceSyncFDExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceSyncFDExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceSyncFDExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceSyncFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceSyncFDExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceSyncFDExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceSyncFDExportInfo>) : WGPUSharedFenceSyncFDExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: Int
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceSyncFDExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceSyncFDExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceSyncFDExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: Int
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceSyncFDExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceSyncFDExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>) : WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: Int
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: Int
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>) : WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: Int
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: Int
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>) : WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: UInt
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceVkSemaphoreZirconHandleDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: UInt
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceVkSemaphoreZirconHandleDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
    actual var chain: WGPUChainedStruct
    actual var handle_2: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceVkSemaphoreZirconHandleExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceVkSemaphoreZirconHandleExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceVkSemaphoreZirconHandleExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>) : WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: UInt
            get() = handle.useContents { this.handle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceVkSemaphoreZirconHandleExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: UInt
            get() = struct.handle
            set(value) { struct.handle = value }
    }
}

fun WGPUSharedFenceVkSemaphoreZirconHandleExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceVkSemaphoreZirconHandleExportInfo> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2
}

actual interface WGPUSharedTextureMemoryAHardwareBufferDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor>) : WGPUSharedTextureMemoryAHardwareBufferDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: NativeAddress?
            get() = handle.useContents { this.handle?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryAHardwareBufferDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: NativeAddress?
            get() = struct.handle?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.handle = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedTextureMemoryAHardwareBufferDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2?.pointer?.takeIf { this@toCValue.handle_2?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedTextureMemoryD3D11BeginState {
    actual var chain: WGPUChainedStruct
    actual var requiresEndAccessFence: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3D11BeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3D11BeginState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryD3D11BeginState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3D11BeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3D11BeginState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryD3D11BeginState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryD3D11BeginState>) : WGPUSharedTextureMemoryD3D11BeginState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var requiresEndAccessFence: UInt
            get() = handle.useContents { this.requiresEndAccessFence }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryD3D11BeginState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryD3D11BeginState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryD3D11BeginState>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var requiresEndAccessFence: UInt
            get() = struct.requiresEndAccessFence
            set(value) { struct.requiresEndAccessFence = value }
    }
}

fun WGPUSharedTextureMemoryD3D11BeginState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryD3D11BeginState> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.requiresEndAccessFence = this@toCValue.requiresEndAccessFence
}

actual interface WGPUSharedTextureMemoryD3DSwapchainBeginState {
    actual var chain: WGPUChainedStruct
    actual var isSwapchain: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryD3DSwapchainBeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryD3DSwapchainBeginState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryD3DSwapchainBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryD3DSwapchainBeginState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState>) : WGPUSharedTextureMemoryD3DSwapchainBeginState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var isSwapchain: UInt
            get() = handle.useContents { this.isSwapchain }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryD3DSwapchainBeginState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var isSwapchain: UInt
            get() = struct.isSwapchain
            set(value) { struct.isSwapchain = value }
    }
}

fun WGPUSharedTextureMemoryD3DSwapchainBeginState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryD3DSwapchainBeginState> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.isSwapchain = this@toCValue.isSwapchain
}

actual interface WGPUSharedTextureMemoryDmaBufPlane {
    actual var fd: Int
    actual var offset: ULong
    actual var stride: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDmaBufPlane = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufPlane =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryDmaBufPlane>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufPlane) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufPlane> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryDmaBufPlane>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryDmaBufPlane>) : WGPUSharedTextureMemoryDmaBufPlane {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var fd: Int
            get() = handle.useContents { this.fd }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var stride: UInt
            get() = handle.useContents { this.stride }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryDmaBufPlane {
        private val struct: webgpu.native.WGPUSharedTextureMemoryDmaBufPlane
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryDmaBufPlane>().pointed
        
        override var fd: Int
            get() = struct.fd
            set(value) { struct.fd = value }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var stride: UInt
            get() = struct.stride
            set(value) { struct.stride = value }
    }
}

fun WGPUSharedTextureMemoryDmaBufPlane.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryDmaBufPlane> = cValue {
    this.fd = this@toCValue.fd
    this.offset = this@toCValue.offset
    this.stride = this@toCValue.stride
}

actual interface WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var handle_2: NativeAddress?
    actual var useKeyedMutex: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDXGISharedHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDXGISharedHandleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDXGISharedHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDXGISharedHandleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor>) : WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var handle_2: NativeAddress?
            get() = handle.useContents { this.handle?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var useKeyedMutex: UInt
            get() = handle.useContents { this.useKeyedMutex }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryDXGISharedHandleDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var handle_2: NativeAddress?
            get() = struct.handle?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.handle = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var useKeyedMutex: UInt
            get() = struct.useKeyedMutex
            set(value) { struct.useKeyedMutex = value }
    }
}

fun WGPUSharedTextureMemoryDXGISharedHandleDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryDXGISharedHandleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.handle = this@toCValue.handle_2?.pointer?.takeIf { this@toCValue.handle_2?.rawValue != 0L }?.reinterpret()
    this.useKeyedMutex = this@toCValue.useKeyedMutex
}

actual interface WGPUSharedTextureMemoryEGLImageDescriptor {
    actual var chain: WGPUChainedStruct
    actual var image: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryEGLImageDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEGLImageDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEGLImageDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEGLImageDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor>) : WGPUSharedTextureMemoryEGLImageDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var image: NativeAddress?
            get() = handle.useContents { this.image?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryEGLImageDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var image: NativeAddress?
            get() = struct.image?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.image = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedTextureMemoryEGLImageDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryEGLImageDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.image = this@toCValue.image?.pointer?.takeIf { this@toCValue.image?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedTextureMemoryIOSurfaceDescriptor {
    actual var chain: WGPUChainedStruct
    actual var ioSurface: NativeAddress?
    actual var allowStorageBinding: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryIOSurfaceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryIOSurfaceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryIOSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryIOSurfaceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor>) : WGPUSharedTextureMemoryIOSurfaceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var ioSurface: NativeAddress?
            get() = handle.useContents { this.ioSurface?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var allowStorageBinding: UInt
            get() = handle.useContents { this.allowStorageBinding }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryIOSurfaceDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var ioSurface: NativeAddress?
            get() = struct.ioSurface?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.ioSurface = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var allowStorageBinding: UInt
            get() = struct.allowStorageBinding
            set(value) { struct.allowStorageBinding = value }
    }
}

fun WGPUSharedTextureMemoryIOSurfaceDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryIOSurfaceDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.ioSurface = this@toCValue.ioSurface?.pointer?.takeIf { this@toCValue.ioSurface?.rawValue != 0L }?.reinterpret()
    this.allowStorageBinding = this@toCValue.allowStorageBinding
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryOpaqueFDDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryOpaqueFDDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryOpaqueFDDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor>) : WGPUSharedTextureMemoryOpaqueFDDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var vkImageCreateInfo: NativeAddress?
            get() = handle.useContents { this.vkImageCreateInfo?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var memoryFD: Int
            get() = handle.useContents { this.memoryFD }
            set(value) { error("Setters not supported on ByValue") }
        override var memoryTypeIndex: UInt
            get() = handle.useContents { this.memoryTypeIndex }
            set(value) { error("Setters not supported on ByValue") }
        override var allocationSize: ULong
            get() = handle.useContents { this.allocationSize }
            set(value) { error("Setters not supported on ByValue") }
        override var dedicatedAllocation: UInt
            get() = handle.useContents { this.dedicatedAllocation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryOpaqueFDDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var vkImageCreateInfo: NativeAddress?
            get() = struct.vkImageCreateInfo?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.vkImageCreateInfo = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var memoryFD: Int
            get() = struct.memoryFD
            set(value) { struct.memoryFD = value }
        override var memoryTypeIndex: UInt
            get() = struct.memoryTypeIndex
            set(value) { struct.memoryTypeIndex = value }
        override var allocationSize: ULong
            get() = struct.allocationSize
            set(value) { struct.allocationSize = value }
        override var dedicatedAllocation: UInt
            get() = struct.dedicatedAllocation
            set(value) { struct.dedicatedAllocation = value }
    }
}

fun WGPUSharedTextureMemoryOpaqueFDDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryOpaqueFDDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.vkImageCreateInfo = this@toCValue.vkImageCreateInfo?.pointer?.takeIf { this@toCValue.vkImageCreateInfo?.rawValue != 0L }?.reinterpret()
    this.memoryFD = this@toCValue.memoryFD
    this.memoryTypeIndex = this@toCValue.memoryTypeIndex
    this.allocationSize = this@toCValue.allocationSize
    this.dedicatedAllocation = this@toCValue.dedicatedAllocation
}

actual interface WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
    actual var chain: WGPUChainedStruct
    actual var dedicatedAllocation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>) : WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var dedicatedAllocation: UInt
            get() = handle.useContents { this.dedicatedAllocation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var dedicatedAllocation: UInt
            get() = struct.dedicatedAllocation
            set(value) { struct.dedicatedAllocation = value }
    }
}

fun WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.dedicatedAllocation = this@toCValue.dedicatedAllocation
}

actual interface WGPUSharedTextureMemoryVkImageLayoutBeginState {
    actual var chain: WGPUChainedStruct
    actual var oldLayout: Int
    actual var newLayout: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutBeginState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutBeginState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutBeginState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutBeginState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState>) : WGPUSharedTextureMemoryVkImageLayoutBeginState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var oldLayout: Int
            get() = handle.useContents { this.oldLayout }
            set(value) { error("Setters not supported on ByValue") }
        override var newLayout: Int
            get() = handle.useContents { this.newLayout }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryVkImageLayoutBeginState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var oldLayout: Int
            get() = struct.oldLayout
            set(value) { struct.oldLayout = value }
        override var newLayout: Int
            get() = struct.newLayout
            set(value) { struct.newLayout = value }
    }
}

fun WGPUSharedTextureMemoryVkImageLayoutBeginState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutBeginState> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.oldLayout = this@toCValue.oldLayout
    this.newLayout = this@toCValue.newLayout
}

actual interface WGPUSharedTextureMemoryVkImageLayoutEndState {
    actual var chain: WGPUChainedStruct
    actual var oldLayout: Int
    actual var newLayout: Int
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryVkImageLayoutEndState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryVkImageLayoutEndState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryVkImageLayoutEndState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryVkImageLayoutEndState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState>) : WGPUSharedTextureMemoryVkImageLayoutEndState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var oldLayout: Int
            get() = handle.useContents { this.oldLayout }
            set(value) { error("Setters not supported on ByValue") }
        override var newLayout: Int
            get() = handle.useContents { this.newLayout }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryVkImageLayoutEndState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var oldLayout: Int
            get() = struct.oldLayout
            set(value) { struct.oldLayout = value }
        override var newLayout: Int
            get() = struct.newLayout
            set(value) { struct.newLayout = value }
    }
}

fun WGPUSharedTextureMemoryVkImageLayoutEndState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryVkImageLayoutEndState> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.oldLayout = this@toCValue.oldLayout
    this.newLayout = this@toCValue.newLayout
}

actual interface WGPUSharedTextureMemoryZirconHandleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var memoryFD: UInt
    actual var allocationSize: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryZirconHandleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryZirconHandleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryZirconHandleDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryZirconHandleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor>) : WGPUSharedTextureMemoryZirconHandleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var memoryFD: UInt
            get() = handle.useContents { this.memoryFD }
            set(value) { error("Setters not supported on ByValue") }
        override var allocationSize: ULong
            get() = handle.useContents { this.allocationSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryZirconHandleDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var memoryFD: UInt
            get() = struct.memoryFD
            set(value) { struct.memoryFD = value }
        override var allocationSize: ULong
            get() = struct.allocationSize
            set(value) { struct.allocationSize = value }
    }
}

fun WGPUSharedTextureMemoryZirconHandleDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryZirconHandleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.memoryFD = this@toCValue.memoryFD
    this.allocationSize = this@toCValue.allocationSize
}

actual interface WGPUStaticSamplerBindingLayout {
    actual var chain: WGPUChainedStruct
    actual var sampler: WGPUSampler?
    actual var sampledTextureBinding: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStaticSamplerBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStaticSamplerBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUStaticSamplerBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStaticSamplerBindingLayout) -> Unit): ArrayHolder<WGPUStaticSamplerBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUStaticSamplerBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUStaticSamplerBindingLayout>) : WGPUStaticSamplerBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sampler: WGPUSampler?
            get() = handle.useContents { this.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var sampledTextureBinding: UInt
            get() = handle.useContents { this.sampledTextureBinding }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUStaticSamplerBindingLayout {
        private val struct: webgpu.native.WGPUStaticSamplerBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUStaticSamplerBindingLayout>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sampler: WGPUSampler?
            get() = struct.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) }
            set(value) { struct.sampler = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var sampledTextureBinding: UInt
            get() = struct.sampledTextureBinding
            set(value) { struct.sampledTextureBinding = value }
    }
}

fun WGPUStaticSamplerBindingLayout.toCValue(): CValue<webgpu.native.WGPUStaticSamplerBindingLayout> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.sampler = this@toCValue.sampler?.handler?.pointer?.takeIf { this@toCValue.sampler?.handler?.rawValue != 0L }?.reinterpret()
    this.sampledTextureBinding = this@toCValue.sampledTextureBinding
}

actual interface WGPUStencilFaceState {
    actual var compare: WGPUCompareFunction
    actual var failOp: WGPUStencilOperation
    actual var depthFailOp: WGPUStencilOperation
    actual var passOp: WGPUStencilOperation
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStencilFaceState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStencilFaceState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStencilFaceState) -> Unit): ArrayHolder<WGPUStencilFaceState> {
            val byteSize = sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUStencilFaceState>) : WGPUStencilFaceState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var compare: WGPUCompareFunction
            get() = handle.useContents { this.compare as WGPUCompareFunction }
            set(value) { error("Setters not supported on ByValue") }
        override var failOp: WGPUStencilOperation
            get() = handle.useContents { this.failOp as WGPUStencilOperation }
            set(value) { error("Setters not supported on ByValue") }
        override var depthFailOp: WGPUStencilOperation
            get() = handle.useContents { this.depthFailOp as WGPUStencilOperation }
            set(value) { error("Setters not supported on ByValue") }
        override var passOp: WGPUStencilOperation
            get() = handle.useContents { this.passOp as WGPUStencilOperation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUStencilFaceState {
        private val struct: webgpu.native.WGPUStencilFaceState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUStencilFaceState>().pointed
        
        override var compare: WGPUCompareFunction
            get() = struct.compare as WGPUCompareFunction
            set(value) { struct.compare = value }
        override var failOp: WGPUStencilOperation
            get() = struct.failOp as WGPUStencilOperation
            set(value) { struct.failOp = value }
        override var depthFailOp: WGPUStencilOperation
            get() = struct.depthFailOp as WGPUStencilOperation
            set(value) { struct.depthFailOp = value }
        override var passOp: WGPUStencilOperation
            get() = struct.passOp as WGPUStencilOperation
            set(value) { struct.passOp = value }
    }
}

fun WGPUStencilFaceState.toCValue(): CValue<webgpu.native.WGPUStencilFaceState> = cValue {
    this.compare = this@toCValue.compare
    this.failOp = this@toCValue.failOp
    this.depthFailOp = this@toCValue.depthFailOp
    this.passOp = this@toCValue.passOp
}

actual interface WGPUStorageTextureBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var access: WGPUStorageTextureAccess
    actual var format: WGPUTextureFormat
    actual var viewDimension: WGPUTextureViewDimension
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUStorageTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUStorageTextureBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUStorageTextureBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUStorageTextureBindingLayout) -> Unit): ArrayHolder<WGPUStorageTextureBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUStorageTextureBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUStorageTextureBindingLayout>) : WGPUStorageTextureBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var access: WGPUStorageTextureAccess
            get() = handle.useContents { this.access as WGPUStorageTextureAccess }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var viewDimension: WGPUTextureViewDimension
            get() = handle.useContents { this.viewDimension as WGPUTextureViewDimension }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUStorageTextureBindingLayout {
        private val struct: webgpu.native.WGPUStorageTextureBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUStorageTextureBindingLayout>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var access: WGPUStorageTextureAccess
            get() = struct.access as WGPUStorageTextureAccess
            set(value) { struct.access = value }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var viewDimension: WGPUTextureViewDimension
            get() = struct.viewDimension as WGPUTextureViewDimension
            set(value) { struct.viewDimension = value }
    }
}

fun WGPUStorageTextureBindingLayout.toCValue(): CValue<webgpu.native.WGPUStorageTextureBindingLayout> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.access = this@toCValue.access
    this.format = this@toCValue.format
    this.viewDimension = this@toCValue.viewDimension
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSubgroupMatrixConfig =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSubgroupMatrixConfig>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSubgroupMatrixConfig) -> Unit): ArrayHolder<WGPUSubgroupMatrixConfig> {
            val byteSize = sizeOf<webgpu.native.WGPUSubgroupMatrixConfig>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSubgroupMatrixConfig>) : WGPUSubgroupMatrixConfig {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var componentType: WGPUSubgroupMatrixComponentType
            get() = handle.useContents { this.componentType as WGPUSubgroupMatrixComponentType }
            set(value) { error("Setters not supported on ByValue") }
        override var resultComponentType: WGPUSubgroupMatrixComponentType
            get() = handle.useContents { this.resultComponentType as WGPUSubgroupMatrixComponentType }
            set(value) { error("Setters not supported on ByValue") }
        override var M: UInt
            get() = handle.useContents { this.M }
            set(value) { error("Setters not supported on ByValue") }
        override var N: UInt
            get() = handle.useContents { this.N }
            set(value) { error("Setters not supported on ByValue") }
        override var K: UInt
            get() = handle.useContents { this.K }
            set(value) { error("Setters not supported on ByValue") }
        override var minSubgroupSize: UInt
            get() = handle.useContents { this.minSubgroupSize }
            set(value) { error("Setters not supported on ByValue") }
        override var maxSubgroupSize: UInt
            get() = handle.useContents { this.maxSubgroupSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSubgroupMatrixConfig {
        private val struct: webgpu.native.WGPUSubgroupMatrixConfig
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSubgroupMatrixConfig>().pointed
        
        override var componentType: WGPUSubgroupMatrixComponentType
            get() = struct.componentType as WGPUSubgroupMatrixComponentType
            set(value) { struct.componentType = value }
        override var resultComponentType: WGPUSubgroupMatrixComponentType
            get() = struct.resultComponentType as WGPUSubgroupMatrixComponentType
            set(value) { struct.resultComponentType = value }
        override var M: UInt
            get() = struct.M
            set(value) { struct.M = value }
        override var N: UInt
            get() = struct.N
            set(value) { struct.N = value }
        override var K: UInt
            get() = struct.K
            set(value) { struct.K = value }
        override var minSubgroupSize: UInt
            get() = struct.minSubgroupSize
            set(value) { struct.minSubgroupSize = value }
        override var maxSubgroupSize: UInt
            get() = struct.maxSubgroupSize
            set(value) { struct.maxSubgroupSize = value }
    }
}

fun WGPUSubgroupMatrixConfig.toCValue(): CValue<webgpu.native.WGPUSubgroupMatrixConfig> = cValue {
    this.componentType = this@toCValue.componentType
    this.resultComponentType = this@toCValue.resultComponentType
    this.M = this@toCValue.M
    this.N = this@toCValue.N
    this.K = this@toCValue.K
    this.minSubgroupSize = this@toCValue.minSubgroupSize
    this.maxSubgroupSize = this@toCValue.maxSubgroupSize
}

actual interface WGPUSupportedFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedFeatures =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSupportedFeatures>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedFeatures) -> Unit): ArrayHolder<WGPUSupportedFeatures> {
            val byteSize = sizeOf<webgpu.native.WGPUSupportedFeatures>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSupportedFeatures>) : WGPUSupportedFeatures {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var featureCount: ULong
            get() = handle.useContents { this.featureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var features: NativeAddress?
            get() = handle.useContents { this.features?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSupportedFeatures {
        private val struct: webgpu.native.WGPUSupportedFeatures
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSupportedFeatures>().pointed
        
        override var featureCount: ULong
            get() = struct.featureCount
            set(value) { struct.featureCount = value }
        override var features: NativeAddress?
            get() = struct.features?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.features = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSupportedFeatures.toCValue(): CValue<webgpu.native.WGPUSupportedFeatures> = cValue {
    this.featureCount = this@toCValue.featureCount
    this.features = this@toCValue.features?.pointer?.takeIf { this@toCValue.features?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSupportedInstanceFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedInstanceFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedInstanceFeatures =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSupportedInstanceFeatures>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedInstanceFeatures) -> Unit): ArrayHolder<WGPUSupportedInstanceFeatures> {
            val byteSize = sizeOf<webgpu.native.WGPUSupportedInstanceFeatures>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSupportedInstanceFeatures>) : WGPUSupportedInstanceFeatures {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var featureCount: ULong
            get() = handle.useContents { this.featureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var features: NativeAddress?
            get() = handle.useContents { this.features?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSupportedInstanceFeatures {
        private val struct: webgpu.native.WGPUSupportedInstanceFeatures
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSupportedInstanceFeatures>().pointed
        
        override var featureCount: ULong
            get() = struct.featureCount
            set(value) { struct.featureCount = value }
        override var features: NativeAddress?
            get() = struct.features?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.features = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSupportedInstanceFeatures.toCValue(): CValue<webgpu.native.WGPUSupportedInstanceFeatures> = cValue {
    this.featureCount = this@toCValue.featureCount
    this.features = this@toCValue.features?.pointer?.takeIf { this@toCValue.features?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSupportedWGSLLanguageFeatures {
    actual var featureCount: ULong
    actual var features: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSupportedWGSLLanguageFeatures = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSupportedWGSLLanguageFeatures =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSupportedWGSLLanguageFeatures>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSupportedWGSLLanguageFeatures) -> Unit): ArrayHolder<WGPUSupportedWGSLLanguageFeatures> {
            val byteSize = sizeOf<webgpu.native.WGPUSupportedWGSLLanguageFeatures>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSupportedWGSLLanguageFeatures>) : WGPUSupportedWGSLLanguageFeatures {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var featureCount: ULong
            get() = handle.useContents { this.featureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var features: NativeAddress?
            get() = handle.useContents { this.features?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSupportedWGSLLanguageFeatures {
        private val struct: webgpu.native.WGPUSupportedWGSLLanguageFeatures
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSupportedWGSLLanguageFeatures>().pointed
        
        override var featureCount: ULong
            get() = struct.featureCount
            set(value) { struct.featureCount = value }
        override var features: NativeAddress?
            get() = struct.features?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.features = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSupportedWGSLLanguageFeatures.toCValue(): CValue<webgpu.native.WGPUSupportedWGSLLanguageFeatures> = cValue {
    this.featureCount = this@toCValue.featureCount
    this.features = this@toCValue.features?.pointer?.takeIf { this@toCValue.features?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceCapabilities =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceCapabilities>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceCapabilities) -> Unit): ArrayHolder<WGPUSurfaceCapabilities> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceCapabilities>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceCapabilities>) : WGPUSurfaceCapabilities {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var usages: ULong
            get() = handle.useContents { this.usages }
            set(value) { error("Setters not supported on ByValue") }
        override var formatCount: ULong
            get() = handle.useContents { this.formatCount }
            set(value) { error("Setters not supported on ByValue") }
        override var formats: NativeAddress?
            get() = handle.useContents { this.formats?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var presentModeCount: ULong
            get() = handle.useContents { this.presentModeCount }
            set(value) { error("Setters not supported on ByValue") }
        override var presentModes: NativeAddress?
            get() = handle.useContents { this.presentModes?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var alphaModeCount: ULong
            get() = handle.useContents { this.alphaModeCount }
            set(value) { error("Setters not supported on ByValue") }
        override var alphaModes: NativeAddress?
            get() = handle.useContents { this.alphaModes?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceCapabilities {
        private val struct: webgpu.native.WGPUSurfaceCapabilities
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceCapabilities>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var usages: ULong
            get() = struct.usages
            set(value) { struct.usages = value }
        override var formatCount: ULong
            get() = struct.formatCount
            set(value) { struct.formatCount = value }
        override var formats: NativeAddress?
            get() = struct.formats?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.formats = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var presentModeCount: ULong
            get() = struct.presentModeCount
            set(value) { struct.presentModeCount = value }
        override var presentModes: NativeAddress?
            get() = struct.presentModes?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.presentModes = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var alphaModeCount: ULong
            get() = struct.alphaModeCount
            set(value) { struct.alphaModeCount = value }
        override var alphaModes: NativeAddress?
            get() = struct.alphaModes?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.alphaModes = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceCapabilities.toCValue(): CValue<webgpu.native.WGPUSurfaceCapabilities> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.usages = this@toCValue.usages
    this.formatCount = this@toCValue.formatCount
    this.formats = this@toCValue.formats?.pointer?.takeIf { this@toCValue.formats?.rawValue != 0L }?.reinterpret()
    this.presentModeCount = this@toCValue.presentModeCount
    this.presentModes = this@toCValue.presentModes?.pointer?.takeIf { this@toCValue.presentModes?.rawValue != 0L }?.reinterpret()
    this.alphaModeCount = this@toCValue.alphaModeCount
    this.alphaModes = this@toCValue.alphaModes?.pointer?.takeIf { this@toCValue.alphaModes?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceColorManagement {
    actual var chain: WGPUChainedStruct
    actual var colorSpace: WGPUPredefinedColorSpace
    actual var toneMappingMode: WGPUToneMappingMode
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceColorManagement = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceColorManagement =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceColorManagement>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceColorManagement) -> Unit): ArrayHolder<WGPUSurfaceColorManagement> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceColorManagement>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceColorManagement>) : WGPUSurfaceColorManagement {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var colorSpace: WGPUPredefinedColorSpace
            get() = handle.useContents { this.colorSpace as WGPUPredefinedColorSpace }
            set(value) { error("Setters not supported on ByValue") }
        override var toneMappingMode: WGPUToneMappingMode
            get() = handle.useContents { this.toneMappingMode as WGPUToneMappingMode }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceColorManagement {
        private val struct: webgpu.native.WGPUSurfaceColorManagement
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceColorManagement>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var colorSpace: WGPUPredefinedColorSpace
            get() = struct.colorSpace as WGPUPredefinedColorSpace
            set(value) { struct.colorSpace = value }
        override var toneMappingMode: WGPUToneMappingMode
            get() = struct.toneMappingMode as WGPUToneMappingMode
            set(value) { struct.toneMappingMode = value }
    }
}

fun WGPUSurfaceColorManagement.toCValue(): CValue<webgpu.native.WGPUSurfaceColorManagement> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.colorSpace = this@toCValue.colorSpace
    this.toneMappingMode = this@toCValue.toneMappingMode
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceConfiguration =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceConfiguration>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceConfiguration) -> Unit): ArrayHolder<WGPUSurfaceConfiguration> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceConfiguration>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceConfiguration>) : WGPUSurfaceConfiguration {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var device: WGPUDevice?
            get() = handle.useContents { this.device?.let { NativeAddress.fromPointer(it) }?.let { WGPUDevice(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        override var width: UInt
            get() = handle.useContents { this.width }
            set(value) { error("Setters not supported on ByValue") }
        override var height: UInt
            get() = handle.useContents { this.height }
            set(value) { error("Setters not supported on ByValue") }
        override var viewFormatCount: ULong
            get() = handle.useContents { this.viewFormatCount }
            set(value) { error("Setters not supported on ByValue") }
        override var viewFormats: NativeAddress?
            get() = handle.useContents { this.viewFormats?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var alphaMode: WGPUCompositeAlphaMode
            get() = handle.useContents { this.alphaMode as WGPUCompositeAlphaMode }
            set(value) { error("Setters not supported on ByValue") }
        override var presentMode: WGPUPresentMode
            get() = handle.useContents { this.presentMode as WGPUPresentMode }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceConfiguration {
        private val struct: webgpu.native.WGPUSurfaceConfiguration
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceConfiguration>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var device: WGPUDevice?
            get() = struct.device?.let { NativeAddress.fromPointer(it) }?.let { WGPUDevice(it) }
            set(value) { struct.device = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
        override var width: UInt
            get() = struct.width
            set(value) { struct.width = value }
        override var height: UInt
            get() = struct.height
            set(value) { struct.height = value }
        override var viewFormatCount: ULong
            get() = struct.viewFormatCount
            set(value) { struct.viewFormatCount = value }
        override var viewFormats: NativeAddress?
            get() = struct.viewFormats?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.viewFormats = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var alphaMode: WGPUCompositeAlphaMode
            get() = struct.alphaMode as WGPUCompositeAlphaMode
            set(value) { struct.alphaMode = value }
        override var presentMode: WGPUPresentMode
            get() = struct.presentMode as WGPUPresentMode
            set(value) { struct.presentMode = value }
    }
}

fun WGPUSurfaceConfiguration.toCValue(): CValue<webgpu.native.WGPUSurfaceConfiguration> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.device = this@toCValue.device?.handler?.pointer?.takeIf { this@toCValue.device?.handler?.rawValue != 0L }?.reinterpret()
    this.format = this@toCValue.format
    this.usage = this@toCValue.usage
    this.width = this@toCValue.width
    this.height = this@toCValue.height
    this.viewFormatCount = this@toCValue.viewFormatCount
    this.viewFormats = this@toCValue.viewFormats?.pointer?.takeIf { this@toCValue.viewFormats?.rawValue != 0L }?.reinterpret()
    this.alphaMode = this@toCValue.alphaMode
    this.presentMode = this@toCValue.presentMode
}

actual interface WGPUSurfaceDescriptorFromWindowsCoreWindow {
    actual var chain: WGPUChainedStruct
    actual var coreWindow: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsCoreWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsCoreWindow =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsCoreWindow) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsCoreWindow> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow>) : WGPUSurfaceDescriptorFromWindowsCoreWindow {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var coreWindow: NativeAddress?
            get() = handle.useContents { this.coreWindow?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceDescriptorFromWindowsCoreWindow {
        private val struct: webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var coreWindow: NativeAddress?
            get() = struct.coreWindow?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.coreWindow = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceDescriptorFromWindowsCoreWindow.toCValue(): CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsCoreWindow> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.coreWindow = this@toCValue.coreWindow?.pointer?.takeIf { this@toCValue.coreWindow?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
    actual var chain: WGPUChainedStruct
    actual var swapChainPanel: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>) : WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var swapChainPanel: NativeAddress?
            get() = handle.useContents { this.swapChainPanel?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel {
        private val struct: webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var swapChainPanel: NativeAddress?
            get() = struct.swapChainPanel?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.swapChainPanel = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel.toCValue(): CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.swapChainPanel = this@toCValue.swapChainPanel?.pointer?.takeIf { this@toCValue.swapChainPanel?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
    actual var chain: WGPUChainedStruct
    actual var swapChainPanel: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel) -> Unit): ArrayHolder<WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>) : WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var swapChainPanel: NativeAddress?
            get() = handle.useContents { this.swapChainPanel?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel {
        private val struct: webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var swapChainPanel: NativeAddress?
            get() = struct.swapChainPanel?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.swapChainPanel = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel.toCValue(): CValue<webgpu.native.WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.swapChainPanel = this@toCValue.swapChainPanel?.pointer?.takeIf { this@toCValue.swapChainPanel?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceSourceAndroidNativeWindow {
    actual var chain: WGPUChainedStruct
    actual var window: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceAndroidNativeWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceAndroidNativeWindow =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceAndroidNativeWindow>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceAndroidNativeWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceAndroidNativeWindow> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceAndroidNativeWindow>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceAndroidNativeWindow>) : WGPUSurfaceSourceAndroidNativeWindow {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var window: NativeAddress?
            get() = handle.useContents { this.window?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceAndroidNativeWindow {
        private val struct: webgpu.native.WGPUSurfaceSourceAndroidNativeWindow
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceAndroidNativeWindow>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var window: NativeAddress?
            get() = struct.window?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.window = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceSourceAndroidNativeWindow.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceAndroidNativeWindow> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.window = this@toCValue.window?.pointer?.takeIf { this@toCValue.window?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceSourceMetalLayer {
    actual var chain: WGPUChainedStruct
    actual var layer: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceMetalLayer = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceMetalLayer =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceMetalLayer>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceMetalLayer) -> Unit): ArrayHolder<WGPUSurfaceSourceMetalLayer> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceMetalLayer>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceMetalLayer>) : WGPUSurfaceSourceMetalLayer {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var layer: NativeAddress?
            get() = handle.useContents { this.layer?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceMetalLayer {
        private val struct: webgpu.native.WGPUSurfaceSourceMetalLayer
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceMetalLayer>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var layer: NativeAddress?
            get() = struct.layer?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.layer = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceSourceMetalLayer.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceMetalLayer> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.layer = this@toCValue.layer?.pointer?.takeIf { this@toCValue.layer?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceSourceWaylandSurface {
    actual var chain: WGPUChainedStruct
    actual var display: NativeAddress?
    actual var surface: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWaylandSurface = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWaylandSurface =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceWaylandSurface>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWaylandSurface) -> Unit): ArrayHolder<WGPUSurfaceSourceWaylandSurface> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceWaylandSurface>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceWaylandSurface>) : WGPUSurfaceSourceWaylandSurface {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var display: NativeAddress?
            get() = handle.useContents { this.display?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var surface: NativeAddress?
            get() = handle.useContents { this.surface?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceWaylandSurface {
        private val struct: webgpu.native.WGPUSurfaceSourceWaylandSurface
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceWaylandSurface>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var display: NativeAddress?
            get() = struct.display?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.display = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var surface: NativeAddress?
            get() = struct.surface?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.surface = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceSourceWaylandSurface.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceWaylandSurface> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.display = this@toCValue.display?.pointer?.takeIf { this@toCValue.display?.rawValue != 0L }?.reinterpret()
    this.surface = this@toCValue.surface?.pointer?.takeIf { this@toCValue.surface?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceSourceWindowsHWND {
    actual var chain: WGPUChainedStruct
    actual var hinstance: NativeAddress?
    actual var hwnd: NativeAddress?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceWindowsHWND = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceWindowsHWND =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceWindowsHWND>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceWindowsHWND) -> Unit): ArrayHolder<WGPUSurfaceSourceWindowsHWND> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceWindowsHWND>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceWindowsHWND>) : WGPUSurfaceSourceWindowsHWND {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var hinstance: NativeAddress?
            get() = handle.useContents { this.hinstance?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var hwnd: NativeAddress?
            get() = handle.useContents { this.hwnd?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceWindowsHWND {
        private val struct: webgpu.native.WGPUSurfaceSourceWindowsHWND
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceWindowsHWND>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var hinstance: NativeAddress?
            get() = struct.hinstance?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.hinstance = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var hwnd: NativeAddress?
            get() = struct.hwnd?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.hwnd = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSurfaceSourceWindowsHWND.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceWindowsHWND> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.hinstance = this@toCValue.hinstance?.pointer?.takeIf { this@toCValue.hinstance?.rawValue != 0L }?.reinterpret()
    this.hwnd = this@toCValue.hwnd?.pointer?.takeIf { this@toCValue.hwnd?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSurfaceSourceXCBWindow {
    actual var chain: WGPUChainedStruct
    actual var connection: NativeAddress?
    actual var window: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXCBWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXCBWindow =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceXCBWindow>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXCBWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXCBWindow> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceXCBWindow>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceXCBWindow>) : WGPUSurfaceSourceXCBWindow {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var connection: NativeAddress?
            get() = handle.useContents { this.connection?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var window: UInt
            get() = handle.useContents { this.window }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceXCBWindow {
        private val struct: webgpu.native.WGPUSurfaceSourceXCBWindow
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceXCBWindow>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var connection: NativeAddress?
            get() = struct.connection?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.connection = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var window: UInt
            get() = struct.window
            set(value) { struct.window = value }
    }
}

fun WGPUSurfaceSourceXCBWindow.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceXCBWindow> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.connection = this@toCValue.connection?.pointer?.takeIf { this@toCValue.connection?.rawValue != 0L }?.reinterpret()
    this.window = this@toCValue.window
}

actual interface WGPUSurfaceSourceXlibWindow {
    actual var chain: WGPUChainedStruct
    actual var display: NativeAddress?
    actual var window: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceSourceXlibWindow = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceSourceXlibWindow =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceSourceXlibWindow>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceSourceXlibWindow) -> Unit): ArrayHolder<WGPUSurfaceSourceXlibWindow> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceSourceXlibWindow>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceSourceXlibWindow>) : WGPUSurfaceSourceXlibWindow {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var display: NativeAddress?
            get() = handle.useContents { this.display?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var window: ULong
            get() = handle.useContents { this.window }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceSourceXlibWindow {
        private val struct: webgpu.native.WGPUSurfaceSourceXlibWindow
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceSourceXlibWindow>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var display: NativeAddress?
            get() = struct.display?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.display = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var window: ULong
            get() = struct.window
            set(value) { struct.window = value }
    }
}

fun WGPUSurfaceSourceXlibWindow.toCValue(): CValue<webgpu.native.WGPUSurfaceSourceXlibWindow> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.display = this@toCValue.display?.pointer?.takeIf { this@toCValue.display?.rawValue != 0L }?.reinterpret()
    this.window = this@toCValue.window
}

actual interface WGPUSurfaceTexture {
    actual var nextInChain: WGPUChainedStruct?
    actual var texture: WGPUTexture?
    actual var status: WGPUSurfaceGetCurrentTextureStatus
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceTexture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceTexture =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceTexture>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceTexture) -> Unit): ArrayHolder<WGPUSurfaceTexture> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceTexture>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceTexture>) : WGPUSurfaceTexture {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var texture: WGPUTexture?
            get() = handle.useContents { this.texture?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var status: WGPUSurfaceGetCurrentTextureStatus
            get() = handle.useContents { this.status as WGPUSurfaceGetCurrentTextureStatus }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceTexture {
        private val struct: webgpu.native.WGPUSurfaceTexture
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceTexture>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var texture: WGPUTexture?
            get() = struct.texture?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) }
            set(value) { struct.texture = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var status: WGPUSurfaceGetCurrentTextureStatus
            get() = struct.status as WGPUSurfaceGetCurrentTextureStatus
            set(value) { struct.status = value }
    }
}

fun WGPUSurfaceTexture.toCValue(): CValue<webgpu.native.WGPUSurfaceTexture> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.texture = this@toCValue.texture?.handler?.pointer?.takeIf { this@toCValue.texture?.handler?.rawValue != 0L }?.reinterpret()
    this.status = this@toCValue.status
}

actual interface WGPUTexelBufferBindingEntry {
    actual var chain: WGPUChainedStruct
    actual var texelBufferView: WGPUTexelBufferView?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingEntry = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingEntry =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelBufferBindingEntry>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingEntry) -> Unit): ArrayHolder<WGPUTexelBufferBindingEntry> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelBufferBindingEntry>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelBufferBindingEntry>) : WGPUTexelBufferBindingEntry {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var texelBufferView: WGPUTexelBufferView?
            get() = handle.useContents { this.texelBufferView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexelBufferView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelBufferBindingEntry {
        private val struct: webgpu.native.WGPUTexelBufferBindingEntry
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelBufferBindingEntry>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var texelBufferView: WGPUTexelBufferView?
            get() = struct.texelBufferView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexelBufferView(it) }
            set(value) { struct.texelBufferView = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUTexelBufferBindingEntry.toCValue(): CValue<webgpu.native.WGPUTexelBufferBindingEntry> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.texelBufferView = this@toCValue.texelBufferView?.handler?.pointer?.takeIf { this@toCValue.texelBufferView?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUTexelBufferBindingLayout {
    actual var chain: WGPUChainedStruct
    actual var access: WGPUTexelBufferAccess
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelBufferBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelBufferBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferBindingLayout) -> Unit): ArrayHolder<WGPUTexelBufferBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelBufferBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelBufferBindingLayout>) : WGPUTexelBufferBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var access: WGPUTexelBufferAccess
            get() = handle.useContents { this.access as WGPUTexelBufferAccess }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelBufferBindingLayout {
        private val struct: webgpu.native.WGPUTexelBufferBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelBufferBindingLayout>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var access: WGPUTexelBufferAccess
            get() = struct.access as WGPUTexelBufferAccess
            set(value) { struct.access = value }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
    }
}

fun WGPUTexelBufferBindingLayout.toCValue(): CValue<webgpu.native.WGPUTexelBufferBindingLayout> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.access = this@toCValue.access
    this.format = this@toCValue.format
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
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelBufferViewDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelBufferViewDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelBufferViewDescriptor) -> Unit): ArrayHolder<WGPUTexelBufferViewDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelBufferViewDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelBufferViewDescriptor>) : WGPUTexelBufferViewDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelBufferViewDescriptor {
        private val struct: webgpu.native.WGPUTexelBufferViewDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelBufferViewDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
    }
}

fun WGPUTexelBufferViewDescriptor.toCValue(): CValue<webgpu.native.WGPUTexelBufferViewDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.format = this@toCValue.format
    this.offset = this@toCValue.offset
    this.size = this@toCValue.size
}

actual interface WGPUTexelCopyBufferLayout {
    actual var offset: ULong
    actual var bytesPerRow: UInt
    actual var rowsPerImage: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelCopyBufferLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferLayout) -> Unit): ArrayHolder<WGPUTexelCopyBufferLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelCopyBufferLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelCopyBufferLayout>) : WGPUTexelCopyBufferLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var bytesPerRow: UInt
            get() = handle.useContents { this.bytesPerRow }
            set(value) { error("Setters not supported on ByValue") }
        override var rowsPerImage: UInt
            get() = handle.useContents { this.rowsPerImage }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelCopyBufferLayout {
        private val struct: webgpu.native.WGPUTexelCopyBufferLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelCopyBufferLayout>().pointed
        
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var bytesPerRow: UInt
            get() = struct.bytesPerRow
            set(value) { struct.bytesPerRow = value }
        override var rowsPerImage: UInt
            get() = struct.rowsPerImage
            set(value) { struct.rowsPerImage = value }
    }
}

fun WGPUTexelCopyBufferLayout.toCValue(): CValue<webgpu.native.WGPUTexelCopyBufferLayout> = cValue {
    this.offset = this@toCValue.offset
    this.bytesPerRow = this@toCValue.bytesPerRow
    this.rowsPerImage = this@toCValue.rowsPerImage
}

actual interface WGPUTextureBindingLayout {
    actual var nextInChain: WGPUChainedStruct?
    actual var sampleType: WGPUTextureSampleType
    actual var viewDimension: WGPUTextureViewDimension
    actual var multisampled: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureBindingLayout = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureBindingLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureBindingLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingLayout) -> Unit): ArrayHolder<WGPUTextureBindingLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureBindingLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureBindingLayout>) : WGPUTextureBindingLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var sampleType: WGPUTextureSampleType
            get() = handle.useContents { this.sampleType as WGPUTextureSampleType }
            set(value) { error("Setters not supported on ByValue") }
        override var viewDimension: WGPUTextureViewDimension
            get() = handle.useContents { this.viewDimension as WGPUTextureViewDimension }
            set(value) { error("Setters not supported on ByValue") }
        override var multisampled: UInt
            get() = handle.useContents { this.multisampled }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureBindingLayout {
        private val struct: webgpu.native.WGPUTextureBindingLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureBindingLayout>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var sampleType: WGPUTextureSampleType
            get() = struct.sampleType as WGPUTextureSampleType
            set(value) { struct.sampleType = value }
        override var viewDimension: WGPUTextureViewDimension
            get() = struct.viewDimension as WGPUTextureViewDimension
            set(value) { struct.viewDimension = value }
        override var multisampled: UInt
            get() = struct.multisampled
            set(value) { struct.multisampled = value }
    }
}

fun WGPUTextureBindingLayout.toCValue(): CValue<webgpu.native.WGPUTextureBindingLayout> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.sampleType = this@toCValue.sampleType
    this.viewDimension = this@toCValue.viewDimension
    this.multisampled = this@toCValue.multisampled
}

actual interface WGPUTextureBindingViewDimension {
    actual var chain: WGPUChainedStruct
    actual var textureBindingViewDimension: WGPUTextureViewDimension
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureBindingViewDimension = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureBindingViewDimension =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureBindingViewDimension>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureBindingViewDimension) -> Unit): ArrayHolder<WGPUTextureBindingViewDimension> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureBindingViewDimension>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureBindingViewDimension>) : WGPUTextureBindingViewDimension {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var textureBindingViewDimension: WGPUTextureViewDimension
            get() = handle.useContents { this.textureBindingViewDimension as WGPUTextureViewDimension }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureBindingViewDimension {
        private val struct: webgpu.native.WGPUTextureBindingViewDimension
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureBindingViewDimension>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var textureBindingViewDimension: WGPUTextureViewDimension
            get() = struct.textureBindingViewDimension as WGPUTextureViewDimension
            set(value) { struct.textureBindingViewDimension = value }
    }
}

fun WGPUTextureBindingViewDimension.toCValue(): CValue<webgpu.native.WGPUTextureBindingViewDimension> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.textureBindingViewDimension = this@toCValue.textureBindingViewDimension
}

actual interface WGPUTextureComponentSwizzle {
    actual var r: WGPUComponentSwizzle
    actual var g: WGPUComponentSwizzle
    actual var b: WGPUComponentSwizzle
    actual var a: WGPUComponentSwizzle
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzle = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzle =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureComponentSwizzle>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzle) -> Unit): ArrayHolder<WGPUTextureComponentSwizzle> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureComponentSwizzle>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureComponentSwizzle>) : WGPUTextureComponentSwizzle {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var r: WGPUComponentSwizzle
            get() = handle.useContents { this.r as WGPUComponentSwizzle }
            set(value) { error("Setters not supported on ByValue") }
        override var g: WGPUComponentSwizzle
            get() = handle.useContents { this.g as WGPUComponentSwizzle }
            set(value) { error("Setters not supported on ByValue") }
        override var b: WGPUComponentSwizzle
            get() = handle.useContents { this.b as WGPUComponentSwizzle }
            set(value) { error("Setters not supported on ByValue") }
        override var a: WGPUComponentSwizzle
            get() = handle.useContents { this.a as WGPUComponentSwizzle }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureComponentSwizzle {
        private val struct: webgpu.native.WGPUTextureComponentSwizzle
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureComponentSwizzle>().pointed
        
        override var r: WGPUComponentSwizzle
            get() = struct.r as WGPUComponentSwizzle
            set(value) { struct.r = value }
        override var g: WGPUComponentSwizzle
            get() = struct.g as WGPUComponentSwizzle
            set(value) { struct.g = value }
        override var b: WGPUComponentSwizzle
            get() = struct.b as WGPUComponentSwizzle
            set(value) { struct.b = value }
        override var a: WGPUComponentSwizzle
            get() = struct.a as WGPUComponentSwizzle
            set(value) { struct.a = value }
    }
}

fun WGPUTextureComponentSwizzle.toCValue(): CValue<webgpu.native.WGPUTextureComponentSwizzle> = cValue {
    this.r = this@toCValue.r
    this.g = this@toCValue.g
    this.b = this@toCValue.b
    this.a = this@toCValue.a
}

actual interface WGPUVertexAttribute {
    actual var nextInChain: WGPUChainedStruct?
    actual var format: WGPUVertexFormat
    actual var offset: ULong
    actual var shaderLocation: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUVertexAttribute = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexAttribute =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUVertexAttribute>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexAttribute) -> Unit): ArrayHolder<WGPUVertexAttribute> {
            val byteSize = sizeOf<webgpu.native.WGPUVertexAttribute>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUVertexAttribute>) : WGPUVertexAttribute {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUVertexFormat
            get() = handle.useContents { this.format as WGPUVertexFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var shaderLocation: UInt
            get() = handle.useContents { this.shaderLocation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUVertexAttribute {
        private val struct: webgpu.native.WGPUVertexAttribute
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUVertexAttribute>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var format: WGPUVertexFormat
            get() = struct.format as WGPUVertexFormat
            set(value) { struct.format = value }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var shaderLocation: UInt
            get() = struct.shaderLocation
            set(value) { struct.shaderLocation = value }
    }
}

fun WGPUVertexAttribute.toCValue(): CValue<webgpu.native.WGPUVertexAttribute> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.format = this@toCValue.format
    this.offset = this@toCValue.offset
    this.shaderLocation = this@toCValue.shaderLocation
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
        actual fun allocate(allocator: MemoryAllocator): WGPUYCbCrVkDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUYCbCrVkDescriptor) -> Unit): ArrayHolder<WGPUYCbCrVkDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUYCbCrVkDescriptor>) : WGPUYCbCrVkDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var vkFormat: UInt
            get() = handle.useContents { this.vkFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var vkYCbCrModel: UInt
            get() = handle.useContents { this.vkYCbCrModel }
            set(value) { error("Setters not supported on ByValue") }
        override var vkYCbCrRange: UInt
            get() = handle.useContents { this.vkYCbCrRange }
            set(value) { error("Setters not supported on ByValue") }
        override var vkComponentSwizzleRed: UInt
            get() = handle.useContents { this.vkComponentSwizzleRed }
            set(value) { error("Setters not supported on ByValue") }
        override var vkComponentSwizzleGreen: UInt
            get() = handle.useContents { this.vkComponentSwizzleGreen }
            set(value) { error("Setters not supported on ByValue") }
        override var vkComponentSwizzleBlue: UInt
            get() = handle.useContents { this.vkComponentSwizzleBlue }
            set(value) { error("Setters not supported on ByValue") }
        override var vkComponentSwizzleAlpha: UInt
            get() = handle.useContents { this.vkComponentSwizzleAlpha }
            set(value) { error("Setters not supported on ByValue") }
        override var vkXChromaOffset: UInt
            get() = handle.useContents { this.vkXChromaOffset }
            set(value) { error("Setters not supported on ByValue") }
        override var vkYChromaOffset: UInt
            get() = handle.useContents { this.vkYChromaOffset }
            set(value) { error("Setters not supported on ByValue") }
        override var vkChromaFilter: WGPUFilterMode
            get() = handle.useContents { this.vkChromaFilter as WGPUFilterMode }
            set(value) { error("Setters not supported on ByValue") }
        override var forceExplicitReconstruction: UInt
            get() = handle.useContents { this.forceExplicitReconstruction }
            set(value) { error("Setters not supported on ByValue") }
        override var externalFormat: ULong
            get() = handle.useContents { this.externalFormat }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUYCbCrVkDescriptor {
        private val struct: webgpu.native.WGPUYCbCrVkDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUYCbCrVkDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var vkFormat: UInt
            get() = struct.vkFormat
            set(value) { struct.vkFormat = value }
        override var vkYCbCrModel: UInt
            get() = struct.vkYCbCrModel
            set(value) { struct.vkYCbCrModel = value }
        override var vkYCbCrRange: UInt
            get() = struct.vkYCbCrRange
            set(value) { struct.vkYCbCrRange = value }
        override var vkComponentSwizzleRed: UInt
            get() = struct.vkComponentSwizzleRed
            set(value) { struct.vkComponentSwizzleRed = value }
        override var vkComponentSwizzleGreen: UInt
            get() = struct.vkComponentSwizzleGreen
            set(value) { struct.vkComponentSwizzleGreen = value }
        override var vkComponentSwizzleBlue: UInt
            get() = struct.vkComponentSwizzleBlue
            set(value) { struct.vkComponentSwizzleBlue = value }
        override var vkComponentSwizzleAlpha: UInt
            get() = struct.vkComponentSwizzleAlpha
            set(value) { struct.vkComponentSwizzleAlpha = value }
        override var vkXChromaOffset: UInt
            get() = struct.vkXChromaOffset
            set(value) { struct.vkXChromaOffset = value }
        override var vkYChromaOffset: UInt
            get() = struct.vkYChromaOffset
            set(value) { struct.vkYChromaOffset = value }
        override var vkChromaFilter: WGPUFilterMode
            get() = struct.vkChromaFilter as WGPUFilterMode
            set(value) { struct.vkChromaFilter = value }
        override var forceExplicitReconstruction: UInt
            get() = struct.forceExplicitReconstruction
            set(value) { struct.forceExplicitReconstruction = value }
        override var externalFormat: ULong
            get() = struct.externalFormat
            set(value) { struct.externalFormat = value }
    }
}

fun WGPUYCbCrVkDescriptor.toCValue(): CValue<webgpu.native.WGPUYCbCrVkDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.vkFormat = this@toCValue.vkFormat
    this.vkYCbCrModel = this@toCValue.vkYCbCrModel
    this.vkYCbCrRange = this@toCValue.vkYCbCrRange
    this.vkComponentSwizzleRed = this@toCValue.vkComponentSwizzleRed
    this.vkComponentSwizzleGreen = this@toCValue.vkComponentSwizzleGreen
    this.vkComponentSwizzleBlue = this@toCValue.vkComponentSwizzleBlue
    this.vkComponentSwizzleAlpha = this@toCValue.vkComponentSwizzleAlpha
    this.vkXChromaOffset = this@toCValue.vkXChromaOffset
    this.vkYChromaOffset = this@toCValue.vkYChromaOffset
    this.vkChromaFilter = this@toCValue.vkChromaFilter
    this.forceExplicitReconstruction = this@toCValue.forceExplicitReconstruction
    this.externalFormat = this@toCValue.externalFormat
}

actual interface WGPUAdapterPropertiesMemoryHeaps {
    actual var chain: WGPUChainedStruct
    actual var heapCount: ULong
    actual var heapInfo: WGPUMemoryHeapInfo?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesMemoryHeaps = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesMemoryHeaps =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesMemoryHeaps>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesMemoryHeaps) -> Unit): ArrayHolder<WGPUAdapterPropertiesMemoryHeaps> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesMemoryHeaps>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesMemoryHeaps>) : WGPUAdapterPropertiesMemoryHeaps {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var heapCount: ULong
            get() = handle.useContents { this.heapCount }
            set(value) { error("Setters not supported on ByValue") }
        override var heapInfo: WGPUMemoryHeapInfo?
            get() = handle.useContents { this.heapInfo?.let { NativeAddress.fromPointer(it) }?.let { WGPUMemoryHeapInfo(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesMemoryHeaps {
        private val struct: webgpu.native.WGPUAdapterPropertiesMemoryHeaps
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesMemoryHeaps>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var heapCount: ULong
            get() = struct.heapCount
            set(value) { struct.heapCount = value }
        override var heapInfo: WGPUMemoryHeapInfo?
            get() = struct.heapInfo?.let { NativeAddress.fromPointer(it) }?.let { WGPUMemoryHeapInfo(it) }
            set(value) { struct.heapInfo = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUAdapterPropertiesMemoryHeaps.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesMemoryHeaps> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.heapCount = this@toCValue.heapCount
    this.heapInfo = this@toCValue.heapInfo?.handler?.pointer?.takeIf { this@toCValue.heapInfo?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUAdapterPropertiesSubgroupMatrixConfigs {
    actual var chain: WGPUChainedStruct
    actual var configCount: ULong
    actual var configs: WGPUSubgroupMatrixConfig?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAdapterPropertiesSubgroupMatrixConfigs = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterPropertiesSubgroupMatrixConfigs =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterPropertiesSubgroupMatrixConfigs) -> Unit): ArrayHolder<WGPUAdapterPropertiesSubgroupMatrixConfigs> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs>) : WGPUAdapterPropertiesSubgroupMatrixConfigs {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var configCount: ULong
            get() = handle.useContents { this.configCount }
            set(value) { error("Setters not supported on ByValue") }
        override var configs: WGPUSubgroupMatrixConfig?
            get() = handle.useContents { this.configs?.let { NativeAddress.fromPointer(it) }?.let { WGPUSubgroupMatrixConfig(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterPropertiesSubgroupMatrixConfigs {
        private val struct: webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var configCount: ULong
            get() = struct.configCount
            set(value) { struct.configCount = value }
        override var configs: WGPUSubgroupMatrixConfig?
            get() = struct.configs?.let { NativeAddress.fromPointer(it) }?.let { WGPUSubgroupMatrixConfig(it) }
            set(value) { struct.configs = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUAdapterPropertiesSubgroupMatrixConfigs.toCValue(): CValue<webgpu.native.WGPUAdapterPropertiesSubgroupMatrixConfigs> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.configCount = this@toCValue.configCount
    this.configs = this@toCValue.configs?.handler?.pointer?.takeIf { this@toCValue.configs?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUAHardwareBufferProperties {
    actual var yCbCrInfo: WGPUYCbCrVkDescriptor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUAHardwareBufferProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUAHardwareBufferProperties =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAHardwareBufferProperties>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUAHardwareBufferProperties> {
            val byteSize = sizeOf<webgpu.native.WGPUAHardwareBufferProperties>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAHardwareBufferProperties>) : WGPUAHardwareBufferProperties {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = handle.useContents { WGPUYCbCrVkDescriptor.ByReference(NativeAddress.fromPointer(this.yCbCrInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAHardwareBufferProperties {
        private val struct: webgpu.native.WGPUAHardwareBufferProperties
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAHardwareBufferProperties>().pointed
        
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByReference(NativeAddress.fromPointer(struct.yCbCrInfo.ptr))
            set(value) {
                val destBytes = struct.yCbCrInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUAHardwareBufferProperties.toCValue(): CValue<webgpu.native.WGPUAHardwareBufferProperties> = cValue {
    val dest_yCbCrInfo = this.yCbCrInfo.ptr.reinterpret<ByteVar>()
    val src_yCbCrInfo = requireNotNull(this@toCValue.yCbCrInfo.handler.pointer).reinterpret<ByteVar>()
    val size_yCbCrInfo = sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()
    for (i in 0L until size_yCbCrInfo) {
        dest_yCbCrInfo[i.toInt()] = src_yCbCrInfo[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupEntry =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBindGroupEntry>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupEntry) -> Unit): ArrayHolder<WGPUBindGroupEntry> {
            val byteSize = sizeOf<webgpu.native.WGPUBindGroupEntry>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBindGroupEntry>) : WGPUBindGroupEntry {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var binding: UInt
            get() = handle.useContents { this.binding }
            set(value) { error("Setters not supported on ByValue") }
        override var buffer: WGPUBuffer?
            get() = handle.useContents { this.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        override var sampler: WGPUSampler?
            get() = handle.useContents { this.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var textureView: WGPUTextureView?
            get() = handle.useContents { this.textureView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBindGroupEntry {
        private val struct: webgpu.native.WGPUBindGroupEntry
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBindGroupEntry>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var binding: UInt
            get() = struct.binding
            set(value) { struct.binding = value }
        override var buffer: WGPUBuffer?
            get() = struct.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
            set(value) { struct.buffer = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
        override var sampler: WGPUSampler?
            get() = struct.sampler?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) }
            set(value) { struct.sampler = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var textureView: WGPUTextureView?
            get() = struct.textureView?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.textureView = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBindGroupEntry.toCValue(): CValue<webgpu.native.WGPUBindGroupEntry> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.binding = this@toCValue.binding
    this.buffer = this@toCValue.buffer?.handler?.pointer?.takeIf { this@toCValue.buffer?.handler?.rawValue != 0L }?.reinterpret()
    this.offset = this@toCValue.offset
    this.size = this@toCValue.size
    this.sampler = this@toCValue.sampler?.handler?.pointer?.takeIf { this@toCValue.sampler?.handler?.rawValue != 0L }?.reinterpret()
    this.textureView = this@toCValue.textureView?.handler?.pointer?.takeIf { this@toCValue.textureView?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutEntry =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBindGroupLayoutEntry>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutEntry) -> Unit): ArrayHolder<WGPUBindGroupLayoutEntry> {
            val byteSize = sizeOf<webgpu.native.WGPUBindGroupLayoutEntry>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBindGroupLayoutEntry>) : WGPUBindGroupLayoutEntry {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var binding: UInt
            get() = handle.useContents { this.binding }
            set(value) { error("Setters not supported on ByValue") }
        override var visibility: ULong
            get() = handle.useContents { this.visibility }
            set(value) { error("Setters not supported on ByValue") }
        override var bindingArraySize: UInt
            get() = handle.useContents { this.bindingArraySize }
            set(value) { error("Setters not supported on ByValue") }
        override var buffer: WGPUBufferBindingLayout
            get() = handle.useContents { WGPUBufferBindingLayout.ByReference(NativeAddress.fromPointer(this.buffer.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var sampler: WGPUSamplerBindingLayout
            get() = handle.useContents { WGPUSamplerBindingLayout.ByReference(NativeAddress.fromPointer(this.sampler.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var texture: WGPUTextureBindingLayout
            get() = handle.useContents { WGPUTextureBindingLayout.ByReference(NativeAddress.fromPointer(this.texture.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var storageTexture: WGPUStorageTextureBindingLayout
            get() = handle.useContents { WGPUStorageTextureBindingLayout.ByReference(NativeAddress.fromPointer(this.storageTexture.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBindGroupLayoutEntry {
        private val struct: webgpu.native.WGPUBindGroupLayoutEntry
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBindGroupLayoutEntry>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var binding: UInt
            get() = struct.binding
            set(value) { struct.binding = value }
        override var visibility: ULong
            get() = struct.visibility
            set(value) { struct.visibility = value }
        override var bindingArraySize: UInt
            get() = struct.bindingArraySize
            set(value) { struct.bindingArraySize = value }
        override var buffer: WGPUBufferBindingLayout
            get() = WGPUBufferBindingLayout.ByReference(NativeAddress.fromPointer(struct.buffer.ptr))
            set(value) {
                val destBytes = struct.buffer.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUBufferBindingLayout>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var sampler: WGPUSamplerBindingLayout
            get() = WGPUSamplerBindingLayout.ByReference(NativeAddress.fromPointer(struct.sampler.ptr))
            set(value) {
                val destBytes = struct.sampler.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUSamplerBindingLayout>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var texture: WGPUTextureBindingLayout
            get() = WGPUTextureBindingLayout.ByReference(NativeAddress.fromPointer(struct.texture.ptr))
            set(value) {
                val destBytes = struct.texture.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUTextureBindingLayout>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var storageTexture: WGPUStorageTextureBindingLayout
            get() = WGPUStorageTextureBindingLayout.ByReference(NativeAddress.fromPointer(struct.storageTexture.ptr))
            set(value) {
                val destBytes = struct.storageTexture.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStorageTextureBindingLayout>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUBindGroupLayoutEntry.toCValue(): CValue<webgpu.native.WGPUBindGroupLayoutEntry> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.binding = this@toCValue.binding
    this.visibility = this@toCValue.visibility
    this.bindingArraySize = this@toCValue.bindingArraySize
    val dest_buffer = this.buffer.ptr.reinterpret<ByteVar>()
    val src_buffer = requireNotNull(this@toCValue.buffer.handler.pointer).reinterpret<ByteVar>()
    val size_buffer = sizeOf<webgpu.native.WGPUBufferBindingLayout>().toLong()
    for (i in 0L until size_buffer) {
        dest_buffer[i.toInt()] = src_buffer[i.toInt()]
    }
    val dest_sampler = this.sampler.ptr.reinterpret<ByteVar>()
    val src_sampler = requireNotNull(this@toCValue.sampler.handler.pointer).reinterpret<ByteVar>()
    val size_sampler = sizeOf<webgpu.native.WGPUSamplerBindingLayout>().toLong()
    for (i in 0L until size_sampler) {
        dest_sampler[i.toInt()] = src_sampler[i.toInt()]
    }
    val dest_texture = this.texture.ptr.reinterpret<ByteVar>()
    val src_texture = requireNotNull(this@toCValue.texture.handler.pointer).reinterpret<ByteVar>()
    val size_texture = sizeOf<webgpu.native.WGPUTextureBindingLayout>().toLong()
    for (i in 0L until size_texture) {
        dest_texture[i.toInt()] = src_texture[i.toInt()]
    }
    val dest_storageTexture = this.storageTexture.ptr.reinterpret<ByteVar>()
    val src_storageTexture = requireNotNull(this@toCValue.storageTexture.handler.pointer).reinterpret<ByteVar>()
    val size_storageTexture = sizeOf<webgpu.native.WGPUStorageTextureBindingLayout>().toLong()
    for (i in 0L until size_storageTexture) {
        dest_storageTexture[i.toInt()] = src_storageTexture[i.toInt()]
    }
}

actual interface WGPUBlendState {
    actual var color: WGPUBlendComponent
    actual var alpha: WGPUBlendComponent
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBlendState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBlendState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBlendState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBlendState) -> Unit): ArrayHolder<WGPUBlendState> {
            val byteSize = sizeOf<webgpu.native.WGPUBlendState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBlendState>) : WGPUBlendState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var color: WGPUBlendComponent
            get() = handle.useContents { WGPUBlendComponent.ByReference(NativeAddress.fromPointer(this.color.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var alpha: WGPUBlendComponent
            get() = handle.useContents { WGPUBlendComponent.ByReference(NativeAddress.fromPointer(this.alpha.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBlendState {
        private val struct: webgpu.native.WGPUBlendState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBlendState>().pointed
        
        override var color: WGPUBlendComponent
            get() = WGPUBlendComponent.ByReference(NativeAddress.fromPointer(struct.color.ptr))
            set(value) {
                val destBytes = struct.color.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUBlendComponent>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var alpha: WGPUBlendComponent
            get() = WGPUBlendComponent.ByReference(NativeAddress.fromPointer(struct.alpha.ptr))
            set(value) {
                val destBytes = struct.alpha.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUBlendComponent>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUBlendState.toCValue(): CValue<webgpu.native.WGPUBlendState> = cValue {
    val dest_color = this.color.ptr.reinterpret<ByteVar>()
    val src_color = requireNotNull(this@toCValue.color.handler.pointer).reinterpret<ByteVar>()
    val size_color = sizeOf<webgpu.native.WGPUBlendComponent>().toLong()
    for (i in 0L until size_color) {
        dest_color[i.toInt()] = src_color[i.toInt()]
    }
    val dest_alpha = this.alpha.ptr.reinterpret<ByteVar>()
    val src_alpha = requireNotNull(this@toCValue.alpha.handler.pointer).reinterpret<ByteVar>()
    val size_alpha = sizeOf<webgpu.native.WGPUBlendComponent>().toLong()
    for (i in 0L until size_alpha) {
        dest_alpha[i.toInt()] = src_alpha[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBufferDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBufferDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBufferDescriptor) -> Unit): ArrayHolder<WGPUBufferDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUBufferDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBufferDescriptor>) : WGPUBufferDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        override var size: ULong
            get() = handle.useContents { this.size }
            set(value) { error("Setters not supported on ByValue") }
        override var mappedAtCreation: UInt
            get() = handle.useContents { this.mappedAtCreation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBufferDescriptor {
        private val struct: webgpu.native.WGPUBufferDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBufferDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
        override var size: ULong
            get() = struct.size
            set(value) { struct.size = value }
        override var mappedAtCreation: UInt
            get() = struct.mappedAtCreation
            set(value) { struct.mappedAtCreation = value }
    }
}

fun WGPUBufferDescriptor.toCValue(): CValue<webgpu.native.WGPUBufferDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.usage = this@toCValue.usage
    this.size = this@toCValue.size
    this.mappedAtCreation = this@toCValue.mappedAtCreation
}

actual interface WGPUCommandEncoderDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCommandEncoderDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCommandEncoderDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCommandEncoderDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCommandEncoderDescriptor) -> Unit): ArrayHolder<WGPUCommandEncoderDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUCommandEncoderDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCommandEncoderDescriptor>) : WGPUCommandEncoderDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCommandEncoderDescriptor {
        private val struct: webgpu.native.WGPUCommandEncoderDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCommandEncoderDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUCommandEncoderDescriptor.toCValue(): CValue<webgpu.native.WGPUCommandEncoderDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationMessage =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCompilationMessage>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationMessage) -> Unit): ArrayHolder<WGPUCompilationMessage> {
            val byteSize = sizeOf<webgpu.native.WGPUCompilationMessage>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCompilationMessage>) : WGPUCompilationMessage {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var message: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.message.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var type: WGPUCompilationMessageType
            get() = handle.useContents { this.type as WGPUCompilationMessageType }
            set(value) { error("Setters not supported on ByValue") }
        override var lineNum: ULong
            get() = handle.useContents { this.lineNum }
            set(value) { error("Setters not supported on ByValue") }
        override var linePos: ULong
            get() = handle.useContents { this.linePos }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var length: ULong
            get() = handle.useContents { this.length }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCompilationMessage {
        private val struct: webgpu.native.WGPUCompilationMessage
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCompilationMessage>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var message: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.message.ptr))
            set(value) {
                val destBytes = struct.message.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var type: WGPUCompilationMessageType
            get() = struct.type as WGPUCompilationMessageType
            set(value) { struct.type = value }
        override var lineNum: ULong
            get() = struct.lineNum
            set(value) { struct.lineNum = value }
        override var linePos: ULong
            get() = struct.linePos
            set(value) { struct.linePos = value }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var length: ULong
            get() = struct.length
            set(value) { struct.length = value }
    }
}

fun WGPUCompilationMessage.toCValue(): CValue<webgpu.native.WGPUCompilationMessage> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_message = this.message.ptr.reinterpret<ByteVar>()
    val src_message = requireNotNull(this@toCValue.message.handler.pointer).reinterpret<ByteVar>()
    val size_message = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_message) {
        dest_message[i.toInt()] = src_message[i.toInt()]
    }
    this.type = this@toCValue.type
    this.lineNum = this@toCValue.lineNum
    this.linePos = this@toCValue.linePos
    this.offset = this@toCValue.offset
    this.length = this@toCValue.length
}

actual interface WGPUComputePassDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var timestampWrites: WGPUPassTimestampWrites?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUComputePassDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUComputePassDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUComputePassDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePassDescriptor) -> Unit): ArrayHolder<WGPUComputePassDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUComputePassDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUComputePassDescriptor>) : WGPUComputePassDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = handle.useContents { this.timestampWrites?.let { NativeAddress.fromPointer(it) }?.let { WGPUPassTimestampWrites(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUComputePassDescriptor {
        private val struct: webgpu.native.WGPUComputePassDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUComputePassDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = struct.timestampWrites?.let { NativeAddress.fromPointer(it) }?.let { WGPUPassTimestampWrites(it) }
            set(value) { struct.timestampWrites = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUComputePassDescriptor.toCValue(): CValue<webgpu.native.WGPUComputePassDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.timestampWrites = this@toCValue.timestampWrites?.handler?.pointer?.takeIf { this@toCValue.timestampWrites?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUComputeState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUComputeState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputeState) -> Unit): ArrayHolder<WGPUComputeState> {
            val byteSize = sizeOf<webgpu.native.WGPUComputeState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUComputeState>) : WGPUComputeState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var module: WGPUShaderModule?
            get() = handle.useContents { this.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var entryPoint: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.entryPoint.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var constantCount: ULong
            get() = handle.useContents { this.constantCount }
            set(value) { error("Setters not supported on ByValue") }
        override var constants: WGPUConstantEntry?
            get() = handle.useContents { this.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUComputeState {
        private val struct: webgpu.native.WGPUComputeState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUComputeState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var module: WGPUShaderModule?
            get() = struct.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) }
            set(value) { struct.module = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.entryPoint.ptr))
            set(value) {
                val destBytes = struct.entryPoint.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var constantCount: ULong
            get() = struct.constantCount
            set(value) { struct.constantCount = value }
        override var constants: WGPUConstantEntry?
            get() = struct.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) }
            set(value) { struct.constants = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUComputeState.toCValue(): CValue<webgpu.native.WGPUComputeState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.module = this@toCValue.module?.handler?.pointer?.takeIf { this@toCValue.module?.handler?.rawValue != 0L }?.reinterpret()
    val dest_entryPoint = this.entryPoint.ptr.reinterpret<ByteVar>()
    val src_entryPoint = requireNotNull(this@toCValue.entryPoint.handler.pointer).reinterpret<ByteVar>()
    val size_entryPoint = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_entryPoint) {
        dest_entryPoint[i.toInt()] = src_entryPoint[i.toInt()]
    }
    this.constantCount = this@toCValue.constantCount
    this.constants = this@toCValue.constants?.handler?.pointer?.takeIf { this@toCValue.constants?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUDawnDrmFormatCapabilities {
    actual var chain: WGPUChainedStruct
    actual var propertiesCount: ULong
    actual var properties: WGPUDawnDrmFormatProperties?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnDrmFormatCapabilities = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnDrmFormatCapabilities =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnDrmFormatCapabilities>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnDrmFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnDrmFormatCapabilities> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnDrmFormatCapabilities>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnDrmFormatCapabilities>) : WGPUDawnDrmFormatCapabilities {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var propertiesCount: ULong
            get() = handle.useContents { this.propertiesCount }
            set(value) { error("Setters not supported on ByValue") }
        override var properties: WGPUDawnDrmFormatProperties?
            get() = handle.useContents { this.properties?.let { NativeAddress.fromPointer(it) }?.let { WGPUDawnDrmFormatProperties(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnDrmFormatCapabilities {
        private val struct: webgpu.native.WGPUDawnDrmFormatCapabilities
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnDrmFormatCapabilities>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var propertiesCount: ULong
            get() = struct.propertiesCount
            set(value) { struct.propertiesCount = value }
        override var properties: WGPUDawnDrmFormatProperties?
            get() = struct.properties?.let { NativeAddress.fromPointer(it) }?.let { WGPUDawnDrmFormatProperties(it) }
            set(value) { struct.properties = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnDrmFormatCapabilities.toCValue(): CValue<webgpu.native.WGPUDawnDrmFormatCapabilities> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.propertiesCount = this@toCValue.propertiesCount
    this.properties = this@toCValue.properties?.handler?.pointer?.takeIf { this@toCValue.properties?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDepthStencilState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDepthStencilState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDepthStencilState) -> Unit): ArrayHolder<WGPUDepthStencilState> {
            val byteSize = sizeOf<webgpu.native.WGPUDepthStencilState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDepthStencilState>) : WGPUDepthStencilState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var depthWriteEnabled: WGPUOptionalBool
            get() = handle.useContents { this.depthWriteEnabled as WGPUOptionalBool }
            set(value) { error("Setters not supported on ByValue") }
        override var depthCompare: WGPUCompareFunction
            get() = handle.useContents { this.depthCompare as WGPUCompareFunction }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilFront: WGPUStencilFaceState
            get() = handle.useContents { WGPUStencilFaceState.ByReference(NativeAddress.fromPointer(this.stencilFront.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilBack: WGPUStencilFaceState
            get() = handle.useContents { WGPUStencilFaceState.ByReference(NativeAddress.fromPointer(this.stencilBack.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilReadMask: UInt
            get() = handle.useContents { this.stencilReadMask }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilWriteMask: UInt
            get() = handle.useContents { this.stencilWriteMask }
            set(value) { error("Setters not supported on ByValue") }
        override var depthBias: Int
            get() = handle.useContents { this.depthBias }
            set(value) { error("Setters not supported on ByValue") }
        override var depthBiasSlopeScale: Float
            get() = handle.useContents { this.depthBiasSlopeScale }
            set(value) { error("Setters not supported on ByValue") }
        override var depthBiasClamp: Float
            get() = handle.useContents { this.depthBiasClamp }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDepthStencilState {
        private val struct: webgpu.native.WGPUDepthStencilState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDepthStencilState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var depthWriteEnabled: WGPUOptionalBool
            get() = struct.depthWriteEnabled as WGPUOptionalBool
            set(value) { struct.depthWriteEnabled = value }
        override var depthCompare: WGPUCompareFunction
            get() = struct.depthCompare as WGPUCompareFunction
            set(value) { struct.depthCompare = value }
        override var stencilFront: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByReference(NativeAddress.fromPointer(struct.stencilFront.ptr))
            set(value) {
                val destBytes = struct.stencilFront.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var stencilBack: WGPUStencilFaceState
            get() = WGPUStencilFaceState.ByReference(NativeAddress.fromPointer(struct.stencilBack.ptr))
            set(value) {
                val destBytes = struct.stencilBack.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var stencilReadMask: UInt
            get() = struct.stencilReadMask
            set(value) { struct.stencilReadMask = value }
        override var stencilWriteMask: UInt
            get() = struct.stencilWriteMask
            set(value) { struct.stencilWriteMask = value }
        override var depthBias: Int
            get() = struct.depthBias
            set(value) { struct.depthBias = value }
        override var depthBiasSlopeScale: Float
            get() = struct.depthBiasSlopeScale
            set(value) { struct.depthBiasSlopeScale = value }
        override var depthBiasClamp: Float
            get() = struct.depthBiasClamp
            set(value) { struct.depthBiasClamp = value }
    }
}

fun WGPUDepthStencilState.toCValue(): CValue<webgpu.native.WGPUDepthStencilState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.format = this@toCValue.format
    this.depthWriteEnabled = this@toCValue.depthWriteEnabled
    this.depthCompare = this@toCValue.depthCompare
    val dest_stencilFront = this.stencilFront.ptr.reinterpret<ByteVar>()
    val src_stencilFront = requireNotNull(this@toCValue.stencilFront.handler.pointer).reinterpret<ByteVar>()
    val size_stencilFront = sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()
    for (i in 0L until size_stencilFront) {
        dest_stencilFront[i.toInt()] = src_stencilFront[i.toInt()]
    }
    val dest_stencilBack = this.stencilBack.ptr.reinterpret<ByteVar>()
    val src_stencilBack = requireNotNull(this@toCValue.stencilBack.handler.pointer).reinterpret<ByteVar>()
    val size_stencilBack = sizeOf<webgpu.native.WGPUStencilFaceState>().toLong()
    for (i in 0L until size_stencilBack) {
        dest_stencilBack[i.toInt()] = src_stencilBack[i.toInt()]
    }
    this.stencilReadMask = this@toCValue.stencilReadMask
    this.stencilWriteMask = this@toCValue.stencilWriteMask
    this.depthBias = this@toCValue.depthBias
    this.depthBiasSlopeScale = this@toCValue.depthBiasSlopeScale
    this.depthBiasClamp = this@toCValue.depthBiasClamp
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
        actual fun allocate(allocator: MemoryAllocator): WGPUExternalTextureDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUExternalTextureDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUExternalTextureDescriptor) -> Unit): ArrayHolder<WGPUExternalTextureDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUExternalTextureDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUExternalTextureDescriptor>) : WGPUExternalTextureDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var plane0: WGPUTextureView?
            get() = handle.useContents { this.plane0?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var plane1: WGPUTextureView?
            get() = handle.useContents { this.plane1?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var cropOrigin: WGPUOrigin2D
            get() = handle.useContents { WGPUOrigin2D.ByReference(NativeAddress.fromPointer(this.cropOrigin.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var cropSize: WGPUExtent2D
            get() = handle.useContents { WGPUExtent2D.ByReference(NativeAddress.fromPointer(this.cropSize.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var apparentSize: WGPUExtent2D
            get() = handle.useContents { WGPUExtent2D.ByReference(NativeAddress.fromPointer(this.apparentSize.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var doYuvToRgbConversionOnly: UInt
            get() = handle.useContents { this.doYuvToRgbConversionOnly }
            set(value) { error("Setters not supported on ByValue") }
        override var yuvToRgbConversionMatrix: NativeAddress?
            get() = handle.useContents { this.yuvToRgbConversionMatrix?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = handle.useContents { this.srcTransferFunctionParameters?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = handle.useContents { this.dstTransferFunctionParameters?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var gamutConversionMatrix: NativeAddress?
            get() = handle.useContents { this.gamutConversionMatrix?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mirrored: UInt
            get() = handle.useContents { this.mirrored }
            set(value) { error("Setters not supported on ByValue") }
        override var rotation: WGPUExternalTextureRotation
            get() = handle.useContents { this.rotation as WGPUExternalTextureRotation }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUExternalTextureDescriptor {
        private val struct: webgpu.native.WGPUExternalTextureDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUExternalTextureDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var plane0: WGPUTextureView?
            get() = struct.plane0?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.plane0 = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var plane1: WGPUTextureView?
            get() = struct.plane1?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.plane1 = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var cropOrigin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByReference(NativeAddress.fromPointer(struct.cropOrigin.ptr))
            set(value) {
                val destBytes = struct.cropOrigin.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUOrigin2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var cropSize: WGPUExtent2D
            get() = WGPUExtent2D.ByReference(NativeAddress.fromPointer(struct.cropSize.ptr))
            set(value) {
                val destBytes = struct.cropSize.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var apparentSize: WGPUExtent2D
            get() = WGPUExtent2D.ByReference(NativeAddress.fromPointer(struct.apparentSize.ptr))
            set(value) {
                val destBytes = struct.apparentSize.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var doYuvToRgbConversionOnly: UInt
            get() = struct.doYuvToRgbConversionOnly
            set(value) { struct.doYuvToRgbConversionOnly = value }
        override var yuvToRgbConversionMatrix: NativeAddress?
            get() = struct.yuvToRgbConversionMatrix?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.yuvToRgbConversionMatrix = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var srcTransferFunctionParameters: NativeAddress?
            get() = struct.srcTransferFunctionParameters?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.srcTransferFunctionParameters = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var dstTransferFunctionParameters: NativeAddress?
            get() = struct.dstTransferFunctionParameters?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.dstTransferFunctionParameters = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var gamutConversionMatrix: NativeAddress?
            get() = struct.gamutConversionMatrix?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.gamutConversionMatrix = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var mirrored: UInt
            get() = struct.mirrored
            set(value) { struct.mirrored = value }
        override var rotation: WGPUExternalTextureRotation
            get() = struct.rotation as WGPUExternalTextureRotation
            set(value) { struct.rotation = value }
    }
}

fun WGPUExternalTextureDescriptor.toCValue(): CValue<webgpu.native.WGPUExternalTextureDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.plane0 = this@toCValue.plane0?.handler?.pointer?.takeIf { this@toCValue.plane0?.handler?.rawValue != 0L }?.reinterpret()
    this.plane1 = this@toCValue.plane1?.handler?.pointer?.takeIf { this@toCValue.plane1?.handler?.rawValue != 0L }?.reinterpret()
    val dest_cropOrigin = this.cropOrigin.ptr.reinterpret<ByteVar>()
    val src_cropOrigin = requireNotNull(this@toCValue.cropOrigin.handler.pointer).reinterpret<ByteVar>()
    val size_cropOrigin = sizeOf<webgpu.native.WGPUOrigin2D>().toLong()
    for (i in 0L until size_cropOrigin) {
        dest_cropOrigin[i.toInt()] = src_cropOrigin[i.toInt()]
    }
    val dest_cropSize = this.cropSize.ptr.reinterpret<ByteVar>()
    val src_cropSize = requireNotNull(this@toCValue.cropSize.handler.pointer).reinterpret<ByteVar>()
    val size_cropSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
    for (i in 0L until size_cropSize) {
        dest_cropSize[i.toInt()] = src_cropSize[i.toInt()]
    }
    val dest_apparentSize = this.apparentSize.ptr.reinterpret<ByteVar>()
    val src_apparentSize = requireNotNull(this@toCValue.apparentSize.handler.pointer).reinterpret<ByteVar>()
    val size_apparentSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
    for (i in 0L until size_apparentSize) {
        dest_apparentSize[i.toInt()] = src_apparentSize[i.toInt()]
    }
    this.doYuvToRgbConversionOnly = this@toCValue.doYuvToRgbConversionOnly
    this.yuvToRgbConversionMatrix = this@toCValue.yuvToRgbConversionMatrix?.pointer?.takeIf { this@toCValue.yuvToRgbConversionMatrix?.rawValue != 0L }?.reinterpret()
    this.srcTransferFunctionParameters = this@toCValue.srcTransferFunctionParameters?.pointer?.takeIf { this@toCValue.srcTransferFunctionParameters?.rawValue != 0L }?.reinterpret()
    this.dstTransferFunctionParameters = this@toCValue.dstTransferFunctionParameters?.pointer?.takeIf { this@toCValue.dstTransferFunctionParameters?.rawValue != 0L }?.reinterpret()
    this.gamutConversionMatrix = this@toCValue.gamutConversionMatrix?.pointer?.takeIf { this@toCValue.gamutConversionMatrix?.rawValue != 0L }?.reinterpret()
    this.mirrored = this@toCValue.mirrored
    this.rotation = this@toCValue.rotation
}

actual interface WGPUFutureWaitInfo {
    actual var future: WGPUFuture
    actual var completed: UInt
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUFutureWaitInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUFutureWaitInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUFutureWaitInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFutureWaitInfo) -> Unit): ArrayHolder<WGPUFutureWaitInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUFutureWaitInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUFutureWaitInfo>) : WGPUFutureWaitInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var future: WGPUFuture
            get() = handle.useContents { WGPUFuture.ByReference(NativeAddress.fromPointer(this.future.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var completed: UInt
            get() = handle.useContents { this.completed }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUFutureWaitInfo {
        private val struct: webgpu.native.WGPUFutureWaitInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUFutureWaitInfo>().pointed
        
        override var future: WGPUFuture
            get() = WGPUFuture.ByReference(NativeAddress.fromPointer(struct.future.ptr))
            set(value) {
                val destBytes = struct.future.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUFuture>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var completed: UInt
            get() = struct.completed
            set(value) { struct.completed = value }
    }
}

fun WGPUFutureWaitInfo.toCValue(): CValue<webgpu.native.WGPUFutureWaitInfo> = cValue {
    val dest_future = this.future.ptr.reinterpret<ByteVar>()
    val src_future = requireNotNull(this@toCValue.future.handler.pointer).reinterpret<ByteVar>()
    val size_future = sizeOf<webgpu.native.WGPUFuture>().toLong()
    for (i in 0L until size_future) {
        dest_future[i.toInt()] = src_future[i.toInt()]
    }
    this.completed = this@toCValue.completed
}

actual interface WGPUImageCopyExternalTexture {
    actual var nextInChain: WGPUChainedStruct?
    actual var externalTexture: WGPUExternalTexture?
    actual var origin: WGPUOrigin3D
    actual var naturalSize: WGPUExtent2D
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUImageCopyExternalTexture = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUImageCopyExternalTexture =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUImageCopyExternalTexture>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUImageCopyExternalTexture) -> Unit): ArrayHolder<WGPUImageCopyExternalTexture> {
            val byteSize = sizeOf<webgpu.native.WGPUImageCopyExternalTexture>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUImageCopyExternalTexture>) : WGPUImageCopyExternalTexture {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var externalTexture: WGPUExternalTexture?
            get() = handle.useContents { this.externalTexture?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var origin: WGPUOrigin3D
            get() = handle.useContents { WGPUOrigin3D.ByReference(NativeAddress.fromPointer(this.origin.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var naturalSize: WGPUExtent2D
            get() = handle.useContents { WGPUExtent2D.ByReference(NativeAddress.fromPointer(this.naturalSize.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUImageCopyExternalTexture {
        private val struct: webgpu.native.WGPUImageCopyExternalTexture
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUImageCopyExternalTexture>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var externalTexture: WGPUExternalTexture?
            get() = struct.externalTexture?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) }
            set(value) { struct.externalTexture = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByReference(NativeAddress.fromPointer(struct.origin.ptr))
            set(value) {
                val destBytes = struct.origin.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUOrigin3D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var naturalSize: WGPUExtent2D
            get() = WGPUExtent2D.ByReference(NativeAddress.fromPointer(struct.naturalSize.ptr))
            set(value) {
                val destBytes = struct.naturalSize.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUImageCopyExternalTexture.toCValue(): CValue<webgpu.native.WGPUImageCopyExternalTexture> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.externalTexture = this@toCValue.externalTexture?.handler?.pointer?.takeIf { this@toCValue.externalTexture?.handler?.rawValue != 0L }?.reinterpret()
    val dest_origin = this.origin.ptr.reinterpret<ByteVar>()
    val src_origin = requireNotNull(this@toCValue.origin.handler.pointer).reinterpret<ByteVar>()
    val size_origin = sizeOf<webgpu.native.WGPUOrigin3D>().toLong()
    for (i in 0L until size_origin) {
        dest_origin[i.toInt()] = src_origin[i.toInt()]
    }
    val dest_naturalSize = this.naturalSize.ptr.reinterpret<ByteVar>()
    val src_naturalSize = requireNotNull(this@toCValue.naturalSize.handler.pointer).reinterpret<ByteVar>()
    val size_naturalSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
    for (i in 0L until size_naturalSize) {
        dest_naturalSize[i.toInt()] = src_naturalSize[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUInstanceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUInstanceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUInstanceDescriptor) -> Unit): ArrayHolder<WGPUInstanceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUInstanceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUInstanceDescriptor>) : WGPUInstanceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredFeatureCount: ULong
            get() = handle.useContents { this.requiredFeatureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredFeatures: NativeAddress?
            get() = handle.useContents { this.requiredFeatures?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredLimits: WGPUInstanceLimits?
            get() = handle.useContents { this.requiredLimits?.let { NativeAddress.fromPointer(it) }?.let { WGPUInstanceLimits(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUInstanceDescriptor {
        private val struct: webgpu.native.WGPUInstanceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUInstanceDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var requiredFeatureCount: ULong
            get() = struct.requiredFeatureCount
            set(value) { struct.requiredFeatureCount = value }
        override var requiredFeatures: NativeAddress?
            get() = struct.requiredFeatures?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.requiredFeatures = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var requiredLimits: WGPUInstanceLimits?
            get() = struct.requiredLimits?.let { NativeAddress.fromPointer(it) }?.let { WGPUInstanceLimits(it) }
            set(value) { struct.requiredLimits = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUInstanceDescriptor.toCValue(): CValue<webgpu.native.WGPUInstanceDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.requiredFeatureCount = this@toCValue.requiredFeatureCount
    this.requiredFeatures = this@toCValue.requiredFeatures?.pointer?.takeIf { this@toCValue.requiredFeatures?.rawValue != 0L }?.reinterpret()
    this.requiredLimits = this@toCValue.requiredLimits?.handler?.pointer?.takeIf { this@toCValue.requiredLimits?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPULimits =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPULimits>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPULimits) -> Unit): ArrayHolder<WGPULimits> {
            val byteSize = sizeOf<webgpu.native.WGPULimits>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPULimits>) : WGPULimits {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var maxTextureDimension1D: UInt
            get() = handle.useContents { this.maxTextureDimension1D }
            set(value) { error("Setters not supported on ByValue") }
        override var maxTextureDimension2D: UInt
            get() = handle.useContents { this.maxTextureDimension2D }
            set(value) { error("Setters not supported on ByValue") }
        override var maxTextureDimension3D: UInt
            get() = handle.useContents { this.maxTextureDimension3D }
            set(value) { error("Setters not supported on ByValue") }
        override var maxTextureArrayLayers: UInt
            get() = handle.useContents { this.maxTextureArrayLayers }
            set(value) { error("Setters not supported on ByValue") }
        override var maxBindGroups: UInt
            get() = handle.useContents { this.maxBindGroups }
            set(value) { error("Setters not supported on ByValue") }
        override var maxBindGroupsPlusVertexBuffers: UInt
            get() = handle.useContents { this.maxBindGroupsPlusVertexBuffers }
            set(value) { error("Setters not supported on ByValue") }
        override var maxBindingsPerBindGroup: UInt
            get() = handle.useContents { this.maxBindingsPerBindGroup }
            set(value) { error("Setters not supported on ByValue") }
        override var maxDynamicUniformBuffersPerPipelineLayout: UInt
            get() = handle.useContents { this.maxDynamicUniformBuffersPerPipelineLayout }
            set(value) { error("Setters not supported on ByValue") }
        override var maxDynamicStorageBuffersPerPipelineLayout: UInt
            get() = handle.useContents { this.maxDynamicStorageBuffersPerPipelineLayout }
            set(value) { error("Setters not supported on ByValue") }
        override var maxSampledTexturesPerShaderStage: UInt
            get() = handle.useContents { this.maxSampledTexturesPerShaderStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxSamplersPerShaderStage: UInt
            get() = handle.useContents { this.maxSamplersPerShaderStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageBuffersPerShaderStage: UInt
            get() = handle.useContents { this.maxStorageBuffersPerShaderStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageTexturesPerShaderStage: UInt
            get() = handle.useContents { this.maxStorageTexturesPerShaderStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxUniformBuffersPerShaderStage: UInt
            get() = handle.useContents { this.maxUniformBuffersPerShaderStage }
            set(value) { error("Setters not supported on ByValue") }
        override var maxUniformBufferBindingSize: ULong
            get() = handle.useContents { this.maxUniformBufferBindingSize }
            set(value) { error("Setters not supported on ByValue") }
        override var maxStorageBufferBindingSize: ULong
            get() = handle.useContents { this.maxStorageBufferBindingSize }
            set(value) { error("Setters not supported on ByValue") }
        override var minUniformBufferOffsetAlignment: UInt
            get() = handle.useContents { this.minUniformBufferOffsetAlignment }
            set(value) { error("Setters not supported on ByValue") }
        override var minStorageBufferOffsetAlignment: UInt
            get() = handle.useContents { this.minStorageBufferOffsetAlignment }
            set(value) { error("Setters not supported on ByValue") }
        override var maxVertexBuffers: UInt
            get() = handle.useContents { this.maxVertexBuffers }
            set(value) { error("Setters not supported on ByValue") }
        override var maxBufferSize: ULong
            get() = handle.useContents { this.maxBufferSize }
            set(value) { error("Setters not supported on ByValue") }
        override var maxVertexAttributes: UInt
            get() = handle.useContents { this.maxVertexAttributes }
            set(value) { error("Setters not supported on ByValue") }
        override var maxVertexBufferArrayStride: UInt
            get() = handle.useContents { this.maxVertexBufferArrayStride }
            set(value) { error("Setters not supported on ByValue") }
        override var maxInterStageShaderVariables: UInt
            get() = handle.useContents { this.maxInterStageShaderVariables }
            set(value) { error("Setters not supported on ByValue") }
        override var maxColorAttachments: UInt
            get() = handle.useContents { this.maxColorAttachments }
            set(value) { error("Setters not supported on ByValue") }
        override var maxColorAttachmentBytesPerSample: UInt
            get() = handle.useContents { this.maxColorAttachmentBytesPerSample }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeWorkgroupStorageSize: UInt
            get() = handle.useContents { this.maxComputeWorkgroupStorageSize }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeInvocationsPerWorkgroup: UInt
            get() = handle.useContents { this.maxComputeInvocationsPerWorkgroup }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeWorkgroupSizeX: UInt
            get() = handle.useContents { this.maxComputeWorkgroupSizeX }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeWorkgroupSizeY: UInt
            get() = handle.useContents { this.maxComputeWorkgroupSizeY }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeWorkgroupSizeZ: UInt
            get() = handle.useContents { this.maxComputeWorkgroupSizeZ }
            set(value) { error("Setters not supported on ByValue") }
        override var maxComputeWorkgroupsPerDimension: UInt
            get() = handle.useContents { this.maxComputeWorkgroupsPerDimension }
            set(value) { error("Setters not supported on ByValue") }
        override var maxImmediateSize: UInt
            get() = handle.useContents { this.maxImmediateSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPULimits {
        private val struct: webgpu.native.WGPULimits
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPULimits>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var maxTextureDimension1D: UInt
            get() = struct.maxTextureDimension1D
            set(value) { struct.maxTextureDimension1D = value }
        override var maxTextureDimension2D: UInt
            get() = struct.maxTextureDimension2D
            set(value) { struct.maxTextureDimension2D = value }
        override var maxTextureDimension3D: UInt
            get() = struct.maxTextureDimension3D
            set(value) { struct.maxTextureDimension3D = value }
        override var maxTextureArrayLayers: UInt
            get() = struct.maxTextureArrayLayers
            set(value) { struct.maxTextureArrayLayers = value }
        override var maxBindGroups: UInt
            get() = struct.maxBindGroups
            set(value) { struct.maxBindGroups = value }
        override var maxBindGroupsPlusVertexBuffers: UInt
            get() = struct.maxBindGroupsPlusVertexBuffers
            set(value) { struct.maxBindGroupsPlusVertexBuffers = value }
        override var maxBindingsPerBindGroup: UInt
            get() = struct.maxBindingsPerBindGroup
            set(value) { struct.maxBindingsPerBindGroup = value }
        override var maxDynamicUniformBuffersPerPipelineLayout: UInt
            get() = struct.maxDynamicUniformBuffersPerPipelineLayout
            set(value) { struct.maxDynamicUniformBuffersPerPipelineLayout = value }
        override var maxDynamicStorageBuffersPerPipelineLayout: UInt
            get() = struct.maxDynamicStorageBuffersPerPipelineLayout
            set(value) { struct.maxDynamicStorageBuffersPerPipelineLayout = value }
        override var maxSampledTexturesPerShaderStage: UInt
            get() = struct.maxSampledTexturesPerShaderStage
            set(value) { struct.maxSampledTexturesPerShaderStage = value }
        override var maxSamplersPerShaderStage: UInt
            get() = struct.maxSamplersPerShaderStage
            set(value) { struct.maxSamplersPerShaderStage = value }
        override var maxStorageBuffersPerShaderStage: UInt
            get() = struct.maxStorageBuffersPerShaderStage
            set(value) { struct.maxStorageBuffersPerShaderStage = value }
        override var maxStorageTexturesPerShaderStage: UInt
            get() = struct.maxStorageTexturesPerShaderStage
            set(value) { struct.maxStorageTexturesPerShaderStage = value }
        override var maxUniformBuffersPerShaderStage: UInt
            get() = struct.maxUniformBuffersPerShaderStage
            set(value) { struct.maxUniformBuffersPerShaderStage = value }
        override var maxUniformBufferBindingSize: ULong
            get() = struct.maxUniformBufferBindingSize
            set(value) { struct.maxUniformBufferBindingSize = value }
        override var maxStorageBufferBindingSize: ULong
            get() = struct.maxStorageBufferBindingSize
            set(value) { struct.maxStorageBufferBindingSize = value }
        override var minUniformBufferOffsetAlignment: UInt
            get() = struct.minUniformBufferOffsetAlignment
            set(value) { struct.minUniformBufferOffsetAlignment = value }
        override var minStorageBufferOffsetAlignment: UInt
            get() = struct.minStorageBufferOffsetAlignment
            set(value) { struct.minStorageBufferOffsetAlignment = value }
        override var maxVertexBuffers: UInt
            get() = struct.maxVertexBuffers
            set(value) { struct.maxVertexBuffers = value }
        override var maxBufferSize: ULong
            get() = struct.maxBufferSize
            set(value) { struct.maxBufferSize = value }
        override var maxVertexAttributes: UInt
            get() = struct.maxVertexAttributes
            set(value) { struct.maxVertexAttributes = value }
        override var maxVertexBufferArrayStride: UInt
            get() = struct.maxVertexBufferArrayStride
            set(value) { struct.maxVertexBufferArrayStride = value }
        override var maxInterStageShaderVariables: UInt
            get() = struct.maxInterStageShaderVariables
            set(value) { struct.maxInterStageShaderVariables = value }
        override var maxColorAttachments: UInt
            get() = struct.maxColorAttachments
            set(value) { struct.maxColorAttachments = value }
        override var maxColorAttachmentBytesPerSample: UInt
            get() = struct.maxColorAttachmentBytesPerSample
            set(value) { struct.maxColorAttachmentBytesPerSample = value }
        override var maxComputeWorkgroupStorageSize: UInt
            get() = struct.maxComputeWorkgroupStorageSize
            set(value) { struct.maxComputeWorkgroupStorageSize = value }
        override var maxComputeInvocationsPerWorkgroup: UInt
            get() = struct.maxComputeInvocationsPerWorkgroup
            set(value) { struct.maxComputeInvocationsPerWorkgroup = value }
        override var maxComputeWorkgroupSizeX: UInt
            get() = struct.maxComputeWorkgroupSizeX
            set(value) { struct.maxComputeWorkgroupSizeX = value }
        override var maxComputeWorkgroupSizeY: UInt
            get() = struct.maxComputeWorkgroupSizeY
            set(value) { struct.maxComputeWorkgroupSizeY = value }
        override var maxComputeWorkgroupSizeZ: UInt
            get() = struct.maxComputeWorkgroupSizeZ
            set(value) { struct.maxComputeWorkgroupSizeZ = value }
        override var maxComputeWorkgroupsPerDimension: UInt
            get() = struct.maxComputeWorkgroupsPerDimension
            set(value) { struct.maxComputeWorkgroupsPerDimension = value }
        override var maxImmediateSize: UInt
            get() = struct.maxImmediateSize
            set(value) { struct.maxImmediateSize = value }
    }
}

fun WGPULimits.toCValue(): CValue<webgpu.native.WGPULimits> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.maxTextureDimension1D = this@toCValue.maxTextureDimension1D
    this.maxTextureDimension2D = this@toCValue.maxTextureDimension2D
    this.maxTextureDimension3D = this@toCValue.maxTextureDimension3D
    this.maxTextureArrayLayers = this@toCValue.maxTextureArrayLayers
    this.maxBindGroups = this@toCValue.maxBindGroups
    this.maxBindGroupsPlusVertexBuffers = this@toCValue.maxBindGroupsPlusVertexBuffers
    this.maxBindingsPerBindGroup = this@toCValue.maxBindingsPerBindGroup
    this.maxDynamicUniformBuffersPerPipelineLayout = this@toCValue.maxDynamicUniformBuffersPerPipelineLayout
    this.maxDynamicStorageBuffersPerPipelineLayout = this@toCValue.maxDynamicStorageBuffersPerPipelineLayout
    this.maxSampledTexturesPerShaderStage = this@toCValue.maxSampledTexturesPerShaderStage
    this.maxSamplersPerShaderStage = this@toCValue.maxSamplersPerShaderStage
    this.maxStorageBuffersPerShaderStage = this@toCValue.maxStorageBuffersPerShaderStage
    this.maxStorageTexturesPerShaderStage = this@toCValue.maxStorageTexturesPerShaderStage
    this.maxUniformBuffersPerShaderStage = this@toCValue.maxUniformBuffersPerShaderStage
    this.maxUniformBufferBindingSize = this@toCValue.maxUniformBufferBindingSize
    this.maxStorageBufferBindingSize = this@toCValue.maxStorageBufferBindingSize
    this.minUniformBufferOffsetAlignment = this@toCValue.minUniformBufferOffsetAlignment
    this.minStorageBufferOffsetAlignment = this@toCValue.minStorageBufferOffsetAlignment
    this.maxVertexBuffers = this@toCValue.maxVertexBuffers
    this.maxBufferSize = this@toCValue.maxBufferSize
    this.maxVertexAttributes = this@toCValue.maxVertexAttributes
    this.maxVertexBufferArrayStride = this@toCValue.maxVertexBufferArrayStride
    this.maxInterStageShaderVariables = this@toCValue.maxInterStageShaderVariables
    this.maxColorAttachments = this@toCValue.maxColorAttachments
    this.maxColorAttachmentBytesPerSample = this@toCValue.maxColorAttachmentBytesPerSample
    this.maxComputeWorkgroupStorageSize = this@toCValue.maxComputeWorkgroupStorageSize
    this.maxComputeInvocationsPerWorkgroup = this@toCValue.maxComputeInvocationsPerWorkgroup
    this.maxComputeWorkgroupSizeX = this@toCValue.maxComputeWorkgroupSizeX
    this.maxComputeWorkgroupSizeY = this@toCValue.maxComputeWorkgroupSizeY
    this.maxComputeWorkgroupSizeZ = this@toCValue.maxComputeWorkgroupSizeZ
    this.maxComputeWorkgroupsPerDimension = this@toCValue.maxComputeWorkgroupsPerDimension
    this.maxImmediateSize = this@toCValue.maxImmediateSize
}

actual interface WGPUPipelineLayoutPixelLocalStorage {
    actual var chain: WGPUChainedStruct
    actual var totalPixelLocalStorageSize: ULong
    actual var storageAttachmentCount: ULong
    actual var storageAttachments: WGPUPipelineLayoutStorageAttachment?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUPipelineLayoutPixelLocalStorage = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutPixelLocalStorage =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPipelineLayoutPixelLocalStorage>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutPixelLocalStorage) -> Unit): ArrayHolder<WGPUPipelineLayoutPixelLocalStorage> {
            val byteSize = sizeOf<webgpu.native.WGPUPipelineLayoutPixelLocalStorage>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPipelineLayoutPixelLocalStorage>) : WGPUPipelineLayoutPixelLocalStorage {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var totalPixelLocalStorageSize: ULong
            get() = handle.useContents { this.totalPixelLocalStorageSize }
            set(value) { error("Setters not supported on ByValue") }
        override var storageAttachmentCount: ULong
            get() = handle.useContents { this.storageAttachmentCount }
            set(value) { error("Setters not supported on ByValue") }
        override var storageAttachments: WGPUPipelineLayoutStorageAttachment?
            get() = handle.useContents { this.storageAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayoutStorageAttachment(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPipelineLayoutPixelLocalStorage {
        private val struct: webgpu.native.WGPUPipelineLayoutPixelLocalStorage
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPipelineLayoutPixelLocalStorage>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var totalPixelLocalStorageSize: ULong
            get() = struct.totalPixelLocalStorageSize
            set(value) { struct.totalPixelLocalStorageSize = value }
        override var storageAttachmentCount: ULong
            get() = struct.storageAttachmentCount
            set(value) { struct.storageAttachmentCount = value }
        override var storageAttachments: WGPUPipelineLayoutStorageAttachment?
            get() = struct.storageAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayoutStorageAttachment(it) }
            set(value) { struct.storageAttachments = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUPipelineLayoutPixelLocalStorage.toCValue(): CValue<webgpu.native.WGPUPipelineLayoutPixelLocalStorage> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.totalPixelLocalStorageSize = this@toCValue.totalPixelLocalStorageSize
    this.storageAttachmentCount = this@toCValue.storageAttachmentCount
    this.storageAttachments = this@toCValue.storageAttachments?.handler?.pointer?.takeIf { this@toCValue.storageAttachments?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderBundleEncoderDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderBundleEncoderDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderBundleEncoderDescriptor) -> Unit): ArrayHolder<WGPURenderBundleEncoderDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPURenderBundleEncoderDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderBundleEncoderDescriptor>) : WGPURenderBundleEncoderDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var colorFormatCount: ULong
            get() = handle.useContents { this.colorFormatCount }
            set(value) { error("Setters not supported on ByValue") }
        override var colorFormats: NativeAddress?
            get() = handle.useContents { this.colorFormats?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var depthStencilFormat: WGPUTextureFormat
            get() = handle.useContents { this.depthStencilFormat as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var sampleCount: UInt
            get() = handle.useContents { this.sampleCount }
            set(value) { error("Setters not supported on ByValue") }
        override var depthReadOnly: UInt
            get() = handle.useContents { this.depthReadOnly }
            set(value) { error("Setters not supported on ByValue") }
        override var stencilReadOnly: UInt
            get() = handle.useContents { this.stencilReadOnly }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderBundleEncoderDescriptor {
        private val struct: webgpu.native.WGPURenderBundleEncoderDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderBundleEncoderDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var colorFormatCount: ULong
            get() = struct.colorFormatCount
            set(value) { struct.colorFormatCount = value }
        override var colorFormats: NativeAddress?
            get() = struct.colorFormats?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.colorFormats = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var depthStencilFormat: WGPUTextureFormat
            get() = struct.depthStencilFormat as WGPUTextureFormat
            set(value) { struct.depthStencilFormat = value }
        override var sampleCount: UInt
            get() = struct.sampleCount
            set(value) { struct.sampleCount = value }
        override var depthReadOnly: UInt
            get() = struct.depthReadOnly
            set(value) { struct.depthReadOnly = value }
        override var stencilReadOnly: UInt
            get() = struct.stencilReadOnly
            set(value) { struct.stencilReadOnly = value }
    }
}

fun WGPURenderBundleEncoderDescriptor.toCValue(): CValue<webgpu.native.WGPURenderBundleEncoderDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.colorFormatCount = this@toCValue.colorFormatCount
    this.colorFormats = this@toCValue.colorFormats?.pointer?.takeIf { this@toCValue.colorFormats?.rawValue != 0L }?.reinterpret()
    this.depthStencilFormat = this@toCValue.depthStencilFormat
    this.sampleCount = this@toCValue.sampleCount
    this.depthReadOnly = this@toCValue.depthReadOnly
    this.stencilReadOnly = this@toCValue.stencilReadOnly
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassColorAttachment =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassColorAttachment>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassColorAttachment) -> Unit): ArrayHolder<WGPURenderPassColorAttachment> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassColorAttachment>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassColorAttachment>) : WGPURenderPassColorAttachment {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var view: WGPUTextureView?
            get() = handle.useContents { this.view?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var depthSlice: UInt
            get() = handle.useContents { this.depthSlice }
            set(value) { error("Setters not supported on ByValue") }
        override var resolveTarget: WGPUTextureView?
            get() = handle.useContents { this.resolveTarget?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var loadOp: WGPULoadOp
            get() = handle.useContents { this.loadOp as WGPULoadOp }
            set(value) { error("Setters not supported on ByValue") }
        override var storeOp: WGPUStoreOp
            get() = handle.useContents { this.storeOp as WGPUStoreOp }
            set(value) { error("Setters not supported on ByValue") }
        override var clearValue: WGPUColor
            get() = handle.useContents { WGPUColor.ByReference(NativeAddress.fromPointer(this.clearValue.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassColorAttachment {
        private val struct: webgpu.native.WGPURenderPassColorAttachment
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassColorAttachment>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var view: WGPUTextureView?
            get() = struct.view?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.view = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var depthSlice: UInt
            get() = struct.depthSlice
            set(value) { struct.depthSlice = value }
        override var resolveTarget: WGPUTextureView?
            get() = struct.resolveTarget?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.resolveTarget = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var loadOp: WGPULoadOp
            get() = struct.loadOp as WGPULoadOp
            set(value) { struct.loadOp = value }
        override var storeOp: WGPUStoreOp
            get() = struct.storeOp as WGPUStoreOp
            set(value) { struct.storeOp = value }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByReference(NativeAddress.fromPointer(struct.clearValue.ptr))
            set(value) {
                val destBytes = struct.clearValue.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUColor>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPURenderPassColorAttachment.toCValue(): CValue<webgpu.native.WGPURenderPassColorAttachment> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.view = this@toCValue.view?.handler?.pointer?.takeIf { this@toCValue.view?.handler?.rawValue != 0L }?.reinterpret()
    this.depthSlice = this@toCValue.depthSlice
    this.resolveTarget = this@toCValue.resolveTarget?.handler?.pointer?.takeIf { this@toCValue.resolveTarget?.handler?.rawValue != 0L }?.reinterpret()
    this.loadOp = this@toCValue.loadOp
    this.storeOp = this@toCValue.storeOp
    val dest_clearValue = this.clearValue.ptr.reinterpret<ByteVar>()
    val src_clearValue = requireNotNull(this@toCValue.clearValue.handler.pointer).reinterpret<ByteVar>()
    val size_clearValue = sizeOf<webgpu.native.WGPUColor>().toLong()
    for (i in 0L until size_clearValue) {
        dest_clearValue[i.toInt()] = src_clearValue[i.toInt()]
    }
}

actual interface WGPURenderPassRenderAreaRect {
    actual var chain: WGPUChainedStruct
    actual var origin: WGPUOrigin2D
    actual var size: WGPUExtent2D
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassRenderAreaRect = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassRenderAreaRect =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassRenderAreaRect>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassRenderAreaRect) -> Unit): ArrayHolder<WGPURenderPassRenderAreaRect> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassRenderAreaRect>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassRenderAreaRect>) : WGPURenderPassRenderAreaRect {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var origin: WGPUOrigin2D
            get() = handle.useContents { WGPUOrigin2D.ByReference(NativeAddress.fromPointer(this.origin.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var size: WGPUExtent2D
            get() = handle.useContents { WGPUExtent2D.ByReference(NativeAddress.fromPointer(this.size.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassRenderAreaRect {
        private val struct: webgpu.native.WGPURenderPassRenderAreaRect
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassRenderAreaRect>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var origin: WGPUOrigin2D
            get() = WGPUOrigin2D.ByReference(NativeAddress.fromPointer(struct.origin.ptr))
            set(value) {
                val destBytes = struct.origin.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUOrigin2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var size: WGPUExtent2D
            get() = WGPUExtent2D.ByReference(NativeAddress.fromPointer(struct.size.ptr))
            set(value) {
                val destBytes = struct.size.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPURenderPassRenderAreaRect.toCValue(): CValue<webgpu.native.WGPURenderPassRenderAreaRect> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_origin = this.origin.ptr.reinterpret<ByteVar>()
    val src_origin = requireNotNull(this@toCValue.origin.handler.pointer).reinterpret<ByteVar>()
    val size_origin = sizeOf<webgpu.native.WGPUOrigin2D>().toLong()
    for (i in 0L until size_origin) {
        dest_origin[i.toInt()] = src_origin[i.toInt()]
    }
    val dest_size = this.size.ptr.reinterpret<ByteVar>()
    val src_size = requireNotNull(this@toCValue.size.handler.pointer).reinterpret<ByteVar>()
    val size_size = sizeOf<webgpu.native.WGPUExtent2D>().toLong()
    for (i in 0L until size_size) {
        dest_size[i.toInt()] = src_size[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassStorageAttachment =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassStorageAttachment>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassStorageAttachment) -> Unit): ArrayHolder<WGPURenderPassStorageAttachment> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassStorageAttachment>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassStorageAttachment>) : WGPURenderPassStorageAttachment {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var offset: ULong
            get() = handle.useContents { this.offset }
            set(value) { error("Setters not supported on ByValue") }
        override var storage: WGPUTextureView?
            get() = handle.useContents { this.storage?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var loadOp: WGPULoadOp
            get() = handle.useContents { this.loadOp as WGPULoadOp }
            set(value) { error("Setters not supported on ByValue") }
        override var storeOp: WGPUStoreOp
            get() = handle.useContents { this.storeOp as WGPUStoreOp }
            set(value) { error("Setters not supported on ByValue") }
        override var clearValue: WGPUColor
            get() = handle.useContents { WGPUColor.ByReference(NativeAddress.fromPointer(this.clearValue.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassStorageAttachment {
        private val struct: webgpu.native.WGPURenderPassStorageAttachment
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassStorageAttachment>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var offset: ULong
            get() = struct.offset
            set(value) { struct.offset = value }
        override var storage: WGPUTextureView?
            get() = struct.storage?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
            set(value) { struct.storage = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var loadOp: WGPULoadOp
            get() = struct.loadOp as WGPULoadOp
            set(value) { struct.loadOp = value }
        override var storeOp: WGPUStoreOp
            get() = struct.storeOp as WGPUStoreOp
            set(value) { struct.storeOp = value }
        override var clearValue: WGPUColor
            get() = WGPUColor.ByReference(NativeAddress.fromPointer(struct.clearValue.ptr))
            set(value) {
                val destBytes = struct.clearValue.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUColor>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPURenderPassStorageAttachment.toCValue(): CValue<webgpu.native.WGPURenderPassStorageAttachment> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.offset = this@toCValue.offset
    this.storage = this@toCValue.storage?.handler?.pointer?.takeIf { this@toCValue.storage?.handler?.rawValue != 0L }?.reinterpret()
    this.loadOp = this@toCValue.loadOp
    this.storeOp = this@toCValue.storeOp
    val dest_clearValue = this.clearValue.ptr.reinterpret<ByteVar>()
    val src_clearValue = requireNotNull(this@toCValue.clearValue.handler.pointer).reinterpret<ByteVar>()
    val size_clearValue = sizeOf<webgpu.native.WGPUColor>().toLong()
    for (i in 0L until size_clearValue) {
        dest_clearValue[i.toInt()] = src_clearValue[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPURequestAdapterOptions =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURequestAdapterOptions>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURequestAdapterOptions) -> Unit): ArrayHolder<WGPURequestAdapterOptions> {
            val byteSize = sizeOf<webgpu.native.WGPURequestAdapterOptions>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURequestAdapterOptions>) : WGPURequestAdapterOptions {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var featureLevel: WGPUFeatureLevel
            get() = handle.useContents { this.featureLevel as WGPUFeatureLevel }
            set(value) { error("Setters not supported on ByValue") }
        override var powerPreference: WGPUPowerPreference
            get() = handle.useContents { this.powerPreference as WGPUPowerPreference }
            set(value) { error("Setters not supported on ByValue") }
        override var forceFallbackAdapter: UInt
            get() = handle.useContents { this.forceFallbackAdapter }
            set(value) { error("Setters not supported on ByValue") }
        override var backendType: WGPUBackendType
            get() = handle.useContents { this.backendType as WGPUBackendType }
            set(value) { error("Setters not supported on ByValue") }
        override var compatibleSurface: WGPUSurface?
            get() = handle.useContents { this.compatibleSurface?.let { NativeAddress.fromPointer(it) }?.let { WGPUSurface(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURequestAdapterOptions {
        private val struct: webgpu.native.WGPURequestAdapterOptions
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURequestAdapterOptions>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var featureLevel: WGPUFeatureLevel
            get() = struct.featureLevel as WGPUFeatureLevel
            set(value) { struct.featureLevel = value }
        override var powerPreference: WGPUPowerPreference
            get() = struct.powerPreference as WGPUPowerPreference
            set(value) { struct.powerPreference = value }
        override var forceFallbackAdapter: UInt
            get() = struct.forceFallbackAdapter
            set(value) { struct.forceFallbackAdapter = value }
        override var backendType: WGPUBackendType
            get() = struct.backendType as WGPUBackendType
            set(value) { struct.backendType = value }
        override var compatibleSurface: WGPUSurface?
            get() = struct.compatibleSurface?.let { NativeAddress.fromPointer(it) }?.let { WGPUSurface(it) }
            set(value) { struct.compatibleSurface = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURequestAdapterOptions.toCValue(): CValue<webgpu.native.WGPURequestAdapterOptions> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.featureLevel = this@toCValue.featureLevel
    this.powerPreference = this@toCValue.powerPreference
    this.forceFallbackAdapter = this@toCValue.forceFallbackAdapter
    this.backendType = this@toCValue.backendType
    this.compatibleSurface = this@toCValue.compatibleSurface?.handler?.pointer?.takeIf { this@toCValue.compatibleSurface?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSamplerDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSamplerDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSamplerDescriptor) -> Unit): ArrayHolder<WGPUSamplerDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSamplerDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSamplerDescriptor>) : WGPUSamplerDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var addressModeU: WGPUAddressMode
            get() = handle.useContents { this.addressModeU as WGPUAddressMode }
            set(value) { error("Setters not supported on ByValue") }
        override var addressModeV: WGPUAddressMode
            get() = handle.useContents { this.addressModeV as WGPUAddressMode }
            set(value) { error("Setters not supported on ByValue") }
        override var addressModeW: WGPUAddressMode
            get() = handle.useContents { this.addressModeW as WGPUAddressMode }
            set(value) { error("Setters not supported on ByValue") }
        override var magFilter: WGPUFilterMode
            get() = handle.useContents { this.magFilter as WGPUFilterMode }
            set(value) { error("Setters not supported on ByValue") }
        override var minFilter: WGPUFilterMode
            get() = handle.useContents { this.minFilter as WGPUFilterMode }
            set(value) { error("Setters not supported on ByValue") }
        override var mipmapFilter: WGPUMipmapFilterMode
            get() = handle.useContents { this.mipmapFilter as WGPUMipmapFilterMode }
            set(value) { error("Setters not supported on ByValue") }
        override var lodMinClamp: Float
            get() = handle.useContents { this.lodMinClamp }
            set(value) { error("Setters not supported on ByValue") }
        override var lodMaxClamp: Float
            get() = handle.useContents { this.lodMaxClamp }
            set(value) { error("Setters not supported on ByValue") }
        override var compare: WGPUCompareFunction
            get() = handle.useContents { this.compare as WGPUCompareFunction }
            set(value) { error("Setters not supported on ByValue") }
        override var maxAnisotropy: UShort
            get() = handle.useContents { this.maxAnisotropy }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSamplerDescriptor {
        private val struct: webgpu.native.WGPUSamplerDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSamplerDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var addressModeU: WGPUAddressMode
            get() = struct.addressModeU as WGPUAddressMode
            set(value) { struct.addressModeU = value }
        override var addressModeV: WGPUAddressMode
            get() = struct.addressModeV as WGPUAddressMode
            set(value) { struct.addressModeV = value }
        override var addressModeW: WGPUAddressMode
            get() = struct.addressModeW as WGPUAddressMode
            set(value) { struct.addressModeW = value }
        override var magFilter: WGPUFilterMode
            get() = struct.magFilter as WGPUFilterMode
            set(value) { struct.magFilter = value }
        override var minFilter: WGPUFilterMode
            get() = struct.minFilter as WGPUFilterMode
            set(value) { struct.minFilter = value }
        override var mipmapFilter: WGPUMipmapFilterMode
            get() = struct.mipmapFilter as WGPUMipmapFilterMode
            set(value) { struct.mipmapFilter = value }
        override var lodMinClamp: Float
            get() = struct.lodMinClamp
            set(value) { struct.lodMinClamp = value }
        override var lodMaxClamp: Float
            get() = struct.lodMaxClamp
            set(value) { struct.lodMaxClamp = value }
        override var compare: WGPUCompareFunction
            get() = struct.compare as WGPUCompareFunction
            set(value) { struct.compare = value }
        override var maxAnisotropy: UShort
            get() = struct.maxAnisotropy
            set(value) { struct.maxAnisotropy = value }
    }
}

fun WGPUSamplerDescriptor.toCValue(): CValue<webgpu.native.WGPUSamplerDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.addressModeU = this@toCValue.addressModeU
    this.addressModeV = this@toCValue.addressModeV
    this.addressModeW = this@toCValue.addressModeW
    this.magFilter = this@toCValue.magFilter
    this.minFilter = this@toCValue.minFilter
    this.mipmapFilter = this@toCValue.mipmapFilter
    this.lodMinClamp = this@toCValue.lodMinClamp
    this.lodMaxClamp = this@toCValue.lodMaxClamp
    this.compare = this@toCValue.compare
    this.maxAnisotropy = this@toCValue.maxAnisotropy
}

actual interface WGPUShaderModuleDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUShaderModuleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUShaderModuleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUShaderModuleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUShaderModuleDescriptor) -> Unit): ArrayHolder<WGPUShaderModuleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUShaderModuleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUShaderModuleDescriptor>) : WGPUShaderModuleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUShaderModuleDescriptor {
        private val struct: webgpu.native.WGPUShaderModuleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUShaderModuleDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUShaderModuleDescriptor.toCValue(): CValue<webgpu.native.WGPUShaderModuleDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPUSharedBufferMemoryDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedBufferMemoryDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedBufferMemoryDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedBufferMemoryDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedBufferMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedBufferMemoryDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedBufferMemoryDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedBufferMemoryDescriptor>) : WGPUSharedBufferMemoryDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedBufferMemoryDescriptor {
        private val struct: webgpu.native.WGPUSharedBufferMemoryDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedBufferMemoryDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedBufferMemoryDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedBufferMemoryDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPUSharedFenceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceDescriptor) -> Unit): ArrayHolder<WGPUSharedFenceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceDescriptor>) : WGPUSharedFenceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceDescriptor {
        private val struct: webgpu.native.WGPUSharedFenceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedFenceDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedFenceDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPUSharedFenceExportInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var type: WGPUSharedFenceType
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedFenceExportInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedFenceExportInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedFenceExportInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedFenceExportInfo) -> Unit): ArrayHolder<WGPUSharedFenceExportInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedFenceExportInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedFenceExportInfo>) : WGPUSharedFenceExportInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var type: WGPUSharedFenceType
            get() = handle.useContents { this.type as WGPUSharedFenceType }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedFenceExportInfo {
        private val struct: webgpu.native.WGPUSharedFenceExportInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedFenceExportInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var type: WGPUSharedFenceType
            get() = struct.type as WGPUSharedFenceType
            set(value) { struct.type = value }
    }
}

fun WGPUSharedFenceExportInfo.toCValue(): CValue<webgpu.native.WGPUSharedFenceExportInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.type = this@toCValue.type
}

actual interface WGPUSharedTextureMemoryAHardwareBufferProperties {
    actual var chain: WGPUChainedStruct
    actual var yCbCrInfo: WGPUYCbCrVkDescriptor
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryAHardwareBufferProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryAHardwareBufferProperties =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryAHardwareBufferProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryAHardwareBufferProperties> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties>) : WGPUSharedTextureMemoryAHardwareBufferProperties {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = handle.useContents { WGPUYCbCrVkDescriptor.ByReference(NativeAddress.fromPointer(this.yCbCrInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryAHardwareBufferProperties {
        private val struct: webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var yCbCrInfo: WGPUYCbCrVkDescriptor
            get() = WGPUYCbCrVkDescriptor.ByReference(NativeAddress.fromPointer(struct.yCbCrInfo.ptr))
            set(value) {
                val destBytes = struct.yCbCrInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedTextureMemoryAHardwareBufferProperties.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryAHardwareBufferProperties> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_yCbCrInfo = this.yCbCrInfo.ptr.reinterpret<ByteVar>()
    val src_yCbCrInfo = requireNotNull(this@toCValue.yCbCrInfo.handler.pointer).reinterpret<ByteVar>()
    val size_yCbCrInfo = sizeOf<webgpu.native.WGPUYCbCrVkDescriptor>().toLong()
    for (i in 0L until size_yCbCrInfo) {
        dest_yCbCrInfo[i.toInt()] = src_yCbCrInfo[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryBeginAccessDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryBeginAccessDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryBeginAccessDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor>) : WGPUSharedTextureMemoryBeginAccessDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var concurrentRead: UInt
            get() = handle.useContents { this.concurrentRead }
            set(value) { error("Setters not supported on ByValue") }
        override var initialized: UInt
            get() = handle.useContents { this.initialized }
            set(value) { error("Setters not supported on ByValue") }
        override var fenceCount: ULong
            get() = handle.useContents { this.fenceCount }
            set(value) { error("Setters not supported on ByValue") }
        override var fences: NativeAddress?
            get() = handle.useContents { this.fences?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValueCount: ULong
            get() = handle.useContents { this.signaledValueCount }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValues: NativeAddress?
            get() = handle.useContents { this.signaledValues?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryBeginAccessDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var concurrentRead: UInt
            get() = struct.concurrentRead
            set(value) { struct.concurrentRead = value }
        override var initialized: UInt
            get() = struct.initialized
            set(value) { struct.initialized = value }
        override var fenceCount: ULong
            get() = struct.fenceCount
            set(value) { struct.fenceCount = value }
        override var fences: NativeAddress?
            get() = struct.fences?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.fences = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var signaledValueCount: ULong
            get() = struct.signaledValueCount
            set(value) { struct.signaledValueCount = value }
        override var signaledValues: NativeAddress?
            get() = struct.signaledValues?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.signaledValues = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedTextureMemoryBeginAccessDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryBeginAccessDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.concurrentRead = this@toCValue.concurrentRead
    this.initialized = this@toCValue.initialized
    this.fenceCount = this@toCValue.fenceCount
    this.fences = this@toCValue.fences?.pointer?.takeIf { this@toCValue.fences?.rawValue != 0L }?.reinterpret()
    this.signaledValueCount = this@toCValue.signaledValueCount
    this.signaledValues = this@toCValue.signaledValues?.pointer?.takeIf { this@toCValue.signaledValues?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDmaBufDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDmaBufDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDmaBufDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor>) : WGPUSharedTextureMemoryDmaBufDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var size: WGPUExtent3D
            get() = handle.useContents { WGPUExtent3D.ByReference(NativeAddress.fromPointer(this.size.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var drmFormat: UInt
            get() = handle.useContents { this.drmFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var drmModifier: ULong
            get() = handle.useContents { this.drmModifier }
            set(value) { error("Setters not supported on ByValue") }
        override var planeCount: ULong
            get() = handle.useContents { this.planeCount }
            set(value) { error("Setters not supported on ByValue") }
        override var planes: WGPUSharedTextureMemoryDmaBufPlane?
            get() = handle.useContents { this.planes?.let { NativeAddress.fromPointer(it) }?.let { WGPUSharedTextureMemoryDmaBufPlane(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryDmaBufDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByReference(NativeAddress.fromPointer(struct.size.ptr))
            set(value) {
                val destBytes = struct.size.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var drmFormat: UInt
            get() = struct.drmFormat
            set(value) { struct.drmFormat = value }
        override var drmModifier: ULong
            get() = struct.drmModifier
            set(value) { struct.drmModifier = value }
        override var planeCount: ULong
            get() = struct.planeCount
            set(value) { struct.planeCount = value }
        override var planes: WGPUSharedTextureMemoryDmaBufPlane?
            get() = struct.planes?.let { NativeAddress.fromPointer(it) }?.let { WGPUSharedTextureMemoryDmaBufPlane(it) }
            set(value) { struct.planes = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedTextureMemoryDmaBufDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryDmaBufDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_size = this.size.ptr.reinterpret<ByteVar>()
    val src_size = requireNotNull(this@toCValue.size.handler.pointer).reinterpret<ByteVar>()
    val size_size = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
    for (i in 0L until size_size) {
        dest_size[i.toInt()] = src_size[i.toInt()]
    }
    this.drmFormat = this@toCValue.drmFormat
    this.drmModifier = this@toCValue.drmModifier
    this.planeCount = this@toCValue.planeCount
    this.planes = this@toCValue.planes?.handler?.pointer?.takeIf { this@toCValue.planes?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedTextureMemoryMetalEndAccessState {
    actual var chain: WGPUChainedStruct
    actual var commandsScheduledFuture: WGPUFuture
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryMetalEndAccessState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryMetalEndAccessState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryMetalEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryMetalEndAccessState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState>) : WGPUSharedTextureMemoryMetalEndAccessState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var commandsScheduledFuture: WGPUFuture
            get() = handle.useContents { WGPUFuture.ByReference(NativeAddress.fromPointer(this.commandsScheduledFuture.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryMetalEndAccessState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var commandsScheduledFuture: WGPUFuture
            get() = WGPUFuture.ByReference(NativeAddress.fromPointer(struct.commandsScheduledFuture.ptr))
            set(value) {
                val destBytes = struct.commandsScheduledFuture.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUFuture>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedTextureMemoryMetalEndAccessState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryMetalEndAccessState> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_commandsScheduledFuture = this.commandsScheduledFuture.ptr.reinterpret<ByteVar>()
    val src_commandsScheduledFuture = requireNotNull(this@toCValue.commandsScheduledFuture.handler.pointer).reinterpret<ByteVar>()
    val size_commandsScheduledFuture = sizeOf<webgpu.native.WGPUFuture>().toLong()
    for (i in 0L until size_commandsScheduledFuture) {
        dest_commandsScheduledFuture[i.toInt()] = src_commandsScheduledFuture[i.toInt()]
    }
}

actual interface WGPUSurfaceDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSurfaceDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSurfaceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSurfaceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSurfaceDescriptor) -> Unit): ArrayHolder<WGPUSurfaceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSurfaceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSurfaceDescriptor>) : WGPUSurfaceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSurfaceDescriptor {
        private val struct: webgpu.native.WGPUSurfaceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSurfaceDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSurfaceDescriptor.toCValue(): CValue<webgpu.native.WGPUSurfaceDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
}

actual interface WGPUTexelCopyBufferInfo {
    actual var layout: WGPUTexelCopyBufferLayout
    actual var buffer: WGPUBuffer?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyBufferInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyBufferInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelCopyBufferInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyBufferInfo) -> Unit): ArrayHolder<WGPUTexelCopyBufferInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelCopyBufferInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelCopyBufferInfo>) : WGPUTexelCopyBufferInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var layout: WGPUTexelCopyBufferLayout
            get() = handle.useContents { WGPUTexelCopyBufferLayout.ByReference(NativeAddress.fromPointer(this.layout.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var buffer: WGPUBuffer?
            get() = handle.useContents { this.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelCopyBufferInfo {
        private val struct: webgpu.native.WGPUTexelCopyBufferInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelCopyBufferInfo>().pointed
        
        override var layout: WGPUTexelCopyBufferLayout
            get() = WGPUTexelCopyBufferLayout.ByReference(NativeAddress.fromPointer(struct.layout.ptr))
            set(value) {
                val destBytes = struct.layout.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUTexelCopyBufferLayout>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var buffer: WGPUBuffer?
            get() = struct.buffer?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
            set(value) { struct.buffer = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUTexelCopyBufferInfo.toCValue(): CValue<webgpu.native.WGPUTexelCopyBufferInfo> = cValue {
    val dest_layout = this.layout.ptr.reinterpret<ByteVar>()
    val src_layout = requireNotNull(this@toCValue.layout.handler.pointer).reinterpret<ByteVar>()
    val size_layout = sizeOf<webgpu.native.WGPUTexelCopyBufferLayout>().toLong()
    for (i in 0L until size_layout) {
        dest_layout[i.toInt()] = src_layout[i.toInt()]
    }
    this.buffer = this@toCValue.buffer?.handler?.pointer?.takeIf { this@toCValue.buffer?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUTexelCopyTextureInfo {
    actual var texture: WGPUTexture?
    actual var mipLevel: UInt
    actual var origin: WGPUOrigin3D
    actual var aspect: WGPUTextureAspect
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTexelCopyTextureInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTexelCopyTextureInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTexelCopyTextureInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTexelCopyTextureInfo) -> Unit): ArrayHolder<WGPUTexelCopyTextureInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUTexelCopyTextureInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTexelCopyTextureInfo>) : WGPUTexelCopyTextureInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var texture: WGPUTexture?
            get() = handle.useContents { this.texture?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var mipLevel: UInt
            get() = handle.useContents { this.mipLevel }
            set(value) { error("Setters not supported on ByValue") }
        override var origin: WGPUOrigin3D
            get() = handle.useContents { WGPUOrigin3D.ByReference(NativeAddress.fromPointer(this.origin.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var aspect: WGPUTextureAspect
            get() = handle.useContents { this.aspect as WGPUTextureAspect }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTexelCopyTextureInfo {
        private val struct: webgpu.native.WGPUTexelCopyTextureInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTexelCopyTextureInfo>().pointed
        
        override var texture: WGPUTexture?
            get() = struct.texture?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) }
            set(value) { struct.texture = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var mipLevel: UInt
            get() = struct.mipLevel
            set(value) { struct.mipLevel = value }
        override var origin: WGPUOrigin3D
            get() = WGPUOrigin3D.ByReference(NativeAddress.fromPointer(struct.origin.ptr))
            set(value) {
                val destBytes = struct.origin.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUOrigin3D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var aspect: WGPUTextureAspect
            get() = struct.aspect as WGPUTextureAspect
            set(value) { struct.aspect = value }
    }
}

fun WGPUTexelCopyTextureInfo.toCValue(): CValue<webgpu.native.WGPUTexelCopyTextureInfo> = cValue {
    this.texture = this@toCValue.texture?.handler?.pointer?.takeIf { this@toCValue.texture?.handler?.rawValue != 0L }?.reinterpret()
    this.mipLevel = this@toCValue.mipLevel
    val dest_origin = this.origin.ptr.reinterpret<ByteVar>()
    val src_origin = requireNotNull(this@toCValue.origin.handler.pointer).reinterpret<ByteVar>()
    val size_origin = sizeOf<webgpu.native.WGPUOrigin3D>().toLong()
    for (i in 0L until size_origin) {
        dest_origin[i.toInt()] = src_origin[i.toInt()]
    }
    this.aspect = this@toCValue.aspect
}

actual interface WGPUTextureComponentSwizzleDescriptor {
    actual var chain: WGPUChainedStruct
    actual var swizzle: WGPUTextureComponentSwizzle
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUTextureComponentSwizzleDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureComponentSwizzleDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureComponentSwizzleDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureComponentSwizzleDescriptor) -> Unit): ArrayHolder<WGPUTextureComponentSwizzleDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureComponentSwizzleDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureComponentSwizzleDescriptor>) : WGPUTextureComponentSwizzleDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var swizzle: WGPUTextureComponentSwizzle
            get() = handle.useContents { WGPUTextureComponentSwizzle.ByReference(NativeAddress.fromPointer(this.swizzle.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureComponentSwizzleDescriptor {
        private val struct: webgpu.native.WGPUTextureComponentSwizzleDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureComponentSwizzleDescriptor>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var swizzle: WGPUTextureComponentSwizzle
            get() = WGPUTextureComponentSwizzle.ByReference(NativeAddress.fromPointer(struct.swizzle.ptr))
            set(value) {
                val destBytes = struct.swizzle.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUTextureComponentSwizzle>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUTextureComponentSwizzleDescriptor.toCValue(): CValue<webgpu.native.WGPUTextureComponentSwizzleDescriptor> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    val dest_swizzle = this.swizzle.ptr.reinterpret<ByteVar>()
    val src_swizzle = requireNotNull(this@toCValue.swizzle.handler.pointer).reinterpret<ByteVar>()
    val size_swizzle = sizeOf<webgpu.native.WGPUTextureComponentSwizzle>().toLong()
    for (i in 0L until size_swizzle) {
        dest_swizzle[i.toInt()] = src_swizzle[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureDescriptor) -> Unit): ArrayHolder<WGPUTextureDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureDescriptor>) : WGPUTextureDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        override var dimension: WGPUTextureDimension
            get() = handle.useContents { this.dimension as WGPUTextureDimension }
            set(value) { error("Setters not supported on ByValue") }
        override var size: WGPUExtent3D
            get() = handle.useContents { WGPUExtent3D.ByReference(NativeAddress.fromPointer(this.size.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var mipLevelCount: UInt
            get() = handle.useContents { this.mipLevelCount }
            set(value) { error("Setters not supported on ByValue") }
        override var sampleCount: UInt
            get() = handle.useContents { this.sampleCount }
            set(value) { error("Setters not supported on ByValue") }
        override var viewFormatCount: ULong
            get() = handle.useContents { this.viewFormatCount }
            set(value) { error("Setters not supported on ByValue") }
        override var viewFormats: NativeAddress?
            get() = handle.useContents { this.viewFormats?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureDescriptor {
        private val struct: webgpu.native.WGPUTextureDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
        override var dimension: WGPUTextureDimension
            get() = struct.dimension as WGPUTextureDimension
            set(value) { struct.dimension = value }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByReference(NativeAddress.fromPointer(struct.size.ptr))
            set(value) {
                val destBytes = struct.size.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var mipLevelCount: UInt
            get() = struct.mipLevelCount
            set(value) { struct.mipLevelCount = value }
        override var sampleCount: UInt
            get() = struct.sampleCount
            set(value) { struct.sampleCount = value }
        override var viewFormatCount: ULong
            get() = struct.viewFormatCount
            set(value) { struct.viewFormatCount = value }
        override var viewFormats: NativeAddress?
            get() = struct.viewFormats?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.viewFormats = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUTextureDescriptor.toCValue(): CValue<webgpu.native.WGPUTextureDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.usage = this@toCValue.usage
    this.dimension = this@toCValue.dimension
    val dest_size = this.size.ptr.reinterpret<ByteVar>()
    val src_size = requireNotNull(this@toCValue.size.handler.pointer).reinterpret<ByteVar>()
    val size_size = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
    for (i in 0L until size_size) {
        dest_size[i.toInt()] = src_size[i.toInt()]
    }
    this.format = this@toCValue.format
    this.mipLevelCount = this@toCValue.mipLevelCount
    this.sampleCount = this@toCValue.sampleCount
    this.viewFormatCount = this@toCValue.viewFormatCount
    this.viewFormats = this@toCValue.viewFormats?.pointer?.takeIf { this@toCValue.viewFormats?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexBufferLayout =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUVertexBufferLayout>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexBufferLayout) -> Unit): ArrayHolder<WGPUVertexBufferLayout> {
            val byteSize = sizeOf<webgpu.native.WGPUVertexBufferLayout>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUVertexBufferLayout>) : WGPUVertexBufferLayout {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var stepMode: WGPUVertexStepMode
            get() = handle.useContents { this.stepMode as WGPUVertexStepMode }
            set(value) { error("Setters not supported on ByValue") }
        override var arrayStride: ULong
            get() = handle.useContents { this.arrayStride }
            set(value) { error("Setters not supported on ByValue") }
        override var attributeCount: ULong
            get() = handle.useContents { this.attributeCount }
            set(value) { error("Setters not supported on ByValue") }
        override var attributes: WGPUVertexAttribute?
            get() = handle.useContents { this.attributes?.let { NativeAddress.fromPointer(it) }?.let { WGPUVertexAttribute(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUVertexBufferLayout {
        private val struct: webgpu.native.WGPUVertexBufferLayout
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUVertexBufferLayout>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var stepMode: WGPUVertexStepMode
            get() = struct.stepMode as WGPUVertexStepMode
            set(value) { struct.stepMode = value }
        override var arrayStride: ULong
            get() = struct.arrayStride
            set(value) { struct.arrayStride = value }
        override var attributeCount: ULong
            get() = struct.attributeCount
            set(value) { struct.attributeCount = value }
        override var attributes: WGPUVertexAttribute?
            get() = struct.attributes?.let { NativeAddress.fromPointer(it) }?.let { WGPUVertexAttribute(it) }
            set(value) { struct.attributes = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUVertexBufferLayout.toCValue(): CValue<webgpu.native.WGPUVertexBufferLayout> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.stepMode = this@toCValue.stepMode
    this.arrayStride = this@toCValue.arrayStride
    this.attributeCount = this@toCValue.attributeCount
    this.attributes = this@toCValue.attributes?.handler?.pointer?.takeIf { this@toCValue.attributes?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUAdapterInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUAdapterInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUAdapterInfo) -> Unit): ArrayHolder<WGPUAdapterInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUAdapterInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUAdapterInfo>) : WGPUAdapterInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var vendor: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.vendor.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var architecture: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.architecture.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var device: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.device.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var description: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.description.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var backendType: WGPUBackendType
            get() = handle.useContents { this.backendType as WGPUBackendType }
            set(value) { error("Setters not supported on ByValue") }
        override var adapterType: WGPUAdapterType
            get() = handle.useContents { this.adapterType as WGPUAdapterType }
            set(value) { error("Setters not supported on ByValue") }
        override var vendorID: UInt
            get() = handle.useContents { this.vendorID }
            set(value) { error("Setters not supported on ByValue") }
        override var deviceID: UInt
            get() = handle.useContents { this.deviceID }
            set(value) { error("Setters not supported on ByValue") }
        override var subgroupMinSize: UInt
            get() = handle.useContents { this.subgroupMinSize }
            set(value) { error("Setters not supported on ByValue") }
        override var subgroupMaxSize: UInt
            get() = handle.useContents { this.subgroupMaxSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUAdapterInfo {
        private val struct: webgpu.native.WGPUAdapterInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUAdapterInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var vendor: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.vendor.ptr))
            set(value) {
                val destBytes = struct.vendor.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var architecture: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.architecture.ptr))
            set(value) {
                val destBytes = struct.architecture.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var device: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.device.ptr))
            set(value) {
                val destBytes = struct.device.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var description: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.description.ptr))
            set(value) {
                val destBytes = struct.description.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var backendType: WGPUBackendType
            get() = struct.backendType as WGPUBackendType
            set(value) { struct.backendType = value }
        override var adapterType: WGPUAdapterType
            get() = struct.adapterType as WGPUAdapterType
            set(value) { struct.adapterType = value }
        override var vendorID: UInt
            get() = struct.vendorID
            set(value) { struct.vendorID = value }
        override var deviceID: UInt
            get() = struct.deviceID
            set(value) { struct.deviceID = value }
        override var subgroupMinSize: UInt
            get() = struct.subgroupMinSize
            set(value) { struct.subgroupMinSize = value }
        override var subgroupMaxSize: UInt
            get() = struct.subgroupMaxSize
            set(value) { struct.subgroupMaxSize = value }
    }
}

fun WGPUAdapterInfo.toCValue(): CValue<webgpu.native.WGPUAdapterInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_vendor = this.vendor.ptr.reinterpret<ByteVar>()
    val src_vendor = requireNotNull(this@toCValue.vendor.handler.pointer).reinterpret<ByteVar>()
    val size_vendor = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_vendor) {
        dest_vendor[i.toInt()] = src_vendor[i.toInt()]
    }
    val dest_architecture = this.architecture.ptr.reinterpret<ByteVar>()
    val src_architecture = requireNotNull(this@toCValue.architecture.handler.pointer).reinterpret<ByteVar>()
    val size_architecture = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_architecture) {
        dest_architecture[i.toInt()] = src_architecture[i.toInt()]
    }
    val dest_device = this.device.ptr.reinterpret<ByteVar>()
    val src_device = requireNotNull(this@toCValue.device.handler.pointer).reinterpret<ByteVar>()
    val size_device = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_device) {
        dest_device[i.toInt()] = src_device[i.toInt()]
    }
    val dest_description = this.description.ptr.reinterpret<ByteVar>()
    val src_description = requireNotNull(this@toCValue.description.handler.pointer).reinterpret<ByteVar>()
    val size_description = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_description) {
        dest_description[i.toInt()] = src_description[i.toInt()]
    }
    this.backendType = this@toCValue.backendType
    this.adapterType = this@toCValue.adapterType
    this.vendorID = this@toCValue.vendorID
    this.deviceID = this@toCValue.deviceID
    this.subgroupMinSize = this@toCValue.subgroupMinSize
    this.subgroupMaxSize = this@toCValue.subgroupMaxSize
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
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBindGroupDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupDescriptor) -> Unit): ArrayHolder<WGPUBindGroupDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUBindGroupDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBindGroupDescriptor>) : WGPUBindGroupDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var layout: WGPUBindGroupLayout?
            get() = handle.useContents { this.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayout(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var entryCount: ULong
            get() = handle.useContents { this.entryCount }
            set(value) { error("Setters not supported on ByValue") }
        override var entries: WGPUBindGroupEntry?
            get() = handle.useContents { this.entries?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupEntry(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBindGroupDescriptor {
        private val struct: webgpu.native.WGPUBindGroupDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBindGroupDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var layout: WGPUBindGroupLayout?
            get() = struct.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayout(it) }
            set(value) { struct.layout = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var entryCount: ULong
            get() = struct.entryCount
            set(value) { struct.entryCount = value }
        override var entries: WGPUBindGroupEntry?
            get() = struct.entries?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupEntry(it) }
            set(value) { struct.entries = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBindGroupDescriptor.toCValue(): CValue<webgpu.native.WGPUBindGroupDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.layout = this@toCValue.layout?.handler?.pointer?.takeIf { this@toCValue.layout?.handler?.rawValue != 0L }?.reinterpret()
    this.entryCount = this@toCValue.entryCount
    this.entries = this@toCValue.entries?.handler?.pointer?.takeIf { this@toCValue.entries?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUBindGroupLayoutDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var entryCount: ULong
    actual var entries: WGPUBindGroupLayoutEntry?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUBindGroupLayoutDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUBindGroupLayoutDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUBindGroupLayoutDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUBindGroupLayoutDescriptor) -> Unit): ArrayHolder<WGPUBindGroupLayoutDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUBindGroupLayoutDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUBindGroupLayoutDescriptor>) : WGPUBindGroupLayoutDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var entryCount: ULong
            get() = handle.useContents { this.entryCount }
            set(value) { error("Setters not supported on ByValue") }
        override var entries: WGPUBindGroupLayoutEntry?
            get() = handle.useContents { this.entries?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayoutEntry(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUBindGroupLayoutDescriptor {
        private val struct: webgpu.native.WGPUBindGroupLayoutDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUBindGroupLayoutDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var entryCount: ULong
            get() = struct.entryCount
            set(value) { struct.entryCount = value }
        override var entries: WGPUBindGroupLayoutEntry?
            get() = struct.entries?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayoutEntry(it) }
            set(value) { struct.entries = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUBindGroupLayoutDescriptor.toCValue(): CValue<webgpu.native.WGPUBindGroupLayoutDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.entryCount = this@toCValue.entryCount
    this.entries = this@toCValue.entries?.handler?.pointer?.takeIf { this@toCValue.entries?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUColorTargetState {
    actual var nextInChain: WGPUChainedStruct?
    actual var format: WGPUTextureFormat
    actual var blend: WGPUBlendState?
    actual var writeMask: ULong
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUColorTargetState = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUColorTargetState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUColorTargetState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUColorTargetState) -> Unit): ArrayHolder<WGPUColorTargetState> {
            val byteSize = sizeOf<webgpu.native.WGPUColorTargetState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUColorTargetState>) : WGPUColorTargetState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var blend: WGPUBlendState?
            get() = handle.useContents { this.blend?.let { NativeAddress.fromPointer(it) }?.let { WGPUBlendState(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var writeMask: ULong
            get() = handle.useContents { this.writeMask }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUColorTargetState {
        private val struct: webgpu.native.WGPUColorTargetState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUColorTargetState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var blend: WGPUBlendState?
            get() = struct.blend?.let { NativeAddress.fromPointer(it) }?.let { WGPUBlendState(it) }
            set(value) { struct.blend = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var writeMask: ULong
            get() = struct.writeMask
            set(value) { struct.writeMask = value }
    }
}

fun WGPUColorTargetState.toCValue(): CValue<webgpu.native.WGPUColorTargetState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.format = this@toCValue.format
    this.blend = this@toCValue.blend?.handler?.pointer?.takeIf { this@toCValue.blend?.handler?.rawValue != 0L }?.reinterpret()
    this.writeMask = this@toCValue.writeMask
}

actual interface WGPUCompilationInfo {
    actual var nextInChain: WGPUChainedStruct?
    actual var messageCount: ULong
    actual var messages: WGPUCompilationMessage?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUCompilationInfo = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUCompilationInfo =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUCompilationInfo>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUCompilationInfo) -> Unit): ArrayHolder<WGPUCompilationInfo> {
            val byteSize = sizeOf<webgpu.native.WGPUCompilationInfo>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUCompilationInfo>) : WGPUCompilationInfo {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var messageCount: ULong
            get() = handle.useContents { this.messageCount }
            set(value) { error("Setters not supported on ByValue") }
        override var messages: WGPUCompilationMessage?
            get() = handle.useContents { this.messages?.let { NativeAddress.fromPointer(it) }?.let { WGPUCompilationMessage(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUCompilationInfo {
        private val struct: webgpu.native.WGPUCompilationInfo
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUCompilationInfo>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var messageCount: ULong
            get() = struct.messageCount
            set(value) { struct.messageCount = value }
        override var messages: WGPUCompilationMessage?
            get() = struct.messages?.let { NativeAddress.fromPointer(it) }?.let { WGPUCompilationMessage(it) }
            set(value) { struct.messages = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUCompilationInfo.toCValue(): CValue<webgpu.native.WGPUCompilationInfo> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.messageCount = this@toCValue.messageCount
    this.messages = this@toCValue.messages?.handler?.pointer?.takeIf { this@toCValue.messages?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUComputePipelineDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual var layout: WGPUPipelineLayout?
    actual var compute: WGPUComputeState
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUComputePipelineDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUComputePipelineDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUComputePipelineDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUComputePipelineDescriptor) -> Unit): ArrayHolder<WGPUComputePipelineDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUComputePipelineDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUComputePipelineDescriptor>) : WGPUComputePipelineDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var layout: WGPUPipelineLayout?
            get() = handle.useContents { this.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayout(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var compute: WGPUComputeState
            get() = handle.useContents { WGPUComputeState.ByReference(NativeAddress.fromPointer(this.compute.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUComputePipelineDescriptor {
        private val struct: webgpu.native.WGPUComputePipelineDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUComputePipelineDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var layout: WGPUPipelineLayout?
            get() = struct.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayout(it) }
            set(value) { struct.layout = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var compute: WGPUComputeState
            get() = WGPUComputeState.ByReference(NativeAddress.fromPointer(struct.compute.ptr))
            set(value) {
                val destBytes = struct.compute.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUComputeState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUComputePipelineDescriptor.toCValue(): CValue<webgpu.native.WGPUComputePipelineDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.layout = this@toCValue.layout?.handler?.pointer?.takeIf { this@toCValue.layout?.handler?.rawValue != 0L }?.reinterpret()
    val dest_compute = this.compute.ptr.reinterpret<ByteVar>()
    val src_compute = requireNotNull(this@toCValue.compute.handler.pointer).reinterpret<ByteVar>()
    val size_compute = sizeOf<webgpu.native.WGPUComputeState>().toLong()
    for (i in 0L until size_compute) {
        dest_compute[i.toInt()] = src_compute[i.toInt()]
    }
}

actual interface WGPUDawnFormatCapabilities {
    actual var nextInChain: WGPUChainedStruct?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUDawnFormatCapabilities = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUDawnFormatCapabilities =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDawnFormatCapabilities>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDawnFormatCapabilities) -> Unit): ArrayHolder<WGPUDawnFormatCapabilities> {
            val byteSize = sizeOf<webgpu.native.WGPUDawnFormatCapabilities>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDawnFormatCapabilities>) : WGPUDawnFormatCapabilities {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDawnFormatCapabilities {
        private val struct: webgpu.native.WGPUDawnFormatCapabilities
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDawnFormatCapabilities>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUDawnFormatCapabilities.toCValue(): CValue<webgpu.native.WGPUDawnFormatCapabilities> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUDeviceDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUDeviceDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUDeviceDescriptor) -> Unit): ArrayHolder<WGPUDeviceDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUDeviceDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUDeviceDescriptor>) : WGPUDeviceDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredFeatureCount: ULong
            get() = handle.useContents { this.requiredFeatureCount }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredFeatures: NativeAddress?
            get() = handle.useContents { this.requiredFeatures?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var requiredLimits: WGPULimits?
            get() = handle.useContents { this.requiredLimits?.let { NativeAddress.fromPointer(it) }?.let { WGPULimits(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var defaultQueue: WGPUQueueDescriptor
            get() = handle.useContents { WGPUQueueDescriptor.ByReference(NativeAddress.fromPointer(this.defaultQueue.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
            get() = handle.useContents { WGPUDeviceLostCallbackInfo.ByReference(NativeAddress.fromPointer(this.deviceLostCallbackInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
            get() = handle.useContents { WGPUUncapturedErrorCallbackInfo.ByReference(NativeAddress.fromPointer(this.uncapturedErrorCallbackInfo.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUDeviceDescriptor {
        private val struct: webgpu.native.WGPUDeviceDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUDeviceDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var requiredFeatureCount: ULong
            get() = struct.requiredFeatureCount
            set(value) { struct.requiredFeatureCount = value }
        override var requiredFeatures: NativeAddress?
            get() = struct.requiredFeatures?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.requiredFeatures = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var requiredLimits: WGPULimits?
            get() = struct.requiredLimits?.let { NativeAddress.fromPointer(it) }?.let { WGPULimits(it) }
            set(value) { struct.requiredLimits = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var defaultQueue: WGPUQueueDescriptor
            get() = WGPUQueueDescriptor.ByReference(NativeAddress.fromPointer(struct.defaultQueue.ptr))
            set(value) {
                val destBytes = struct.defaultQueue.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUQueueDescriptor>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var deviceLostCallbackInfo: WGPUDeviceLostCallbackInfo
            get() = WGPUDeviceLostCallbackInfo.ByReference(NativeAddress.fromPointer(struct.deviceLostCallbackInfo.ptr))
            set(value) {
                val destBytes = struct.deviceLostCallbackInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUDeviceLostCallbackInfo>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var uncapturedErrorCallbackInfo: WGPUUncapturedErrorCallbackInfo
            get() = WGPUUncapturedErrorCallbackInfo.ByReference(NativeAddress.fromPointer(struct.uncapturedErrorCallbackInfo.ptr))
            set(value) {
                val destBytes = struct.uncapturedErrorCallbackInfo.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUUncapturedErrorCallbackInfo>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUDeviceDescriptor.toCValue(): CValue<webgpu.native.WGPUDeviceDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.requiredFeatureCount = this@toCValue.requiredFeatureCount
    this.requiredFeatures = this@toCValue.requiredFeatures?.pointer?.takeIf { this@toCValue.requiredFeatures?.rawValue != 0L }?.reinterpret()
    this.requiredLimits = this@toCValue.requiredLimits?.handler?.pointer?.takeIf { this@toCValue.requiredLimits?.handler?.rawValue != 0L }?.reinterpret()
    val dest_defaultQueue = this.defaultQueue.ptr.reinterpret<ByteVar>()
    val src_defaultQueue = requireNotNull(this@toCValue.defaultQueue.handler.pointer).reinterpret<ByteVar>()
    val size_defaultQueue = sizeOf<webgpu.native.WGPUQueueDescriptor>().toLong()
    for (i in 0L until size_defaultQueue) {
        dest_defaultQueue[i.toInt()] = src_defaultQueue[i.toInt()]
    }
    val dest_deviceLostCallbackInfo = this.deviceLostCallbackInfo.ptr.reinterpret<ByteVar>()
    val src_deviceLostCallbackInfo = requireNotNull(this@toCValue.deviceLostCallbackInfo.handler.pointer).reinterpret<ByteVar>()
    val size_deviceLostCallbackInfo = sizeOf<webgpu.native.WGPUDeviceLostCallbackInfo>().toLong()
    for (i in 0L until size_deviceLostCallbackInfo) {
        dest_deviceLostCallbackInfo[i.toInt()] = src_deviceLostCallbackInfo[i.toInt()]
    }
    val dest_uncapturedErrorCallbackInfo = this.uncapturedErrorCallbackInfo.ptr.reinterpret<ByteVar>()
    val src_uncapturedErrorCallbackInfo = requireNotNull(this@toCValue.uncapturedErrorCallbackInfo.handler.pointer).reinterpret<ByteVar>()
    val size_uncapturedErrorCallbackInfo = sizeOf<webgpu.native.WGPUUncapturedErrorCallbackInfo>().toLong()
    for (i in 0L until size_uncapturedErrorCallbackInfo) {
        dest_uncapturedErrorCallbackInfo[i.toInt()] = src_uncapturedErrorCallbackInfo[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUPipelineLayoutDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUPipelineLayoutDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUPipelineLayoutDescriptor) -> Unit): ArrayHolder<WGPUPipelineLayoutDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUPipelineLayoutDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUPipelineLayoutDescriptor>) : WGPUPipelineLayoutDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var bindGroupLayoutCount: ULong
            get() = handle.useContents { this.bindGroupLayoutCount }
            set(value) { error("Setters not supported on ByValue") }
        override var bindGroupLayouts: NativeAddress?
            get() = handle.useContents { this.bindGroupLayouts?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var immediateSize: UInt
            get() = handle.useContents { this.immediateSize }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUPipelineLayoutDescriptor {
        private val struct: webgpu.native.WGPUPipelineLayoutDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUPipelineLayoutDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var bindGroupLayoutCount: ULong
            get() = struct.bindGroupLayoutCount
            set(value) { struct.bindGroupLayoutCount = value }
        override var bindGroupLayouts: NativeAddress?
            get() = struct.bindGroupLayouts?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.bindGroupLayouts = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var immediateSize: UInt
            get() = struct.immediateSize
            set(value) { struct.immediateSize = value }
    }
}

fun WGPUPipelineLayoutDescriptor.toCValue(): CValue<webgpu.native.WGPUPipelineLayoutDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.bindGroupLayoutCount = this@toCValue.bindGroupLayoutCount
    this.bindGroupLayouts = this@toCValue.bindGroupLayouts?.pointer?.takeIf { this@toCValue.bindGroupLayouts?.rawValue != 0L }?.reinterpret()
    this.immediateSize = this@toCValue.immediateSize
}

actual interface WGPURenderPassPixelLocalStorage {
    actual var chain: WGPUChainedStruct
    actual var totalPixelLocalStorageSize: ULong
    actual var storageAttachmentCount: ULong
    actual var storageAttachments: WGPURenderPassStorageAttachment?
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPURenderPassPixelLocalStorage = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassPixelLocalStorage =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassPixelLocalStorage>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassPixelLocalStorage) -> Unit): ArrayHolder<WGPURenderPassPixelLocalStorage> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassPixelLocalStorage>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassPixelLocalStorage>) : WGPURenderPassPixelLocalStorage {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var chain: WGPUChainedStruct
            get() = handle.useContents { WGPUChainedStruct.ByReference(NativeAddress.fromPointer(this.chain.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var totalPixelLocalStorageSize: ULong
            get() = handle.useContents { this.totalPixelLocalStorageSize }
            set(value) { error("Setters not supported on ByValue") }
        override var storageAttachmentCount: ULong
            get() = handle.useContents { this.storageAttachmentCount }
            set(value) { error("Setters not supported on ByValue") }
        override var storageAttachments: WGPURenderPassStorageAttachment?
            get() = handle.useContents { this.storageAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassStorageAttachment(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassPixelLocalStorage {
        private val struct: webgpu.native.WGPURenderPassPixelLocalStorage
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassPixelLocalStorage>().pointed
        
        override var chain: WGPUChainedStruct
            get() = WGPUChainedStruct.ByReference(NativeAddress.fromPointer(struct.chain.ptr))
            set(value) {
                val destBytes = struct.chain.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var totalPixelLocalStorageSize: ULong
            get() = struct.totalPixelLocalStorageSize
            set(value) { struct.totalPixelLocalStorageSize = value }
        override var storageAttachmentCount: ULong
            get() = struct.storageAttachmentCount
            set(value) { struct.storageAttachmentCount = value }
        override var storageAttachments: WGPURenderPassStorageAttachment?
            get() = struct.storageAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassStorageAttachment(it) }
            set(value) { struct.storageAttachments = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURenderPassPixelLocalStorage.toCValue(): CValue<webgpu.native.WGPURenderPassPixelLocalStorage> = cValue {
    val dest_chain = this.chain.ptr.reinterpret<ByteVar>()
    val src_chain = requireNotNull(this@toCValue.chain.handler.pointer).reinterpret<ByteVar>()
    val size_chain = sizeOf<webgpu.native.WGPUChainedStruct>().toLong()
    for (i in 0L until size_chain) {
        dest_chain[i.toInt()] = src_chain[i.toInt()]
    }
    this.totalPixelLocalStorageSize = this@toCValue.totalPixelLocalStorageSize
    this.storageAttachmentCount = this@toCValue.storageAttachmentCount
    this.storageAttachments = this@toCValue.storageAttachments?.handler?.pointer?.takeIf { this@toCValue.storageAttachments?.handler?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedTextureMemoryDescriptor {
    actual var nextInChain: WGPUChainedStruct?
    actual var label: WGPUStringView
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryDescriptor = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryDescriptor) -> Unit): ArrayHolder<WGPUSharedTextureMemoryDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryDescriptor>) : WGPUSharedTextureMemoryDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryDescriptor {
        private val struct: webgpu.native.WGPUSharedTextureMemoryDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
    }
}

fun WGPUSharedTextureMemoryDescriptor.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
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
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryEndAccessState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryEndAccessState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryEndAccessState) -> Unit): ArrayHolder<WGPUSharedTextureMemoryEndAccessState> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryEndAccessState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryEndAccessState>) : WGPUSharedTextureMemoryEndAccessState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var initialized: UInt
            get() = handle.useContents { this.initialized }
            set(value) { error("Setters not supported on ByValue") }
        override var fenceCount: ULong
            get() = handle.useContents { this.fenceCount }
            set(value) { error("Setters not supported on ByValue") }
        override var fences: NativeAddress?
            get() = handle.useContents { this.fences?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValueCount: ULong
            get() = handle.useContents { this.signaledValueCount }
            set(value) { error("Setters not supported on ByValue") }
        override var signaledValues: NativeAddress?
            get() = handle.useContents { this.signaledValues?.let { NativeAddress.fromPointer(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryEndAccessState {
        private val struct: webgpu.native.WGPUSharedTextureMemoryEndAccessState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryEndAccessState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var initialized: UInt
            get() = struct.initialized
            set(value) { struct.initialized = value }
        override var fenceCount: ULong
            get() = struct.fenceCount
            set(value) { struct.fenceCount = value }
        override var fences: NativeAddress?
            get() = struct.fences?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.fences = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
        override var signaledValueCount: ULong
            get() = struct.signaledValueCount
            set(value) { struct.signaledValueCount = value }
        override var signaledValues: NativeAddress?
            get() = struct.signaledValues?.let { NativeAddress.fromPointer(it) }
            set(value) { struct.signaledValues = value?.pointer?.takeIf { value.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUSharedTextureMemoryEndAccessState.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryEndAccessState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.initialized = this@toCValue.initialized
    this.fenceCount = this@toCValue.fenceCount
    this.fences = this@toCValue.fences?.pointer?.takeIf { this@toCValue.fences?.rawValue != 0L }?.reinterpret()
    this.signaledValueCount = this@toCValue.signaledValueCount
    this.signaledValues = this@toCValue.signaledValues?.pointer?.takeIf { this@toCValue.signaledValues?.rawValue != 0L }?.reinterpret()
}

actual interface WGPUSharedTextureMemoryProperties {
    actual var nextInChain: WGPUChainedStruct?
    actual var usage: ULong
    actual var size: WGPUExtent3D
    actual var format: WGPUTextureFormat
    actual val handler: NativeAddress
    actual companion object {
        actual operator fun invoke(address: NativeAddress): WGPUSharedTextureMemoryProperties = ByReference(address)
        actual fun allocate(allocator: MemoryAllocator): WGPUSharedTextureMemoryProperties =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUSharedTextureMemoryProperties>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUSharedTextureMemoryProperties) -> Unit): ArrayHolder<WGPUSharedTextureMemoryProperties> {
            val byteSize = sizeOf<webgpu.native.WGPUSharedTextureMemoryProperties>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUSharedTextureMemoryProperties>) : WGPUSharedTextureMemoryProperties {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        override var size: WGPUExtent3D
            get() = handle.useContents { WGPUExtent3D.ByReference(NativeAddress.fromPointer(this.size.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUSharedTextureMemoryProperties {
        private val struct: webgpu.native.WGPUSharedTextureMemoryProperties
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUSharedTextureMemoryProperties>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
        override var size: WGPUExtent3D
            get() = WGPUExtent3D.ByReference(NativeAddress.fromPointer(struct.size.ptr))
            set(value) {
                val destBytes = struct.size.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
    }
}

fun WGPUSharedTextureMemoryProperties.toCValue(): CValue<webgpu.native.WGPUSharedTextureMemoryProperties> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.usage = this@toCValue.usage
    val dest_size = this.size.ptr.reinterpret<ByteVar>()
    val src_size = requireNotNull(this@toCValue.size.handler.pointer).reinterpret<ByteVar>()
    val size_size = sizeOf<webgpu.native.WGPUExtent3D>().toLong()
    for (i in 0L until size_size) {
        dest_size[i.toInt()] = src_size[i.toInt()]
    }
    this.format = this@toCValue.format
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
        actual fun allocate(allocator: MemoryAllocator): WGPUTextureViewDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUTextureViewDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUTextureViewDescriptor) -> Unit): ArrayHolder<WGPUTextureViewDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPUTextureViewDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUTextureViewDescriptor>) : WGPUTextureViewDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var format: WGPUTextureFormat
            get() = handle.useContents { this.format as WGPUTextureFormat }
            set(value) { error("Setters not supported on ByValue") }
        override var dimension: WGPUTextureViewDimension
            get() = handle.useContents { this.dimension as WGPUTextureViewDimension }
            set(value) { error("Setters not supported on ByValue") }
        override var baseMipLevel: UInt
            get() = handle.useContents { this.baseMipLevel }
            set(value) { error("Setters not supported on ByValue") }
        override var mipLevelCount: UInt
            get() = handle.useContents { this.mipLevelCount }
            set(value) { error("Setters not supported on ByValue") }
        override var baseArrayLayer: UInt
            get() = handle.useContents { this.baseArrayLayer }
            set(value) { error("Setters not supported on ByValue") }
        override var arrayLayerCount: UInt
            get() = handle.useContents { this.arrayLayerCount }
            set(value) { error("Setters not supported on ByValue") }
        override var aspect: WGPUTextureAspect
            get() = handle.useContents { this.aspect as WGPUTextureAspect }
            set(value) { error("Setters not supported on ByValue") }
        override var usage: ULong
            get() = handle.useContents { this.usage }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUTextureViewDescriptor {
        private val struct: webgpu.native.WGPUTextureViewDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUTextureViewDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var format: WGPUTextureFormat
            get() = struct.format as WGPUTextureFormat
            set(value) { struct.format = value }
        override var dimension: WGPUTextureViewDimension
            get() = struct.dimension as WGPUTextureViewDimension
            set(value) { struct.dimension = value }
        override var baseMipLevel: UInt
            get() = struct.baseMipLevel
            set(value) { struct.baseMipLevel = value }
        override var mipLevelCount: UInt
            get() = struct.mipLevelCount
            set(value) { struct.mipLevelCount = value }
        override var baseArrayLayer: UInt
            get() = struct.baseArrayLayer
            set(value) { struct.baseArrayLayer = value }
        override var arrayLayerCount: UInt
            get() = struct.arrayLayerCount
            set(value) { struct.arrayLayerCount = value }
        override var aspect: WGPUTextureAspect
            get() = struct.aspect as WGPUTextureAspect
            set(value) { struct.aspect = value }
        override var usage: ULong
            get() = struct.usage
            set(value) { struct.usage = value }
    }
}

fun WGPUTextureViewDescriptor.toCValue(): CValue<webgpu.native.WGPUTextureViewDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.format = this@toCValue.format
    this.dimension = this@toCValue.dimension
    this.baseMipLevel = this@toCValue.baseMipLevel
    this.mipLevelCount = this@toCValue.mipLevelCount
    this.baseArrayLayer = this@toCValue.baseArrayLayer
    this.arrayLayerCount = this@toCValue.arrayLayerCount
    this.aspect = this@toCValue.aspect
    this.usage = this@toCValue.usage
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
        actual fun allocate(allocator: MemoryAllocator): WGPUVertexState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUVertexState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUVertexState) -> Unit): ArrayHolder<WGPUVertexState> {
            val byteSize = sizeOf<webgpu.native.WGPUVertexState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUVertexState>) : WGPUVertexState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var module: WGPUShaderModule?
            get() = handle.useContents { this.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var entryPoint: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.entryPoint.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var constantCount: ULong
            get() = handle.useContents { this.constantCount }
            set(value) { error("Setters not supported on ByValue") }
        override var constants: WGPUConstantEntry?
            get() = handle.useContents { this.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var bufferCount: ULong
            get() = handle.useContents { this.bufferCount }
            set(value) { error("Setters not supported on ByValue") }
        override var buffers: WGPUVertexBufferLayout?
            get() = handle.useContents { this.buffers?.let { NativeAddress.fromPointer(it) }?.let { WGPUVertexBufferLayout(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUVertexState {
        private val struct: webgpu.native.WGPUVertexState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUVertexState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var module: WGPUShaderModule?
            get() = struct.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) }
            set(value) { struct.module = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.entryPoint.ptr))
            set(value) {
                val destBytes = struct.entryPoint.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var constantCount: ULong
            get() = struct.constantCount
            set(value) { struct.constantCount = value }
        override var constants: WGPUConstantEntry?
            get() = struct.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) }
            set(value) { struct.constants = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var bufferCount: ULong
            get() = struct.bufferCount
            set(value) { struct.bufferCount = value }
        override var buffers: WGPUVertexBufferLayout?
            get() = struct.buffers?.let { NativeAddress.fromPointer(it) }?.let { WGPUVertexBufferLayout(it) }
            set(value) { struct.buffers = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUVertexState.toCValue(): CValue<webgpu.native.WGPUVertexState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.module = this@toCValue.module?.handler?.pointer?.takeIf { this@toCValue.module?.handler?.rawValue != 0L }?.reinterpret()
    val dest_entryPoint = this.entryPoint.ptr.reinterpret<ByteVar>()
    val src_entryPoint = requireNotNull(this@toCValue.entryPoint.handler.pointer).reinterpret<ByteVar>()
    val size_entryPoint = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_entryPoint) {
        dest_entryPoint[i.toInt()] = src_entryPoint[i.toInt()]
    }
    this.constantCount = this@toCValue.constantCount
    this.constants = this@toCValue.constants?.handler?.pointer?.takeIf { this@toCValue.constants?.handler?.rawValue != 0L }?.reinterpret()
    this.bufferCount = this@toCValue.bufferCount
    this.buffers = this@toCValue.buffers?.handler?.pointer?.takeIf { this@toCValue.buffers?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPUFragmentState =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPUFragmentState>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPUFragmentState) -> Unit): ArrayHolder<WGPUFragmentState> {
            val byteSize = sizeOf<webgpu.native.WGPUFragmentState>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPUFragmentState>) : WGPUFragmentState {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var module: WGPUShaderModule?
            get() = handle.useContents { this.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var entryPoint: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.entryPoint.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var constantCount: ULong
            get() = handle.useContents { this.constantCount }
            set(value) { error("Setters not supported on ByValue") }
        override var constants: WGPUConstantEntry?
            get() = handle.useContents { this.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var targetCount: ULong
            get() = handle.useContents { this.targetCount }
            set(value) { error("Setters not supported on ByValue") }
        override var targets: WGPUColorTargetState?
            get() = handle.useContents { this.targets?.let { NativeAddress.fromPointer(it) }?.let { WGPUColorTargetState(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPUFragmentState {
        private val struct: webgpu.native.WGPUFragmentState
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPUFragmentState>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var module: WGPUShaderModule?
            get() = struct.module?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) }
            set(value) { struct.module = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var entryPoint: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.entryPoint.ptr))
            set(value) {
                val destBytes = struct.entryPoint.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var constantCount: ULong
            get() = struct.constantCount
            set(value) { struct.constantCount = value }
        override var constants: WGPUConstantEntry?
            get() = struct.constants?.let { NativeAddress.fromPointer(it) }?.let { WGPUConstantEntry(it) }
            set(value) { struct.constants = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var targetCount: ULong
            get() = struct.targetCount
            set(value) { struct.targetCount = value }
        override var targets: WGPUColorTargetState?
            get() = struct.targets?.let { NativeAddress.fromPointer(it) }?.let { WGPUColorTargetState(it) }
            set(value) { struct.targets = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPUFragmentState.toCValue(): CValue<webgpu.native.WGPUFragmentState> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    this.module = this@toCValue.module?.handler?.pointer?.takeIf { this@toCValue.module?.handler?.rawValue != 0L }?.reinterpret()
    val dest_entryPoint = this.entryPoint.ptr.reinterpret<ByteVar>()
    val src_entryPoint = requireNotNull(this@toCValue.entryPoint.handler.pointer).reinterpret<ByteVar>()
    val size_entryPoint = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_entryPoint) {
        dest_entryPoint[i.toInt()] = src_entryPoint[i.toInt()]
    }
    this.constantCount = this@toCValue.constantCount
    this.constants = this@toCValue.constants?.handler?.pointer?.takeIf { this@toCValue.constants?.handler?.rawValue != 0L }?.reinterpret()
    this.targetCount = this@toCValue.targetCount
    this.targets = this@toCValue.targets?.handler?.pointer?.takeIf { this@toCValue.targets?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPassDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPassDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPassDescriptor) -> Unit): ArrayHolder<WGPURenderPassDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPassDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPassDescriptor>) : WGPURenderPassDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var colorAttachmentCount: ULong
            get() = handle.useContents { this.colorAttachmentCount }
            set(value) { error("Setters not supported on ByValue") }
        override var colorAttachments: WGPURenderPassColorAttachment?
            get() = handle.useContents { this.colorAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassColorAttachment(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
            get() = handle.useContents { this.depthStencilAttachment?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassDepthStencilAttachment(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var occlusionQuerySet: WGPUQuerySet?
            get() = handle.useContents { this.occlusionQuerySet?.let { NativeAddress.fromPointer(it) }?.let { WGPUQuerySet(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = handle.useContents { this.timestampWrites?.let { NativeAddress.fromPointer(it) }?.let { WGPUPassTimestampWrites(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPassDescriptor {
        private val struct: webgpu.native.WGPURenderPassDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPassDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var colorAttachmentCount: ULong
            get() = struct.colorAttachmentCount
            set(value) { struct.colorAttachmentCount = value }
        override var colorAttachments: WGPURenderPassColorAttachment?
            get() = struct.colorAttachments?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassColorAttachment(it) }
            set(value) { struct.colorAttachments = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var depthStencilAttachment: WGPURenderPassDepthStencilAttachment?
            get() = struct.depthStencilAttachment?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassDepthStencilAttachment(it) }
            set(value) { struct.depthStencilAttachment = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var occlusionQuerySet: WGPUQuerySet?
            get() = struct.occlusionQuerySet?.let { NativeAddress.fromPointer(it) }?.let { WGPUQuerySet(it) }
            set(value) { struct.occlusionQuerySet = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var timestampWrites: WGPUPassTimestampWrites?
            get() = struct.timestampWrites?.let { NativeAddress.fromPointer(it) }?.let { WGPUPassTimestampWrites(it) }
            set(value) { struct.timestampWrites = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURenderPassDescriptor.toCValue(): CValue<webgpu.native.WGPURenderPassDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.colorAttachmentCount = this@toCValue.colorAttachmentCount
    this.colorAttachments = this@toCValue.colorAttachments?.handler?.pointer?.takeIf { this@toCValue.colorAttachments?.handler?.rawValue != 0L }?.reinterpret()
    this.depthStencilAttachment = this@toCValue.depthStencilAttachment?.handler?.pointer?.takeIf { this@toCValue.depthStencilAttachment?.handler?.rawValue != 0L }?.reinterpret()
    this.occlusionQuerySet = this@toCValue.occlusionQuerySet?.handler?.pointer?.takeIf { this@toCValue.occlusionQuerySet?.handler?.rawValue != 0L }?.reinterpret()
    this.timestampWrites = this@toCValue.timestampWrites?.handler?.pointer?.takeIf { this@toCValue.timestampWrites?.handler?.rawValue != 0L }?.reinterpret()
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
        actual fun allocate(allocator: MemoryAllocator): WGPURenderPipelineDescriptor =
            ByReference(allocator.allocate(sizeOf<webgpu.native.WGPURenderPipelineDescriptor>().toLong()))
        
        actual fun allocateArray(allocator: MemoryAllocator, size: UInt, provider: (UInt, WGPURenderPipelineDescriptor) -> Unit): ArrayHolder<WGPURenderPipelineDescriptor> {
            val byteSize = sizeOf<webgpu.native.WGPURenderPipelineDescriptor>().toLong()
            val segment = allocator.allocate(byteSize * size.toLong())
            for (i in 0 until size.toInt()) {
                val rawAddr = segment.rawValue + i.toLong() * byteSize
                provider(i.toUInt(), ByReference(NativeAddress(rawAddr)))
            }
            return ArrayHolder(segment)
        }
    }
    
        value class ByValue(val handle: CValue<webgpu.native.WGPURenderPipelineDescriptor>) : WGPURenderPipelineDescriptor {
        override val handler: NativeAddress
            get() = error("should not be call on CValue")
        
        override var nextInChain: WGPUChainedStruct?
            get() = handle.useContents { this.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var label: WGPUStringView
            get() = handle.useContents { WGPUStringView.ByReference(NativeAddress.fromPointer(this.label.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var layout: WGPUPipelineLayout?
            get() = handle.useContents { this.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayout(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var vertex: WGPUVertexState
            get() = handle.useContents { WGPUVertexState.ByReference(NativeAddress.fromPointer(this.vertex.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var primitive: WGPUPrimitiveState
            get() = handle.useContents { WGPUPrimitiveState.ByReference(NativeAddress.fromPointer(this.primitive.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var depthStencil: WGPUDepthStencilState?
            get() = handle.useContents { this.depthStencil?.let { NativeAddress.fromPointer(it) }?.let { WGPUDepthStencilState(it) } }
            set(value) { error("Setters not supported on ByValue") }
        override var multisample: WGPUMultisampleState
            get() = handle.useContents { WGPUMultisampleState.ByReference(NativeAddress.fromPointer(this.multisample.ptr)) }
            set(value) { error("Setters not supported on ByValue") }
        override var fragment: WGPUFragmentState?
            get() = handle.useContents { this.fragment?.let { NativeAddress.fromPointer(it) }?.let { WGPUFragmentState(it) } }
            set(value) { error("Setters not supported on ByValue") }
        }
    
    class ByReference(override val handler: NativeAddress) : WGPURenderPipelineDescriptor {
        private val struct: webgpu.native.WGPURenderPipelineDescriptor
            get() = requireNotNull(handler.pointer).reinterpret<webgpu.native.WGPURenderPipelineDescriptor>().pointed
        
        override var nextInChain: WGPUChainedStruct?
            get() = struct.nextInChain?.let { NativeAddress.fromPointer(it) }?.let { WGPUChainedStruct(it) }
            set(value) { struct.nextInChain = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var label: WGPUStringView
            get() = WGPUStringView.ByReference(NativeAddress.fromPointer(struct.label.ptr))
            set(value) {
                val destBytes = struct.label.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUStringView>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var layout: WGPUPipelineLayout?
            get() = struct.layout?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayout(it) }
            set(value) { struct.layout = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var vertex: WGPUVertexState
            get() = WGPUVertexState.ByReference(NativeAddress.fromPointer(struct.vertex.ptr))
            set(value) {
                val destBytes = struct.vertex.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUVertexState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var primitive: WGPUPrimitiveState
            get() = WGPUPrimitiveState.ByReference(NativeAddress.fromPointer(struct.primitive.ptr))
            set(value) {
                val destBytes = struct.primitive.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUPrimitiveState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var depthStencil: WGPUDepthStencilState?
            get() = struct.depthStencil?.let { NativeAddress.fromPointer(it) }?.let { WGPUDepthStencilState(it) }
            set(value) { struct.depthStencil = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
        override var multisample: WGPUMultisampleState
            get() = WGPUMultisampleState.ByReference(NativeAddress.fromPointer(struct.multisample.ptr))
            set(value) {
                val destBytes = struct.multisample.ptr.reinterpret<ByteVar>()
                val srcBytes = requireNotNull(value.handler.pointer).reinterpret<ByteVar>()
                val byteSize = sizeOf<webgpu.native.WGPUMultisampleState>().toLong()
                for (i in 0L until byteSize) {
                    destBytes[i.toInt()] = srcBytes[i.toInt()]
                }
            }
        override var fragment: WGPUFragmentState?
            get() = struct.fragment?.let { NativeAddress.fromPointer(it) }?.let { WGPUFragmentState(it) }
            set(value) { struct.fragment = value?.handler?.pointer?.takeIf { value.handler.rawValue != 0L }?.reinterpret() }
    }
}

fun WGPURenderPipelineDescriptor.toCValue(): CValue<webgpu.native.WGPURenderPipelineDescriptor> = cValue {
    this.nextInChain = this@toCValue.nextInChain?.handler?.pointer?.takeIf { this@toCValue.nextInChain?.handler?.rawValue != 0L }?.reinterpret()
    val dest_label = this.label.ptr.reinterpret<ByteVar>()
    val src_label = requireNotNull(this@toCValue.label.handler.pointer).reinterpret<ByteVar>()
    val size_label = sizeOf<webgpu.native.WGPUStringView>().toLong()
    for (i in 0L until size_label) {
        dest_label[i.toInt()] = src_label[i.toInt()]
    }
    this.layout = this@toCValue.layout?.handler?.pointer?.takeIf { this@toCValue.layout?.handler?.rawValue != 0L }?.reinterpret()
    val dest_vertex = this.vertex.ptr.reinterpret<ByteVar>()
    val src_vertex = requireNotNull(this@toCValue.vertex.handler.pointer).reinterpret<ByteVar>()
    val size_vertex = sizeOf<webgpu.native.WGPUVertexState>().toLong()
    for (i in 0L until size_vertex) {
        dest_vertex[i.toInt()] = src_vertex[i.toInt()]
    }
    val dest_primitive = this.primitive.ptr.reinterpret<ByteVar>()
    val src_primitive = requireNotNull(this@toCValue.primitive.handler.pointer).reinterpret<ByteVar>()
    val size_primitive = sizeOf<webgpu.native.WGPUPrimitiveState>().toLong()
    for (i in 0L until size_primitive) {
        dest_primitive[i.toInt()] = src_primitive[i.toInt()]
    }
    this.depthStencil = this@toCValue.depthStencil?.handler?.pointer?.takeIf { this@toCValue.depthStencil?.handler?.rawValue != 0L }?.reinterpret()
    val dest_multisample = this.multisample.ptr.reinterpret<ByteVar>()
    val src_multisample = requireNotNull(this@toCValue.multisample.handler.pointer).reinterpret<ByteVar>()
    val size_multisample = sizeOf<webgpu.native.WGPUMultisampleState>().toLong()
    for (i in 0L until size_multisample) {
        dest_multisample[i.toInt()] = src_multisample[i.toInt()]
    }
    this.fragment = this@toCValue.fragment?.handler?.pointer?.takeIf { this@toCValue.fragment?.handler?.rawValue != 0L }?.reinterpret()
}

actual fun wgpuCreateInstance(descriptor: WGPUInstanceDescriptor?): WGPUInstance? {
    return webgpu.native.wgpuCreateInstance(descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUInstance(it) }
}

actual fun wgpuGetInstanceFeatures(features: WGPUSupportedInstanceFeatures?): Unit {
    webgpu.native.wgpuGetInstanceFeatures(features?.handler?.pointer?.takeIf { features.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuGetInstanceLimits(limits: WGPUInstanceLimits?): WGPUStatus {
    return webgpu.native.wgpuGetInstanceLimits(limits?.handler?.pointer?.takeIf { limits.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuHasInstanceFeature(feature: WGPUInstanceFeatureName): UInt {
    return webgpu.native.wgpuHasInstanceFeature(feature)
}

actual fun wgpuGetProcAddress(procName: WGPUStringView): NativeAddress? {
    return webgpu.native.wgpuGetProcAddress(procName.toCValue())?.let { NativeAddress.fromPointer(it) }
}

actual fun wgpuAdapterCreateDevice(adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?): WGPUDevice? {
    return webgpu.native.wgpuAdapterCreateDevice(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUDevice(it) }
}

actual fun wgpuAdapterGetFeatures(adapter: WGPUAdapter?, features: WGPUSupportedFeatures?): Unit {
    webgpu.native.wgpuAdapterGetFeatures(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), features?.handler?.pointer?.takeIf { features.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuAdapterGetFormatCapabilities(adapter: WGPUAdapter?, format: WGPUTextureFormat, capabilities: WGPUDawnFormatCapabilities?): WGPUStatus {
    return webgpu.native.wgpuAdapterGetFormatCapabilities(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), format, capabilities?.handler?.pointer?.takeIf { capabilities.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuAdapterGetInfo(adapter: WGPUAdapter?, info: WGPUAdapterInfo?): WGPUStatus {
    return webgpu.native.wgpuAdapterGetInfo(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), info?.handler?.pointer?.takeIf { info.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuAdapterGetInstance(adapter: WGPUAdapter?): WGPUInstance? {
    return webgpu.native.wgpuAdapterGetInstance(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUInstance(it) }
}

actual fun wgpuAdapterGetLimits(adapter: WGPUAdapter?, limits: WGPULimits?): WGPUStatus {
    return webgpu.native.wgpuAdapterGetLimits(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), limits?.handler?.pointer?.takeIf { limits.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuAdapterHasFeature(adapter: WGPUAdapter?, feature: WGPUFeatureName): UInt {
    return webgpu.native.wgpuAdapterHasFeature(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), feature)
}

actual fun wgpuAdapterRequestDevice(allocator: MemoryAllocator, adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?, callbackInfo: WGPURequestDeviceCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuAdapterRequestDevice(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuAdapterAddRef(adapter: WGPUAdapter?): Unit {
    webgpu.native.wgpuAdapterAddRef(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuAdapterRelease(adapter: WGPUAdapter?): Unit {
    webgpu.native.wgpuAdapterRelease(adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuAdapterInfoFreeMembers(adapterInfo: WGPUAdapterInfo): Unit {
    webgpu.native.wgpuAdapterInfoFreeMembers(adapterInfo.toCValue())
    return
}

actual fun wgpuAdapterPropertiesMemoryHeapsFreeMembers(adapterPropertiesMemoryHeaps: WGPUAdapterPropertiesMemoryHeaps): Unit {
    webgpu.native.wgpuAdapterPropertiesMemoryHeapsFreeMembers(adapterPropertiesMemoryHeaps.toCValue())
    return
}

actual fun wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers(adapterPropertiesSubgroupMatrixConfigs: WGPUAdapterPropertiesSubgroupMatrixConfigs): Unit {
    webgpu.native.wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers(adapterPropertiesSubgroupMatrixConfigs.toCValue())
    return
}

actual fun wgpuBindGroupSetLabel(bindGroup: WGPUBindGroup?, label: WGPUStringView): Unit {
    webgpu.native.wgpuBindGroupSetLabel(bindGroup?.handler?.pointer?.takeIf { bindGroup.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuBindGroupAddRef(bindGroup: WGPUBindGroup?): Unit {
    webgpu.native.wgpuBindGroupAddRef(bindGroup?.handler?.pointer?.takeIf { bindGroup.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBindGroupRelease(bindGroup: WGPUBindGroup?): Unit {
    webgpu.native.wgpuBindGroupRelease(bindGroup?.handler?.pointer?.takeIf { bindGroup.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBindGroupLayoutSetLabel(bindGroupLayout: WGPUBindGroupLayout?, label: WGPUStringView): Unit {
    webgpu.native.wgpuBindGroupLayoutSetLabel(bindGroupLayout?.handler?.pointer?.takeIf { bindGroupLayout.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuBindGroupLayoutAddRef(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    webgpu.native.wgpuBindGroupLayoutAddRef(bindGroupLayout?.handler?.pointer?.takeIf { bindGroupLayout.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBindGroupLayoutRelease(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    webgpu.native.wgpuBindGroupLayoutRelease(bindGroupLayout?.handler?.pointer?.takeIf { bindGroupLayout.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBufferCreateTexelView(buffer: WGPUBuffer?, descriptor: WGPUTexelBufferViewDescriptor?): WGPUTexelBufferView? {
    return webgpu.native.wgpuBufferCreateTexelView(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexelBufferView(it) }
}

actual fun wgpuBufferDestroy(buffer: WGPUBuffer?): Unit {
    webgpu.native.wgpuBufferDestroy(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBufferGetConstMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return webgpu.native.wgpuBufferGetConstMappedRange(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, size)?.let { NativeAddress.fromPointer(it) }
}

actual fun wgpuBufferGetMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return webgpu.native.wgpuBufferGetMappedRange(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, size)?.let { NativeAddress.fromPointer(it) }
}

actual fun wgpuBufferGetMapState(buffer: WGPUBuffer?): WGPUBufferMapState {
    return webgpu.native.wgpuBufferGetMapState(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuBufferGetSize(buffer: WGPUBuffer?): ULong {
    return webgpu.native.wgpuBufferGetSize(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuBufferGetUsage(buffer: WGPUBuffer?): ULong {
    return webgpu.native.wgpuBufferGetUsage(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuBufferMapAsync(allocator: MemoryAllocator, buffer: WGPUBuffer?, mode: ULong, offset: ULong, size: ULong, callbackInfo: WGPUBufferMapCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuBufferMapAsync(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), mode, offset, size, callbackInfo.toCValue()))
}

actual fun wgpuBufferReadMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return webgpu.native.wgpuBufferReadMappedRange(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
}

actual fun wgpuBufferSetLabel(buffer: WGPUBuffer?, label: WGPUStringView): Unit {
    webgpu.native.wgpuBufferSetLabel(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuBufferUnmap(buffer: WGPUBuffer?): Unit {
    webgpu.native.wgpuBufferUnmap(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBufferWriteMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return webgpu.native.wgpuBufferWriteMappedRange(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
}

actual fun wgpuBufferAddRef(buffer: WGPUBuffer?): Unit {
    webgpu.native.wgpuBufferAddRef(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuBufferRelease(buffer: WGPUBuffer?): Unit {
    webgpu.native.wgpuBufferRelease(buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandBufferSetLabel(commandBuffer: WGPUCommandBuffer?, label: WGPUStringView): Unit {
    webgpu.native.wgpuCommandBufferSetLabel(commandBuffer?.handler?.pointer?.takeIf { commandBuffer.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuCommandBufferAddRef(commandBuffer: WGPUCommandBuffer?): Unit {
    webgpu.native.wgpuCommandBufferAddRef(commandBuffer?.handler?.pointer?.takeIf { commandBuffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandBufferRelease(commandBuffer: WGPUCommandBuffer?): Unit {
    webgpu.native.wgpuCommandBufferRelease(commandBuffer?.handler?.pointer?.takeIf { commandBuffer.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderBeginComputePass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUComputePassDescriptor?): WGPUComputePassEncoder? {
    return webgpu.native.wgpuCommandEncoderBeginComputePass(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUComputePassEncoder(it) }
}

actual fun wgpuCommandEncoderBeginRenderPass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPURenderPassDescriptor?): WGPURenderPassEncoder? {
    return webgpu.native.wgpuCommandEncoderBeginRenderPass(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPassEncoder(it) }
}

actual fun wgpuCommandEncoderClearBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    webgpu.native.wgpuCommandEncoderClearBuffer(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, size)
    return
}

actual fun wgpuCommandEncoderCopyBufferToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUBuffer?, sourceOffset: ULong, destination: WGPUBuffer?, destinationOffset: ULong, size: ULong): Unit {
    webgpu.native.wgpuCommandEncoderCopyBufferToBuffer(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), sourceOffset, destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), destinationOffset, size)
    return
}

actual fun wgpuCommandEncoderCopyBufferToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyBufferInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    webgpu.native.wgpuCommandEncoderCopyBufferToTexture(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), copySize?.handler?.pointer?.takeIf { copySize.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderCopyTextureToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyBufferInfo?, copySize: WGPUExtent3D?): Unit {
    webgpu.native.wgpuCommandEncoderCopyTextureToBuffer(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), copySize?.handler?.pointer?.takeIf { copySize.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderCopyTextureToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    webgpu.native.wgpuCommandEncoderCopyTextureToTexture(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), copySize?.handler?.pointer?.takeIf { copySize.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderFinish(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUCommandBufferDescriptor?): WGPUCommandBuffer? {
    return webgpu.native.wgpuCommandEncoderFinish(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUCommandBuffer(it) }
}

actual fun wgpuCommandEncoderInjectValidationError(commandEncoder: WGPUCommandEncoder?, message: WGPUStringView): Unit {
    webgpu.native.wgpuCommandEncoderInjectValidationError(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), message.toCValue())
    return
}

actual fun wgpuCommandEncoderInsertDebugMarker(commandEncoder: WGPUCommandEncoder?, markerLabel: WGPUStringView): Unit {
    webgpu.native.wgpuCommandEncoderInsertDebugMarker(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), markerLabel.toCValue())
    return
}

actual fun wgpuCommandEncoderPopDebugGroup(commandEncoder: WGPUCommandEncoder?): Unit {
    webgpu.native.wgpuCommandEncoderPopDebugGroup(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderPushDebugGroup(commandEncoder: WGPUCommandEncoder?, groupLabel: WGPUStringView): Unit {
    webgpu.native.wgpuCommandEncoderPushDebugGroup(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), groupLabel.toCValue())
    return
}

actual fun wgpuCommandEncoderResolveQuerySet(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, firstQuery: UInt, queryCount: UInt, destination: WGPUBuffer?, destinationOffset: ULong): Unit {
    webgpu.native.wgpuCommandEncoderResolveQuerySet(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret(), firstQuery, queryCount, destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), destinationOffset)
    return
}

actual fun wgpuCommandEncoderSetLabel(commandEncoder: WGPUCommandEncoder?, label: WGPUStringView): Unit {
    webgpu.native.wgpuCommandEncoderSetLabel(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuCommandEncoderWriteBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    webgpu.native.wgpuCommandEncoderWriteBuffer(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), bufferOffset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
    return
}

actual fun wgpuCommandEncoderWriteTimestamp(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    webgpu.native.wgpuCommandEncoderWriteTimestamp(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret(), querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret(), queryIndex)
    return
}

actual fun wgpuCommandEncoderAddRef(commandEncoder: WGPUCommandEncoder?): Unit {
    webgpu.native.wgpuCommandEncoderAddRef(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuCommandEncoderRelease(commandEncoder: WGPUCommandEncoder?): Unit {
    webgpu.native.wgpuCommandEncoderRelease(commandEncoder?.handler?.pointer?.takeIf { commandEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder: WGPUComputePassEncoder?, workgroupCountX: UInt, workgroupCountY: UInt, workgroupCountZ: UInt): Unit {
    webgpu.native.wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), workgroupCountX, workgroupCountY, workgroupCountZ)
    return
}

actual fun wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder: WGPUComputePassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    webgpu.native.wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset)
    return
}

actual fun wgpuComputePassEncoderEnd(computePassEncoder: WGPUComputePassEncoder?): Unit {
    webgpu.native.wgpuComputePassEncoderEnd(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderInsertDebugMarker(computePassEncoder: WGPUComputePassEncoder?, markerLabel: WGPUStringView): Unit {
    webgpu.native.wgpuComputePassEncoderInsertDebugMarker(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), markerLabel.toCValue())
    return
}

actual fun wgpuComputePassEncoderPopDebugGroup(computePassEncoder: WGPUComputePassEncoder?): Unit {
    webgpu.native.wgpuComputePassEncoderPopDebugGroup(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderPushDebugGroup(computePassEncoder: WGPUComputePassEncoder?, groupLabel: WGPUStringView): Unit {
    webgpu.native.wgpuComputePassEncoderPushDebugGroup(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), groupLabel.toCValue())
    return
}

actual fun wgpuComputePassEncoderSetBindGroup(computePassEncoder: WGPUComputePassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    webgpu.native.wgpuComputePassEncoderSetBindGroup(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), groupIndex, group?.handler?.pointer?.takeIf { group.handler.rawValue != 0L }?.reinterpret(), dynamicOffsetCount, dynamicOffsets?.pointer?.takeIf { dynamicOffsets.rawValue != 0L }?.reinterpret<UIntVar>())
    return
}

actual fun wgpuComputePassEncoderSetImmediates(computePassEncoder: WGPUComputePassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    webgpu.native.wgpuComputePassEncoderSetImmediates(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), offset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
    return
}

actual fun wgpuComputePassEncoderSetLabel(computePassEncoder: WGPUComputePassEncoder?, label: WGPUStringView): Unit {
    webgpu.native.wgpuComputePassEncoderSetLabel(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuComputePassEncoderSetPipeline(computePassEncoder: WGPUComputePassEncoder?, pipeline: WGPUComputePipeline?): Unit {
    webgpu.native.wgpuComputePassEncoderSetPipeline(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), pipeline?.handler?.pointer?.takeIf { pipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderSetResourceTable(computePassEncoder: WGPUComputePassEncoder?, table: WGPUResourceTable?): Unit {
    webgpu.native.wgpuComputePassEncoderSetResourceTable(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), table?.handler?.pointer?.takeIf { table.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderWriteTimestamp(computePassEncoder: WGPUComputePassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    webgpu.native.wgpuComputePassEncoderWriteTimestamp(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret(), querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret(), queryIndex)
    return
}

actual fun wgpuComputePassEncoderAddRef(computePassEncoder: WGPUComputePassEncoder?): Unit {
    webgpu.native.wgpuComputePassEncoderAddRef(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePassEncoderRelease(computePassEncoder: WGPUComputePassEncoder?): Unit {
    webgpu.native.wgpuComputePassEncoderRelease(computePassEncoder?.handler?.pointer?.takeIf { computePassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePipelineGetBindGroupLayout(computePipeline: WGPUComputePipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return webgpu.native.wgpuComputePipelineGetBindGroupLayout(computePipeline?.handler?.pointer?.takeIf { computePipeline.handler.rawValue != 0L }?.reinterpret(), groupIndex)?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayout(it) }
}

actual fun wgpuComputePipelineSetLabel(computePipeline: WGPUComputePipeline?, label: WGPUStringView): Unit {
    webgpu.native.wgpuComputePipelineSetLabel(computePipeline?.handler?.pointer?.takeIf { computePipeline.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuComputePipelineAddRef(computePipeline: WGPUComputePipeline?): Unit {
    webgpu.native.wgpuComputePipelineAddRef(computePipeline?.handler?.pointer?.takeIf { computePipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuComputePipelineRelease(computePipeline: WGPUComputePipeline?): Unit {
    webgpu.native.wgpuComputePipelineRelease(computePipeline?.handler?.pointer?.takeIf { computePipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDawnDrmFormatCapabilitiesFreeMembers(dawnDrmFormatCapabilities: WGPUDawnDrmFormatCapabilities): Unit {
    webgpu.native.wgpuDawnDrmFormatCapabilitiesFreeMembers(dawnDrmFormatCapabilities.toCValue())
    return
}

actual fun wgpuDeviceCreateBindGroup(device: WGPUDevice?, descriptor: WGPUBindGroupDescriptor?): WGPUBindGroup? {
    return webgpu.native.wgpuDeviceCreateBindGroup(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroup(it) }
}

actual fun wgpuDeviceCreateBindGroupLayout(device: WGPUDevice?, descriptor: WGPUBindGroupLayoutDescriptor?): WGPUBindGroupLayout? {
    return webgpu.native.wgpuDeviceCreateBindGroupLayout(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayout(it) }
}

actual fun wgpuDeviceCreateBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return webgpu.native.wgpuDeviceCreateBuffer(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
}

actual fun wgpuDeviceCreateCommandEncoder(device: WGPUDevice?, descriptor: WGPUCommandEncoderDescriptor?): WGPUCommandEncoder? {
    return webgpu.native.wgpuDeviceCreateCommandEncoder(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUCommandEncoder(it) }
}

actual fun wgpuDeviceCreateComputePipeline(device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?): WGPUComputePipeline? {
    return webgpu.native.wgpuDeviceCreateComputePipeline(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUComputePipeline(it) }
}

actual fun wgpuDeviceCreateComputePipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?, callbackInfo: WGPUCreateComputePipelineAsyncCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuDeviceCreateComputePipelineAsync(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuDeviceCreateErrorBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return webgpu.native.wgpuDeviceCreateErrorBuffer(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
}

actual fun wgpuDeviceCreateErrorComputePipeline(device: WGPUDevice?, label: WGPUStringView): WGPUComputePipeline? {
    return webgpu.native.wgpuDeviceCreateErrorComputePipeline(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), label.toCValue())?.let { NativeAddress.fromPointer(it) }?.let { WGPUComputePipeline(it) }
}

actual fun wgpuDeviceCreateErrorExternalTexture(device: WGPUDevice?): WGPUExternalTexture? {
    return webgpu.native.wgpuDeviceCreateErrorExternalTexture(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) }
}

actual fun wgpuDeviceCreateErrorRenderPipeline(device: WGPUDevice?, label: WGPUStringView): WGPURenderPipeline? {
    return webgpu.native.wgpuDeviceCreateErrorRenderPipeline(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), label.toCValue())?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPipeline(it) }
}

actual fun wgpuDeviceCreateErrorShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?, errorMessage: WGPUStringView): WGPUShaderModule? {
    return webgpu.native.wgpuDeviceCreateErrorShaderModule(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret(), errorMessage.toCValue())?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) }
}

actual fun wgpuDeviceCreateErrorTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return webgpu.native.wgpuDeviceCreateErrorTexture(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) }
}

actual fun wgpuDeviceCreateExternalTexture(device: WGPUDevice?, externalTextureDescriptor: WGPUExternalTextureDescriptor?): WGPUExternalTexture? {
    return webgpu.native.wgpuDeviceCreateExternalTexture(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), externalTextureDescriptor?.handler?.pointer?.takeIf { externalTextureDescriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUExternalTexture(it) }
}

actual fun wgpuDeviceCreatePipelineLayout(device: WGPUDevice?, descriptor: WGPUPipelineLayoutDescriptor?): WGPUPipelineLayout? {
    return webgpu.native.wgpuDeviceCreatePipelineLayout(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUPipelineLayout(it) }
}

actual fun wgpuDeviceCreateQuerySet(device: WGPUDevice?, descriptor: WGPUQuerySetDescriptor?): WGPUQuerySet? {
    return webgpu.native.wgpuDeviceCreateQuerySet(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUQuerySet(it) }
}

actual fun wgpuDeviceCreateRenderBundleEncoder(device: WGPUDevice?, descriptor: WGPURenderBundleEncoderDescriptor?): WGPURenderBundleEncoder? {
    return webgpu.native.wgpuDeviceCreateRenderBundleEncoder(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderBundleEncoder(it) }
}

actual fun wgpuDeviceCreateRenderPipeline(device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?): WGPURenderPipeline? {
    return webgpu.native.wgpuDeviceCreateRenderPipeline(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPipeline(it) }
}

actual fun wgpuDeviceCreateRenderPipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?, callbackInfo: WGPUCreateRenderPipelineAsyncCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuDeviceCreateRenderPipelineAsync(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuDeviceCreateResourceTable(device: WGPUDevice?, descriptor: WGPUResourceTableDescriptor?): WGPUResourceTable? {
    return webgpu.native.wgpuDeviceCreateResourceTable(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUResourceTable(it) }
}

actual fun wgpuDeviceCreateSampler(device: WGPUDevice?, descriptor: WGPUSamplerDescriptor?): WGPUSampler? {
    return webgpu.native.wgpuDeviceCreateSampler(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUSampler(it) }
}

actual fun wgpuDeviceCreateShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?): WGPUShaderModule? {
    return webgpu.native.wgpuDeviceCreateShaderModule(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUShaderModule(it) }
}

actual fun wgpuDeviceCreateTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return webgpu.native.wgpuDeviceCreateTexture(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) }
}

actual fun wgpuDeviceDestroy(device: WGPUDevice?): Unit {
    webgpu.native.wgpuDeviceDestroy(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDeviceForceLoss(device: WGPUDevice?, type: WGPUDeviceLostReason, message: WGPUStringView): Unit {
    webgpu.native.wgpuDeviceForceLoss(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), type, message.toCValue())
    return
}

actual fun wgpuDeviceGetAdapter(device: WGPUDevice?): WGPUAdapter? {
    return webgpu.native.wgpuDeviceGetAdapter(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUAdapter(it) }
}

actual fun wgpuDeviceGetAdapterInfo(device: WGPUDevice?, adapterInfo: WGPUAdapterInfo?): WGPUStatus {
    return webgpu.native.wgpuDeviceGetAdapterInfo(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), adapterInfo?.handler?.pointer?.takeIf { adapterInfo.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuDeviceGetAHardwareBufferProperties(device: WGPUDevice?, handle: NativeAddress?, properties: WGPUAHardwareBufferProperties?): WGPUStatus {
    return webgpu.native.wgpuDeviceGetAHardwareBufferProperties(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), handle?.pointer?.takeIf { handle.rawValue != 0L }, properties?.handler?.pointer?.takeIf { properties.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuDeviceGetFeatures(device: WGPUDevice?, features: WGPUSupportedFeatures?): Unit {
    webgpu.native.wgpuDeviceGetFeatures(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), features?.handler?.pointer?.takeIf { features.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDeviceGetLimits(device: WGPUDevice?, limits: WGPULimits?): WGPUStatus {
    return webgpu.native.wgpuDeviceGetLimits(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), limits?.handler?.pointer?.takeIf { limits.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuDeviceGetLostFuture(allocator: MemoryAllocator, device: WGPUDevice?): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuDeviceGetLostFuture(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret()))
}

actual fun wgpuDeviceGetQueue(device: WGPUDevice?): WGPUQueue? {
    return webgpu.native.wgpuDeviceGetQueue(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUQueue(it) }
}

actual fun wgpuDeviceHasFeature(device: WGPUDevice?, feature: WGPUFeatureName): UInt {
    return webgpu.native.wgpuDeviceHasFeature(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), feature)
}

actual fun wgpuDeviceImportSharedBufferMemory(device: WGPUDevice?, descriptor: WGPUSharedBufferMemoryDescriptor?): WGPUSharedBufferMemory? {
    return webgpu.native.wgpuDeviceImportSharedBufferMemory(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUSharedBufferMemory(it) }
}

actual fun wgpuDeviceImportSharedFence(device: WGPUDevice?, descriptor: WGPUSharedFenceDescriptor?): WGPUSharedFence? {
    return webgpu.native.wgpuDeviceImportSharedFence(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUSharedFence(it) }
}

actual fun wgpuDeviceImportSharedTextureMemory(device: WGPUDevice?, descriptor: WGPUSharedTextureMemoryDescriptor?): WGPUSharedTextureMemory? {
    return webgpu.native.wgpuDeviceImportSharedTextureMemory(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUSharedTextureMemory(it) }
}

actual fun wgpuDeviceInjectError(device: WGPUDevice?, type: WGPUErrorType, message: WGPUStringView): Unit {
    webgpu.native.wgpuDeviceInjectError(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), type, message.toCValue())
    return
}

actual fun wgpuDevicePopErrorScope(allocator: MemoryAllocator, device: WGPUDevice?, callbackInfo: WGPUPopErrorScopeCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuDevicePopErrorScope(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuDevicePushErrorScope(device: WGPUDevice?, filter: WGPUErrorFilter): Unit {
    webgpu.native.wgpuDevicePushErrorScope(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), filter)
    return
}

actual fun wgpuDeviceSetLabel(device: WGPUDevice?, label: WGPUStringView): Unit {
    webgpu.native.wgpuDeviceSetLabel(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuDeviceSetLoggingCallback(device: WGPUDevice?, callbackInfo: WGPULoggingCallbackInfo): Unit {
    webgpu.native.wgpuDeviceSetLoggingCallback(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue())
    return
}

actual fun wgpuDeviceTick(device: WGPUDevice?): Unit {
    webgpu.native.wgpuDeviceTick(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDeviceValidateTextureDescriptor(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): Unit {
    webgpu.native.wgpuDeviceValidateTextureDescriptor(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDeviceAddRef(device: WGPUDevice?): Unit {
    webgpu.native.wgpuDeviceAddRef(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuDeviceRelease(device: WGPUDevice?): Unit {
    webgpu.native.wgpuDeviceRelease(device?.handler?.pointer?.takeIf { device.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuExternalTextureDestroy(externalTexture: WGPUExternalTexture?): Unit {
    webgpu.native.wgpuExternalTextureDestroy(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuExternalTextureExpire(externalTexture: WGPUExternalTexture?): Unit {
    webgpu.native.wgpuExternalTextureExpire(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuExternalTextureRefresh(externalTexture: WGPUExternalTexture?): Unit {
    webgpu.native.wgpuExternalTextureRefresh(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuExternalTextureSetLabel(externalTexture: WGPUExternalTexture?, label: WGPUStringView): Unit {
    webgpu.native.wgpuExternalTextureSetLabel(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuExternalTextureAddRef(externalTexture: WGPUExternalTexture?): Unit {
    webgpu.native.wgpuExternalTextureAddRef(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuExternalTextureRelease(externalTexture: WGPUExternalTexture?): Unit {
    webgpu.native.wgpuExternalTextureRelease(externalTexture?.handler?.pointer?.takeIf { externalTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuInstanceCreateSurface(instance: WGPUInstance?, descriptor: WGPUSurfaceDescriptor?): WGPUSurface? {
    return webgpu.native.wgpuInstanceCreateSurface(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUSurface(it) }
}

actual fun wgpuInstanceGetWGSLLanguageFeatures(instance: WGPUInstance?, features: WGPUSupportedWGSLLanguageFeatures?): Unit {
    webgpu.native.wgpuInstanceGetWGSLLanguageFeatures(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret(), features?.handler?.pointer?.takeIf { features.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuInstanceHasWGSLLanguageFeature(instance: WGPUInstance?, feature: WGPUWGSLLanguageFeatureName): UInt {
    return webgpu.native.wgpuInstanceHasWGSLLanguageFeature(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret(), feature)
}

actual fun wgpuInstanceProcessEvents(instance: WGPUInstance?): Unit {
    webgpu.native.wgpuInstanceProcessEvents(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuInstanceRequestAdapter(allocator: MemoryAllocator, instance: WGPUInstance?, options: WGPURequestAdapterOptions?, callbackInfo: WGPURequestAdapterCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuInstanceRequestAdapter(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret(), options?.handler?.pointer?.takeIf { options.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuInstanceWaitAny(instance: WGPUInstance?, futureCount: ULong, futures: WGPUFutureWaitInfo?, timeoutNS: ULong): WGPUWaitStatus {
    return webgpu.native.wgpuInstanceWaitAny(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret(), futureCount, futures?.handler?.pointer?.takeIf { futures.handler.rawValue != 0L }?.reinterpret(), timeoutNS)
}

actual fun wgpuInstanceAddRef(instance: WGPUInstance?): Unit {
    webgpu.native.wgpuInstanceAddRef(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuInstanceRelease(instance: WGPUInstance?): Unit {
    webgpu.native.wgpuInstanceRelease(instance?.handler?.pointer?.takeIf { instance.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuPipelineLayoutSetLabel(pipelineLayout: WGPUPipelineLayout?, label: WGPUStringView): Unit {
    webgpu.native.wgpuPipelineLayoutSetLabel(pipelineLayout?.handler?.pointer?.takeIf { pipelineLayout.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuPipelineLayoutAddRef(pipelineLayout: WGPUPipelineLayout?): Unit {
    webgpu.native.wgpuPipelineLayoutAddRef(pipelineLayout?.handler?.pointer?.takeIf { pipelineLayout.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuPipelineLayoutRelease(pipelineLayout: WGPUPipelineLayout?): Unit {
    webgpu.native.wgpuPipelineLayoutRelease(pipelineLayout?.handler?.pointer?.takeIf { pipelineLayout.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQuerySetDestroy(querySet: WGPUQuerySet?): Unit {
    webgpu.native.wgpuQuerySetDestroy(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQuerySetGetCount(querySet: WGPUQuerySet?): UInt {
    return webgpu.native.wgpuQuerySetGetCount(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuQuerySetGetType(querySet: WGPUQuerySet?): WGPUQueryType {
    return webgpu.native.wgpuQuerySetGetType(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuQuerySetSetLabel(querySet: WGPUQuerySet?, label: WGPUStringView): Unit {
    webgpu.native.wgpuQuerySetSetLabel(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuQuerySetAddRef(querySet: WGPUQuerySet?): Unit {
    webgpu.native.wgpuQuerySetAddRef(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQuerySetRelease(querySet: WGPUQuerySet?): Unit {
    webgpu.native.wgpuQuerySetRelease(querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueCopyExternalTextureForBrowser(queue: WGPUQueue?, source: WGPUImageCopyExternalTexture?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    webgpu.native.wgpuQueueCopyExternalTextureForBrowser(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), copySize?.handler?.pointer?.takeIf { copySize.handler.rawValue != 0L }?.reinterpret(), options?.handler?.pointer?.takeIf { options.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueCopyTextureForBrowser(queue: WGPUQueue?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    webgpu.native.wgpuQueueCopyTextureForBrowser(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), source?.handler?.pointer?.takeIf { source.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), copySize?.handler?.pointer?.takeIf { copySize.handler.rawValue != 0L }?.reinterpret(), options?.handler?.pointer?.takeIf { options.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueOnSubmittedWorkDone(allocator: MemoryAllocator, queue: WGPUQueue?, callbackInfo: WGPUQueueWorkDoneCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuQueueOnSubmittedWorkDone(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuQueueSetLabel(queue: WGPUQueue?, label: WGPUStringView): Unit {
    webgpu.native.wgpuQueueSetLabel(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuQueueSubmit(queue: WGPUQueue?, commandCount: ULong, commands: NativeAddress?): Unit {
    webgpu.native.wgpuQueueSubmit(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), commandCount, commands?.pointer?.takeIf { commands.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueWriteBuffer(queue: WGPUQueue?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    webgpu.native.wgpuQueueWriteBuffer(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), bufferOffset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
    return
}

actual fun wgpuQueueWriteTexture(queue: WGPUQueue?, destination: WGPUTexelCopyTextureInfo?, data: NativeAddress?, dataSize: ULong, dataLayout: WGPUTexelCopyBufferLayout?, writeSize: WGPUExtent3D?): Unit {
    webgpu.native.wgpuQueueWriteTexture(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret(), destination?.handler?.pointer?.takeIf { destination.handler.rawValue != 0L }?.reinterpret(), data?.pointer?.takeIf { data.rawValue != 0L }, dataSize, dataLayout?.handler?.pointer?.takeIf { dataLayout.handler.rawValue != 0L }?.reinterpret(), writeSize?.handler?.pointer?.takeIf { writeSize.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueAddRef(queue: WGPUQueue?): Unit {
    webgpu.native.wgpuQueueAddRef(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuQueueRelease(queue: WGPUQueue?): Unit {
    webgpu.native.wgpuQueueRelease(queue?.handler?.pointer?.takeIf { queue.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleSetLabel(renderBundle: WGPURenderBundle?, label: WGPUStringView): Unit {
    webgpu.native.wgpuRenderBundleSetLabel(renderBundle?.handler?.pointer?.takeIf { renderBundle.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuRenderBundleAddRef(renderBundle: WGPURenderBundle?): Unit {
    webgpu.native.wgpuRenderBundleAddRef(renderBundle?.handler?.pointer?.takeIf { renderBundle.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleRelease(renderBundle: WGPURenderBundle?): Unit {
    webgpu.native.wgpuRenderBundleRelease(renderBundle?.handler?.pointer?.takeIf { renderBundle.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleEncoderDraw(renderBundleEncoder: WGPURenderBundleEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    webgpu.native.wgpuRenderBundleEncoderDraw(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), vertexCount, instanceCount, firstVertex, firstInstance)
    return
}

actual fun wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder: WGPURenderBundleEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    webgpu.native.wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
    return
}

actual fun wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    webgpu.native.wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset)
    return
}

actual fun wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    webgpu.native.wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset)
    return
}

actual fun wgpuRenderBundleEncoderFinish(renderBundleEncoder: WGPURenderBundleEncoder?, descriptor: WGPURenderBundleDescriptor?): WGPURenderBundle? {
    return webgpu.native.wgpuRenderBundleEncoderFinish(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderBundle(it) }
}

actual fun wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder: WGPURenderBundleEncoder?, markerLabel: WGPUStringView): Unit {
    webgpu.native.wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), markerLabel.toCValue())
    return
}

actual fun wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    webgpu.native.wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupLabel: WGPUStringView): Unit {
    webgpu.native.wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), groupLabel.toCValue())
    return
}

actual fun wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), groupIndex, group?.handler?.pointer?.takeIf { group.handler.rawValue != 0L }?.reinterpret(), dynamicOffsetCount, dynamicOffsets?.pointer?.takeIf { dynamicOffsets.rawValue != 0L }?.reinterpret<UIntVar>())
    return
}

actual fun wgpuRenderBundleEncoderSetImmediates(renderBundleEncoder: WGPURenderBundleEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetImmediates(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), offset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
    return
}

actual fun wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), format, offset, size)
    return
}

actual fun wgpuRenderBundleEncoderSetLabel(renderBundleEncoder: WGPURenderBundleEncoder?, label: WGPUStringView): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetLabel(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder: WGPURenderBundleEncoder?, pipeline: WGPURenderPipeline?): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), pipeline?.handler?.pointer?.takeIf { pipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    webgpu.native.wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret(), slot, buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, size)
    return
}

actual fun wgpuRenderBundleEncoderAddRef(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    webgpu.native.wgpuRenderBundleEncoderAddRef(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderBundleEncoderRelease(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    webgpu.native.wgpuRenderBundleEncoderRelease(renderBundleEncoder?.handler?.pointer?.takeIf { renderBundleEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?, queryIndex: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), queryIndex)
    return
}

actual fun wgpuRenderPassEncoderDraw(renderPassEncoder: WGPURenderPassEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderDraw(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), vertexCount, instanceCount, firstVertex, firstInstance)
    return
}

actual fun wgpuRenderPassEncoderDrawIndexed(renderPassEncoder: WGPURenderPassEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderDrawIndexed(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), indexCount, instanceCount, firstIndex, baseVertex, firstInstance)
    return
}

actual fun wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset)
    return
}

actual fun wgpuRenderPassEncoderDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderDrawIndirect(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset)
    return
}

actual fun wgpuRenderPassEncoderEnd(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderEnd(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderExecuteBundles(renderPassEncoder: WGPURenderPassEncoder?, bundleCount: ULong, bundles: NativeAddress?): Unit {
    webgpu.native.wgpuRenderPassEncoderExecuteBundles(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), bundleCount, bundles?.pointer?.takeIf { bundles.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder: WGPURenderPassEncoder?, markerLabel: WGPUStringView): Unit {
    webgpu.native.wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), markerLabel.toCValue())
    return
}

actual fun wgpuRenderPassEncoderMultiDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderMultiDrawIndexedIndirect(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset, maxDrawCount, drawCountBuffer?.handler?.pointer?.takeIf { drawCountBuffer.handler.rawValue != 0L }?.reinterpret(), drawCountBufferOffset)
    return
}

actual fun wgpuRenderPassEncoderMultiDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderMultiDrawIndirect(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), indirectBuffer?.handler?.pointer?.takeIf { indirectBuffer.handler.rawValue != 0L }?.reinterpret(), indirectOffset, maxDrawCount, drawCountBuffer?.handler?.pointer?.takeIf { drawCountBuffer.handler.rawValue != 0L }?.reinterpret(), drawCountBufferOffset)
    return
}

actual fun wgpuRenderPassEncoderPixelLocalStorageBarrier(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderPixelLocalStorageBarrier(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder: WGPURenderPassEncoder?, groupLabel: WGPUStringView): Unit {
    webgpu.native.wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), groupLabel.toCValue())
    return
}

actual fun wgpuRenderPassEncoderSetBindGroup(renderPassEncoder: WGPURenderPassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    webgpu.native.wgpuRenderPassEncoderSetBindGroup(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), groupIndex, group?.handler?.pointer?.takeIf { group.handler.rawValue != 0L }?.reinterpret(), dynamicOffsetCount, dynamicOffsets?.pointer?.takeIf { dynamicOffsets.rawValue != 0L }?.reinterpret<UIntVar>())
    return
}

actual fun wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder: WGPURenderPassEncoder?, color: WGPUColor?): Unit {
    webgpu.native.wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), color?.handler?.pointer?.takeIf { color.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderSetImmediates(renderPassEncoder: WGPURenderPassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderSetImmediates(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), offset, data?.pointer?.takeIf { data.rawValue != 0L }, size)
    return
}

actual fun wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder: WGPURenderPassEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), format, offset, size)
    return
}

actual fun wgpuRenderPassEncoderSetLabel(renderPassEncoder: WGPURenderPassEncoder?, label: WGPUStringView): Unit {
    webgpu.native.wgpuRenderPassEncoderSetLabel(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuRenderPassEncoderSetPipeline(renderPassEncoder: WGPURenderPassEncoder?, pipeline: WGPURenderPipeline?): Unit {
    webgpu.native.wgpuRenderPassEncoderSetPipeline(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), pipeline?.handler?.pointer?.takeIf { pipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderSetResourceTable(renderPassEncoder: WGPURenderPassEncoder?, table: WGPUResourceTable?): Unit {
    webgpu.native.wgpuRenderPassEncoderSetResourceTable(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), table?.handler?.pointer?.takeIf { table.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderSetScissorRect(renderPassEncoder: WGPURenderPassEncoder?, x: UInt, y: UInt, width: UInt, height: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderSetScissorRect(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), x, y, width, height)
    return
}

actual fun wgpuRenderPassEncoderSetStencilReference(renderPassEncoder: WGPURenderPassEncoder?, reference: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderSetStencilReference(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), reference)
    return
}

actual fun wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder: WGPURenderPassEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    webgpu.native.wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), slot, buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), offset, size)
    return
}

actual fun wgpuRenderPassEncoderSetViewport(renderPassEncoder: WGPURenderPassEncoder?, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float): Unit {
    webgpu.native.wgpuRenderPassEncoderSetViewport(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), x, y, width, height, minDepth, maxDepth)
    return
}

actual fun wgpuRenderPassEncoderWriteTimestamp(renderPassEncoder: WGPURenderPassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    webgpu.native.wgpuRenderPassEncoderWriteTimestamp(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret(), querySet?.handler?.pointer?.takeIf { querySet.handler.rawValue != 0L }?.reinterpret(), queryIndex)
    return
}

actual fun wgpuRenderPassEncoderAddRef(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderAddRef(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPassEncoderRelease(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    webgpu.native.wgpuRenderPassEncoderRelease(renderPassEncoder?.handler?.pointer?.takeIf { renderPassEncoder.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPipelineGetBindGroupLayout(renderPipeline: WGPURenderPipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return webgpu.native.wgpuRenderPipelineGetBindGroupLayout(renderPipeline?.handler?.pointer?.takeIf { renderPipeline.handler.rawValue != 0L }?.reinterpret(), groupIndex)?.let { NativeAddress.fromPointer(it) }?.let { WGPUBindGroupLayout(it) }
}

actual fun wgpuRenderPipelineSetLabel(renderPipeline: WGPURenderPipeline?, label: WGPUStringView): Unit {
    webgpu.native.wgpuRenderPipelineSetLabel(renderPipeline?.handler?.pointer?.takeIf { renderPipeline.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuRenderPipelineAddRef(renderPipeline: WGPURenderPipeline?): Unit {
    webgpu.native.wgpuRenderPipelineAddRef(renderPipeline?.handler?.pointer?.takeIf { renderPipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuRenderPipelineRelease(renderPipeline: WGPURenderPipeline?): Unit {
    webgpu.native.wgpuRenderPipelineRelease(renderPipeline?.handler?.pointer?.takeIf { renderPipeline.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuResourceTableDestroy(resourceTable: WGPUResourceTable?): Unit {
    webgpu.native.wgpuResourceTableDestroy(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuResourceTableGetSize(resourceTable: WGPUResourceTable?): UInt {
    return webgpu.native.wgpuResourceTableGetSize(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuResourceTableInsert(resourceTable: WGPUResourceTable?, resource: WGPUBindingResource?): UInt {
    return webgpu.native.wgpuResourceTableInsert(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret(), resource?.handler?.pointer?.takeIf { resource.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuResourceTableRemove(resourceTable: WGPUResourceTable?, slot: UInt): WGPUStatus {
    return webgpu.native.wgpuResourceTableRemove(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret(), slot)
}

actual fun wgpuResourceTableSetLabel(resourceTable: WGPUResourceTable?, label: WGPUStringView): Unit {
    webgpu.native.wgpuResourceTableSetLabel(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuResourceTableUpdate(resourceTable: WGPUResourceTable?, slot: UInt, resource: WGPUBindingResource?): WGPUStatus {
    return webgpu.native.wgpuResourceTableUpdate(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret(), slot, resource?.handler?.pointer?.takeIf { resource.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuResourceTableAddRef(resourceTable: WGPUResourceTable?): Unit {
    webgpu.native.wgpuResourceTableAddRef(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuResourceTableRelease(resourceTable: WGPUResourceTable?): Unit {
    webgpu.native.wgpuResourceTableRelease(resourceTable?.handler?.pointer?.takeIf { resourceTable.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSamplerSetLabel(sampler: WGPUSampler?, label: WGPUStringView): Unit {
    webgpu.native.wgpuSamplerSetLabel(sampler?.handler?.pointer?.takeIf { sampler.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuSamplerAddRef(sampler: WGPUSampler?): Unit {
    webgpu.native.wgpuSamplerAddRef(sampler?.handler?.pointer?.takeIf { sampler.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSamplerRelease(sampler: WGPUSampler?): Unit {
    webgpu.native.wgpuSamplerRelease(sampler?.handler?.pointer?.takeIf { sampler.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuShaderModuleGetCompilationInfo(allocator: MemoryAllocator, shaderModule: WGPUShaderModule?, callbackInfo: WGPUCompilationInfoCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(webgpu.native.wgpuShaderModuleGetCompilationInfo(shaderModule?.handler?.pointer?.takeIf { shaderModule.handler.rawValue != 0L }?.reinterpret(), callbackInfo.toCValue()))
}

actual fun wgpuShaderModuleSetLabel(shaderModule: WGPUShaderModule?, label: WGPUStringView): Unit {
    webgpu.native.wgpuShaderModuleSetLabel(shaderModule?.handler?.pointer?.takeIf { shaderModule.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuShaderModuleAddRef(shaderModule: WGPUShaderModule?): Unit {
    webgpu.native.wgpuShaderModuleAddRef(shaderModule?.handler?.pointer?.takeIf { shaderModule.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuShaderModuleRelease(shaderModule: WGPUShaderModule?): Unit {
    webgpu.native.wgpuShaderModuleRelease(shaderModule?.handler?.pointer?.takeIf { shaderModule.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedBufferMemoryBeginAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryBeginAccessDescriptor?): WGPUStatus {
    return webgpu.native.wgpuSharedBufferMemoryBeginAccess(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedBufferMemoryCreateBuffer(sharedBufferMemory: WGPUSharedBufferMemory?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return webgpu.native.wgpuSharedBufferMemoryCreateBuffer(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUBuffer(it) }
}

actual fun wgpuSharedBufferMemoryEndAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryEndAccessState?): WGPUStatus {
    return webgpu.native.wgpuSharedBufferMemoryEndAccess(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret(), buffer?.handler?.pointer?.takeIf { buffer.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedBufferMemoryGetProperties(sharedBufferMemory: WGPUSharedBufferMemory?, properties: WGPUSharedBufferMemoryProperties?): WGPUStatus {
    return webgpu.native.wgpuSharedBufferMemoryGetProperties(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret(), properties?.handler?.pointer?.takeIf { properties.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedBufferMemoryIsDeviceLost(sharedBufferMemory: WGPUSharedBufferMemory?): UInt {
    return webgpu.native.wgpuSharedBufferMemoryIsDeviceLost(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedBufferMemorySetLabel(sharedBufferMemory: WGPUSharedBufferMemory?, label: WGPUStringView): Unit {
    webgpu.native.wgpuSharedBufferMemorySetLabel(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuSharedBufferMemoryAddRef(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    webgpu.native.wgpuSharedBufferMemoryAddRef(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedBufferMemoryRelease(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    webgpu.native.wgpuSharedBufferMemoryRelease(sharedBufferMemory?.handler?.pointer?.takeIf { sharedBufferMemory.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedBufferMemoryEndAccessStateFreeMembers(sharedBufferMemoryEndAccessState: WGPUSharedBufferMemoryEndAccessState): Unit {
    webgpu.native.wgpuSharedBufferMemoryEndAccessStateFreeMembers(sharedBufferMemoryEndAccessState.toCValue())
    return
}

actual fun wgpuSharedFenceExportInfo(sharedFence: WGPUSharedFence?, info: WGPUSharedFenceExportInfo?): Unit {
    webgpu.native.wgpuSharedFenceExportInfo(sharedFence?.handler?.pointer?.takeIf { sharedFence.handler.rawValue != 0L }?.reinterpret(), info?.handler?.pointer?.takeIf { info.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedFenceSetLabel(sharedFence: WGPUSharedFence?, label: WGPUStringView): Unit {
    webgpu.native.wgpuSharedFenceSetLabel(sharedFence?.handler?.pointer?.takeIf { sharedFence.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuSharedFenceAddRef(sharedFence: WGPUSharedFence?): Unit {
    webgpu.native.wgpuSharedFenceAddRef(sharedFence?.handler?.pointer?.takeIf { sharedFence.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedFenceRelease(sharedFence: WGPUSharedFence?): Unit {
    webgpu.native.wgpuSharedFenceRelease(sharedFence?.handler?.pointer?.takeIf { sharedFence.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedTextureMemoryBeginAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryBeginAccessDescriptor?): WGPUStatus {
    return webgpu.native.wgpuSharedTextureMemoryBeginAccess(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret(), texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedTextureMemoryCreateTexture(sharedTextureMemory: WGPUSharedTextureMemory?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return webgpu.native.wgpuSharedTextureMemoryCreateTexture(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTexture(it) }
}

actual fun wgpuSharedTextureMemoryEndAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryEndAccessState?): WGPUStatus {
    return webgpu.native.wgpuSharedTextureMemoryEndAccess(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret(), texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedTextureMemoryGetProperties(sharedTextureMemory: WGPUSharedTextureMemory?, properties: WGPUSharedTextureMemoryProperties?): WGPUStatus {
    return webgpu.native.wgpuSharedTextureMemoryGetProperties(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret(), properties?.handler?.pointer?.takeIf { properties.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedTextureMemoryIsDeviceLost(sharedTextureMemory: WGPUSharedTextureMemory?): UInt {
    return webgpu.native.wgpuSharedTextureMemoryIsDeviceLost(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSharedTextureMemorySetLabel(sharedTextureMemory: WGPUSharedTextureMemory?, label: WGPUStringView): Unit {
    webgpu.native.wgpuSharedTextureMemorySetLabel(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuSharedTextureMemoryAddRef(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    webgpu.native.wgpuSharedTextureMemoryAddRef(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedTextureMemoryRelease(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    webgpu.native.wgpuSharedTextureMemoryRelease(sharedTextureMemory?.handler?.pointer?.takeIf { sharedTextureMemory.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSharedTextureMemoryEndAccessStateFreeMembers(sharedTextureMemoryEndAccessState: WGPUSharedTextureMemoryEndAccessState): Unit {
    webgpu.native.wgpuSharedTextureMemoryEndAccessStateFreeMembers(sharedTextureMemoryEndAccessState.toCValue())
    return
}

actual fun wgpuSupportedFeaturesFreeMembers(supportedFeatures: WGPUSupportedFeatures): Unit {
    webgpu.native.wgpuSupportedFeaturesFreeMembers(supportedFeatures.toCValue())
    return
}

actual fun wgpuSupportedInstanceFeaturesFreeMembers(supportedInstanceFeatures: WGPUSupportedInstanceFeatures): Unit {
    webgpu.native.wgpuSupportedInstanceFeaturesFreeMembers(supportedInstanceFeatures.toCValue())
    return
}

actual fun wgpuSupportedWGSLLanguageFeaturesFreeMembers(supportedWGSLLanguageFeatures: WGPUSupportedWGSLLanguageFeatures): Unit {
    webgpu.native.wgpuSupportedWGSLLanguageFeaturesFreeMembers(supportedWGSLLanguageFeatures.toCValue())
    return
}

actual fun wgpuSurfaceConfigure(surface: WGPUSurface?, config: WGPUSurfaceConfiguration?): Unit {
    webgpu.native.wgpuSurfaceConfigure(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret(), config?.handler?.pointer?.takeIf { config.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSurfaceGetCapabilities(surface: WGPUSurface?, adapter: WGPUAdapter?, capabilities: WGPUSurfaceCapabilities?): WGPUStatus {
    return webgpu.native.wgpuSurfaceGetCapabilities(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret(), adapter?.handler?.pointer?.takeIf { adapter.handler.rawValue != 0L }?.reinterpret(), capabilities?.handler?.pointer?.takeIf { capabilities.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSurfaceGetCurrentTexture(surface: WGPUSurface?, surfaceTexture: WGPUSurfaceTexture?): Unit {
    webgpu.native.wgpuSurfaceGetCurrentTexture(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret(), surfaceTexture?.handler?.pointer?.takeIf { surfaceTexture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSurfacePresent(surface: WGPUSurface?): WGPUStatus {
    return webgpu.native.wgpuSurfacePresent(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuSurfaceSetLabel(surface: WGPUSurface?, label: WGPUStringView): Unit {
    webgpu.native.wgpuSurfaceSetLabel(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuSurfaceUnconfigure(surface: WGPUSurface?): Unit {
    webgpu.native.wgpuSurfaceUnconfigure(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSurfaceAddRef(surface: WGPUSurface?): Unit {
    webgpu.native.wgpuSurfaceAddRef(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSurfaceRelease(surface: WGPUSurface?): Unit {
    webgpu.native.wgpuSurfaceRelease(surface?.handler?.pointer?.takeIf { surface.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities: WGPUSurfaceCapabilities): Unit {
    webgpu.native.wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities.toCValue())
    return
}

actual fun wgpuTexelBufferViewSetLabel(texelBufferView: WGPUTexelBufferView?, label: WGPUStringView): Unit {
    webgpu.native.wgpuTexelBufferViewSetLabel(texelBufferView?.handler?.pointer?.takeIf { texelBufferView.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuTexelBufferViewAddRef(texelBufferView: WGPUTexelBufferView?): Unit {
    webgpu.native.wgpuTexelBufferViewAddRef(texelBufferView?.handler?.pointer?.takeIf { texelBufferView.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTexelBufferViewRelease(texelBufferView: WGPUTexelBufferView?): Unit {
    webgpu.native.wgpuTexelBufferViewRelease(texelBufferView?.handler?.pointer?.takeIf { texelBufferView.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTextureCreateErrorView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return webgpu.native.wgpuTextureCreateErrorView(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
}

actual fun wgpuTextureCreateView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return webgpu.native.wgpuTextureCreateView(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), descriptor?.handler?.pointer?.takeIf { descriptor.handler.rawValue != 0L }?.reinterpret())?.let { NativeAddress.fromPointer(it) }?.let { WGPUTextureView(it) }
}

actual fun wgpuTextureDestroy(texture: WGPUTexture?): Unit {
    webgpu.native.wgpuTextureDestroy(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTextureGetDepthOrArrayLayers(texture: WGPUTexture?): UInt {
    return webgpu.native.wgpuTextureGetDepthOrArrayLayers(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetDimension(texture: WGPUTexture?): WGPUTextureDimension {
    return webgpu.native.wgpuTextureGetDimension(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetFormat(texture: WGPUTexture?): WGPUTextureFormat {
    return webgpu.native.wgpuTextureGetFormat(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetHeight(texture: WGPUTexture?): UInt {
    return webgpu.native.wgpuTextureGetHeight(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetMipLevelCount(texture: WGPUTexture?): UInt {
    return webgpu.native.wgpuTextureGetMipLevelCount(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetSampleCount(texture: WGPUTexture?): UInt {
    return webgpu.native.wgpuTextureGetSampleCount(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetTextureBindingViewDimension(texture: WGPUTexture?): WGPUTextureViewDimension {
    return webgpu.native.wgpuTextureGetTextureBindingViewDimension(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetUsage(texture: WGPUTexture?): ULong {
    return webgpu.native.wgpuTextureGetUsage(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureGetWidth(texture: WGPUTexture?): UInt {
    return webgpu.native.wgpuTextureGetWidth(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
}

actual fun wgpuTextureSetLabel(texture: WGPUTexture?, label: WGPUStringView): Unit {
    webgpu.native.wgpuTextureSetLabel(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuTextureSetOwnershipForMemoryDump(texture: WGPUTexture?, ownerGuid: ULong): Unit {
    webgpu.native.wgpuTextureSetOwnershipForMemoryDump(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret(), ownerGuid)
    return
}

actual fun wgpuTextureAddRef(texture: WGPUTexture?): Unit {
    webgpu.native.wgpuTextureAddRef(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTextureRelease(texture: WGPUTexture?): Unit {
    webgpu.native.wgpuTextureRelease(texture?.handler?.pointer?.takeIf { texture.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTextureViewSetLabel(textureView: WGPUTextureView?, label: WGPUStringView): Unit {
    webgpu.native.wgpuTextureViewSetLabel(textureView?.handler?.pointer?.takeIf { textureView.handler.rawValue != 0L }?.reinterpret(), label.toCValue())
    return
}

actual fun wgpuTextureViewAddRef(textureView: WGPUTextureView?): Unit {
    webgpu.native.wgpuTextureViewAddRef(textureView?.handler?.pointer?.takeIf { textureView.handler.rawValue != 0L }?.reinterpret())
    return
}

actual fun wgpuTextureViewRelease(textureView: WGPUTextureView?): Unit {
    webgpu.native.wgpuTextureViewRelease(textureView?.handler?.pointer?.takeIf { textureView.handler.rawValue != 0L }?.reinterpret())
    return
}

@OptIn(CallbackRuntimeApi::class)
private val WGPUCallbackTrampoline = staticCFunction<COpaquePointer?, Unit> { userdata ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUCallbackType,
            userdata = userdata?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke()
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCallback,
): CallbackRegistration<WGPUCallback> = CallbackRuntime.register(
    type = WGPUCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUDawnStoreCacheDataFunctionTrampoline = staticCFunction<COpaquePointer?, ULong, COpaquePointer?, ULong, COpaquePointer?, Unit> { key, keySize, value, valueSize, userdata ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUDawnStoreCacheDataFunctionType,
            userdata = userdata?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                key?.let { NativeAddress.fromPointer(it) },
                keySize,
                value?.let { NativeAddress.fromPointer(it) },
                valueSize,
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDawnStoreCacheDataFunction.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataFunction,
): CallbackRegistration<WGPUDawnStoreCacheDataFunction> = CallbackRuntime.register(
    type = WGPUDawnStoreCacheDataFunctionType,
    trampoline = NativeAddress.fromPointer(WGPUDawnStoreCacheDataFunctionTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUDawnStoreCacheDataFunctionTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUProcTrampoline = staticCFunction<Unit> {
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

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUProc.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUProc,
): CallbackRegistration<WGPUProc> = CallbackRuntime.register(
    type = WGPUProcType,
    trampoline = NativeAddress.fromPointer(WGPUProcTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUProcTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUProcTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUBufferMapCallbackTrampoline = staticCFunction<UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUBufferMapCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUMapAsyncStatus,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUBufferMapCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUBufferMapCallback,
): CallbackRegistration<WGPUBufferMapCallback> = CallbackRuntime.register(
    type = WGPUBufferMapCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUBufferMapCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUBufferMapCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUCompilationInfoCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, COpaquePointer?, COpaquePointer?, Unit> { status, compilationInfo, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUCompilationInfoCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUCompilationInfoRequestStatus,
                compilationInfo?.let { NativeAddress.fromPointer(it) },
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCompilationInfoCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCompilationInfoCallback,
): CallbackRegistration<WGPUCompilationInfoCallback> = CallbackRuntime.register(
    type = WGPUCompilationInfoCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUCompilationInfoCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUCompilationInfoCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUCreateComputePipelineAsyncCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, pipeline, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUCreateComputePipelineAsyncCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUCreatePipelineAsyncStatus,
                pipeline?.let { NativeAddress.fromPointer(it) }?.let { WGPUComputePipeline(it) },
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCreateComputePipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateComputePipelineAsyncCallback,
): CallbackRegistration<WGPUCreateComputePipelineAsyncCallback> = CallbackRuntime.register(
    type = WGPUCreateComputePipelineAsyncCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUCreateComputePipelineAsyncCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUCreateComputePipelineAsyncCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUCreateRenderPipelineAsyncCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, pipeline, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUCreateRenderPipelineAsyncCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUCreatePipelineAsyncStatus,
                pipeline?.let { NativeAddress.fromPointer(it) }?.let { WGPURenderPipeline(it) },
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUCreateRenderPipelineAsyncCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUCreateRenderPipelineAsyncCallback,
): CallbackRegistration<WGPUCreateRenderPipelineAsyncCallback> = CallbackRuntime.register(
    type = WGPUCreateRenderPipelineAsyncCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUCreateRenderPipelineAsyncCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUCreateRenderPipelineAsyncCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUDawnStoreCacheDataCallbackTrampoline = staticCFunction<ULong, COpaquePointer?, ULong, COpaquePointer?, COpaquePointer?, COpaquePointer?, Unit> { keySize, key, valueSize, value, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUDawnStoreCacheDataCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                keySize,
                key?.let { NativeAddress.fromPointer(it) },
                valueSize,
                value?.let { NativeAddress.fromPointer(it) },
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDawnStoreCacheDataCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDawnStoreCacheDataCallback,
): CallbackRegistration<WGPUDawnStoreCacheDataCallback> = CallbackRuntime.register(
    type = WGPUDawnStoreCacheDataCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUDawnStoreCacheDataCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUDawnStoreCacheDataCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUDeviceLostCallbackTrampoline = staticCFunction<COpaquePointer?, UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { device, reason, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUDeviceLostCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                device?.let { NativeAddress.fromPointer(it) },
                reason.toUInt() as WGPUDeviceLostReason,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDeviceLostCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDeviceLostCallback,
): CallbackRegistration<WGPUDeviceLostCallback> = CallbackRuntime.register(
    type = WGPUDeviceLostCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUDeviceLostCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUDeviceLostCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUDisposeCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, COpaquePointer?, Unit> { status, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUDisposeCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUCallbackStatus,
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUDisposeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUDisposeCallback,
): CallbackRegistration<WGPUDisposeCallback> = CallbackRuntime.register(
    type = WGPUDisposeCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUDisposeCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUDisposeCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPULoggingCallbackTrampoline = staticCFunction<UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { type, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPULoggingCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                type.toUInt() as WGPULoggingType,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPULoggingCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPULoggingCallback,
): CallbackRegistration<WGPULoggingCallback> = CallbackRuntime.register(
    type = WGPULoggingCallbackType,
    trampoline = NativeAddress.fromPointer(WGPULoggingCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPULoggingCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUPopErrorScopeCallbackTrampoline = staticCFunction<UInt, UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, type, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUPopErrorScopeCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUPopErrorScopeStatus,
                type.toUInt() as WGPUErrorType,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUPopErrorScopeCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUPopErrorScopeCallback,
): CallbackRegistration<WGPUPopErrorScopeCallback> = CallbackRuntime.register(
    type = WGPUPopErrorScopeCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUPopErrorScopeCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUPopErrorScopeCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUQueueWorkDoneCallbackTrampoline = staticCFunction<UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUQueueWorkDoneCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPUQueueWorkDoneStatus,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUQueueWorkDoneCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUQueueWorkDoneCallback,
): CallbackRegistration<WGPUQueueWorkDoneCallback> = CallbackRuntime.register(
    type = WGPUQueueWorkDoneCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUQueueWorkDoneCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUQueueWorkDoneCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPURequestAdapterCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, adapter, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPURequestAdapterCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPURequestAdapterStatus,
                adapter?.let { NativeAddress.fromPointer(it) }?.let { WGPUAdapter(it) },
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPURequestAdapterCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestAdapterCallback,
): CallbackRegistration<WGPURequestAdapterCallback> = CallbackRuntime.register(
    type = WGPURequestAdapterCallbackType,
    trampoline = NativeAddress.fromPointer(WGPURequestAdapterCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPURequestAdapterCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPURequestDeviceCallbackTrampoline = staticCFunction<UInt, COpaquePointer?, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { status, device, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPURequestDeviceCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                status.toUInt() as WGPURequestDeviceStatus,
                device?.let { NativeAddress.fromPointer(it) }?.let { WGPUDevice(it) },
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPURequestDeviceCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPURequestDeviceCallback,
): CallbackRegistration<WGPURequestDeviceCallback> = CallbackRuntime.register(
    type = WGPURequestDeviceCallbackType,
    trampoline = NativeAddress.fromPointer(WGPURequestDeviceCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPURequestDeviceCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

@OptIn(CallbackRuntimeApi::class)
private val WGPUUncapturedErrorCallbackTrampoline = staticCFunction<COpaquePointer?, UInt, CValue<webgpu.native.WGPUStringView>, COpaquePointer?, COpaquePointer?, Unit> { device, type, message, userdata1, userdata2 ->
    try {
        CallbackRuntime.dispatchSafely(
            type = WGPUUncapturedErrorCallbackType,
            userdata = userdata2?.let { NativeAddress.fromPointer(it) },
        ) { callback ->
            callback.invoke(
                device?.let { NativeAddress.fromPointer(it) },
                type.toUInt() as WGPUErrorType,
                WGPUStringView.ByValue(message),
                userdata1?.let { NativeAddress.fromPointer(it) },
            )
        }
    } catch (failure: Throwable) {
        CallbackRuntime.reportUnroutedFailure(failure)
    }
}

@OptIn(CallbackRuntimeApi::class)
actual fun WGPUUncapturedErrorCallback.Companion.register(
    policy: CallbackPolicy,
    onError: CallbackExceptionHandler,
    callback: WGPUUncapturedErrorCallback,
): CallbackRegistration<WGPUUncapturedErrorCallback> = CallbackRuntime.register(
    type = WGPUUncapturedErrorCallbackType,
    trampoline = NativeAddress.fromPointer(WGPUUncapturedErrorCallbackTrampoline),
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
    trampoline = NativeAddress.fromPointer(WGPUUncapturedErrorCallbackTrampoline),
    policy = policy,
    onError = onError,
    callback = callback,
)

