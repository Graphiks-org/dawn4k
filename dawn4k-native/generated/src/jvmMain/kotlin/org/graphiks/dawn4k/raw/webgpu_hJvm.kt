package org.graphiks.dawn4k.raw

import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.engine.JvmDowncallEngine
import org.graphiks.kffi.engine.JvmUpcallEngine
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
import org.graphiks.kffi.findOrThrow
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandle
import java.lang.invoke.MethodHandles
import kotlin.OptIn
import kotlin.Suppress
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmStatic

private object KextractNativeBootstrap {
    @kotlin.jvm.Volatile private var loaded: kotlin.Boolean = false

    fun resolve(name: kotlin.String): kotlin.Long {
        load()
        return findOrThrow(name)
    }

    private fun load() {
        if (loaded) return
        kotlin.synchronized(this) {
            if (loaded) return
            val platform = currentPlatform()
            val bundle = bundles[platform]
            val bundleDirectory = bundle?.let { extractBundle(platform, it) }
            val libraryPath0 = bundle?.libraryPaths?.get("webgpu_dawn")
            if (bundleDirectory != null && libraryPath0 != null) {
                java.lang.System.load(bundleDirectory.resolve(libraryPath0).toAbsolutePath().normalize().toString())
            } else {
                java.lang.System.loadLibrary("webgpu_dawn")
            }
            loaded = true
        }
    }

    private data class Resource(val path: kotlin.String, val sha256: kotlin.String)
    private data class Bundle(val key: kotlin.String, val resources: kotlin.collections.List<Resource>, val libraryPaths: kotlin.collections.Map<kotlin.String, kotlin.String>)

    private val bundles: kotlin.collections.Map<kotlin.String, Bundle> = kotlin.collections.mapOf(
        "darwin-aarch64" to Bundle(
            key = "9ea52c86e533caaf35bd5237ce0009d691049485d7d2024b5eebf50833427055",
            resources = kotlin.collections.listOf(
                Resource("libwebgpu_dawn.dylib", "1894cc28c24596682af2e1b9b7ba37e0cb5c93640f7b46bdf27b409acbd18d70"),
            ),
            libraryPaths = kotlin.collections.mapOf(
                "webgpu_dawn" to "libwebgpu_dawn.dylib",
            ),
        ),
        "linux-x86-64" to Bundle(
            key = "345a4e331de72b199af738c8627315c22ff57fdb5bfa2d4240d536a12191145c",
            resources = kotlin.collections.listOf(
                Resource("libwebgpu_dawn.so", "b98dcf9180fcae291f16f64e8eb7df07823b51393f1664b62b412c19c5a782f2"),
            ),
            libraryPaths = kotlin.collections.mapOf(
                "webgpu_dawn" to "libwebgpu_dawn.so",
            ),
        ),
    )

    private fun currentPlatform(): kotlin.String {
        val os = java.lang.System.getProperty("os.name").lowercase(java.util.Locale.ROOT)
        val architecture = java.lang.System.getProperty("os.arch").lowercase(java.util.Locale.ROOT)
        val osId = when {
            os.contains("mac") || os.contains("darwin") -> "darwin"
            os.contains("linux") -> "linux"
            os.contains("windows") -> "win32"
            else -> os.replace(kotlin.text.Regex("[^a-z0-9]+"), "-").trim('-')
        }
        val architectureId = when (architecture) {
            "aarch64", "arm64" -> "aarch64"
            "amd64", "x86_64", "x64" -> "x86-64"
            else -> architecture.replace(kotlin.text.Regex("[^a-z0-9]+"), "-").trim('-')
        }
        return "$osId-$architectureId"
    }

    private fun extractBundle(platform: kotlin.String, bundle: Bundle): java.nio.file.Path {
        val configuredCache = java.lang.System.getProperty("kextract.native.cache.dir")
        val cacheRoot = if (configuredCache.isNullOrBlank()) {
            java.nio.file.Path.of(java.lang.System.getProperty("java.io.tmpdir"), "kextract-native")
        } else {
            java.nio.file.Path.of(configuredCache)
        }
        java.nio.file.Files.createDirectories(cacheRoot)
        val bundleDirectory = cacheRoot.resolve(bundle.key).toAbsolutePath().normalize()
        val lockPath = cacheRoot.resolve("${bundle.key}.lock")
        val processLock = lockPath.toAbsolutePath().normalize().toString().intern()
        kotlin.synchronized(processLock) {
            java.nio.channels.FileChannel.open(lockPath, java.nio.file.StandardOpenOption.CREATE, java.nio.file.StandardOpenOption.WRITE).use { channel ->
                channel.lock().use {
                    java.nio.file.Files.createDirectories(bundleDirectory)
                    bundle.resources.forEach { resource ->
                        val destination = bundleDirectory.resolve(resource.path).normalize()
                        if (!destination.startsWith(bundleDirectory)) {
                            throw java.io.IOException("Native resource escapes cache directory: ${resource.path}")
                        }
                        if (!java.nio.file.Files.isRegularFile(destination) || sha256(destination) != resource.sha256) {
                            copyResource(platform, resource, destination)
                        }
                    }
                }
            }
        }
        return bundleDirectory
    }

    private fun copyResource(platform: kotlin.String, resource: Resource, destination: java.nio.file.Path) {
        java.nio.file.Files.createDirectories(destination.parent)
        val temporary = java.nio.file.Files.createTempFile(destination.parent, ".${destination.fileName}.", ".tmp")
        try {
            val resourceName = "$platform/${resource.path}"
            val classLoader = KextractNativeBootstrap::class.java.classLoader
            val candidates = if (classLoader == null) {
                java.lang.ClassLoader.getSystemResources(resourceName)
            } else {
                classLoader.getResources(resourceName)
            }
            var candidateCount = 0
            var matched = false
            while (candidates.hasMoreElements()) {
                candidateCount += 1
                candidates.nextElement().openStream().use { input ->
                    java.nio.file.Files.copy(input, temporary, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
                }
                if (sha256(temporary) == resource.sha256) {
                    matched = true
                    break
                }
            }
            if (!matched) {
                if (candidateCount == 0) {
                    throw java.io.FileNotFoundException("Native resource not found: /$resourceName")
                }
                throw java.io.IOException("No native resource candidate matched SHA-256 ${resource.sha256}: /$resourceName")
            }
            try {
                java.nio.file.Files.move(temporary, destination, java.nio.file.StandardCopyOption.ATOMIC_MOVE, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            } catch (_: java.nio.file.AtomicMoveNotSupportedException) {
                java.nio.file.Files.move(temporary, destination, java.nio.file.StandardCopyOption.REPLACE_EXISTING)
            }
        } finally {
            java.nio.file.Files.deleteIfExists(temporary)
        }
    }

    private fun sha256(path: java.nio.file.Path): kotlin.String {
        val digest = java.security.MessageDigest.getInstance("SHA-256")
        java.nio.file.Files.newInputStream(path).use { input ->
            val buffer = kotlin.ByteArray(8192)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return java.util.HexFormat.of().formatHex(digest.digest())
    }
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

private val wgpuCreateInstance_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCreateInstance") }
actual fun wgpuCreateInstance(descriptor: WGPUInstanceDescriptor?): WGPUInstance? {
    return JvmDowncallEngine.callP1P(wgpuCreateInstance_ADDR, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUInstance)
}

private val wgpuGetInstanceFeatures_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuGetInstanceFeatures") }
actual fun wgpuGetInstanceFeatures(features: WGPUSupportedInstanceFeatures?): Unit {
    JvmDowncallEngine.callV1P(wgpuGetInstanceFeatures_ADDR, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuGetInstanceLimits_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuGetInstanceLimits") }
actual fun wgpuGetInstanceLimits(limits: WGPUInstanceLimits?): WGPUStatus {
    return (JvmDowncallEngine.callI1P(wgpuGetInstanceLimits_ADDR, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuHasInstanceFeature_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuHasInstanceFeature") }
actual fun wgpuHasInstanceFeature(feature: WGPUInstanceFeatureName): UInt {
    return JvmDowncallEngine.callI1I(wgpuHasInstanceFeature_ADDR, feature.toInt()).toInt().toUInt()
}

private val wgpuGetProcAddress_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuGetProcAddress") }
actual fun wgpuGetProcAddress(procName: WGPUStringView): NativeAddress? {
    return JvmDowncallEngine.callStructArgWGPUStringViewRetP(wgpuGetProcAddress_ADDR, procName.handler.rawValue).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuAdapterCreateDevice_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterCreateDevice") }
actual fun wgpuAdapterCreateDevice(adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?): WGPUDevice? {
    return JvmDowncallEngine.callP2PP(wgpuAdapterCreateDevice_ADDR, adapter?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUDevice)
}

private val wgpuAdapterGetFeatures_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterGetFeatures") }
actual fun wgpuAdapterGetFeatures(adapter: WGPUAdapter?, features: WGPUSupportedFeatures?): Unit {
    JvmDowncallEngine.callV2PP(wgpuAdapterGetFeatures_ADDR, adapter?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterGetFormatCapabilities_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterGetFormatCapabilities") }
actual fun wgpuAdapterGetFormatCapabilities(adapter: WGPUAdapter?, format: WGPUTextureFormat, capabilities: WGPUDawnFormatCapabilities?): WGPUStatus {
    return (JvmDowncallEngine.callI3PIP(wgpuAdapterGetFormatCapabilities_ADDR, adapter?.handler?.rawValue ?: 0L, format.toInt(), capabilities?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterGetInfo_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterGetInfo") }
actual fun wgpuAdapterGetInfo(adapter: WGPUAdapter?, info: WGPUAdapterInfo?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuAdapterGetInfo_ADDR, adapter?.handler?.rawValue ?: 0L, info?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterGetInstance_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterGetInstance") }
actual fun wgpuAdapterGetInstance(adapter: WGPUAdapter?): WGPUInstance? {
    return JvmDowncallEngine.callP1P(wgpuAdapterGetInstance_ADDR, adapter?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUInstance)
}

private val wgpuAdapterGetLimits_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterGetLimits") }
actual fun wgpuAdapterGetLimits(adapter: WGPUAdapter?, limits: WGPULimits?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuAdapterGetLimits_ADDR, adapter?.handler?.rawValue ?: 0L, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuAdapterHasFeature_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterHasFeature") }
actual fun wgpuAdapterHasFeature(adapter: WGPUAdapter?, feature: WGPUFeatureName): UInt {
    return JvmDowncallEngine.callI2PI(wgpuAdapterHasFeature_ADDR, adapter?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuAdapterRequestDevice_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterRequestDevice") }
actual fun wgpuAdapterRequestDevice(allocator: MemoryAllocator, adapter: WGPUAdapter?, descriptor: WGPUDeviceDescriptor?, callbackInfo: WGPURequestDeviceCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPURequestDeviceCallbackInfo(wgpuAdapterRequestDevice_ADDR, allocator, adapter?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuAdapterAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterAddRef") }
actual fun wgpuAdapterAddRef(adapter: WGPUAdapter?): Unit {
    JvmDowncallEngine.callV1P(wgpuAdapterAddRef_ADDR, adapter?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterRelease") }
actual fun wgpuAdapterRelease(adapter: WGPUAdapter?): Unit {
    JvmDowncallEngine.callV1P(wgpuAdapterRelease_ADDR, adapter?.handler?.rawValue ?: 0L)
    return
}

private val wgpuAdapterInfoFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterInfoFreeMembers") }
actual fun wgpuAdapterInfoFreeMembers(adapterInfo: WGPUAdapterInfo): Unit {
    JvmDowncallEngine.callStructArgWGPUAdapterInfo(wgpuAdapterInfoFreeMembers_ADDR, adapterInfo.handler.rawValue)
    return
}

private val wgpuAdapterPropertiesMemoryHeapsFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterPropertiesMemoryHeapsFreeMembers") }
actual fun wgpuAdapterPropertiesMemoryHeapsFreeMembers(adapterPropertiesMemoryHeaps: WGPUAdapterPropertiesMemoryHeaps): Unit {
    JvmDowncallEngine.callGeneric(wgpuAdapterPropertiesMemoryHeapsFreeMembers_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Struct("WGPUAdapterPropertiesMemoryHeaps"))), adapterPropertiesMemoryHeaps.handler.rawValue)
    return
}

private val wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers") }
actual fun wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers(adapterPropertiesSubgroupMatrixConfigs: WGPUAdapterPropertiesSubgroupMatrixConfigs): Unit {
    JvmDowncallEngine.callGeneric(wgpuAdapterPropertiesSubgroupMatrixConfigsFreeMembers_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Struct("WGPUAdapterPropertiesSubgroupMatrixConfigs"))), adapterPropertiesSubgroupMatrixConfigs.handler.rawValue)
    return
}

private val wgpuBindGroupSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupSetLabel") }
actual fun wgpuBindGroupSetLabel(bindGroup: WGPUBindGroup?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuBindGroupSetLabel_ADDR, bindGroup?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuBindGroupAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupAddRef") }
actual fun wgpuBindGroupAddRef(bindGroup: WGPUBindGroup?): Unit {
    JvmDowncallEngine.callV1P(wgpuBindGroupAddRef_ADDR, bindGroup?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupRelease") }
actual fun wgpuBindGroupRelease(bindGroup: WGPUBindGroup?): Unit {
    JvmDowncallEngine.callV1P(wgpuBindGroupRelease_ADDR, bindGroup?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupLayoutSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupLayoutSetLabel") }
actual fun wgpuBindGroupLayoutSetLabel(bindGroupLayout: WGPUBindGroupLayout?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuBindGroupLayoutSetLabel_ADDR, bindGroupLayout?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuBindGroupLayoutAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupLayoutAddRef") }
actual fun wgpuBindGroupLayoutAddRef(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    JvmDowncallEngine.callV1P(wgpuBindGroupLayoutAddRef_ADDR, bindGroupLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBindGroupLayoutRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBindGroupLayoutRelease") }
actual fun wgpuBindGroupLayoutRelease(bindGroupLayout: WGPUBindGroupLayout?): Unit {
    JvmDowncallEngine.callV1P(wgpuBindGroupLayoutRelease_ADDR, bindGroupLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferCreateTexelView_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferCreateTexelView") }
actual fun wgpuBufferCreateTexelView(buffer: WGPUBuffer?, descriptor: WGPUTexelBufferViewDescriptor?): WGPUTexelBufferView? {
    return JvmDowncallEngine.callP2PP(wgpuBufferCreateTexelView_ADDR, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexelBufferView)
}

private val wgpuBufferDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferDestroy") }
actual fun wgpuBufferDestroy(buffer: WGPUBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuBufferDestroy_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferGetConstMappedRange_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferGetConstMappedRange") }
actual fun wgpuBufferGetConstMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return JvmDowncallEngine.callP3PLL(wgpuBufferGetConstMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong()).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuBufferGetMappedRange_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferGetMappedRange") }
actual fun wgpuBufferGetMappedRange(buffer: WGPUBuffer?, offset: ULong, size: ULong): NativeAddress? {
    return JvmDowncallEngine.callP3PLL(wgpuBufferGetMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong()).takeIf { it != 0L }?.let(::NativeAddress)
}

private val wgpuBufferGetMapState_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferGetMapState") }
actual fun wgpuBufferGetMapState(buffer: WGPUBuffer?): WGPUBufferMapState {
    return (JvmDowncallEngine.callI1P(wgpuBufferGetMapState_ADDR, buffer?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuBufferGetSize_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferGetSize") }
actual fun wgpuBufferGetSize(buffer: WGPUBuffer?): ULong {
    return JvmDowncallEngine.callL1P(wgpuBufferGetSize_ADDR, buffer?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuBufferGetUsage_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferGetUsage") }
actual fun wgpuBufferGetUsage(buffer: WGPUBuffer?): ULong {
    return JvmDowncallEngine.callL1P(wgpuBufferGetUsage_ADDR, buffer?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuBufferMapAsync_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferMapAsync") }
actual fun wgpuBufferMapAsync(allocator: MemoryAllocator, buffer: WGPUBuffer?, mode: ULong, offset: ULong, size: ULong, callbackInfo: WGPUBufferMapCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUBufferMapCallbackInfo(wgpuBufferMapAsync_ADDR, allocator, buffer?.handler?.rawValue ?: 0L, mode.toLong(), offset.toLong(), size.toLong(), callbackInfo.handler.rawValue))
}

private val wgpuBufferReadMappedRange_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferReadMappedRange") }
actual fun wgpuBufferReadMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return (JvmDowncallEngine.callI4PLPL(wgpuBufferReadMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), data?.rawValue ?: 0L, size.toLong()).toInt()).toUInt()
}

private val wgpuBufferSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferSetLabel") }
actual fun wgpuBufferSetLabel(buffer: WGPUBuffer?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuBufferSetLabel_ADDR, buffer?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuBufferUnmap_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferUnmap") }
actual fun wgpuBufferUnmap(buffer: WGPUBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuBufferUnmap_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferWriteMappedRange_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferWriteMappedRange") }
actual fun wgpuBufferWriteMappedRange(buffer: WGPUBuffer?, offset: ULong, data: NativeAddress?, size: ULong): WGPUStatus {
    return (JvmDowncallEngine.callI4PLPL(wgpuBufferWriteMappedRange_ADDR, buffer?.handler?.rawValue ?: 0L, offset.toLong(), data?.rawValue ?: 0L, size.toLong()).toInt()).toUInt()
}

private val wgpuBufferAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferAddRef") }
actual fun wgpuBufferAddRef(buffer: WGPUBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuBufferAddRef_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuBufferRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuBufferRelease") }
actual fun wgpuBufferRelease(buffer: WGPUBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuBufferRelease_ADDR, buffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandBufferSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandBufferSetLabel") }
actual fun wgpuCommandBufferSetLabel(commandBuffer: WGPUCommandBuffer?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuCommandBufferSetLabel_ADDR, commandBuffer?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuCommandBufferAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandBufferAddRef") }
actual fun wgpuCommandBufferAddRef(commandBuffer: WGPUCommandBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuCommandBufferAddRef_ADDR, commandBuffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandBufferRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandBufferRelease") }
actual fun wgpuCommandBufferRelease(commandBuffer: WGPUCommandBuffer?): Unit {
    JvmDowncallEngine.callV1P(wgpuCommandBufferRelease_ADDR, commandBuffer?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderBeginComputePass_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderBeginComputePass") }
actual fun wgpuCommandEncoderBeginComputePass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUComputePassDescriptor?): WGPUComputePassEncoder? {
    return JvmDowncallEngine.callP2PP(wgpuCommandEncoderBeginComputePass_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePassEncoder)
}

private val wgpuCommandEncoderBeginRenderPass_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderBeginRenderPass") }
actual fun wgpuCommandEncoderBeginRenderPass(commandEncoder: WGPUCommandEncoder?, descriptor: WGPURenderPassDescriptor?): WGPURenderPassEncoder? {
    return JvmDowncallEngine.callP2PP(wgpuCommandEncoderBeginRenderPass_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPassEncoder)
}

private val wgpuCommandEncoderClearBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderClearBuffer") }
actual fun wgpuCommandEncoderClearBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV4PPLL(wgpuCommandEncoderClearBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuCommandEncoderCopyBufferToBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderCopyBufferToBuffer") }
actual fun wgpuCommandEncoderCopyBufferToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUBuffer?, sourceOffset: ULong, destination: WGPUBuffer?, destinationOffset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV6PPLPLL(wgpuCommandEncoderCopyBufferToBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, sourceOffset.toLong(), destination?.handler?.rawValue ?: 0L, destinationOffset.toLong(), size.toLong())
    return
}

private val wgpuCommandEncoderCopyBufferToTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderCopyBufferToTexture") }
actual fun wgpuCommandEncoderCopyBufferToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyBufferInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    JvmDowncallEngine.callV4PPPP(wgpuCommandEncoderCopyBufferToTexture_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderCopyTextureToBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderCopyTextureToBuffer") }
actual fun wgpuCommandEncoderCopyTextureToBuffer(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyBufferInfo?, copySize: WGPUExtent3D?): Unit {
    JvmDowncallEngine.callV4PPPP(wgpuCommandEncoderCopyTextureToBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderCopyTextureToTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderCopyTextureToTexture") }
actual fun wgpuCommandEncoderCopyTextureToTexture(commandEncoder: WGPUCommandEncoder?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?): Unit {
    JvmDowncallEngine.callV4PPPP(wgpuCommandEncoderCopyTextureToTexture_ADDR, commandEncoder?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderFinish_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderFinish") }
actual fun wgpuCommandEncoderFinish(commandEncoder: WGPUCommandEncoder?, descriptor: WGPUCommandBufferDescriptor?): WGPUCommandBuffer? {
    return JvmDowncallEngine.callP2PP(wgpuCommandEncoderFinish_ADDR, commandEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUCommandBuffer)
}

private val wgpuCommandEncoderInjectValidationError_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderInjectValidationError") }
actual fun wgpuCommandEncoderInjectValidationError(commandEncoder: WGPUCommandEncoder?, message: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuCommandEncoderInjectValidationError_ADDR, commandEncoder?.handler?.rawValue ?: 0L, message.handler.rawValue)
    return
}

private val wgpuCommandEncoderInsertDebugMarker_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderInsertDebugMarker") }
actual fun wgpuCommandEncoderInsertDebugMarker(commandEncoder: WGPUCommandEncoder?, markerLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuCommandEncoderInsertDebugMarker_ADDR, commandEncoder?.handler?.rawValue ?: 0L, markerLabel.handler.rawValue)
    return
}

private val wgpuCommandEncoderPopDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderPopDebugGroup") }
actual fun wgpuCommandEncoderPopDebugGroup(commandEncoder: WGPUCommandEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuCommandEncoderPopDebugGroup_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderPushDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderPushDebugGroup") }
actual fun wgpuCommandEncoderPushDebugGroup(commandEncoder: WGPUCommandEncoder?, groupLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuCommandEncoderPushDebugGroup_ADDR, commandEncoder?.handler?.rawValue ?: 0L, groupLabel.handler.rawValue)
    return
}

private val wgpuCommandEncoderResolveQuerySet_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderResolveQuerySet") }
actual fun wgpuCommandEncoderResolveQuerySet(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, firstQuery: UInt, queryCount: UInt, destination: WGPUBuffer?, destinationOffset: ULong): Unit {
    JvmDowncallEngine.callV6PPIIPL(wgpuCommandEncoderResolveQuerySet_ADDR, commandEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, firstQuery.toInt(), queryCount.toInt(), destination?.handler?.rawValue ?: 0L, destinationOffset.toLong())
    return
}

private val wgpuCommandEncoderSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderSetLabel") }
actual fun wgpuCommandEncoderSetLabel(commandEncoder: WGPUCommandEncoder?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuCommandEncoderSetLabel_ADDR, commandEncoder?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuCommandEncoderWriteBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderWriteBuffer") }
actual fun wgpuCommandEncoderWriteBuffer(commandEncoder: WGPUCommandEncoder?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    JvmDowncallEngine.callV5PPLPL(wgpuCommandEncoderWriteBuffer_ADDR, commandEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, bufferOffset.toLong(), data?.rawValue ?: 0L, size.toLong())
    return
}

private val wgpuCommandEncoderWriteTimestamp_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderWriteTimestamp") }
actual fun wgpuCommandEncoderWriteTimestamp(commandEncoder: WGPUCommandEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    JvmDowncallEngine.callV3PPI(wgpuCommandEncoderWriteTimestamp_ADDR, commandEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuCommandEncoderAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderAddRef") }
actual fun wgpuCommandEncoderAddRef(commandEncoder: WGPUCommandEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuCommandEncoderAddRef_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuCommandEncoderRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuCommandEncoderRelease") }
actual fun wgpuCommandEncoderRelease(commandEncoder: WGPUCommandEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuCommandEncoderRelease_ADDR, commandEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderDispatchWorkgroups_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderDispatchWorkgroups") }
actual fun wgpuComputePassEncoderDispatchWorkgroups(computePassEncoder: WGPUComputePassEncoder?, workgroupCountX: UInt, workgroupCountY: UInt, workgroupCountZ: UInt): Unit {
    JvmDowncallEngine.callV4PIII(wgpuComputePassEncoderDispatchWorkgroups_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, workgroupCountX.toInt(), workgroupCountY.toInt(), workgroupCountZ.toInt())
    return
}

private val wgpuComputePassEncoderDispatchWorkgroupsIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderDispatchWorkgroupsIndirect") }
actual fun wgpuComputePassEncoderDispatchWorkgroupsIndirect(computePassEncoder: WGPUComputePassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    JvmDowncallEngine.callV3PPL(wgpuComputePassEncoderDispatchWorkgroupsIndirect_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuComputePassEncoderEnd_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderEnd") }
actual fun wgpuComputePassEncoderEnd(computePassEncoder: WGPUComputePassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePassEncoderEnd_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderInsertDebugMarker_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderInsertDebugMarker") }
actual fun wgpuComputePassEncoderInsertDebugMarker(computePassEncoder: WGPUComputePassEncoder?, markerLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuComputePassEncoderInsertDebugMarker_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, markerLabel.handler.rawValue)
    return
}

private val wgpuComputePassEncoderPopDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderPopDebugGroup") }
actual fun wgpuComputePassEncoderPopDebugGroup(computePassEncoder: WGPUComputePassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePassEncoderPopDebugGroup_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderPushDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderPushDebugGroup") }
actual fun wgpuComputePassEncoderPushDebugGroup(computePassEncoder: WGPUComputePassEncoder?, groupLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuComputePassEncoderPushDebugGroup_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, groupLabel.handler.rawValue)
    return
}

private val wgpuComputePassEncoderSetBindGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderSetBindGroup") }
actual fun wgpuComputePassEncoderSetBindGroup(computePassEncoder: WGPUComputePassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    JvmDowncallEngine.callV5PIPLP(wgpuComputePassEncoderSetBindGroup_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderSetImmediates_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderSetImmediates") }
actual fun wgpuComputePassEncoderSetImmediates(computePassEncoder: WGPUComputePassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuComputePassEncoderSetImmediates_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), computePassEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data?.rawValue ?: 0L, size.toLong())
    return
}

private val wgpuComputePassEncoderSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderSetLabel") }
actual fun wgpuComputePassEncoderSetLabel(computePassEncoder: WGPUComputePassEncoder?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuComputePassEncoderSetLabel_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuComputePassEncoderSetPipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderSetPipeline") }
actual fun wgpuComputePassEncoderSetPipeline(computePassEncoder: WGPUComputePassEncoder?, pipeline: WGPUComputePipeline?): Unit {
    JvmDowncallEngine.callV2PP(wgpuComputePassEncoderSetPipeline_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderSetResourceTable_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderSetResourceTable") }
actual fun wgpuComputePassEncoderSetResourceTable(computePassEncoder: WGPUComputePassEncoder?, table: WGPUResourceTable?): Unit {
    JvmDowncallEngine.callV2PP(wgpuComputePassEncoderSetResourceTable_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, table?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderWriteTimestamp_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderWriteTimestamp") }
actual fun wgpuComputePassEncoderWriteTimestamp(computePassEncoder: WGPUComputePassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    JvmDowncallEngine.callV3PPI(wgpuComputePassEncoderWriteTimestamp_ADDR, computePassEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuComputePassEncoderAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderAddRef") }
actual fun wgpuComputePassEncoderAddRef(computePassEncoder: WGPUComputePassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePassEncoderAddRef_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePassEncoderRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePassEncoderRelease") }
actual fun wgpuComputePassEncoderRelease(computePassEncoder: WGPUComputePassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePassEncoderRelease_ADDR, computePassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePipelineGetBindGroupLayout_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePipelineGetBindGroupLayout") }
actual fun wgpuComputePipelineGetBindGroupLayout(computePipeline: WGPUComputePipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return JvmDowncallEngine.callP2PI(wgpuComputePipelineGetBindGroupLayout_ADDR, computePipeline?.handler?.rawValue ?: 0L, groupIndex.toInt()).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuComputePipelineSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePipelineSetLabel") }
actual fun wgpuComputePipelineSetLabel(computePipeline: WGPUComputePipeline?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuComputePipelineSetLabel_ADDR, computePipeline?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuComputePipelineAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePipelineAddRef") }
actual fun wgpuComputePipelineAddRef(computePipeline: WGPUComputePipeline?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePipelineAddRef_ADDR, computePipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuComputePipelineRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuComputePipelineRelease") }
actual fun wgpuComputePipelineRelease(computePipeline: WGPUComputePipeline?): Unit {
    JvmDowncallEngine.callV1P(wgpuComputePipelineRelease_ADDR, computePipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDawnDrmFormatCapabilitiesFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDawnDrmFormatCapabilitiesFreeMembers") }
actual fun wgpuDawnDrmFormatCapabilitiesFreeMembers(dawnDrmFormatCapabilities: WGPUDawnDrmFormatCapabilities): Unit {
    JvmDowncallEngine.callGeneric(wgpuDawnDrmFormatCapabilitiesFreeMembers_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Struct("WGPUDawnDrmFormatCapabilities"))), dawnDrmFormatCapabilities.handler.rawValue)
    return
}

private val wgpuDeviceCreateBindGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateBindGroup") }
actual fun wgpuDeviceCreateBindGroup(device: WGPUDevice?, descriptor: WGPUBindGroupDescriptor?): WGPUBindGroup? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateBindGroup_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroup)
}

private val wgpuDeviceCreateBindGroupLayout_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateBindGroupLayout") }
actual fun wgpuDeviceCreateBindGroupLayout(device: WGPUDevice?, descriptor: WGPUBindGroupLayoutDescriptor?): WGPUBindGroupLayout? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateBindGroupLayout_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuDeviceCreateBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateBuffer") }
actual fun wgpuDeviceCreateBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateBuffer_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuDeviceCreateCommandEncoder_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateCommandEncoder") }
actual fun wgpuDeviceCreateCommandEncoder(device: WGPUDevice?, descriptor: WGPUCommandEncoderDescriptor?): WGPUCommandEncoder? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateCommandEncoder_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUCommandEncoder)
}

private val wgpuDeviceCreateComputePipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateComputePipeline") }
actual fun wgpuDeviceCreateComputePipeline(device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?): WGPUComputePipeline? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateComputePipeline_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePipeline)
}

private val wgpuDeviceCreateComputePipelineAsync_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateComputePipelineAsync") }
actual fun wgpuDeviceCreateComputePipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPUComputePipelineDescriptor?, callbackInfo: WGPUCreateComputePipelineAsyncCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUCreateComputePipelineAsyncCallbackInfo(wgpuDeviceCreateComputePipelineAsync_ADDR, allocator, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuDeviceCreateErrorBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorBuffer") }
actual fun wgpuDeviceCreateErrorBuffer(device: WGPUDevice?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateErrorBuffer_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuDeviceCreateErrorComputePipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorComputePipeline") }
actual fun wgpuDeviceCreateErrorComputePipeline(device: WGPUDevice?, label: WGPUStringView): WGPUComputePipeline? {
    val _kffiGenericResult = JvmDowncallEngine.callGeneric(wgpuDeviceCreateErrorComputePipeline_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Pointer, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Struct("WGPUStringView"))), device?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return (_kffiGenericResult as MemorySegment).address().takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUComputePipeline)
}

private val wgpuDeviceCreateErrorExternalTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorExternalTexture") }
actual fun wgpuDeviceCreateErrorExternalTexture(device: WGPUDevice?): WGPUExternalTexture? {
    return JvmDowncallEngine.callP1P(wgpuDeviceCreateErrorExternalTexture_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUExternalTexture)
}

private val wgpuDeviceCreateErrorRenderPipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorRenderPipeline") }
actual fun wgpuDeviceCreateErrorRenderPipeline(device: WGPUDevice?, label: WGPUStringView): WGPURenderPipeline? {
    val _kffiGenericResult_2 = JvmDowncallEngine.callGeneric(wgpuDeviceCreateErrorRenderPipeline_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Pointer, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Struct("WGPUStringView"))), device?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return (_kffiGenericResult_2 as MemorySegment).address().takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPipeline)
}

private val wgpuDeviceCreateErrorShaderModule_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorShaderModule") }
actual fun wgpuDeviceCreateErrorShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?, errorMessage: WGPUStringView): WGPUShaderModule? {
    val _kffiGenericResult_3 = JvmDowncallEngine.callGeneric(wgpuDeviceCreateErrorShaderModule_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Pointer, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Struct("WGPUStringView"))), device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L, errorMessage.handler.rawValue)
    return (_kffiGenericResult_3 as MemorySegment).address().takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUShaderModule)
}

private val wgpuDeviceCreateErrorTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateErrorTexture") }
actual fun wgpuDeviceCreateErrorTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateErrorTexture_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuDeviceCreateExternalTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateExternalTexture") }
actual fun wgpuDeviceCreateExternalTexture(device: WGPUDevice?, externalTextureDescriptor: WGPUExternalTextureDescriptor?): WGPUExternalTexture? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateExternalTexture_ADDR, device?.handler?.rawValue ?: 0L, externalTextureDescriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUExternalTexture)
}

private val wgpuDeviceCreatePipelineLayout_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreatePipelineLayout") }
actual fun wgpuDeviceCreatePipelineLayout(device: WGPUDevice?, descriptor: WGPUPipelineLayoutDescriptor?): WGPUPipelineLayout? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreatePipelineLayout_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUPipelineLayout)
}

private val wgpuDeviceCreateQuerySet_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateQuerySet") }
actual fun wgpuDeviceCreateQuerySet(device: WGPUDevice?, descriptor: WGPUQuerySetDescriptor?): WGPUQuerySet? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateQuerySet_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUQuerySet)
}

private val wgpuDeviceCreateRenderBundleEncoder_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateRenderBundleEncoder") }
actual fun wgpuDeviceCreateRenderBundleEncoder(device: WGPUDevice?, descriptor: WGPURenderBundleEncoderDescriptor?): WGPURenderBundleEncoder? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateRenderBundleEncoder_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderBundleEncoder)
}

private val wgpuDeviceCreateRenderPipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateRenderPipeline") }
actual fun wgpuDeviceCreateRenderPipeline(device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?): WGPURenderPipeline? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateRenderPipeline_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderPipeline)
}

private val wgpuDeviceCreateRenderPipelineAsync_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateRenderPipelineAsync") }
actual fun wgpuDeviceCreateRenderPipelineAsync(allocator: MemoryAllocator, device: WGPUDevice?, descriptor: WGPURenderPipelineDescriptor?, callbackInfo: WGPUCreateRenderPipelineAsyncCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUCreateRenderPipelineAsyncCallbackInfo(wgpuDeviceCreateRenderPipelineAsync_ADDR, allocator, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuDeviceCreateResourceTable_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateResourceTable") }
actual fun wgpuDeviceCreateResourceTable(device: WGPUDevice?, descriptor: WGPUResourceTableDescriptor?): WGPUResourceTable? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateResourceTable_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUResourceTable)
}

private val wgpuDeviceCreateSampler_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateSampler") }
actual fun wgpuDeviceCreateSampler(device: WGPUDevice?, descriptor: WGPUSamplerDescriptor?): WGPUSampler? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateSampler_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSampler)
}

private val wgpuDeviceCreateShaderModule_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateShaderModule") }
actual fun wgpuDeviceCreateShaderModule(device: WGPUDevice?, descriptor: WGPUShaderModuleDescriptor?): WGPUShaderModule? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateShaderModule_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUShaderModule)
}

private val wgpuDeviceCreateTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceCreateTexture") }
actual fun wgpuDeviceCreateTexture(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceCreateTexture_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuDeviceDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceDestroy") }
actual fun wgpuDeviceDestroy(device: WGPUDevice?): Unit {
    JvmDowncallEngine.callV1P(wgpuDeviceDestroy_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceForceLoss_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceForceLoss") }
actual fun wgpuDeviceForceLoss(device: WGPUDevice?, type: WGPUDeviceLostReason, message: WGPUStringView): Unit {
    JvmDowncallEngine.callGeneric(wgpuDeviceForceLoss_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Struct("WGPUStringView"))), device?.handler?.rawValue ?: 0L, type.toInt(), message.handler.rawValue)
    return
}

private val wgpuDeviceGetAdapter_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetAdapter") }
actual fun wgpuDeviceGetAdapter(device: WGPUDevice?): WGPUAdapter? {
    return JvmDowncallEngine.callP1P(wgpuDeviceGetAdapter_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUAdapter)
}

private val wgpuDeviceGetAdapterInfo_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetAdapterInfo") }
actual fun wgpuDeviceGetAdapterInfo(device: WGPUDevice?, adapterInfo: WGPUAdapterInfo?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuDeviceGetAdapterInfo_ADDR, device?.handler?.rawValue ?: 0L, adapterInfo?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetAHardwareBufferProperties_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetAHardwareBufferProperties") }
actual fun wgpuDeviceGetAHardwareBufferProperties(device: WGPUDevice?, handle: NativeAddress?, properties: WGPUAHardwareBufferProperties?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuDeviceGetAHardwareBufferProperties_ADDR, device?.handler?.rawValue ?: 0L, handle?.rawValue ?: 0L, properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetFeatures_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetFeatures") }
actual fun wgpuDeviceGetFeatures(device: WGPUDevice?, features: WGPUSupportedFeatures?): Unit {
    JvmDowncallEngine.callV2PP(wgpuDeviceGetFeatures_ADDR, device?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceGetLimits_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetLimits") }
actual fun wgpuDeviceGetLimits(device: WGPUDevice?, limits: WGPULimits?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuDeviceGetLimits_ADDR, device?.handler?.rawValue ?: 0L, limits?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuDeviceGetLostFuture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetLostFuture") }
actual fun wgpuDeviceGetLostFuture(allocator: MemoryAllocator, device: WGPUDevice?): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFuture(wgpuDeviceGetLostFuture_ADDR, allocator, device?.handler?.rawValue ?: 0L))
}

private val wgpuDeviceGetQueue_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceGetQueue") }
actual fun wgpuDeviceGetQueue(device: WGPUDevice?): WGPUQueue? {
    return JvmDowncallEngine.callP1P(wgpuDeviceGetQueue_ADDR, device?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUQueue)
}

private val wgpuDeviceHasFeature_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceHasFeature") }
actual fun wgpuDeviceHasFeature(device: WGPUDevice?, feature: WGPUFeatureName): UInt {
    return JvmDowncallEngine.callI2PI(wgpuDeviceHasFeature_ADDR, device?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuDeviceImportSharedBufferMemory_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceImportSharedBufferMemory") }
actual fun wgpuDeviceImportSharedBufferMemory(device: WGPUDevice?, descriptor: WGPUSharedBufferMemoryDescriptor?): WGPUSharedBufferMemory? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceImportSharedBufferMemory_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedBufferMemory)
}

private val wgpuDeviceImportSharedFence_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceImportSharedFence") }
actual fun wgpuDeviceImportSharedFence(device: WGPUDevice?, descriptor: WGPUSharedFenceDescriptor?): WGPUSharedFence? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceImportSharedFence_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedFence)
}

private val wgpuDeviceImportSharedTextureMemory_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceImportSharedTextureMemory") }
actual fun wgpuDeviceImportSharedTextureMemory(device: WGPUDevice?, descriptor: WGPUSharedTextureMemoryDescriptor?): WGPUSharedTextureMemory? {
    return JvmDowncallEngine.callP2PP(wgpuDeviceImportSharedTextureMemory_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSharedTextureMemory)
}

private val wgpuDeviceInjectError_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceInjectError") }
actual fun wgpuDeviceInjectError(device: WGPUDevice?, type: WGPUErrorType, message: WGPUStringView): Unit {
    JvmDowncallEngine.callGeneric(wgpuDeviceInjectError_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Struct("WGPUStringView"))), device?.handler?.rawValue ?: 0L, type.toInt(), message.handler.rawValue)
    return
}

private val wgpuDevicePopErrorScope_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDevicePopErrorScope") }
actual fun wgpuDevicePopErrorScope(allocator: MemoryAllocator, device: WGPUDevice?, callbackInfo: WGPUPopErrorScopeCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUPopErrorScopeCallbackInfo(wgpuDevicePopErrorScope_ADDR, allocator, device?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuDevicePushErrorScope_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDevicePushErrorScope") }
actual fun wgpuDevicePushErrorScope(device: WGPUDevice?, filter: WGPUErrorFilter): Unit {
    JvmDowncallEngine.callV2PI(wgpuDevicePushErrorScope_ADDR, device?.handler?.rawValue ?: 0L, filter.toInt())
    return
}

private val wgpuDeviceSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceSetLabel") }
actual fun wgpuDeviceSetLabel(device: WGPUDevice?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuDeviceSetLabel_ADDR, device?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuDeviceSetLoggingCallback_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceSetLoggingCallback") }
actual fun wgpuDeviceSetLoggingCallback(device: WGPUDevice?, callbackInfo: WGPULoggingCallbackInfo): Unit {
    JvmDowncallEngine.callGeneric(wgpuDeviceSetLoggingCallback_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Struct("WGPULoggingCallbackInfo"))), device?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue)
    return
}

private val wgpuDeviceTick_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceTick") }
actual fun wgpuDeviceTick(device: WGPUDevice?): Unit {
    JvmDowncallEngine.callV1P(wgpuDeviceTick_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceValidateTextureDescriptor_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceValidateTextureDescriptor") }
actual fun wgpuDeviceValidateTextureDescriptor(device: WGPUDevice?, descriptor: WGPUTextureDescriptor?): Unit {
    JvmDowncallEngine.callV2PP(wgpuDeviceValidateTextureDescriptor_ADDR, device?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceAddRef") }
actual fun wgpuDeviceAddRef(device: WGPUDevice?): Unit {
    JvmDowncallEngine.callV1P(wgpuDeviceAddRef_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuDeviceRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuDeviceRelease") }
actual fun wgpuDeviceRelease(device: WGPUDevice?): Unit {
    JvmDowncallEngine.callV1P(wgpuDeviceRelease_ADDR, device?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureDestroy") }
actual fun wgpuExternalTextureDestroy(externalTexture: WGPUExternalTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuExternalTextureDestroy_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureExpire_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureExpire") }
actual fun wgpuExternalTextureExpire(externalTexture: WGPUExternalTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuExternalTextureExpire_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureRefresh_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureRefresh") }
actual fun wgpuExternalTextureRefresh(externalTexture: WGPUExternalTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuExternalTextureRefresh_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureSetLabel") }
actual fun wgpuExternalTextureSetLabel(externalTexture: WGPUExternalTexture?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuExternalTextureSetLabel_ADDR, externalTexture?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuExternalTextureAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureAddRef") }
actual fun wgpuExternalTextureAddRef(externalTexture: WGPUExternalTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuExternalTextureAddRef_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuExternalTextureRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuExternalTextureRelease") }
actual fun wgpuExternalTextureRelease(externalTexture: WGPUExternalTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuExternalTextureRelease_ADDR, externalTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceCreateSurface_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceCreateSurface") }
actual fun wgpuInstanceCreateSurface(instance: WGPUInstance?, descriptor: WGPUSurfaceDescriptor?): WGPUSurface? {
    return JvmDowncallEngine.callP2PP(wgpuInstanceCreateSurface_ADDR, instance?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUSurface)
}

private val wgpuInstanceGetWGSLLanguageFeatures_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceGetWGSLLanguageFeatures") }
actual fun wgpuInstanceGetWGSLLanguageFeatures(instance: WGPUInstance?, features: WGPUSupportedWGSLLanguageFeatures?): Unit {
    JvmDowncallEngine.callV2PP(wgpuInstanceGetWGSLLanguageFeatures_ADDR, instance?.handler?.rawValue ?: 0L, features?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceHasWGSLLanguageFeature_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceHasWGSLLanguageFeature") }
actual fun wgpuInstanceHasWGSLLanguageFeature(instance: WGPUInstance?, feature: WGPUWGSLLanguageFeatureName): UInt {
    return JvmDowncallEngine.callI2PI(wgpuInstanceHasWGSLLanguageFeature_ADDR, instance?.handler?.rawValue ?: 0L, feature.toInt()).toInt().toUInt()
}

private val wgpuInstanceProcessEvents_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceProcessEvents") }
actual fun wgpuInstanceProcessEvents(instance: WGPUInstance?): Unit {
    JvmDowncallEngine.callV1P(wgpuInstanceProcessEvents_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceRequestAdapter_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceRequestAdapter") }
actual fun wgpuInstanceRequestAdapter(allocator: MemoryAllocator, instance: WGPUInstance?, options: WGPURequestAdapterOptions?, callbackInfo: WGPURequestAdapterCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPURequestAdapterCallbackInfo(wgpuInstanceRequestAdapter_ADDR, allocator, instance?.handler?.rawValue ?: 0L, options?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuInstanceWaitAny_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceWaitAny") }
actual fun wgpuInstanceWaitAny(instance: WGPUInstance?, futureCount: ULong, futures: WGPUFutureWaitInfo?, timeoutNS: ULong): WGPUWaitStatus {
    return (JvmDowncallEngine.callI4PLPL(wgpuInstanceWaitAny_ADDR, instance?.handler?.rawValue ?: 0L, futureCount.toLong(), futures?.handler?.rawValue ?: 0L, timeoutNS.toLong()).toInt()).toUInt()
}

private val wgpuInstanceAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceAddRef") }
actual fun wgpuInstanceAddRef(instance: WGPUInstance?): Unit {
    JvmDowncallEngine.callV1P(wgpuInstanceAddRef_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuInstanceRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuInstanceRelease") }
actual fun wgpuInstanceRelease(instance: WGPUInstance?): Unit {
    JvmDowncallEngine.callV1P(wgpuInstanceRelease_ADDR, instance?.handler?.rawValue ?: 0L)
    return
}

private val wgpuPipelineLayoutSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuPipelineLayoutSetLabel") }
actual fun wgpuPipelineLayoutSetLabel(pipelineLayout: WGPUPipelineLayout?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuPipelineLayoutSetLabel_ADDR, pipelineLayout?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuPipelineLayoutAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuPipelineLayoutAddRef") }
actual fun wgpuPipelineLayoutAddRef(pipelineLayout: WGPUPipelineLayout?): Unit {
    JvmDowncallEngine.callV1P(wgpuPipelineLayoutAddRef_ADDR, pipelineLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuPipelineLayoutRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuPipelineLayoutRelease") }
actual fun wgpuPipelineLayoutRelease(pipelineLayout: WGPUPipelineLayout?): Unit {
    JvmDowncallEngine.callV1P(wgpuPipelineLayoutRelease_ADDR, pipelineLayout?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetDestroy") }
actual fun wgpuQuerySetDestroy(querySet: WGPUQuerySet?): Unit {
    JvmDowncallEngine.callV1P(wgpuQuerySetDestroy_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetGetCount_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetGetCount") }
actual fun wgpuQuerySetGetCount(querySet: WGPUQuerySet?): UInt {
    return JvmDowncallEngine.callI1P(wgpuQuerySetGetCount_ADDR, querySet?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuQuerySetGetType_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetGetType") }
actual fun wgpuQuerySetGetType(querySet: WGPUQuerySet?): WGPUQueryType {
    return (JvmDowncallEngine.callI1P(wgpuQuerySetGetType_ADDR, querySet?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuQuerySetSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetSetLabel") }
actual fun wgpuQuerySetSetLabel(querySet: WGPUQuerySet?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuQuerySetSetLabel_ADDR, querySet?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuQuerySetAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetAddRef") }
actual fun wgpuQuerySetAddRef(querySet: WGPUQuerySet?): Unit {
    JvmDowncallEngine.callV1P(wgpuQuerySetAddRef_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQuerySetRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQuerySetRelease") }
actual fun wgpuQuerySetRelease(querySet: WGPUQuerySet?): Unit {
    JvmDowncallEngine.callV1P(wgpuQuerySetRelease_ADDR, querySet?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueCopyExternalTextureForBrowser_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueCopyExternalTextureForBrowser") }
actual fun wgpuQueueCopyExternalTextureForBrowser(queue: WGPUQueue?, source: WGPUImageCopyExternalTexture?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    JvmDowncallEngine.callGeneric(wgpuQueueCopyExternalTextureForBrowser_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer)), queue?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L, options?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueCopyTextureForBrowser_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueCopyTextureForBrowser") }
actual fun wgpuQueueCopyTextureForBrowser(queue: WGPUQueue?, source: WGPUTexelCopyTextureInfo?, destination: WGPUTexelCopyTextureInfo?, copySize: WGPUExtent3D?, options: WGPUCopyTextureForBrowserOptions?): Unit {
    JvmDowncallEngine.callGeneric(wgpuQueueCopyTextureForBrowser_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer)), queue?.handler?.rawValue ?: 0L, source?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, copySize?.handler?.rawValue ?: 0L, options?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueOnSubmittedWorkDone_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueOnSubmittedWorkDone") }
actual fun wgpuQueueOnSubmittedWorkDone(allocator: MemoryAllocator, queue: WGPUQueue?, callbackInfo: WGPUQueueWorkDoneCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUQueueWorkDoneCallbackInfo(wgpuQueueOnSubmittedWorkDone_ADDR, allocator, queue?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuQueueSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueSetLabel") }
actual fun wgpuQueueSetLabel(queue: WGPUQueue?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuQueueSetLabel_ADDR, queue?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuQueueSubmit_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueSubmit") }
actual fun wgpuQueueSubmit(queue: WGPUQueue?, commandCount: ULong, commands: NativeAddress?): Unit {
    JvmDowncallEngine.callV3PLP(wgpuQueueSubmit_ADDR, queue?.handler?.rawValue ?: 0L, commandCount.toLong(), commands?.rawValue ?: 0L)
    return
}

private val wgpuQueueWriteBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueWriteBuffer") }
actual fun wgpuQueueWriteBuffer(queue: WGPUQueue?, buffer: WGPUBuffer?, bufferOffset: ULong, data: NativeAddress?, size: ULong): Unit {
    JvmDowncallEngine.callV5PPLPL(wgpuQueueWriteBuffer_ADDR, queue?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, bufferOffset.toLong(), data?.rawValue ?: 0L, size.toLong())
    return
}

private val wgpuQueueWriteTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueWriteTexture") }
actual fun wgpuQueueWriteTexture(queue: WGPUQueue?, destination: WGPUTexelCopyTextureInfo?, data: NativeAddress?, dataSize: ULong, dataLayout: WGPUTexelCopyBufferLayout?, writeSize: WGPUExtent3D?): Unit {
    JvmDowncallEngine.callV6PPPLPP(wgpuQueueWriteTexture_ADDR, queue?.handler?.rawValue ?: 0L, destination?.handler?.rawValue ?: 0L, data?.rawValue ?: 0L, dataSize.toLong(), dataLayout?.handler?.rawValue ?: 0L, writeSize?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueAddRef") }
actual fun wgpuQueueAddRef(queue: WGPUQueue?): Unit {
    JvmDowncallEngine.callV1P(wgpuQueueAddRef_ADDR, queue?.handler?.rawValue ?: 0L)
    return
}

private val wgpuQueueRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuQueueRelease") }
actual fun wgpuQueueRelease(queue: WGPUQueue?): Unit {
    JvmDowncallEngine.callV1P(wgpuQueueRelease_ADDR, queue?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleSetLabel") }
actual fun wgpuRenderBundleSetLabel(renderBundle: WGPURenderBundle?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderBundleSetLabel_ADDR, renderBundle?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuRenderBundleAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleAddRef") }
actual fun wgpuRenderBundleAddRef(renderBundle: WGPURenderBundle?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderBundleAddRef_ADDR, renderBundle?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleRelease") }
actual fun wgpuRenderBundleRelease(renderBundle: WGPURenderBundle?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderBundleRelease_ADDR, renderBundle?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderDraw_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderDraw") }
actual fun wgpuRenderBundleEncoderDraw(renderBundleEncoder: WGPURenderBundleEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    JvmDowncallEngine.callV5PIIII(wgpuRenderBundleEncoderDraw_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, vertexCount.toInt(), instanceCount.toInt(), firstVertex.toInt(), firstInstance.toInt())
    return
}

private val wgpuRenderBundleEncoderDrawIndexed_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderDrawIndexed") }
actual fun wgpuRenderBundleEncoderDrawIndexed(renderBundleEncoder: WGPURenderBundleEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    JvmDowncallEngine.callV6PIIIII(wgpuRenderBundleEncoderDrawIndexed_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indexCount.toInt(), instanceCount.toInt(), firstIndex.toInt(), baseVertex, firstInstance.toInt())
    return
}

private val wgpuRenderBundleEncoderDrawIndexedIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderDrawIndexedIndirect") }
actual fun wgpuRenderBundleEncoderDrawIndexedIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    JvmDowncallEngine.callV3PPL(wgpuRenderBundleEncoderDrawIndexedIndirect_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderBundleEncoderDrawIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderDrawIndirect") }
actual fun wgpuRenderBundleEncoderDrawIndirect(renderBundleEncoder: WGPURenderBundleEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    JvmDowncallEngine.callV3PPL(wgpuRenderBundleEncoderDrawIndirect_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderBundleEncoderFinish_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderFinish") }
actual fun wgpuRenderBundleEncoderFinish(renderBundleEncoder: WGPURenderBundleEncoder?, descriptor: WGPURenderBundleDescriptor?): WGPURenderBundle? {
    return JvmDowncallEngine.callP2PP(wgpuRenderBundleEncoderFinish_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPURenderBundle)
}

private val wgpuRenderBundleEncoderInsertDebugMarker_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderInsertDebugMarker") }
actual fun wgpuRenderBundleEncoderInsertDebugMarker(renderBundleEncoder: WGPURenderBundleEncoder?, markerLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderBundleEncoderInsertDebugMarker_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, markerLabel.handler.rawValue)
    return
}

private val wgpuRenderBundleEncoderPopDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderPopDebugGroup") }
actual fun wgpuRenderBundleEncoderPopDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderBundleEncoderPopDebugGroup_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderPushDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderPushDebugGroup") }
actual fun wgpuRenderBundleEncoderPushDebugGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderBundleEncoderPushDebugGroup_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, groupLabel.handler.rawValue)
    return
}

private val wgpuRenderBundleEncoderSetBindGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetBindGroup") }
actual fun wgpuRenderBundleEncoderSetBindGroup(renderBundleEncoder: WGPURenderBundleEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    JvmDowncallEngine.callV5PIPLP(wgpuRenderBundleEncoderSetBindGroup_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderSetImmediates_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetImmediates") }
actual fun wgpuRenderBundleEncoderSetImmediates(renderBundleEncoder: WGPURenderBundleEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuRenderBundleEncoderSetImmediates_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), renderBundleEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data?.rawValue ?: 0L, size.toLong())
    return
}

private val wgpuRenderBundleEncoderSetIndexBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetIndexBuffer") }
actual fun wgpuRenderBundleEncoderSetIndexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV5PPILL(wgpuRenderBundleEncoderSetIndexBuffer_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, format.toInt(), offset.toLong(), size.toLong())
    return
}

private val wgpuRenderBundleEncoderSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetLabel") }
actual fun wgpuRenderBundleEncoderSetLabel(renderBundleEncoder: WGPURenderBundleEncoder?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderBundleEncoderSetLabel_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuRenderBundleEncoderSetPipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetPipeline") }
actual fun wgpuRenderBundleEncoderSetPipeline(renderBundleEncoder: WGPURenderBundleEncoder?, pipeline: WGPURenderPipeline?): Unit {
    JvmDowncallEngine.callV2PP(wgpuRenderBundleEncoderSetPipeline_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderSetVertexBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderSetVertexBuffer") }
actual fun wgpuRenderBundleEncoderSetVertexBuffer(renderBundleEncoder: WGPURenderBundleEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV5PIPLL(wgpuRenderBundleEncoderSetVertexBuffer_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L, slot.toInt(), buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuRenderBundleEncoderAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderAddRef") }
actual fun wgpuRenderBundleEncoderAddRef(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderBundleEncoderAddRef_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderBundleEncoderRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderBundleEncoderRelease") }
actual fun wgpuRenderBundleEncoderRelease(renderBundleEncoder: WGPURenderBundleEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderBundleEncoderRelease_ADDR, renderBundleEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderBeginOcclusionQuery_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderBeginOcclusionQuery") }
actual fun wgpuRenderPassEncoderBeginOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?, queryIndex: UInt): Unit {
    JvmDowncallEngine.callV2PI(wgpuRenderPassEncoderBeginOcclusionQuery_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuRenderPassEncoderDraw_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderDraw") }
actual fun wgpuRenderPassEncoderDraw(renderPassEncoder: WGPURenderPassEncoder?, vertexCount: UInt, instanceCount: UInt, firstVertex: UInt, firstInstance: UInt): Unit {
    JvmDowncallEngine.callV5PIIII(wgpuRenderPassEncoderDraw_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, vertexCount.toInt(), instanceCount.toInt(), firstVertex.toInt(), firstInstance.toInt())
    return
}

private val wgpuRenderPassEncoderDrawIndexed_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderDrawIndexed") }
actual fun wgpuRenderPassEncoderDrawIndexed(renderPassEncoder: WGPURenderPassEncoder?, indexCount: UInt, instanceCount: UInt, firstIndex: UInt, baseVertex: Int, firstInstance: UInt): Unit {
    JvmDowncallEngine.callV6PIIIII(wgpuRenderPassEncoderDrawIndexed_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indexCount.toInt(), instanceCount.toInt(), firstIndex.toInt(), baseVertex, firstInstance.toInt())
    return
}

private val wgpuRenderPassEncoderDrawIndexedIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderDrawIndexedIndirect") }
actual fun wgpuRenderPassEncoderDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    JvmDowncallEngine.callV3PPL(wgpuRenderPassEncoderDrawIndexedIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderPassEncoderDrawIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderDrawIndirect") }
actual fun wgpuRenderPassEncoderDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong): Unit {
    JvmDowncallEngine.callV3PPL(wgpuRenderPassEncoderDrawIndirect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong())
    return
}

private val wgpuRenderPassEncoderEnd_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderEnd") }
actual fun wgpuRenderPassEncoderEnd(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderEnd_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderEndOcclusionQuery_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderEndOcclusionQuery") }
actual fun wgpuRenderPassEncoderEndOcclusionQuery(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderEndOcclusionQuery_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderExecuteBundles_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderExecuteBundles") }
actual fun wgpuRenderPassEncoderExecuteBundles(renderPassEncoder: WGPURenderPassEncoder?, bundleCount: ULong, bundles: NativeAddress?): Unit {
    JvmDowncallEngine.callV3PLP(wgpuRenderPassEncoderExecuteBundles_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, bundleCount.toLong(), bundles?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderInsertDebugMarker_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderInsertDebugMarker") }
actual fun wgpuRenderPassEncoderInsertDebugMarker(renderPassEncoder: WGPURenderPassEncoder?, markerLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderPassEncoderInsertDebugMarker_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, markerLabel.handler.rawValue)
    return
}

private val wgpuRenderPassEncoderMultiDrawIndexedIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderMultiDrawIndexedIndirect") }
actual fun wgpuRenderPassEncoderMultiDrawIndexedIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuRenderPassEncoderMultiDrawIndexedIndirect_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong(), maxDrawCount.toInt(), drawCountBuffer?.handler?.rawValue ?: 0L, drawCountBufferOffset.toLong())
    return
}

private val wgpuRenderPassEncoderMultiDrawIndirect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderMultiDrawIndirect") }
actual fun wgpuRenderPassEncoderMultiDrawIndirect(renderPassEncoder: WGPURenderPassEncoder?, indirectBuffer: WGPUBuffer?, indirectOffset: ULong, maxDrawCount: UInt, drawCountBuffer: WGPUBuffer?, drawCountBufferOffset: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuRenderPassEncoderMultiDrawIndirect_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), renderPassEncoder?.handler?.rawValue ?: 0L, indirectBuffer?.handler?.rawValue ?: 0L, indirectOffset.toLong(), maxDrawCount.toInt(), drawCountBuffer?.handler?.rawValue ?: 0L, drawCountBufferOffset.toLong())
    return
}

private val wgpuRenderPassEncoderPixelLocalStorageBarrier_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderPixelLocalStorageBarrier") }
actual fun wgpuRenderPassEncoderPixelLocalStorageBarrier(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderPixelLocalStorageBarrier_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderPopDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderPopDebugGroup") }
actual fun wgpuRenderPassEncoderPopDebugGroup(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderPopDebugGroup_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderPushDebugGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderPushDebugGroup") }
actual fun wgpuRenderPassEncoderPushDebugGroup(renderPassEncoder: WGPURenderPassEncoder?, groupLabel: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderPassEncoderPushDebugGroup_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, groupLabel.handler.rawValue)
    return
}

private val wgpuRenderPassEncoderSetBindGroup_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetBindGroup") }
actual fun wgpuRenderPassEncoderSetBindGroup(renderPassEncoder: WGPURenderPassEncoder?, groupIndex: UInt, group: WGPUBindGroup?, dynamicOffsetCount: ULong, dynamicOffsets: NativeAddress?): Unit {
    JvmDowncallEngine.callV5PIPLP(wgpuRenderPassEncoderSetBindGroup_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, groupIndex.toInt(), group?.handler?.rawValue ?: 0L, dynamicOffsetCount.toLong(), dynamicOffsets?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetBlendConstant_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetBlendConstant") }
actual fun wgpuRenderPassEncoderSetBlendConstant(renderPassEncoder: WGPURenderPassEncoder?, color: WGPUColor?): Unit {
    JvmDowncallEngine.callV2PP(wgpuRenderPassEncoderSetBlendConstant_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, color?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetImmediates_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetImmediates") }
actual fun wgpuRenderPassEncoderSetImmediates(renderPassEncoder: WGPURenderPassEncoder?, offset: UInt, data: NativeAddress?, size: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuRenderPassEncoderSetImmediates_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I32, JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), renderPassEncoder?.handler?.rawValue ?: 0L, offset.toInt(), data?.rawValue ?: 0L, size.toLong())
    return
}

private val wgpuRenderPassEncoderSetIndexBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetIndexBuffer") }
actual fun wgpuRenderPassEncoderSetIndexBuffer(renderPassEncoder: WGPURenderPassEncoder?, buffer: WGPUBuffer?, format: WGPUIndexFormat, offset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV5PPILL(wgpuRenderPassEncoderSetIndexBuffer_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, format.toInt(), offset.toLong(), size.toLong())
    return
}

private val wgpuRenderPassEncoderSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetLabel") }
actual fun wgpuRenderPassEncoderSetLabel(renderPassEncoder: WGPURenderPassEncoder?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderPassEncoderSetLabel_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuRenderPassEncoderSetPipeline_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetPipeline") }
actual fun wgpuRenderPassEncoderSetPipeline(renderPassEncoder: WGPURenderPassEncoder?, pipeline: WGPURenderPipeline?): Unit {
    JvmDowncallEngine.callV2PP(wgpuRenderPassEncoderSetPipeline_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, pipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetResourceTable_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetResourceTable") }
actual fun wgpuRenderPassEncoderSetResourceTable(renderPassEncoder: WGPURenderPassEncoder?, table: WGPUResourceTable?): Unit {
    JvmDowncallEngine.callV2PP(wgpuRenderPassEncoderSetResourceTable_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, table?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderSetScissorRect_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetScissorRect") }
actual fun wgpuRenderPassEncoderSetScissorRect(renderPassEncoder: WGPURenderPassEncoder?, x: UInt, y: UInt, width: UInt, height: UInt): Unit {
    JvmDowncallEngine.callV5PIIII(wgpuRenderPassEncoderSetScissorRect_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, x.toInt(), y.toInt(), width.toInt(), height.toInt())
    return
}

private val wgpuRenderPassEncoderSetStencilReference_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetStencilReference") }
actual fun wgpuRenderPassEncoderSetStencilReference(renderPassEncoder: WGPURenderPassEncoder?, reference: UInt): Unit {
    JvmDowncallEngine.callV2PI(wgpuRenderPassEncoderSetStencilReference_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, reference.toInt())
    return
}

private val wgpuRenderPassEncoderSetVertexBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetVertexBuffer") }
actual fun wgpuRenderPassEncoderSetVertexBuffer(renderPassEncoder: WGPURenderPassEncoder?, slot: UInt, buffer: WGPUBuffer?, offset: ULong, size: ULong): Unit {
    JvmDowncallEngine.callV5PIPLL(wgpuRenderPassEncoderSetVertexBuffer_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, slot.toInt(), buffer?.handler?.rawValue ?: 0L, offset.toLong(), size.toLong())
    return
}

private val wgpuRenderPassEncoderSetViewport_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderSetViewport") }
actual fun wgpuRenderPassEncoderSetViewport(renderPassEncoder: WGPURenderPassEncoder?, x: Float, y: Float, width: Float, height: Float, minDepth: Float, maxDepth: Float): Unit {
    JvmDowncallEngine.callV7PFFFFFF(wgpuRenderPassEncoderSetViewport_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, x, y, width, height, minDepth, maxDepth)
    return
}

private val wgpuRenderPassEncoderWriteTimestamp_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderWriteTimestamp") }
actual fun wgpuRenderPassEncoderWriteTimestamp(renderPassEncoder: WGPURenderPassEncoder?, querySet: WGPUQuerySet?, queryIndex: UInt): Unit {
    JvmDowncallEngine.callV3PPI(wgpuRenderPassEncoderWriteTimestamp_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L, querySet?.handler?.rawValue ?: 0L, queryIndex.toInt())
    return
}

private val wgpuRenderPassEncoderAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderAddRef") }
actual fun wgpuRenderPassEncoderAddRef(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderAddRef_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPassEncoderRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPassEncoderRelease") }
actual fun wgpuRenderPassEncoderRelease(renderPassEncoder: WGPURenderPassEncoder?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPassEncoderRelease_ADDR, renderPassEncoder?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPipelineGetBindGroupLayout_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPipelineGetBindGroupLayout") }
actual fun wgpuRenderPipelineGetBindGroupLayout(renderPipeline: WGPURenderPipeline?, groupIndex: UInt): WGPUBindGroupLayout? {
    return JvmDowncallEngine.callP2PI(wgpuRenderPipelineGetBindGroupLayout_ADDR, renderPipeline?.handler?.rawValue ?: 0L, groupIndex.toInt()).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBindGroupLayout)
}

private val wgpuRenderPipelineSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPipelineSetLabel") }
actual fun wgpuRenderPipelineSetLabel(renderPipeline: WGPURenderPipeline?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuRenderPipelineSetLabel_ADDR, renderPipeline?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuRenderPipelineAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPipelineAddRef") }
actual fun wgpuRenderPipelineAddRef(renderPipeline: WGPURenderPipeline?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPipelineAddRef_ADDR, renderPipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuRenderPipelineRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuRenderPipelineRelease") }
actual fun wgpuRenderPipelineRelease(renderPipeline: WGPURenderPipeline?): Unit {
    JvmDowncallEngine.callV1P(wgpuRenderPipelineRelease_ADDR, renderPipeline?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableDestroy") }
actual fun wgpuResourceTableDestroy(resourceTable: WGPUResourceTable?): Unit {
    JvmDowncallEngine.callV1P(wgpuResourceTableDestroy_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableGetSize_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableGetSize") }
actual fun wgpuResourceTableGetSize(resourceTable: WGPUResourceTable?): UInt {
    return JvmDowncallEngine.callI1P(wgpuResourceTableGetSize_ADDR, resourceTable?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuResourceTableInsert_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableInsert") }
actual fun wgpuResourceTableInsert(resourceTable: WGPUResourceTable?, resource: WGPUBindingResource?): UInt {
    return JvmDowncallEngine.callI2PP(wgpuResourceTableInsert_ADDR, resourceTable?.handler?.rawValue ?: 0L, resource?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuResourceTableRemove_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableRemove") }
actual fun wgpuResourceTableRemove(resourceTable: WGPUResourceTable?, slot: UInt): WGPUStatus {
    return (JvmDowncallEngine.callI2PI(wgpuResourceTableRemove_ADDR, resourceTable?.handler?.rawValue ?: 0L, slot.toInt()).toInt()).toUInt()
}

private val wgpuResourceTableSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableSetLabel") }
actual fun wgpuResourceTableSetLabel(resourceTable: WGPUResourceTable?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuResourceTableSetLabel_ADDR, resourceTable?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuResourceTableUpdate_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableUpdate") }
actual fun wgpuResourceTableUpdate(resourceTable: WGPUResourceTable?, slot: UInt, resource: WGPUBindingResource?): WGPUStatus {
    return (JvmDowncallEngine.callI3PIP(wgpuResourceTableUpdate_ADDR, resourceTable?.handler?.rawValue ?: 0L, slot.toInt(), resource?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuResourceTableAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableAddRef") }
actual fun wgpuResourceTableAddRef(resourceTable: WGPUResourceTable?): Unit {
    JvmDowncallEngine.callV1P(wgpuResourceTableAddRef_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuResourceTableRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuResourceTableRelease") }
actual fun wgpuResourceTableRelease(resourceTable: WGPUResourceTable?): Unit {
    JvmDowncallEngine.callV1P(wgpuResourceTableRelease_ADDR, resourceTable?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSamplerSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSamplerSetLabel") }
actual fun wgpuSamplerSetLabel(sampler: WGPUSampler?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuSamplerSetLabel_ADDR, sampler?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuSamplerAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSamplerAddRef") }
actual fun wgpuSamplerAddRef(sampler: WGPUSampler?): Unit {
    JvmDowncallEngine.callV1P(wgpuSamplerAddRef_ADDR, sampler?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSamplerRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSamplerRelease") }
actual fun wgpuSamplerRelease(sampler: WGPUSampler?): Unit {
    JvmDowncallEngine.callV1P(wgpuSamplerRelease_ADDR, sampler?.handler?.rawValue ?: 0L)
    return
}

private val wgpuShaderModuleGetCompilationInfo_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuShaderModuleGetCompilationInfo") }
actual fun wgpuShaderModuleGetCompilationInfo(allocator: MemoryAllocator, shaderModule: WGPUShaderModule?, callbackInfo: WGPUCompilationInfoCallbackInfo): WGPUFuture {
    return WGPUFuture.ByValue(JvmDowncallEngine.callStructReturnWGPUFutureWGPUCompilationInfoCallbackInfo(wgpuShaderModuleGetCompilationInfo_ADDR, allocator, shaderModule?.handler?.rawValue ?: 0L, callbackInfo.handler.rawValue))
}

private val wgpuShaderModuleSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuShaderModuleSetLabel") }
actual fun wgpuShaderModuleSetLabel(shaderModule: WGPUShaderModule?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuShaderModuleSetLabel_ADDR, shaderModule?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuShaderModuleAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuShaderModuleAddRef") }
actual fun wgpuShaderModuleAddRef(shaderModule: WGPUShaderModule?): Unit {
    JvmDowncallEngine.callV1P(wgpuShaderModuleAddRef_ADDR, shaderModule?.handler?.rawValue ?: 0L)
    return
}

private val wgpuShaderModuleRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuShaderModuleRelease") }
actual fun wgpuShaderModuleRelease(shaderModule: WGPUShaderModule?): Unit {
    JvmDowncallEngine.callV1P(wgpuShaderModuleRelease_ADDR, shaderModule?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryBeginAccess_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryBeginAccess") }
actual fun wgpuSharedBufferMemoryBeginAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryBeginAccessDescriptor?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuSharedBufferMemoryBeginAccess_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryCreateBuffer_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryCreateBuffer") }
actual fun wgpuSharedBufferMemoryCreateBuffer(sharedBufferMemory: WGPUSharedBufferMemory?, descriptor: WGPUBufferDescriptor?): WGPUBuffer? {
    return JvmDowncallEngine.callP2PP(wgpuSharedBufferMemoryCreateBuffer_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUBuffer)
}

private val wgpuSharedBufferMemoryEndAccess_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryEndAccess") }
actual fun wgpuSharedBufferMemoryEndAccess(sharedBufferMemory: WGPUSharedBufferMemory?, buffer: WGPUBuffer?, descriptor: WGPUSharedBufferMemoryEndAccessState?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuSharedBufferMemoryEndAccess_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, buffer?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryGetProperties_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryGetProperties") }
actual fun wgpuSharedBufferMemoryGetProperties(sharedBufferMemory: WGPUSharedBufferMemory?, properties: WGPUSharedBufferMemoryProperties?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuSharedBufferMemoryGetProperties_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedBufferMemoryIsDeviceLost_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryIsDeviceLost") }
actual fun wgpuSharedBufferMemoryIsDeviceLost(sharedBufferMemory: WGPUSharedBufferMemory?): UInt {
    return JvmDowncallEngine.callI1P(wgpuSharedBufferMemoryIsDeviceLost_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuSharedBufferMemorySetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemorySetLabel") }
actual fun wgpuSharedBufferMemorySetLabel(sharedBufferMemory: WGPUSharedBufferMemory?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuSharedBufferMemorySetLabel_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuSharedBufferMemoryAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryAddRef") }
actual fun wgpuSharedBufferMemoryAddRef(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedBufferMemoryAddRef_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryRelease") }
actual fun wgpuSharedBufferMemoryRelease(sharedBufferMemory: WGPUSharedBufferMemory?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedBufferMemoryRelease_ADDR, sharedBufferMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedBufferMemoryEndAccessStateFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedBufferMemoryEndAccessStateFreeMembers") }
actual fun wgpuSharedBufferMemoryEndAccessStateFreeMembers(sharedBufferMemoryEndAccessState: WGPUSharedBufferMemoryEndAccessState): Unit {
    JvmDowncallEngine.callGeneric(wgpuSharedBufferMemoryEndAccessStateFreeMembers_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Struct("WGPUSharedBufferMemoryEndAccessState"))), sharedBufferMemoryEndAccessState.handler.rawValue)
    return
}

private val wgpuSharedFenceExportInfo_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedFenceExportInfo") }
actual fun wgpuSharedFenceExportInfo(sharedFence: WGPUSharedFence?, info: WGPUSharedFenceExportInfo?): Unit {
    JvmDowncallEngine.callV2PP(wgpuSharedFenceExportInfo_ADDR, sharedFence?.handler?.rawValue ?: 0L, info?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedFenceSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedFenceSetLabel") }
actual fun wgpuSharedFenceSetLabel(sharedFence: WGPUSharedFence?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuSharedFenceSetLabel_ADDR, sharedFence?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuSharedFenceAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedFenceAddRef") }
actual fun wgpuSharedFenceAddRef(sharedFence: WGPUSharedFence?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedFenceAddRef_ADDR, sharedFence?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedFenceRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedFenceRelease") }
actual fun wgpuSharedFenceRelease(sharedFence: WGPUSharedFence?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedFenceRelease_ADDR, sharedFence?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryBeginAccess_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryBeginAccess") }
actual fun wgpuSharedTextureMemoryBeginAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryBeginAccessDescriptor?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuSharedTextureMemoryBeginAccess_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryCreateTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryCreateTexture") }
actual fun wgpuSharedTextureMemoryCreateTexture(sharedTextureMemory: WGPUSharedTextureMemory?, descriptor: WGPUTextureDescriptor?): WGPUTexture? {
    return JvmDowncallEngine.callP2PP(wgpuSharedTextureMemoryCreateTexture_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTexture)
}

private val wgpuSharedTextureMemoryEndAccess_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryEndAccess") }
actual fun wgpuSharedTextureMemoryEndAccess(sharedTextureMemory: WGPUSharedTextureMemory?, texture: WGPUTexture?, descriptor: WGPUSharedTextureMemoryEndAccessState?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuSharedTextureMemoryEndAccess_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryGetProperties_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryGetProperties") }
actual fun wgpuSharedTextureMemoryGetProperties(sharedTextureMemory: WGPUSharedTextureMemory?, properties: WGPUSharedTextureMemoryProperties?): WGPUStatus {
    return (JvmDowncallEngine.callI2PP(wgpuSharedTextureMemoryGetProperties_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, properties?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSharedTextureMemoryIsDeviceLost_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryIsDeviceLost") }
actual fun wgpuSharedTextureMemoryIsDeviceLost(sharedTextureMemory: WGPUSharedTextureMemory?): UInt {
    return JvmDowncallEngine.callI1P(wgpuSharedTextureMemoryIsDeviceLost_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuSharedTextureMemorySetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemorySetLabel") }
actual fun wgpuSharedTextureMemorySetLabel(sharedTextureMemory: WGPUSharedTextureMemory?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuSharedTextureMemorySetLabel_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuSharedTextureMemoryAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryAddRef") }
actual fun wgpuSharedTextureMemoryAddRef(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedTextureMemoryAddRef_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryRelease") }
actual fun wgpuSharedTextureMemoryRelease(sharedTextureMemory: WGPUSharedTextureMemory?): Unit {
    JvmDowncallEngine.callV1P(wgpuSharedTextureMemoryRelease_ADDR, sharedTextureMemory?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSharedTextureMemoryEndAccessStateFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSharedTextureMemoryEndAccessStateFreeMembers") }
actual fun wgpuSharedTextureMemoryEndAccessStateFreeMembers(sharedTextureMemoryEndAccessState: WGPUSharedTextureMemoryEndAccessState): Unit {
    JvmDowncallEngine.callGeneric(wgpuSharedTextureMemoryEndAccessStateFreeMembers_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Struct("WGPUSharedTextureMemoryEndAccessState"))), sharedTextureMemoryEndAccessState.handler.rawValue)
    return
}

private val wgpuSupportedFeaturesFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSupportedFeaturesFreeMembers") }
actual fun wgpuSupportedFeaturesFreeMembers(supportedFeatures: WGPUSupportedFeatures): Unit {
    JvmDowncallEngine.callStructArgWGPUSupportedFeatures(wgpuSupportedFeaturesFreeMembers_ADDR, supportedFeatures.handler.rawValue)
    return
}

private val wgpuSupportedInstanceFeaturesFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSupportedInstanceFeaturesFreeMembers") }
actual fun wgpuSupportedInstanceFeaturesFreeMembers(supportedInstanceFeatures: WGPUSupportedInstanceFeatures): Unit {
    JvmDowncallEngine.callStructArgWGPUSupportedInstanceFeatures(wgpuSupportedInstanceFeaturesFreeMembers_ADDR, supportedInstanceFeatures.handler.rawValue)
    return
}

private val wgpuSupportedWGSLLanguageFeaturesFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSupportedWGSLLanguageFeaturesFreeMembers") }
actual fun wgpuSupportedWGSLLanguageFeaturesFreeMembers(supportedWGSLLanguageFeatures: WGPUSupportedWGSLLanguageFeatures): Unit {
    JvmDowncallEngine.callStructArgWGPUSupportedWGSLLanguageFeatures(wgpuSupportedWGSLLanguageFeaturesFreeMembers_ADDR, supportedWGSLLanguageFeatures.handler.rawValue)
    return
}

private val wgpuSurfaceConfigure_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceConfigure") }
actual fun wgpuSurfaceConfigure(surface: WGPUSurface?, config: WGPUSurfaceConfiguration?): Unit {
    JvmDowncallEngine.callV2PP(wgpuSurfaceConfigure_ADDR, surface?.handler?.rawValue ?: 0L, config?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceGetCapabilities_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceGetCapabilities") }
actual fun wgpuSurfaceGetCapabilities(surface: WGPUSurface?, adapter: WGPUAdapter?, capabilities: WGPUSurfaceCapabilities?): WGPUStatus {
    return (JvmDowncallEngine.callI3PPP(wgpuSurfaceGetCapabilities_ADDR, surface?.handler?.rawValue ?: 0L, adapter?.handler?.rawValue ?: 0L, capabilities?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSurfaceGetCurrentTexture_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceGetCurrentTexture") }
actual fun wgpuSurfaceGetCurrentTexture(surface: WGPUSurface?, surfaceTexture: WGPUSurfaceTexture?): Unit {
    JvmDowncallEngine.callV2PP(wgpuSurfaceGetCurrentTexture_ADDR, surface?.handler?.rawValue ?: 0L, surfaceTexture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfacePresent_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfacePresent") }
actual fun wgpuSurfacePresent(surface: WGPUSurface?): WGPUStatus {
    return (JvmDowncallEngine.callI1P(wgpuSurfacePresent_ADDR, surface?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuSurfaceSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceSetLabel") }
actual fun wgpuSurfaceSetLabel(surface: WGPUSurface?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuSurfaceSetLabel_ADDR, surface?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuSurfaceUnconfigure_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceUnconfigure") }
actual fun wgpuSurfaceUnconfigure(surface: WGPUSurface?): Unit {
    JvmDowncallEngine.callV1P(wgpuSurfaceUnconfigure_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceAddRef") }
actual fun wgpuSurfaceAddRef(surface: WGPUSurface?): Unit {
    JvmDowncallEngine.callV1P(wgpuSurfaceAddRef_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceRelease") }
actual fun wgpuSurfaceRelease(surface: WGPUSurface?): Unit {
    JvmDowncallEngine.callV1P(wgpuSurfaceRelease_ADDR, surface?.handler?.rawValue ?: 0L)
    return
}

private val wgpuSurfaceCapabilitiesFreeMembers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuSurfaceCapabilitiesFreeMembers") }
actual fun wgpuSurfaceCapabilitiesFreeMembers(surfaceCapabilities: WGPUSurfaceCapabilities): Unit {
    JvmDowncallEngine.callStructArgWGPUSurfaceCapabilities(wgpuSurfaceCapabilitiesFreeMembers_ADDR, surfaceCapabilities.handler.rawValue)
    return
}

private val wgpuTexelBufferViewSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTexelBufferViewSetLabel") }
actual fun wgpuTexelBufferViewSetLabel(texelBufferView: WGPUTexelBufferView?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuTexelBufferViewSetLabel_ADDR, texelBufferView?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuTexelBufferViewAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTexelBufferViewAddRef") }
actual fun wgpuTexelBufferViewAddRef(texelBufferView: WGPUTexelBufferView?): Unit {
    JvmDowncallEngine.callV1P(wgpuTexelBufferViewAddRef_ADDR, texelBufferView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTexelBufferViewRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTexelBufferViewRelease") }
actual fun wgpuTexelBufferViewRelease(texelBufferView: WGPUTexelBufferView?): Unit {
    JvmDowncallEngine.callV1P(wgpuTexelBufferViewRelease_ADDR, texelBufferView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureCreateErrorView_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureCreateErrorView") }
actual fun wgpuTextureCreateErrorView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return JvmDowncallEngine.callP2PP(wgpuTextureCreateErrorView_ADDR, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTextureView)
}

private val wgpuTextureCreateView_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureCreateView") }
actual fun wgpuTextureCreateView(texture: WGPUTexture?, descriptor: WGPUTextureViewDescriptor?): WGPUTextureView? {
    return JvmDowncallEngine.callP2PP(wgpuTextureCreateView_ADDR, texture?.handler?.rawValue ?: 0L, descriptor?.handler?.rawValue ?: 0L).takeIf { it != 0L }?.let(::NativeAddress)?.let(::WGPUTextureView)
}

private val wgpuTextureDestroy_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureDestroy") }
actual fun wgpuTextureDestroy(texture: WGPUTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuTextureDestroy_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureGetDepthOrArrayLayers_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetDepthOrArrayLayers") }
actual fun wgpuTextureGetDepthOrArrayLayers(texture: WGPUTexture?): UInt {
    return JvmDowncallEngine.callI1P(wgpuTextureGetDepthOrArrayLayers_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetDimension_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetDimension") }
actual fun wgpuTextureGetDimension(texture: WGPUTexture?): WGPUTextureDimension {
    return (JvmDowncallEngine.callI1P(wgpuTextureGetDimension_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetFormat_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetFormat") }
actual fun wgpuTextureGetFormat(texture: WGPUTexture?): WGPUTextureFormat {
    return (JvmDowncallEngine.callI1P(wgpuTextureGetFormat_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetHeight_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetHeight") }
actual fun wgpuTextureGetHeight(texture: WGPUTexture?): UInt {
    return JvmDowncallEngine.callI1P(wgpuTextureGetHeight_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetMipLevelCount_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetMipLevelCount") }
actual fun wgpuTextureGetMipLevelCount(texture: WGPUTexture?): UInt {
    return JvmDowncallEngine.callI1P(wgpuTextureGetMipLevelCount_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetSampleCount_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetSampleCount") }
actual fun wgpuTextureGetSampleCount(texture: WGPUTexture?): UInt {
    return JvmDowncallEngine.callI1P(wgpuTextureGetSampleCount_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureGetTextureBindingViewDimension_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetTextureBindingViewDimension") }
actual fun wgpuTextureGetTextureBindingViewDimension(texture: WGPUTexture?): WGPUTextureViewDimension {
    return (JvmDowncallEngine.callI1P(wgpuTextureGetTextureBindingViewDimension_ADDR, texture?.handler?.rawValue ?: 0L).toInt()).toUInt()
}

private val wgpuTextureGetUsage_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetUsage") }
actual fun wgpuTextureGetUsage(texture: WGPUTexture?): ULong {
    return JvmDowncallEngine.callL1P(wgpuTextureGetUsage_ADDR, texture?.handler?.rawValue ?: 0L).toULong()
}

private val wgpuTextureGetWidth_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureGetWidth") }
actual fun wgpuTextureGetWidth(texture: WGPUTexture?): UInt {
    return JvmDowncallEngine.callI1P(wgpuTextureGetWidth_ADDR, texture?.handler?.rawValue ?: 0L).toInt().toUInt()
}

private val wgpuTextureSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureSetLabel") }
actual fun wgpuTextureSetLabel(texture: WGPUTexture?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuTextureSetLabel_ADDR, texture?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuTextureSetOwnershipForMemoryDump_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureSetOwnershipForMemoryDump") }
actual fun wgpuTextureSetOwnershipForMemoryDump(texture: WGPUTexture?, ownerGuid: ULong): Unit {
    JvmDowncallEngine.callGeneric(wgpuTextureSetOwnershipForMemoryDump_ADDR, JvmDowncallEngine.FunctionShape(result = JvmDowncallEngine.AbiType.Void, arguments = listOf(JvmDowncallEngine.AbiType.Pointer, JvmDowncallEngine.AbiType.I64)), texture?.handler?.rawValue ?: 0L, ownerGuid.toLong())
    return
}

private val wgpuTextureAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureAddRef") }
actual fun wgpuTextureAddRef(texture: WGPUTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuTextureAddRef_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureRelease") }
actual fun wgpuTextureRelease(texture: WGPUTexture?): Unit {
    JvmDowncallEngine.callV1P(wgpuTextureRelease_ADDR, texture?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureViewSetLabel_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureViewSetLabel") }
actual fun wgpuTextureViewSetLabel(textureView: WGPUTextureView?, label: WGPUStringView): Unit {
    JvmDowncallEngine.callStructArgWGPUStringView(wgpuTextureViewSetLabel_ADDR, textureView?.handler?.rawValue ?: 0L, label.handler.rawValue)
    return
}

private val wgpuTextureViewAddRef_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureViewAddRef") }
actual fun wgpuTextureViewAddRef(textureView: WGPUTextureView?): Unit {
    JvmDowncallEngine.callV1P(wgpuTextureViewAddRef_ADDR, textureView?.handler?.rawValue ?: 0L)
    return
}

private val wgpuTextureViewRelease_ADDR: Long by lazy { KextractNativeBootstrap.resolve("wgpuTextureViewRelease") }
actual fun wgpuTextureViewRelease(textureView: WGPUTextureView?): Unit {
    JvmDowncallEngine.callV1P(wgpuTextureViewRelease_ADDR, textureView?.handler?.rawValue ?: 0L)
    return
}

@OptIn(CallbackRuntimeApi::class)
private object WGPUCallbackTrampoline {
    val address: NativeAddress by lazy {
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "(J)V",
        )
    }
    
    @JvmStatic
    fun dispatch(
        userdata: Long,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCallbackType,
                userdata = userdata.takeIf { it != 0L }?.let(::NativeAddress),
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
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDawnStoreCacheDataFunctionTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "(JJJJJ)V",
        )
    }
    
    @JvmStatic
    fun dispatch(
        key: Long,
        keySize: Long,
        value: Long,
        valueSize: Long,
        userdata: Long,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDawnStoreCacheDataFunctionType,
                userdata = userdata.takeIf { it != 0L }?.let(::NativeAddress),
            ) { callback ->
                callback.invoke(
                    key.takeIf { it != 0L }?.let(::NativeAddress),
                    keySize.toULong(),
                    value.takeIf { it != 0L }?.let(::NativeAddress),
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
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUProcTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "()V",
        )
    }
    
    @JvmStatic
    fun dispatch(
    ) {
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUBufferMapCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUBufferMapCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUMapAsyncStatus,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUCompilationInfoCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "(IJJJ)V",
        )
    }
    
    @JvmStatic
    fun dispatch(
        status: Int,
        compilationInfo: Long,
        userdata1: Long,
        userdata2: Long,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCompilationInfoCallbackType,
                userdata = userdata2.takeIf { it != 0L }?.let(::NativeAddress),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCompilationInfoRequestStatus,
                    compilationInfo.takeIf { it != 0L }?.let(::NativeAddress),
                    userdata1.takeIf { it != 0L }?.let(::NativeAddress),
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUCreateComputePipelineAsyncCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        pipeline: MemorySegment,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCreateComputePipelineAsyncCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCreatePipelineAsyncStatus,
                    pipeline.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) }?.let { WGPUComputePipeline(it) },
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUCreateRenderPipelineAsyncCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        pipeline: MemorySegment,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUCreateRenderPipelineAsyncCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCreatePipelineAsyncStatus,
                    pipeline.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) }?.let { WGPURenderPipeline(it) },
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDawnStoreCacheDataCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "(JJJJJJ)V",
        )
    }
    
    @JvmStatic
    fun dispatch(
        keySize: Long,
        key: Long,
        valueSize: Long,
        value: Long,
        userdata1: Long,
        userdata2: Long,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDawnStoreCacheDataCallbackType,
                userdata = userdata2.takeIf { it != 0L }?.let(::NativeAddress),
            ) { callback ->
                callback.invoke(
                    keySize.toULong(),
                    key.takeIf { it != 0L }?.let(::NativeAddress),
                    valueSize.toULong(),
                    value.takeIf { it != 0L }?.let(::NativeAddress),
                    userdata1.takeIf { it != 0L }?.let(::NativeAddress),
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUDeviceLostCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        device: MemorySegment,
        reason: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDeviceLostCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    device.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
                    reason.toUInt() as WGPUDeviceLostReason,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
        JvmUpcallEngine.allocateTrampoline(
            dispatcherClass = WGPUDisposeCallbackTrampoline::class.java,
            dispatchMethod = "dispatch",
            dispatchSig = "(IJJ)V",
        )
    }
    
    @JvmStatic
    fun dispatch(
        status: Int,
        userdata1: Long,
        userdata2: Long,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUDisposeCallbackType,
                userdata = userdata2.takeIf { it != 0L }?.let(::NativeAddress),
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUCallbackStatus,
                    userdata1.takeIf { it != 0L }?.let(::NativeAddress),
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPULoggingCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        type: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPULoggingCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    type.toUInt() as WGPULoggingType,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUPopErrorScopeCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        type: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUPopErrorScopeCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUPopErrorScopeStatus,
                    type.toUInt() as WGPUErrorType,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUQueueWorkDoneCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUQueueWorkDoneCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPUQueueWorkDoneStatus,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPURequestAdapterCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        adapter: MemorySegment,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPURequestAdapterCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPURequestAdapterStatus,
                    adapter.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) }?.let { WGPUAdapter(it) },
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.JAVA_INT, ValueLayout.ADDRESS, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPURequestDeviceCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        status: Int,
        device: MemorySegment,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPURequestDeviceCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    status.toUInt() as WGPURequestDeviceStatus,
                    device.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) }?.let { WGPUDevice(it) },
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
    private val descriptor: FunctionDescriptor = FunctionDescriptor.ofVoid(ValueLayout.ADDRESS, ValueLayout.JAVA_INT, JvmDowncallEngine.structLayout("WGPUStringView"), ValueLayout.ADDRESS, ValueLayout.ADDRESS)
    private val methodHandle: MethodHandle by lazy {
        MethodHandles.lookup().findStatic(
            WGPUUncapturedErrorCallbackTrampoline::class.java,
            "invoke",
            descriptor.toMethodType(),
        )
    }
    val address: NativeAddress by lazy {
        NativeAddress(Linker.nativeLinker().upcallStub(methodHandle, descriptor, Arena.global()).address())
    }
    
    @JvmStatic
    private fun invoke(
        device: MemorySegment,
        type: Int,
        message: MemorySegment,
        userdata1: MemorySegment,
        userdata2: MemorySegment,
    ) {
        try {
            CallbackRuntime.dispatchSafely(
                type = WGPUUncapturedErrorCallbackType,
                userdata = userdata2.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
            ) { callback ->
                callback.invoke(
                    device.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
                    type.toUInt() as WGPUErrorType,
                    WGPUStringView(NativeAddress(message.address())),
                    userdata1.takeIf { it != MemorySegment.NULL }?.let { NativeAddress(it.address()) },
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
// Layouts des structs par valeur : enregistrés au chargement du fichier
// (classe façade), donc avant tout downcall — les companions de structs
// imbriqués ne sont pas garantis initialisés à ce moment.
// Les listes de champs sont découpées en fonctions : un bloc unique dépasse
// la limite JVM de 64 Ko par méthode pour les gros en-têtes (structs opaques).
private fun __kffiJvmStructFields0_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("data", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("length", JvmDowncallEngine.FieldKind.UINT64, 8L),
)
private fun __kffiJvmRegisterStructLayout0() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUStringView",
        16L, 8L,
        __kffiJvmStructFields0_0(),
    )
}
private fun __kffiJvmStructFields1_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("next", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("sType", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout1() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUChainedStruct",
        16L, 8L,
        __kffiJvmStructFields1_0(),
    )
}
private fun __kffiJvmStructFields2_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout2() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBufferMapCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields2_0(),
    )
}
private fun __kffiJvmStructFields3_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout3() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCompilationInfoCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields3_0(),
    )
}
private fun __kffiJvmStructFields4_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout4() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCreateComputePipelineAsyncCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields4_0(),
    )
}
private fun __kffiJvmStructFields5_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout5() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCreateRenderPipelineAsyncCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields5_0(),
    )
}
private fun __kffiJvmStructFields6_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout6() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnLoadCacheDataCallbackInfo",
        32L, 8L,
        __kffiJvmStructFields6_0(),
    )
}
private fun __kffiJvmStructFields7_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout7() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnStoreCacheDataCallbackInfo",
        32L, 8L,
        __kffiJvmStructFields7_0(),
    )
}
private fun __kffiJvmStructFields8_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout8() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDeviceLostCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields8_0(),
    )
}
private fun __kffiJvmStructFields9_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout9() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDisposeCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields9_0(),
    )
}
private fun __kffiJvmStructFields10_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout10() {
    JvmDowncallEngine.registerStructLayout(
        "WGPULoggingCallbackInfo",
        32L, 8L,
        __kffiJvmStructFields10_0(),
    )
}
private fun __kffiJvmStructFields11_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout11() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPopErrorScopeCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields11_0(),
    )
}
private fun __kffiJvmStructFields12_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout12() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUQueueWorkDoneCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields12_0(),
    )
}
private fun __kffiJvmStructFields13_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout13() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURequestAdapterCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields13_0(),
    )
}
private fun __kffiJvmStructFields14_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout14() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURequestDeviceCallbackInfo",
        40L, 8L,
        __kffiJvmStructFields14_0(),
    )
}
private fun __kffiJvmStructFields15_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("callback", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("userdata1", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("userdata2", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout15() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUUncapturedErrorCallbackInfo",
        32L, 8L,
        __kffiJvmStructFields15_0(),
    )
}
private fun __kffiJvmStructFields16_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("shaderModel", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("adapterLUIDLowPart", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("adapterLUIDHighPart", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout16() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesD3D",
        32L, 8L,
        __kffiJvmStructFields16_0(),
    )
}
private fun __kffiJvmStructFields17_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("hasPrimary", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("hasRender", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("primaryMajor", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("primaryMinor", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("renderMajor", JvmDowncallEngine.FieldKind.UINT64, 40L),
    JvmDowncallEngine.StructField("renderMinor", JvmDowncallEngine.FieldKind.UINT64, 48L),
)
private fun __kffiJvmRegisterStructLayout17() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesDrm",
        56L, 8L,
        __kffiJvmStructFields17_0(),
    )
}
private fun __kffiJvmStructFields18_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("driverVersion", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout18() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesVk",
        24L, 8L,
        __kffiJvmStructFields18_0(),
    )
}
private fun __kffiJvmStructFields19_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("backendType", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout19() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesWGPU",
        24L, 8L,
        __kffiJvmStructFields19_0(),
    )
}
private fun __kffiJvmStructFields20_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("buffer", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("sampler", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("textureView", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout20() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBindingResource",
        48L, 8L,
        __kffiJvmStructFields20_0(),
    )
}
private fun __kffiJvmStructFields21_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("operation", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("srcFactor", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("dstFactor", JvmDowncallEngine.FieldKind.UINT32, 8L),
)
private fun __kffiJvmRegisterStructLayout21() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBlendComponent",
        12L, 4L,
        __kffiJvmStructFields21_0(),
    )
}
private fun __kffiJvmStructFields22_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("type", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("hasDynamicOffset", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("minBindingSize", JvmDowncallEngine.FieldKind.UINT64, 16L),
)
private fun __kffiJvmRegisterStructLayout22() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBufferBindingLayout",
        24L, 8L,
        __kffiJvmStructFields22_0(),
    )
}
private fun __kffiJvmStructFields23_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("pointer", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("disposeCallback", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("userdata", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout23() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBufferHostMappedPointer",
        40L, 8L,
        __kffiJvmStructFields23_0(),
    )
}
private fun __kffiJvmStructFields24_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("r", JvmDowncallEngine.FieldKind.FLOAT64, 0L),
    JvmDowncallEngine.StructField("g", JvmDowncallEngine.FieldKind.FLOAT64, 8L),
    JvmDowncallEngine.StructField("b", JvmDowncallEngine.FieldKind.FLOAT64, 16L),
    JvmDowncallEngine.StructField("a", JvmDowncallEngine.FieldKind.FLOAT64, 24L),
)
private fun __kffiJvmRegisterStructLayout24() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUColor",
        32L, 8L,
        __kffiJvmStructFields24_0(),
    )
}
private fun __kffiJvmStructFields25_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("primaries", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("transfer", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("yCbCrRange", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("yCbCrMatrix", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("hdrReferenceWhiteLuminance", JvmDowncallEngine.FieldKind.FLOAT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout25() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUColorSpaceDawn",
        32L, 8L,
        __kffiJvmStructFields25_0(),
    )
}
private fun __kffiJvmStructFields26_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("enabled", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout26() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUColorTargetStateExpandResolveTextureDawn",
        24L, 8L,
        __kffiJvmStructFields26_0(),
    )
}
private fun __kffiJvmStructFields27_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout27() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCommandBufferDescriptor",
        24L, 8L,
        __kffiJvmStructFields27_0(),
    )
}
private fun __kffiJvmStructFields28_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("maxStorageBuffersInVertexStage", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("maxStorageTexturesInVertexStage", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("maxStorageBuffersInFragmentStage", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("maxStorageTexturesInFragmentStage", JvmDowncallEngine.FieldKind.UINT32, 28L),
)
private fun __kffiJvmRegisterStructLayout28() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCompatibilityModeLimits",
        32L, 8L,
        __kffiJvmStructFields28_0(),
    )
}
private fun __kffiJvmStructFields29_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("value", JvmDowncallEngine.FieldKind.FLOAT64, 24L),
)
private fun __kffiJvmRegisterStructLayout29() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUConstantEntry",
        32L, 8L,
        __kffiJvmStructFields29_0(),
    )
}
private fun __kffiJvmStructFields30_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("flipY", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("needsColorSpaceConversion", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("srcAlphaMode", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("srcTransferFunctionParameters", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("conversionMatrix", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("dstTransferFunctionParameters", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("dstAlphaMode", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("internalUsage", JvmDowncallEngine.FieldKind.UINT32, 52L),
)
private fun __kffiJvmRegisterStructLayout30() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCopyTextureForBrowserOptions",
        56L, 8L,
        __kffiJvmStructFields30_0(),
    )
}
private fun __kffiJvmStructFields31_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("powerPreference", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout31() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnAdapterPropertiesPowerPreference",
        24L, 8L,
        __kffiJvmStructFields31_0(),
    )
}
private fun __kffiJvmStructFields32_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("outOfMemory", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout32() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnBufferDescriptorErrorInfoFromWireClient",
        24L, 8L,
        __kffiJvmStructFields32_0(),
    )
}
private fun __kffiJvmStructFields33_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("WGPUDawnLoadCacheDataCallbackInfo", JvmDowncallEngine.FieldKind.STRUCT, 32L),
    JvmDowncallEngine.StructField("WGPUDawnStoreCacheDataCallbackInfo", JvmDowncallEngine.FieldKind.STRUCT, 64L),
)
private fun __kffiJvmRegisterStructLayout33() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnCacheDeviceDescriptor",
        96L, 8L,
        __kffiJvmStructFields33_0(),
    )
}
private fun __kffiJvmStructFields34_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("linePos", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("length", JvmDowncallEngine.FieldKind.UINT64, 32L),
)
private fun __kffiJvmRegisterStructLayout34() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnCompilationMessageUtf16",
        40L, 8L,
        __kffiJvmStructFields34_0(),
    )
}
private fun __kffiJvmStructFields35_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("consumeAdapter", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout35() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnConsumeAdapterDescriptor",
        24L, 8L,
        __kffiJvmStructFields35_0(),
    )
}
private fun __kffiJvmStructFields36_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("allocatorHeapBlockSize", JvmDowncallEngine.FieldKind.UINT64, 16L),
)
private fun __kffiJvmRegisterStructLayout36() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnDeviceAllocatorControl",
        24L, 8L,
        __kffiJvmStructFields36_0(),
    )
}
private fun __kffiJvmStructFields37_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("modifier", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("modifierPlaneCount", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout37() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnDrmFormatProperties",
        16L, 8L,
        __kffiJvmStructFields37_0(),
    )
}
private fun __kffiJvmStructFields38_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("useInternalUsages", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout38() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnEncoderInternalUsageDescriptor",
        24L, 8L,
        __kffiJvmStructFields38_0(),
    )
}
private fun __kffiJvmStructFields39_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("fakeOOMAtWireClientMap", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("fakeOOMAtNativeMap", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("fakeOOMAtDevice", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout39() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnFakeBufferOOMForTesting",
        32L, 8L,
        __kffiJvmStructFields39_0(),
    )
}
private fun __kffiJvmStructFields40_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
)
private fun __kffiJvmRegisterStructLayout40() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnFakeDeviceInitializeErrorForTesting",
        16L, 8L,
        __kffiJvmStructFields40_0(),
    )
}
private fun __kffiJvmStructFields41_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("hostMappedPointerAlignment", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout41() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnHostMappedPointerLimits",
        24L, 8L,
        __kffiJvmStructFields41_0(),
    )
}
private fun __kffiJvmStructFields42_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("invalidSType", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout42() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnInjectedInvalidSType",
        24L, 8L,
        __kffiJvmStructFields42_0(),
    )
}
private fun __kffiJvmStructFields43_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sampleCount", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout43() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnRenderPassSampleCount",
        24L, 8L,
        __kffiJvmStructFields43_0(),
    )
}
private fun __kffiJvmStructFields44_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("allowNonUniformDerivatives", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout44() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnShaderModuleSPIRVOptionsDescriptor",
        24L, 8L,
        __kffiJvmStructFields44_0(),
    )
}
private fun __kffiJvmStructFields45_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("codeSize", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("code", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout45() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnShaderSourceSPIRV",
        32L, 8L,
        __kffiJvmStructFields45_0(),
    )
}
private fun __kffiJvmStructFields46_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("minTexelCopyBufferRowAlignment", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout46() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnTexelCopyBufferRowAlignmentLimits",
        24L, 8L,
        __kffiJvmStructFields46_0(),
    )
}
private fun __kffiJvmStructFields47_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("internalUsage", JvmDowncallEngine.FieldKind.UINT64, 16L),
)
private fun __kffiJvmRegisterStructLayout47() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnTextureInternalUsageDescriptor",
        24L, 8L,
        __kffiJvmStructFields47_0(),
    )
}
private fun __kffiJvmStructFields48_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("enabledToggleCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("enabledToggles", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("disabledToggleCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("disabledToggles", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout48() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnTogglesDescriptor",
        48L, 8L,
        __kffiJvmStructFields48_0(),
    )
}
private fun __kffiJvmStructFields49_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("blocklistedFeatureCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("blocklistedFeatures", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout49() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnWGSLBlocklist",
        32L, 8L,
        __kffiJvmStructFields49_0(),
    )
}
private fun __kffiJvmStructFields50_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("enableExperimental", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("enableUnsafe", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("enableTesting", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout50() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnWireWGSLControl",
        32L, 8L,
        __kffiJvmStructFields50_0(),
    )
}
private fun __kffiJvmStructFields51_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
)
private fun __kffiJvmRegisterStructLayout51() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUEmscriptenSurfaceSourceCanvasHTMLSelector",
        32L, 8L,
        __kffiJvmStructFields51_0(),
    )
}
private fun __kffiJvmStructFields52_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("width", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("height", JvmDowncallEngine.FieldKind.UINT32, 4L),
)
private fun __kffiJvmRegisterStructLayout52() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUExtent2D",
        8L, 4L,
        __kffiJvmStructFields52_0(),
    )
}
private fun __kffiJvmStructFields53_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("width", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("height", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("depthOrArrayLayers", JvmDowncallEngine.FieldKind.UINT32, 8L),
)
private fun __kffiJvmRegisterStructLayout53() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUExtent3D",
        12L, 4L,
        __kffiJvmStructFields53_0(),
    )
}
private fun __kffiJvmStructFields54_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("externalTexture", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout54() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUExternalTextureBindingEntry",
        24L, 8L,
        __kffiJvmStructFields54_0(),
    )
}
private fun __kffiJvmStructFields55_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
)
private fun __kffiJvmRegisterStructLayout55() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUExternalTextureBindingLayout",
        16L, 8L,
        __kffiJvmStructFields55_0(),
    )
}
private fun __kffiJvmStructFields56_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("id", JvmDowncallEngine.FieldKind.UINT64, 0L),
)
private fun __kffiJvmRegisterStructLayout56() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUFuture",
        8L, 8L,
        __kffiJvmStructFields56_0(),
    )
}
private fun __kffiJvmStructFields57_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("timedWaitAnyMaxCount", JvmDowncallEngine.FieldKind.UINT64, 8L),
)
private fun __kffiJvmRegisterStructLayout57() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUInstanceLimits",
        16L, 8L,
        __kffiJvmStructFields57_0(),
    )
}
private fun __kffiJvmStructFields58_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("unused", JvmDowncallEngine.FieldKind.UINT32, 0L),
)
private fun __kffiJvmRegisterStructLayout58() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUINTERNAL_HAVE_EMDAWNWEBGPU_HEADER",
        4L, 4L,
        __kffiJvmStructFields58_0(),
    )
}
private fun __kffiJvmStructFields59_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("properties", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 8L),
)
private fun __kffiJvmRegisterStructLayout59() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUMemoryHeapInfo",
        16L, 8L,
        __kffiJvmStructFields59_0(),
    )
}
private fun __kffiJvmStructFields60_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("count", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("mask", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("alphaToCoverageEnabled", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout60() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUMultisampleState",
        24L, 8L,
        __kffiJvmStructFields60_0(),
    )
}
private fun __kffiJvmStructFields61_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("x", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("y", JvmDowncallEngine.FieldKind.UINT32, 4L),
)
private fun __kffiJvmRegisterStructLayout61() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUOrigin2D",
        8L, 4L,
        __kffiJvmStructFields61_0(),
    )
}
private fun __kffiJvmStructFields62_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("x", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("y", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("z", JvmDowncallEngine.FieldKind.UINT32, 8L),
)
private fun __kffiJvmRegisterStructLayout62() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUOrigin3D",
        12L, 4L,
        __kffiJvmStructFields62_0(),
    )
}
private fun __kffiJvmStructFields63_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("querySet", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("beginningOfPassWriteIndex", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("endOfPassWriteIndex", JvmDowncallEngine.FieldKind.UINT32, 20L),
)
private fun __kffiJvmRegisterStructLayout63() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPassTimestampWrites",
        24L, 8L,
        __kffiJvmStructFields63_0(),
    )
}
private fun __kffiJvmStructFields64_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("usesResourceTable", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout64() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPipelineLayoutResourceTable",
        24L, 8L,
        __kffiJvmStructFields64_0(),
    )
}
private fun __kffiJvmStructFields65_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout65() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPipelineLayoutStorageAttachment",
        24L, 8L,
        __kffiJvmStructFields65_0(),
    )
}
private fun __kffiJvmStructFields66_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("topology", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("stripIndexFormat", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("frontFace", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("cullMode", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("unclippedDepth", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout66() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPrimitiveState",
        32L, 8L,
        __kffiJvmStructFields66_0(),
    )
}
private fun __kffiJvmStructFields67_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("type", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("count", JvmDowncallEngine.FieldKind.UINT32, 28L),
)
private fun __kffiJvmRegisterStructLayout67() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUQuerySetDescriptor",
        32L, 8L,
        __kffiJvmStructFields67_0(),
    )
}
private fun __kffiJvmStructFields68_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout68() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUQueueDescriptor",
        24L, 8L,
        __kffiJvmStructFields68_0(),
    )
}
private fun __kffiJvmStructFields69_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout69() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderBundleDescriptor",
        24L, 8L,
        __kffiJvmStructFields69_0(),
    )
}
private fun __kffiJvmStructFields70_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("usesResourceTable", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout70() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderBundleEncoderResourceTable",
        24L, 8L,
        __kffiJvmStructFields70_0(),
    )
}
private fun __kffiJvmStructFields71_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("view", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("depthLoadOp", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("depthStoreOp", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("depthClearValue", JvmDowncallEngine.FieldKind.FLOAT32, 24L),
    JvmDowncallEngine.StructField("depthReadOnly", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("stencilLoadOp", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("stencilStoreOp", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("stencilClearValue", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("stencilReadOnly", JvmDowncallEngine.FieldKind.UINT32, 44L),
)
private fun __kffiJvmRegisterStructLayout71() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassDepthStencilAttachment",
        48L, 8L,
        __kffiJvmStructFields71_0(),
    )
}
private fun __kffiJvmStructFields72_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("colorOffsetX", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("colorOffsetY", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("resolveOffsetX", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("resolveOffsetY", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("width", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("height", JvmDowncallEngine.FieldKind.UINT32, 36L),
)
private fun __kffiJvmRegisterStructLayout72() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassDescriptorResolveRect",
        40L, 8L,
        __kffiJvmStructFields72_0(),
    )
}
private fun __kffiJvmStructFields73_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("maxDrawCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
)
private fun __kffiJvmRegisterStructLayout73() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassMaxDrawCount",
        24L, 8L,
        __kffiJvmStructFields73_0(),
    )
}
private fun __kffiJvmStructFields74_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
)
private fun __kffiJvmRegisterStructLayout74() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURequestAdapterWebGPUBackendOptions",
        16L, 8L,
        __kffiJvmStructFields74_0(),
    )
}
private fun __kffiJvmStructFields75_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("xrCompatible", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout75() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURequestAdapterWebXROptions",
        24L, 8L,
        __kffiJvmStructFields75_0(),
    )
}
private fun __kffiJvmStructFields76_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout76() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUResourceTableDescriptor",
        32L, 8L,
        __kffiJvmStructFields76_0(),
    )
}
private fun __kffiJvmStructFields77_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("type", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout77() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSamplerBindingLayout",
        16L, 8L,
        __kffiJvmStructFields77_0(),
    )
}
private fun __kffiJvmStructFields78_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("strictMath", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout78() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUShaderModuleCompilationOptions",
        24L, 8L,
        __kffiJvmStructFields78_0(),
    )
}
private fun __kffiJvmStructFields79_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("codeSize", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("code", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout79() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUShaderSourceSPIRV",
        32L, 8L,
        __kffiJvmStructFields79_0(),
    )
}
private fun __kffiJvmStructFields80_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
)
private fun __kffiJvmRegisterStructLayout80() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUShaderSourceWGSL",
        32L, 8L,
        __kffiJvmStructFields80_0(),
    )
}
private fun __kffiJvmStructFields81_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("initialized", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("fenceCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("fences", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("signaledValueCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("signaledValues", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout81() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryBeginAccessDescriptor",
        48L, 8L,
        __kffiJvmStructFields81_0(),
    )
}
private fun __kffiJvmStructFields82_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("initialized", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("fenceCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("fences", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("signaledValueCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("signaledValues", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout82() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryEndAccessState",
        48L, 8L,
        __kffiJvmStructFields82_0(),
    )
}
private fun __kffiJvmStructFields83_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 24L),
)
private fun __kffiJvmRegisterStructLayout83() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryFromWindowsHandleDescriptor",
        32L, 8L,
        __kffiJvmStructFields83_0(),
    )
}
private fun __kffiJvmStructFields84_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("pointer", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("WGPUDisposeCallbackInfo", JvmDowncallEngine.FieldKind.STRUCT, 32L),
)
private fun __kffiJvmRegisterStructLayout84() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryHostPointerDescriptor",
        72L, 8L,
        __kffiJvmStructFields84_0(),
    )
}
private fun __kffiJvmStructFields85_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 16L),
)
private fun __kffiJvmRegisterStructLayout85() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryProperties",
        24L, 8L,
        __kffiJvmStructFields85_0(),
    )
}
private fun __kffiJvmStructFields86_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout86() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceDXGISharedHandleDescriptor",
        24L, 8L,
        __kffiJvmStructFields86_0(),
    )
}
private fun __kffiJvmStructFields87_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout87() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceDXGISharedHandleExportInfo",
        24L, 8L,
        __kffiJvmStructFields87_0(),
    )
}
private fun __kffiJvmStructFields88_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sync", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout88() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceEGLSyncDescriptor",
        24L, 8L,
        __kffiJvmStructFields88_0(),
    )
}
private fun __kffiJvmStructFields89_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sync", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout89() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceEGLSyncExportInfo",
        24L, 8L,
        __kffiJvmStructFields89_0(),
    )
}
private fun __kffiJvmStructFields90_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sharedEvent", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout90() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceMTLSharedEventDescriptor",
        24L, 8L,
        __kffiJvmStructFields90_0(),
    )
}
private fun __kffiJvmStructFields91_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sharedEvent", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout91() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceMTLSharedEventExportInfo",
        24L, 8L,
        __kffiJvmStructFields91_0(),
    )
}
private fun __kffiJvmStructFields92_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout92() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceSyncFDDescriptor",
        24L, 8L,
        __kffiJvmStructFields92_0(),
    )
}
private fun __kffiJvmStructFields93_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout93() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceSyncFDExportInfo",
        24L, 8L,
        __kffiJvmStructFields93_0(),
    )
}
private fun __kffiJvmStructFields94_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout94() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceVkSemaphoreOpaqueFDDescriptor",
        24L, 8L,
        __kffiJvmStructFields94_0(),
    )
}
private fun __kffiJvmStructFields95_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout95() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceVkSemaphoreOpaqueFDExportInfo",
        24L, 8L,
        __kffiJvmStructFields95_0(),
    )
}
private fun __kffiJvmStructFields96_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout96() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceVkSemaphoreZirconHandleDescriptor",
        24L, 8L,
        __kffiJvmStructFields96_0(),
    )
}
private fun __kffiJvmStructFields97_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout97() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceVkSemaphoreZirconHandleExportInfo",
        24L, 8L,
        __kffiJvmStructFields97_0(),
    )
}
private fun __kffiJvmStructFields98_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout98() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryAHardwareBufferDescriptor",
        24L, 8L,
        __kffiJvmStructFields98_0(),
    )
}
private fun __kffiJvmStructFields99_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("requiresEndAccessFence", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout99() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryD3D11BeginState",
        24L, 8L,
        __kffiJvmStructFields99_0(),
    )
}
private fun __kffiJvmStructFields100_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("isSwapchain", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout100() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryD3DSwapchainBeginState",
        24L, 8L,
        __kffiJvmStructFields100_0(),
    )
}
private fun __kffiJvmStructFields101_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("fd", JvmDowncallEngine.FieldKind.INT32, 0L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("stride", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout101() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryDmaBufPlane",
        24L, 8L,
        __kffiJvmStructFields101_0(),
    )
}
private fun __kffiJvmStructFields102_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("handle", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("useKeyedMutex", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout102() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryDXGISharedHandleDescriptor",
        32L, 8L,
        __kffiJvmStructFields102_0(),
    )
}
private fun __kffiJvmStructFields103_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("image", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout103() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryEGLImageDescriptor",
        24L, 8L,
        __kffiJvmStructFields103_0(),
    )
}
private fun __kffiJvmStructFields104_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("ioSurface", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("allowStorageBinding", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout104() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryIOSurfaceDescriptor",
        32L, 8L,
        __kffiJvmStructFields104_0(),
    )
}
private fun __kffiJvmStructFields105_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("vkImageCreateInfo", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("memoryFD", JvmDowncallEngine.FieldKind.INT32, 24L),
    JvmDowncallEngine.StructField("memoryTypeIndex", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("allocationSize", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("dedicatedAllocation", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout105() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryOpaqueFDDescriptor",
        48L, 8L,
        __kffiJvmStructFields105_0(),
    )
}
private fun __kffiJvmStructFields106_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("dedicatedAllocation", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout106() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryVkDedicatedAllocationDescriptor",
        24L, 8L,
        __kffiJvmStructFields106_0(),
    )
}
private fun __kffiJvmStructFields107_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("oldLayout", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("newLayout", JvmDowncallEngine.FieldKind.INT32, 20L),
)
private fun __kffiJvmRegisterStructLayout107() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryVkImageLayoutBeginState",
        24L, 8L,
        __kffiJvmStructFields107_0(),
    )
}
private fun __kffiJvmStructFields108_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("oldLayout", JvmDowncallEngine.FieldKind.INT32, 16L),
    JvmDowncallEngine.StructField("newLayout", JvmDowncallEngine.FieldKind.INT32, 20L),
)
private fun __kffiJvmRegisterStructLayout108() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryVkImageLayoutEndState",
        24L, 8L,
        __kffiJvmStructFields108_0(),
    )
}
private fun __kffiJvmStructFields109_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("memoryFD", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("allocationSize", JvmDowncallEngine.FieldKind.UINT64, 24L),
)
private fun __kffiJvmRegisterStructLayout109() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryZirconHandleDescriptor",
        32L, 8L,
        __kffiJvmStructFields109_0(),
    )
}
private fun __kffiJvmStructFields110_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("sampler", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("sampledTextureBinding", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout110() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUStaticSamplerBindingLayout",
        32L, 8L,
        __kffiJvmStructFields110_0(),
    )
}
private fun __kffiJvmStructFields111_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("compare", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("failOp", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("depthFailOp", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("passOp", JvmDowncallEngine.FieldKind.UINT32, 12L),
)
private fun __kffiJvmRegisterStructLayout111() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUStencilFaceState",
        16L, 4L,
        __kffiJvmStructFields111_0(),
    )
}
private fun __kffiJvmStructFields112_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("access", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("viewDimension", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout112() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUStorageTextureBindingLayout",
        24L, 8L,
        __kffiJvmStructFields112_0(),
    )
}
private fun __kffiJvmStructFields113_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("componentType", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("resultComponentType", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("M", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("N", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("K", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("minSubgroupSize", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("maxSubgroupSize", JvmDowncallEngine.FieldKind.UINT32, 24L),
)
private fun __kffiJvmRegisterStructLayout113() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSubgroupMatrixConfig",
        28L, 4L,
        __kffiJvmStructFields113_0(),
    )
}
private fun __kffiJvmStructFields114_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("featureCount", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("features", JvmDowncallEngine.FieldKind.POINTER, 8L),
)
private fun __kffiJvmRegisterStructLayout114() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSupportedFeatures",
        16L, 8L,
        __kffiJvmStructFields114_0(),
    )
}
private fun __kffiJvmStructFields115_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("featureCount", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("features", JvmDowncallEngine.FieldKind.POINTER, 8L),
)
private fun __kffiJvmRegisterStructLayout115() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSupportedInstanceFeatures",
        16L, 8L,
        __kffiJvmStructFields115_0(),
    )
}
private fun __kffiJvmStructFields116_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("featureCount", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("features", JvmDowncallEngine.FieldKind.POINTER, 8L),
)
private fun __kffiJvmRegisterStructLayout116() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSupportedWGSLLanguageFeatures",
        16L, 8L,
        __kffiJvmStructFields116_0(),
    )
}
private fun __kffiJvmStructFields117_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("usages", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("formatCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("formats", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("presentModeCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("presentModes", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("alphaModeCount", JvmDowncallEngine.FieldKind.UINT64, 48L),
    JvmDowncallEngine.StructField("alphaModes", JvmDowncallEngine.FieldKind.POINTER, 56L),
)
private fun __kffiJvmRegisterStructLayout117() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceCapabilities",
        64L, 8L,
        __kffiJvmStructFields117_0(),
    )
}
private fun __kffiJvmStructFields118_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("colorSpace", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("toneMappingMode", JvmDowncallEngine.FieldKind.UINT32, 20L),
)
private fun __kffiJvmRegisterStructLayout118() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceColorManagement",
        24L, 8L,
        __kffiJvmStructFields118_0(),
    )
}
private fun __kffiJvmStructFields119_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("device", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("width", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("height", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("viewFormatCount", JvmDowncallEngine.FieldKind.UINT64, 40L),
    JvmDowncallEngine.StructField("viewFormats", JvmDowncallEngine.FieldKind.POINTER, 48L),
    JvmDowncallEngine.StructField("alphaMode", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("presentMode", JvmDowncallEngine.FieldKind.UINT32, 60L),
)
private fun __kffiJvmRegisterStructLayout119() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceConfiguration",
        64L, 8L,
        __kffiJvmStructFields119_0(),
    )
}
private fun __kffiJvmStructFields120_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("coreWindow", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout120() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceDescriptorFromWindowsCoreWindow",
        24L, 8L,
        __kffiJvmStructFields120_0(),
    )
}
private fun __kffiJvmStructFields121_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("swapChainPanel", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout121() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceDescriptorFromWindowsUWPSwapChainPanel",
        24L, 8L,
        __kffiJvmStructFields121_0(),
    )
}
private fun __kffiJvmStructFields122_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("swapChainPanel", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout122() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceDescriptorFromWindowsWinUISwapChainPanel",
        24L, 8L,
        __kffiJvmStructFields122_0(),
    )
}
private fun __kffiJvmStructFields123_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("window", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout123() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceAndroidNativeWindow",
        24L, 8L,
        __kffiJvmStructFields123_0(),
    )
}
private fun __kffiJvmStructFields124_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("layer", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout124() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceMetalLayer",
        24L, 8L,
        __kffiJvmStructFields124_0(),
    )
}
private fun __kffiJvmStructFields125_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("display", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("surface", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout125() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceWaylandSurface",
        32L, 8L,
        __kffiJvmStructFields125_0(),
    )
}
private fun __kffiJvmStructFields126_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("hinstance", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("hwnd", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout126() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceWindowsHWND",
        32L, 8L,
        __kffiJvmStructFields126_0(),
    )
}
private fun __kffiJvmStructFields127_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("connection", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("window", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout127() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceXCBWindow",
        32L, 8L,
        __kffiJvmStructFields127_0(),
    )
}
private fun __kffiJvmStructFields128_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("display", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("window", JvmDowncallEngine.FieldKind.UINT64, 24L),
)
private fun __kffiJvmRegisterStructLayout128() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceSourceXlibWindow",
        32L, 8L,
        __kffiJvmStructFields128_0(),
    )
}
private fun __kffiJvmStructFields129_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("texture", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("status", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout129() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceTexture",
        24L, 8L,
        __kffiJvmStructFields129_0(),
    )
}
private fun __kffiJvmStructFields130_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("texelBufferView", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout130() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelBufferBindingEntry",
        24L, 8L,
        __kffiJvmStructFields130_0(),
    )
}
private fun __kffiJvmStructFields131_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("access", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 20L),
)
private fun __kffiJvmRegisterStructLayout131() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelBufferBindingLayout",
        24L, 8L,
        __kffiJvmStructFields131_0(),
    )
}
private fun __kffiJvmStructFields132_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 40L),
)
private fun __kffiJvmRegisterStructLayout132() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelBufferViewDescriptor",
        48L, 8L,
        __kffiJvmStructFields132_0(),
    )
}
private fun __kffiJvmStructFields133_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 0L),
    JvmDowncallEngine.StructField("bytesPerRow", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("rowsPerImage", JvmDowncallEngine.FieldKind.UINT32, 12L),
)
private fun __kffiJvmRegisterStructLayout133() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelCopyBufferLayout",
        16L, 8L,
        __kffiJvmStructFields133_0(),
    )
}
private fun __kffiJvmStructFields134_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("sampleType", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("viewDimension", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("multisampled", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout134() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureBindingLayout",
        24L, 8L,
        __kffiJvmStructFields134_0(),
    )
}
private fun __kffiJvmStructFields135_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("textureBindingViewDimension", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout135() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureBindingViewDimension",
        24L, 8L,
        __kffiJvmStructFields135_0(),
    )
}
private fun __kffiJvmStructFields136_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("r", JvmDowncallEngine.FieldKind.UINT32, 0L),
    JvmDowncallEngine.StructField("g", JvmDowncallEngine.FieldKind.UINT32, 4L),
    JvmDowncallEngine.StructField("b", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("a", JvmDowncallEngine.FieldKind.UINT32, 12L),
)
private fun __kffiJvmRegisterStructLayout136() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureComponentSwizzle",
        16L, 4L,
        __kffiJvmStructFields136_0(),
    )
}
private fun __kffiJvmStructFields137_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("shaderLocation", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout137() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUVertexAttribute",
        32L, 8L,
        __kffiJvmStructFields137_0(),
    )
}
private fun __kffiJvmStructFields138_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("vkFormat", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("vkYCbCrModel", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("vkYCbCrRange", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("vkComponentSwizzleRed", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("vkComponentSwizzleGreen", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("vkComponentSwizzleBlue", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("vkComponentSwizzleAlpha", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("vkXChromaOffset", JvmDowncallEngine.FieldKind.UINT32, 44L),
    JvmDowncallEngine.StructField("vkYChromaOffset", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("vkChromaFilter", JvmDowncallEngine.FieldKind.UINT32, 52L),
    JvmDowncallEngine.StructField("forceExplicitReconstruction", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("externalFormat", JvmDowncallEngine.FieldKind.UINT64, 64L),
)
private fun __kffiJvmRegisterStructLayout138() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUYCbCrVkDescriptor",
        72L, 8L,
        __kffiJvmStructFields138_0(),
    )
}
private fun __kffiJvmStructFields139_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("heapCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("heapInfo", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout139() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesMemoryHeaps",
        32L, 8L,
        __kffiJvmStructFields139_0(),
    )
}
private fun __kffiJvmStructFields140_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("configCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("configs", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout140() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterPropertiesSubgroupMatrixConfigs",
        32L, 8L,
        __kffiJvmStructFields140_0(),
    )
}
private fun __kffiJvmStructFields141_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUYCbCrVkDescriptor", JvmDowncallEngine.FieldKind.STRUCT, 0L),
)
private fun __kffiJvmRegisterStructLayout141() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAHardwareBufferProperties",
        72L, 8L,
        __kffiJvmStructFields141_0(),
    )
}
private fun __kffiJvmStructFields142_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("binding", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("buffer", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("sampler", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("textureView", JvmDowncallEngine.FieldKind.POINTER, 48L),
)
private fun __kffiJvmRegisterStructLayout142() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBindGroupEntry",
        56L, 8L,
        __kffiJvmStructFields142_0(),
    )
}
private fun __kffiJvmStructFields143_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("binding", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("visibility", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("bindingArraySize", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("WGPUBufferBindingLayout", JvmDowncallEngine.FieldKind.STRUCT, 32L),
    JvmDowncallEngine.StructField("WGPUSamplerBindingLayout", JvmDowncallEngine.FieldKind.STRUCT, 56L),
    JvmDowncallEngine.StructField("WGPUTextureBindingLayout", JvmDowncallEngine.FieldKind.STRUCT, 72L),
    JvmDowncallEngine.StructField("WGPUStorageTextureBindingLayout", JvmDowncallEngine.FieldKind.STRUCT, 96L),
)
private fun __kffiJvmRegisterStructLayout143() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBindGroupLayoutEntry",
        120L, 8L,
        __kffiJvmStructFields143_0(),
    )
}
private fun __kffiJvmStructFields144_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUBlendComponent", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUBlendComponent", JvmDowncallEngine.FieldKind.STRUCT, 12L),
)
private fun __kffiJvmRegisterStructLayout144() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBlendState",
        24L, 4L,
        __kffiJvmStructFields144_0(),
    )
}
private fun __kffiJvmStructFields145_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("size", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("mappedAtCreation", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout145() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBufferDescriptor",
        48L, 8L,
        __kffiJvmStructFields145_0(),
    )
}
private fun __kffiJvmStructFields146_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout146() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCommandEncoderDescriptor",
        24L, 8L,
        __kffiJvmStructFields146_0(),
    )
}
private fun __kffiJvmStructFields147_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("type", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("lineNum", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("linePos", JvmDowncallEngine.FieldKind.UINT64, 40L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 48L),
    JvmDowncallEngine.StructField("length", JvmDowncallEngine.FieldKind.UINT64, 56L),
)
private fun __kffiJvmRegisterStructLayout147() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCompilationMessage",
        64L, 8L,
        __kffiJvmStructFields147_0(),
    )
}
private fun __kffiJvmStructFields148_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("timestampWrites", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout148() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUComputePassDescriptor",
        32L, 8L,
        __kffiJvmStructFields148_0(),
    )
}
private fun __kffiJvmStructFields149_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("module", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("constantCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("constants", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout149() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUComputeState",
        48L, 8L,
        __kffiJvmStructFields149_0(),
    )
}
private fun __kffiJvmStructFields150_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("propertiesCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("properties", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout150() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnDrmFormatCapabilities",
        32L, 8L,
        __kffiJvmStructFields150_0(),
    )
}
private fun __kffiJvmStructFields151_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("depthWriteEnabled", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("depthCompare", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("WGPUStencilFaceState", JvmDowncallEngine.FieldKind.STRUCT, 20L),
    JvmDowncallEngine.StructField("WGPUStencilFaceState", JvmDowncallEngine.FieldKind.STRUCT, 36L),
    JvmDowncallEngine.StructField("stencilReadMask", JvmDowncallEngine.FieldKind.UINT32, 52L),
    JvmDowncallEngine.StructField("stencilWriteMask", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("depthBias", JvmDowncallEngine.FieldKind.INT32, 60L),
    JvmDowncallEngine.StructField("depthBiasSlopeScale", JvmDowncallEngine.FieldKind.FLOAT32, 64L),
    JvmDowncallEngine.StructField("depthBiasClamp", JvmDowncallEngine.FieldKind.FLOAT32, 68L),
)
private fun __kffiJvmRegisterStructLayout151() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDepthStencilState",
        72L, 8L,
        __kffiJvmStructFields151_0(),
    )
}
private fun __kffiJvmStructFields152_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("plane0", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("plane1", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("WGPUOrigin2D", JvmDowncallEngine.FieldKind.STRUCT, 40L),
    JvmDowncallEngine.StructField("WGPUExtent2D", JvmDowncallEngine.FieldKind.STRUCT, 48L),
    JvmDowncallEngine.StructField("WGPUExtent2D", JvmDowncallEngine.FieldKind.STRUCT, 56L),
    JvmDowncallEngine.StructField("doYuvToRgbConversionOnly", JvmDowncallEngine.FieldKind.UINT32, 64L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("yuvToRgbConversionMatrix", JvmDowncallEngine.FieldKind.POINTER, 72L),
    JvmDowncallEngine.StructField("srcTransferFunctionParameters", JvmDowncallEngine.FieldKind.POINTER, 80L),
    JvmDowncallEngine.StructField("dstTransferFunctionParameters", JvmDowncallEngine.FieldKind.POINTER, 88L),
    JvmDowncallEngine.StructField("gamutConversionMatrix", JvmDowncallEngine.FieldKind.POINTER, 96L),
    JvmDowncallEngine.StructField("mirrored", JvmDowncallEngine.FieldKind.UINT32, 104L),
    JvmDowncallEngine.StructField("rotation", JvmDowncallEngine.FieldKind.UINT32, 108L),
)
private fun __kffiJvmRegisterStructLayout152() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUExternalTextureDescriptor",
        112L, 8L,
        __kffiJvmStructFields152_0(),
    )
}
private fun __kffiJvmStructFields153_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUFuture", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("completed", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout153() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUFutureWaitInfo",
        16L, 8L,
        __kffiJvmStructFields153_0(),
    )
}
private fun __kffiJvmStructFields154_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("externalTexture", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("WGPUOrigin3D", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("WGPUExtent2D", JvmDowncallEngine.FieldKind.STRUCT, 28L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout154() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUImageCopyExternalTexture",
        40L, 8L,
        __kffiJvmStructFields154_0(),
    )
}
private fun __kffiJvmStructFields155_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("requiredFeatureCount", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("requiredFeatures", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("requiredLimits", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout155() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUInstanceDescriptor",
        32L, 8L,
        __kffiJvmStructFields155_0(),
    )
}
private fun __kffiJvmStructFields156_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("maxTextureDimension1D", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("maxTextureDimension2D", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("maxTextureDimension3D", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("maxTextureArrayLayers", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("maxBindGroups", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("maxBindGroupsPlusVertexBuffers", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("maxBindingsPerBindGroup", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("maxDynamicUniformBuffersPerPipelineLayout", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("maxDynamicStorageBuffersPerPipelineLayout", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("maxSampledTexturesPerShaderStage", JvmDowncallEngine.FieldKind.UINT32, 44L),
    JvmDowncallEngine.StructField("maxSamplersPerShaderStage", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("maxStorageBuffersPerShaderStage", JvmDowncallEngine.FieldKind.UINT32, 52L),
    JvmDowncallEngine.StructField("maxStorageTexturesPerShaderStage", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("maxUniformBuffersPerShaderStage", JvmDowncallEngine.FieldKind.UINT32, 60L),
    JvmDowncallEngine.StructField("maxUniformBufferBindingSize", JvmDowncallEngine.FieldKind.UINT64, 64L),
    JvmDowncallEngine.StructField("maxStorageBufferBindingSize", JvmDowncallEngine.FieldKind.UINT64, 72L),
    JvmDowncallEngine.StructField("minUniformBufferOffsetAlignment", JvmDowncallEngine.FieldKind.UINT32, 80L),
    JvmDowncallEngine.StructField("minStorageBufferOffsetAlignment", JvmDowncallEngine.FieldKind.UINT32, 84L),
    JvmDowncallEngine.StructField("maxVertexBuffers", JvmDowncallEngine.FieldKind.UINT32, 88L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("maxBufferSize", JvmDowncallEngine.FieldKind.UINT64, 96L),
    JvmDowncallEngine.StructField("maxVertexAttributes", JvmDowncallEngine.FieldKind.UINT32, 104L),
    JvmDowncallEngine.StructField("maxVertexBufferArrayStride", JvmDowncallEngine.FieldKind.UINT32, 108L),
    JvmDowncallEngine.StructField("maxInterStageShaderVariables", JvmDowncallEngine.FieldKind.UINT32, 112L),
    JvmDowncallEngine.StructField("maxColorAttachments", JvmDowncallEngine.FieldKind.UINT32, 116L),
    JvmDowncallEngine.StructField("maxColorAttachmentBytesPerSample", JvmDowncallEngine.FieldKind.UINT32, 120L),
    JvmDowncallEngine.StructField("maxComputeWorkgroupStorageSize", JvmDowncallEngine.FieldKind.UINT32, 124L),
    JvmDowncallEngine.StructField("maxComputeInvocationsPerWorkgroup", JvmDowncallEngine.FieldKind.UINT32, 128L),
    JvmDowncallEngine.StructField("maxComputeWorkgroupSizeX", JvmDowncallEngine.FieldKind.UINT32, 132L),
    JvmDowncallEngine.StructField("maxComputeWorkgroupSizeY", JvmDowncallEngine.FieldKind.UINT32, 136L),
    JvmDowncallEngine.StructField("maxComputeWorkgroupSizeZ", JvmDowncallEngine.FieldKind.UINT32, 140L),
    JvmDowncallEngine.StructField("maxComputeWorkgroupsPerDimension", JvmDowncallEngine.FieldKind.UINT32, 144L),
    JvmDowncallEngine.StructField("maxImmediateSize", JvmDowncallEngine.FieldKind.UINT32, 148L),
)
private fun __kffiJvmRegisterStructLayout156() {
    JvmDowncallEngine.registerStructLayout(
        "WGPULimits",
        152L, 8L,
        __kffiJvmStructFields156_0(),
    )
}
private fun __kffiJvmStructFields157_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("totalPixelLocalStorageSize", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("storageAttachmentCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("storageAttachments", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout157() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPipelineLayoutPixelLocalStorage",
        40L, 8L,
        __kffiJvmStructFields157_0(),
    )
}
private fun __kffiJvmStructFields158_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("colorFormatCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("colorFormats", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("depthStencilFormat", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("sampleCount", JvmDowncallEngine.FieldKind.UINT32, 44L),
    JvmDowncallEngine.StructField("depthReadOnly", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("stencilReadOnly", JvmDowncallEngine.FieldKind.UINT32, 52L),
)
private fun __kffiJvmRegisterStructLayout158() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderBundleEncoderDescriptor",
        56L, 8L,
        __kffiJvmStructFields158_0(),
    )
}
private fun __kffiJvmStructFields159_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("view", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("depthSlice", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("resolveTarget", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("loadOp", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("storeOp", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("WGPUColor", JvmDowncallEngine.FieldKind.STRUCT, 40L),
)
private fun __kffiJvmRegisterStructLayout159() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassColorAttachment",
        72L, 8L,
        __kffiJvmStructFields159_0(),
    )
}
private fun __kffiJvmStructFields160_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUOrigin2D", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("WGPUExtent2D", JvmDowncallEngine.FieldKind.STRUCT, 24L),
)
private fun __kffiJvmRegisterStructLayout160() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassRenderAreaRect",
        32L, 8L,
        __kffiJvmStructFields160_0(),
    )
}
private fun __kffiJvmStructFields161_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("offset", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("storage", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("loadOp", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("storeOp", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("WGPUColor", JvmDowncallEngine.FieldKind.STRUCT, 32L),
)
private fun __kffiJvmRegisterStructLayout161() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassStorageAttachment",
        64L, 8L,
        __kffiJvmStructFields161_0(),
    )
}
private fun __kffiJvmStructFields162_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("featureLevel", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("powerPreference", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("forceFallbackAdapter", JvmDowncallEngine.FieldKind.UINT32, 16L),
    JvmDowncallEngine.StructField("backendType", JvmDowncallEngine.FieldKind.UINT32, 20L),
    JvmDowncallEngine.StructField("compatibleSurface", JvmDowncallEngine.FieldKind.POINTER, 24L),
)
private fun __kffiJvmRegisterStructLayout162() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURequestAdapterOptions",
        32L, 8L,
        __kffiJvmStructFields162_0(),
    )
}
private fun __kffiJvmStructFields163_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("addressModeU", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("addressModeV", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("addressModeW", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("magFilter", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("minFilter", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("mipmapFilter", JvmDowncallEngine.FieldKind.UINT32, 44L),
    JvmDowncallEngine.StructField("lodMinClamp", JvmDowncallEngine.FieldKind.FLOAT32, 48L),
    JvmDowncallEngine.StructField("lodMaxClamp", JvmDowncallEngine.FieldKind.FLOAT32, 52L),
    JvmDowncallEngine.StructField("compare", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("maxAnisotropy", JvmDowncallEngine.FieldKind.UINT16, 60L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 2L),
)
private fun __kffiJvmRegisterStructLayout163() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSamplerDescriptor",
        64L, 8L,
        __kffiJvmStructFields163_0(),
    )
}
private fun __kffiJvmStructFields164_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout164() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUShaderModuleDescriptor",
        24L, 8L,
        __kffiJvmStructFields164_0(),
    )
}
private fun __kffiJvmStructFields165_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout165() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedBufferMemoryDescriptor",
        24L, 8L,
        __kffiJvmStructFields165_0(),
    )
}
private fun __kffiJvmStructFields166_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout166() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceDescriptor",
        24L, 8L,
        __kffiJvmStructFields166_0(),
    )
}
private fun __kffiJvmStructFields167_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("type", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout167() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedFenceExportInfo",
        16L, 8L,
        __kffiJvmStructFields167_0(),
    )
}
private fun __kffiJvmStructFields168_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUYCbCrVkDescriptor", JvmDowncallEngine.FieldKind.STRUCT, 16L),
)
private fun __kffiJvmRegisterStructLayout168() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryAHardwareBufferProperties",
        88L, 8L,
        __kffiJvmStructFields168_0(),
    )
}
private fun __kffiJvmStructFields169_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("concurrentRead", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("initialized", JvmDowncallEngine.FieldKind.UINT32, 12L),
    JvmDowncallEngine.StructField("fenceCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("fences", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("signaledValueCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("signaledValues", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout169() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryBeginAccessDescriptor",
        48L, 8L,
        __kffiJvmStructFields169_0(),
    )
}
private fun __kffiJvmStructFields170_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUExtent3D", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("drmFormat", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("drmModifier", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("planeCount", JvmDowncallEngine.FieldKind.UINT64, 40L),
    JvmDowncallEngine.StructField("planes", JvmDowncallEngine.FieldKind.POINTER, 48L),
)
private fun __kffiJvmRegisterStructLayout170() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryDmaBufDescriptor",
        56L, 8L,
        __kffiJvmStructFields170_0(),
    )
}
private fun __kffiJvmStructFields171_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUFuture", JvmDowncallEngine.FieldKind.STRUCT, 16L),
)
private fun __kffiJvmRegisterStructLayout171() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryMetalEndAccessState",
        24L, 8L,
        __kffiJvmStructFields171_0(),
    )
}
private fun __kffiJvmStructFields172_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout172() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSurfaceDescriptor",
        24L, 8L,
        __kffiJvmStructFields172_0(),
    )
}
private fun __kffiJvmStructFields173_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUTexelCopyBufferLayout", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("buffer", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout173() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelCopyBufferInfo",
        24L, 8L,
        __kffiJvmStructFields173_0(),
    )
}
private fun __kffiJvmStructFields174_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("texture", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("mipLevel", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("WGPUOrigin3D", JvmDowncallEngine.FieldKind.STRUCT, 12L),
    JvmDowncallEngine.StructField("aspect", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout174() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTexelCopyTextureInfo",
        32L, 8L,
        __kffiJvmStructFields174_0(),
    )
}
private fun __kffiJvmStructFields175_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("WGPUTextureComponentSwizzle", JvmDowncallEngine.FieldKind.STRUCT, 16L),
)
private fun __kffiJvmRegisterStructLayout175() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureComponentSwizzleDescriptor",
        32L, 8L,
        __kffiJvmStructFields175_0(),
    )
}
private fun __kffiJvmStructFields176_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("dimension", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("WGPUExtent3D", JvmDowncallEngine.FieldKind.STRUCT, 36L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("mipLevelCount", JvmDowncallEngine.FieldKind.UINT32, 52L),
    JvmDowncallEngine.StructField("sampleCount", JvmDowncallEngine.FieldKind.UINT32, 56L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("viewFormatCount", JvmDowncallEngine.FieldKind.UINT64, 64L),
    JvmDowncallEngine.StructField("viewFormats", JvmDowncallEngine.FieldKind.POINTER, 72L),
)
private fun __kffiJvmRegisterStructLayout176() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureDescriptor",
        80L, 8L,
        __kffiJvmStructFields176_0(),
    )
}
private fun __kffiJvmStructFields177_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("stepMode", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("arrayStride", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("attributeCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("attributes", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout177() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUVertexBufferLayout",
        40L, 8L,
        __kffiJvmStructFields177_0(),
    )
}
private fun __kffiJvmStructFields178_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 24L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 40L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 56L),
    JvmDowncallEngine.StructField("backendType", JvmDowncallEngine.FieldKind.UINT32, 72L),
    JvmDowncallEngine.StructField("adapterType", JvmDowncallEngine.FieldKind.UINT32, 76L),
    JvmDowncallEngine.StructField("vendorID", JvmDowncallEngine.FieldKind.UINT32, 80L),
    JvmDowncallEngine.StructField("deviceID", JvmDowncallEngine.FieldKind.UINT32, 84L),
    JvmDowncallEngine.StructField("subgroupMinSize", JvmDowncallEngine.FieldKind.UINT32, 88L),
    JvmDowncallEngine.StructField("subgroupMaxSize", JvmDowncallEngine.FieldKind.UINT32, 92L),
)
private fun __kffiJvmRegisterStructLayout178() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUAdapterInfo",
        96L, 8L,
        __kffiJvmStructFields178_0(),
    )
}
private fun __kffiJvmStructFields179_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("layout", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("entryCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("entries", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout179() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBindGroupDescriptor",
        48L, 8L,
        __kffiJvmStructFields179_0(),
    )
}
private fun __kffiJvmStructFields180_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("entryCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("entries", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout180() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUBindGroupLayoutDescriptor",
        40L, 8L,
        __kffiJvmStructFields180_0(),
    )
}
private fun __kffiJvmStructFields181_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("blend", JvmDowncallEngine.FieldKind.POINTER, 16L),
    JvmDowncallEngine.StructField("writeMask", JvmDowncallEngine.FieldKind.UINT64, 24L),
)
private fun __kffiJvmRegisterStructLayout181() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUColorTargetState",
        32L, 8L,
        __kffiJvmStructFields181_0(),
    )
}
private fun __kffiJvmStructFields182_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("messageCount", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("messages", JvmDowncallEngine.FieldKind.POINTER, 16L),
)
private fun __kffiJvmRegisterStructLayout182() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUCompilationInfo",
        24L, 8L,
        __kffiJvmStructFields182_0(),
    )
}
private fun __kffiJvmStructFields183_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("layout", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("WGPUComputeState", JvmDowncallEngine.FieldKind.STRUCT, 32L),
)
private fun __kffiJvmRegisterStructLayout183() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUComputePipelineDescriptor",
        80L, 8L,
        __kffiJvmStructFields183_0(),
    )
}
private fun __kffiJvmStructFields184_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
)
private fun __kffiJvmRegisterStructLayout184() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDawnFormatCapabilities",
        8L, 8L,
        __kffiJvmStructFields184_0(),
    )
}
private fun __kffiJvmStructFields185_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("requiredFeatureCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("requiredFeatures", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("requiredLimits", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("WGPUQueueDescriptor", JvmDowncallEngine.FieldKind.STRUCT, 48L),
    JvmDowncallEngine.StructField("WGPUDeviceLostCallbackInfo", JvmDowncallEngine.FieldKind.STRUCT, 72L),
    JvmDowncallEngine.StructField("WGPUUncapturedErrorCallbackInfo", JvmDowncallEngine.FieldKind.STRUCT, 112L),
)
private fun __kffiJvmRegisterStructLayout185() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUDeviceDescriptor",
        144L, 8L,
        __kffiJvmStructFields185_0(),
    )
}
private fun __kffiJvmStructFields186_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("bindGroupLayoutCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("bindGroupLayouts", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("immediateSize", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
)
private fun __kffiJvmRegisterStructLayout186() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUPipelineLayoutDescriptor",
        48L, 8L,
        __kffiJvmStructFields186_0(),
    )
}
private fun __kffiJvmStructFields187_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("WGPUChainedStruct", JvmDowncallEngine.FieldKind.STRUCT, 0L),
    JvmDowncallEngine.StructField("totalPixelLocalStorageSize", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("storageAttachmentCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("storageAttachments", JvmDowncallEngine.FieldKind.POINTER, 32L),
)
private fun __kffiJvmRegisterStructLayout187() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassPixelLocalStorage",
        40L, 8L,
        __kffiJvmStructFields187_0(),
    )
}
private fun __kffiJvmStructFields188_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
)
private fun __kffiJvmRegisterStructLayout188() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryDescriptor",
        24L, 8L,
        __kffiJvmStructFields188_0(),
    )
}
private fun __kffiJvmStructFields189_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("initialized", JvmDowncallEngine.FieldKind.UINT32, 8L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("fenceCount", JvmDowncallEngine.FieldKind.UINT64, 16L),
    JvmDowncallEngine.StructField("fences", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("signaledValueCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("signaledValues", JvmDowncallEngine.FieldKind.POINTER, 40L),
)
private fun __kffiJvmRegisterStructLayout189() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryEndAccessState",
        48L, 8L,
        __kffiJvmStructFields189_0(),
    )
}
private fun __kffiJvmStructFields190_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 8L),
    JvmDowncallEngine.StructField("WGPUExtent3D", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 28L),
)
private fun __kffiJvmRegisterStructLayout190() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUSharedTextureMemoryProperties",
        32L, 8L,
        __kffiJvmStructFields190_0(),
    )
}
private fun __kffiJvmStructFields191_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("format", JvmDowncallEngine.FieldKind.UINT32, 24L),
    JvmDowncallEngine.StructField("dimension", JvmDowncallEngine.FieldKind.UINT32, 28L),
    JvmDowncallEngine.StructField("baseMipLevel", JvmDowncallEngine.FieldKind.UINT32, 32L),
    JvmDowncallEngine.StructField("mipLevelCount", JvmDowncallEngine.FieldKind.UINT32, 36L),
    JvmDowncallEngine.StructField("baseArrayLayer", JvmDowncallEngine.FieldKind.UINT32, 40L),
    JvmDowncallEngine.StructField("arrayLayerCount", JvmDowncallEngine.FieldKind.UINT32, 44L),
    JvmDowncallEngine.StructField("aspect", JvmDowncallEngine.FieldKind.UINT32, 48L),
    JvmDowncallEngine.StructField("__pad", JvmDowncallEngine.FieldKind.PADDING, 4L),
    JvmDowncallEngine.StructField("usage", JvmDowncallEngine.FieldKind.UINT64, 56L),
)
private fun __kffiJvmRegisterStructLayout191() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUTextureViewDescriptor",
        64L, 8L,
        __kffiJvmStructFields191_0(),
    )
}
private fun __kffiJvmStructFields192_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("module", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("constantCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("constants", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("bufferCount", JvmDowncallEngine.FieldKind.UINT64, 48L),
    JvmDowncallEngine.StructField("buffers", JvmDowncallEngine.FieldKind.POINTER, 56L),
)
private fun __kffiJvmRegisterStructLayout192() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUVertexState",
        64L, 8L,
        __kffiJvmStructFields192_0(),
    )
}
private fun __kffiJvmStructFields193_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("module", JvmDowncallEngine.FieldKind.POINTER, 8L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 16L),
    JvmDowncallEngine.StructField("constantCount", JvmDowncallEngine.FieldKind.UINT64, 32L),
    JvmDowncallEngine.StructField("constants", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("targetCount", JvmDowncallEngine.FieldKind.UINT64, 48L),
    JvmDowncallEngine.StructField("targets", JvmDowncallEngine.FieldKind.POINTER, 56L),
)
private fun __kffiJvmRegisterStructLayout193() {
    JvmDowncallEngine.registerStructLayout(
        "WGPUFragmentState",
        64L, 8L,
        __kffiJvmStructFields193_0(),
    )
}
private fun __kffiJvmStructFields194_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("colorAttachmentCount", JvmDowncallEngine.FieldKind.UINT64, 24L),
    JvmDowncallEngine.StructField("colorAttachments", JvmDowncallEngine.FieldKind.POINTER, 32L),
    JvmDowncallEngine.StructField("depthStencilAttachment", JvmDowncallEngine.FieldKind.POINTER, 40L),
    JvmDowncallEngine.StructField("occlusionQuerySet", JvmDowncallEngine.FieldKind.POINTER, 48L),
    JvmDowncallEngine.StructField("timestampWrites", JvmDowncallEngine.FieldKind.POINTER, 56L),
)
private fun __kffiJvmRegisterStructLayout194() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPassDescriptor",
        64L, 8L,
        __kffiJvmStructFields194_0(),
    )
}
private fun __kffiJvmStructFields195_0(): kotlin.collections.List<JvmDowncallEngine.StructField> = listOf(
    JvmDowncallEngine.StructField("nextInChain", JvmDowncallEngine.FieldKind.POINTER, 0L),
    JvmDowncallEngine.StructField("WGPUStringView", JvmDowncallEngine.FieldKind.STRUCT, 8L),
    JvmDowncallEngine.StructField("layout", JvmDowncallEngine.FieldKind.POINTER, 24L),
    JvmDowncallEngine.StructField("WGPUVertexState", JvmDowncallEngine.FieldKind.STRUCT, 32L),
    JvmDowncallEngine.StructField("WGPUPrimitiveState", JvmDowncallEngine.FieldKind.STRUCT, 96L),
    JvmDowncallEngine.StructField("depthStencil", JvmDowncallEngine.FieldKind.POINTER, 128L),
    JvmDowncallEngine.StructField("WGPUMultisampleState", JvmDowncallEngine.FieldKind.STRUCT, 136L),
    JvmDowncallEngine.StructField("fragment", JvmDowncallEngine.FieldKind.POINTER, 160L),
)
private fun __kffiJvmRegisterStructLayout195() {
    JvmDowncallEngine.registerStructLayout(
        "WGPURenderPipelineDescriptor",
        168L, 8L,
        __kffiJvmStructFields195_0(),
    )
}
private val __kffiJvmStructLayouts: Unit = run {
    __kffiJvmRegisterStructLayout0()
    __kffiJvmRegisterStructLayout1()
    __kffiJvmRegisterStructLayout2()
    __kffiJvmRegisterStructLayout3()
    __kffiJvmRegisterStructLayout4()
    __kffiJvmRegisterStructLayout5()
    __kffiJvmRegisterStructLayout6()
    __kffiJvmRegisterStructLayout7()
    __kffiJvmRegisterStructLayout8()
    __kffiJvmRegisterStructLayout9()
    __kffiJvmRegisterStructLayout10()
    __kffiJvmRegisterStructLayout11()
    __kffiJvmRegisterStructLayout12()
    __kffiJvmRegisterStructLayout13()
    __kffiJvmRegisterStructLayout14()
    __kffiJvmRegisterStructLayout15()
    __kffiJvmRegisterStructLayout16()
    __kffiJvmRegisterStructLayout17()
    __kffiJvmRegisterStructLayout18()
    __kffiJvmRegisterStructLayout19()
    __kffiJvmRegisterStructLayout20()
    __kffiJvmRegisterStructLayout21()
    __kffiJvmRegisterStructLayout22()
    __kffiJvmRegisterStructLayout23()
    __kffiJvmRegisterStructLayout24()
    __kffiJvmRegisterStructLayout25()
    __kffiJvmRegisterStructLayout26()
    __kffiJvmRegisterStructLayout27()
    __kffiJvmRegisterStructLayout28()
    __kffiJvmRegisterStructLayout29()
    __kffiJvmRegisterStructLayout30()
    __kffiJvmRegisterStructLayout31()
    __kffiJvmRegisterStructLayout32()
    __kffiJvmRegisterStructLayout33()
    __kffiJvmRegisterStructLayout34()
    __kffiJvmRegisterStructLayout35()
    __kffiJvmRegisterStructLayout36()
    __kffiJvmRegisterStructLayout37()
    __kffiJvmRegisterStructLayout38()
    __kffiJvmRegisterStructLayout39()
    __kffiJvmRegisterStructLayout40()
    __kffiJvmRegisterStructLayout41()
    __kffiJvmRegisterStructLayout42()
    __kffiJvmRegisterStructLayout43()
    __kffiJvmRegisterStructLayout44()
    __kffiJvmRegisterStructLayout45()
    __kffiJvmRegisterStructLayout46()
    __kffiJvmRegisterStructLayout47()
    __kffiJvmRegisterStructLayout48()
    __kffiJvmRegisterStructLayout49()
    __kffiJvmRegisterStructLayout50()
    __kffiJvmRegisterStructLayout51()
    __kffiJvmRegisterStructLayout52()
    __kffiJvmRegisterStructLayout53()
    __kffiJvmRegisterStructLayout54()
    __kffiJvmRegisterStructLayout55()
    __kffiJvmRegisterStructLayout56()
    __kffiJvmRegisterStructLayout57()
    __kffiJvmRegisterStructLayout58()
    __kffiJvmRegisterStructLayout59()
    __kffiJvmRegisterStructLayout60()
    __kffiJvmRegisterStructLayout61()
    __kffiJvmRegisterStructLayout62()
    __kffiJvmRegisterStructLayout63()
    __kffiJvmRegisterStructLayout64()
    __kffiJvmRegisterStructLayout65()
    __kffiJvmRegisterStructLayout66()
    __kffiJvmRegisterStructLayout67()
    __kffiJvmRegisterStructLayout68()
    __kffiJvmRegisterStructLayout69()
    __kffiJvmRegisterStructLayout70()
    __kffiJvmRegisterStructLayout71()
    __kffiJvmRegisterStructLayout72()
    __kffiJvmRegisterStructLayout73()
    __kffiJvmRegisterStructLayout74()
    __kffiJvmRegisterStructLayout75()
    __kffiJvmRegisterStructLayout76()
    __kffiJvmRegisterStructLayout77()
    __kffiJvmRegisterStructLayout78()
    __kffiJvmRegisterStructLayout79()
    __kffiJvmRegisterStructLayout80()
    __kffiJvmRegisterStructLayout81()
    __kffiJvmRegisterStructLayout82()
    __kffiJvmRegisterStructLayout83()
    __kffiJvmRegisterStructLayout84()
    __kffiJvmRegisterStructLayout85()
    __kffiJvmRegisterStructLayout86()
    __kffiJvmRegisterStructLayout87()
    __kffiJvmRegisterStructLayout88()
    __kffiJvmRegisterStructLayout89()
    __kffiJvmRegisterStructLayout90()
    __kffiJvmRegisterStructLayout91()
    __kffiJvmRegisterStructLayout92()
    __kffiJvmRegisterStructLayout93()
    __kffiJvmRegisterStructLayout94()
    __kffiJvmRegisterStructLayout95()
    __kffiJvmRegisterStructLayout96()
    __kffiJvmRegisterStructLayout97()
    __kffiJvmRegisterStructLayout98()
    __kffiJvmRegisterStructLayout99()
    __kffiJvmRegisterStructLayout100()
    __kffiJvmRegisterStructLayout101()
    __kffiJvmRegisterStructLayout102()
    __kffiJvmRegisterStructLayout103()
    __kffiJvmRegisterStructLayout104()
    __kffiJvmRegisterStructLayout105()
    __kffiJvmRegisterStructLayout106()
    __kffiJvmRegisterStructLayout107()
    __kffiJvmRegisterStructLayout108()
    __kffiJvmRegisterStructLayout109()
    __kffiJvmRegisterStructLayout110()
    __kffiJvmRegisterStructLayout111()
    __kffiJvmRegisterStructLayout112()
    __kffiJvmRegisterStructLayout113()
    __kffiJvmRegisterStructLayout114()
    __kffiJvmRegisterStructLayout115()
    __kffiJvmRegisterStructLayout116()
    __kffiJvmRegisterStructLayout117()
    __kffiJvmRegisterStructLayout118()
    __kffiJvmRegisterStructLayout119()
    __kffiJvmRegisterStructLayout120()
    __kffiJvmRegisterStructLayout121()
    __kffiJvmRegisterStructLayout122()
    __kffiJvmRegisterStructLayout123()
    __kffiJvmRegisterStructLayout124()
    __kffiJvmRegisterStructLayout125()
    __kffiJvmRegisterStructLayout126()
    __kffiJvmRegisterStructLayout127()
    __kffiJvmRegisterStructLayout128()
    __kffiJvmRegisterStructLayout129()
    __kffiJvmRegisterStructLayout130()
    __kffiJvmRegisterStructLayout131()
    __kffiJvmRegisterStructLayout132()
    __kffiJvmRegisterStructLayout133()
    __kffiJvmRegisterStructLayout134()
    __kffiJvmRegisterStructLayout135()
    __kffiJvmRegisterStructLayout136()
    __kffiJvmRegisterStructLayout137()
    __kffiJvmRegisterStructLayout138()
    __kffiJvmRegisterStructLayout139()
    __kffiJvmRegisterStructLayout140()
    __kffiJvmRegisterStructLayout141()
    __kffiJvmRegisterStructLayout142()
    __kffiJvmRegisterStructLayout143()
    __kffiJvmRegisterStructLayout144()
    __kffiJvmRegisterStructLayout145()
    __kffiJvmRegisterStructLayout146()
    __kffiJvmRegisterStructLayout147()
    __kffiJvmRegisterStructLayout148()
    __kffiJvmRegisterStructLayout149()
    __kffiJvmRegisterStructLayout150()
    __kffiJvmRegisterStructLayout151()
    __kffiJvmRegisterStructLayout152()
    __kffiJvmRegisterStructLayout153()
    __kffiJvmRegisterStructLayout154()
    __kffiJvmRegisterStructLayout155()
    __kffiJvmRegisterStructLayout156()
    __kffiJvmRegisterStructLayout157()
    __kffiJvmRegisterStructLayout158()
    __kffiJvmRegisterStructLayout159()
    __kffiJvmRegisterStructLayout160()
    __kffiJvmRegisterStructLayout161()
    __kffiJvmRegisterStructLayout162()
    __kffiJvmRegisterStructLayout163()
    __kffiJvmRegisterStructLayout164()
    __kffiJvmRegisterStructLayout165()
    __kffiJvmRegisterStructLayout166()
    __kffiJvmRegisterStructLayout167()
    __kffiJvmRegisterStructLayout168()
    __kffiJvmRegisterStructLayout169()
    __kffiJvmRegisterStructLayout170()
    __kffiJvmRegisterStructLayout171()
    __kffiJvmRegisterStructLayout172()
    __kffiJvmRegisterStructLayout173()
    __kffiJvmRegisterStructLayout174()
    __kffiJvmRegisterStructLayout175()
    __kffiJvmRegisterStructLayout176()
    __kffiJvmRegisterStructLayout177()
    __kffiJvmRegisterStructLayout178()
    __kffiJvmRegisterStructLayout179()
    __kffiJvmRegisterStructLayout180()
    __kffiJvmRegisterStructLayout181()
    __kffiJvmRegisterStructLayout182()
    __kffiJvmRegisterStructLayout183()
    __kffiJvmRegisterStructLayout184()
    __kffiJvmRegisterStructLayout185()
    __kffiJvmRegisterStructLayout186()
    __kffiJvmRegisterStructLayout187()
    __kffiJvmRegisterStructLayout188()
    __kffiJvmRegisterStructLayout189()
    __kffiJvmRegisterStructLayout190()
    __kffiJvmRegisterStructLayout191()
    __kffiJvmRegisterStructLayout192()
    __kffiJvmRegisterStructLayout193()
    __kffiJvmRegisterStructLayout194()
    __kffiJvmRegisterStructLayout195()
}

