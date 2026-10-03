package org.graphiks.dawn4k.smoke

import org.graphiks.dawn4k.raw.WGPUAdapter
import org.graphiks.dawn4k.raw.WGPUAdapterInfo
import org.graphiks.dawn4k.raw.WGPUBackendType_Metal
import org.graphiks.dawn4k.raw.WGPUBackendType_Vulkan
import org.graphiks.dawn4k.raw.WGPUBufferDescriptor
import org.graphiks.dawn4k.raw.WGPUBufferMapState_Mapped
import org.graphiks.dawn4k.raw.WGPUBufferUsage_CopySrc
import org.graphiks.dawn4k.raw.WGPUBufferUsage_MapWrite
import org.graphiks.dawn4k.raw.WGPUCallbackMode_AllowProcessEvents
import org.graphiks.dawn4k.raw.WGPUDevice
import org.graphiks.dawn4k.raw.WGPUDeviceDescriptor
import org.graphiks.dawn4k.raw.WGPUDeviceLostCallback
import org.graphiks.dawn4k.raw.WGPUDeviceLostCallbackInfo
import org.graphiks.dawn4k.raw.WGPUFeatureLevel_Core
import org.graphiks.dawn4k.raw.WGPUInstance
import org.graphiks.dawn4k.raw.WGPUPowerPreference_HighPerformance
import org.graphiks.dawn4k.raw.WGPURequestAdapterCallback
import org.graphiks.dawn4k.raw.WGPURequestAdapterCallbackInfo
import org.graphiks.dawn4k.raw.WGPURequestAdapterOptions
import org.graphiks.dawn4k.raw.WGPURequestAdapterStatus_Success
import org.graphiks.dawn4k.raw.WGPURequestDeviceCallback
import org.graphiks.dawn4k.raw.WGPURequestDeviceCallbackInfo
import org.graphiks.dawn4k.raw.WGPURequestDeviceStatus_Success
import org.graphiks.dawn4k.raw.WGPUStatus_Success
import org.graphiks.dawn4k.raw.WGPUStringView
import org.graphiks.dawn4k.raw.WGPUUncapturedErrorCallback
import org.graphiks.dawn4k.raw.WGPUUncapturedErrorCallbackInfo
import org.graphiks.dawn4k.raw.allocate
import org.graphiks.dawn4k.raw.register
import org.graphiks.dawn4k.raw.wgpuAdapterGetInfo
import org.graphiks.dawn4k.raw.wgpuAdapterInfoFreeMembers
import org.graphiks.dawn4k.raw.wgpuAdapterRelease
import org.graphiks.dawn4k.raw.wgpuAdapterRequestDevice
import org.graphiks.dawn4k.raw.wgpuBufferDestroy
import org.graphiks.dawn4k.raw.wgpuBufferGetMapState
import org.graphiks.dawn4k.raw.wgpuBufferGetMappedRange
import org.graphiks.dawn4k.raw.wgpuBufferGetSize
import org.graphiks.dawn4k.raw.wgpuBufferRelease
import org.graphiks.dawn4k.raw.wgpuBufferUnmap
import org.graphiks.dawn4k.raw.wgpuCreateInstance
import org.graphiks.dawn4k.raw.wgpuDeviceCreateBuffer
import org.graphiks.dawn4k.raw.wgpuDeviceRelease
import org.graphiks.dawn4k.raw.wgpuInstanceProcessEvents
import org.graphiks.dawn4k.raw.wgpuInstanceRelease
import org.graphiks.dawn4k.raw.wgpuInstanceRequestAdapter
import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.memoryScope
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.TimeSource

/** Dawn identity the bindings were generated from (mirrors `bindings/dawn.lock.json`). */
private const val DAWN_REVISION = "chromium/8077@531028367c60ce07251ec0231c1b95bedb4495bc"

fun runSmokeMain() {
    val result = try {
        runSmoke()
    } catch (throwable: Throwable) {
        SmokeResult(
            target = smokeTargetName(),
            revision = DAWN_REVISION,
            backend = "unknown",
            adapterDescription = "",
            callbackCount = 0,
            bufferSize = 0,
            status = "failed",
            diagnostic = "exception: ${throwable.message}",
        )
    }
    println(result.toJson())
    if (result.status != "passed") {
        exitProcess(1)
    }
}

private fun runSmoke(): SmokeResult {
    val target = smokeTargetName()
    val isMac = smokeIsMacOs()
    val backend = if (isMac) WGPUBackendType_Metal else WGPUBackendType_Vulkan
    val backendName = if (isMac) "Metal" else "Vulkan"

    var callbackCount = 0
    var diagnostic = ""
    var adapterDescription = ""

    val instance = wgpuCreateInstance(null)
        ?: return failed(target, backendName, "wgpuCreateInstance returned null")

    var adapter: WGPUAdapter? = null
    var adapterDone = false
    val adapterRegistration = WGPURequestAdapterCallback.register(
        policy = CallbackPolicy.ONCE,
    ) { status, requestedAdapter, message, _ ->
        callbackCount += 1
        diagnostic = message.copyString()
        if (status == WGPURequestAdapterStatus_Success) {
            adapter = requestedAdapter
        }
        adapterDone = true
    }

    var device: WGPUDevice? = null
    var deviceDone = false
    val deviceLostRegistration = WGPUDeviceLostCallback.register(
        policy = CallbackPolicy.REPEATING,
    ) { _, _, message, _ ->
        diagnostic = "device lost: ${message.copyString()}"
    }
    val uncapturedRegistration = WGPUUncapturedErrorCallback.register(
        policy = CallbackPolicy.REPEATING,
    ) { _, _, message, _ ->
        diagnostic = "uncaptured error: ${message.copyString()}"
    }
    val requestDeviceRegistration = WGPURequestDeviceCallback.register(
        policy = CallbackPolicy.ONCE,
    ) { status, requestedDevice, message, _ ->
        callbackCount += 1
        if (message.length > 0uL) {
            diagnostic = message.copyString()
        }
        if (status == WGPURequestDeviceStatus_Success) {
            device = requestedDevice
        }
        deviceDone = true
    }

    var bufferSize = 0L
    try {
        memoryScope { allocator ->
            val options = WGPURequestAdapterOptions.allocate(allocator)
            options.backendType = backend
            options.featureLevel = WGPUFeatureLevel_Core
            options.powerPreference = WGPUPowerPreference_HighPerformance
            options.forceFallbackAdapter = 0u
            val callbackInfo = WGPURequestAdapterCallbackInfo.allocate(
                allocator = allocator,
                mode = WGPUCallbackMode_AllowProcessEvents,
                registration = adapterRegistration,
            )
            wgpuInstanceRequestAdapter(allocator, instance, options, callbackInfo)
        }

        pumpUntil(instance, 30.seconds) { adapterDone }
        if (!adapterDone) {
            return failed(target, backendName, "adapter request timed out", callbackCount)
        }
        val adapterHandle = adapter
            ?: return failed(target, backendName, "no adapter: $diagnostic", callbackCount)

        adapterDescription = memoryScope { allocator ->
            val info = WGPUAdapterInfo.allocate(allocator)
            val status = wgpuAdapterGetInfo(adapterHandle, info)
            val description = if (status == WGPUStatus_Success) info.description.copyString() else ""
            wgpuAdapterInfoFreeMembers(info)
            description
        }

        memoryScope { allocator ->
            val descriptor = WGPUDeviceDescriptor.allocate(allocator)
            val deviceLostInfo = WGPUDeviceLostCallbackInfo.allocate(
                allocator = allocator,
                mode = WGPUCallbackMode_AllowProcessEvents,
                registration = deviceLostRegistration,
            )
            val uncapturedInfo = WGPUUncapturedErrorCallbackInfo.allocate(
                allocator = allocator,
                registration = uncapturedRegistration,
            )
            descriptor.deviceLostCallbackInfo = deviceLostInfo
            descriptor.uncapturedErrorCallbackInfo = uncapturedInfo
            val callbackInfo = WGPURequestDeviceCallbackInfo.allocate(
                allocator = allocator,
                mode = WGPUCallbackMode_AllowProcessEvents,
                registration = requestDeviceRegistration,
            )
            wgpuAdapterRequestDevice(allocator, adapterHandle, descriptor, callbackInfo)
        }

        pumpUntil(instance, 30.seconds) { deviceDone }
        if (!deviceDone) {
            return failed(target, backendName, "device request timed out", callbackCount, adapterDescription)
        }
        val deviceHandle = device
            ?: return failed(target, backendName, "no device: $diagnostic", callbackCount, adapterDescription)

        val buffer = memoryScope { allocator ->
            val descriptor = WGPUBufferDescriptor.allocate(allocator)
            descriptor.size = 16uL
            descriptor.usage = WGPUBufferUsage_MapWrite or WGPUBufferUsage_CopySrc
            descriptor.mappedAtCreation = 1u
            wgpuDeviceCreateBuffer(deviceHandle, descriptor)
        } ?: return failed(target, backendName, "wgpuDeviceCreateBuffer returned null", callbackCount, adapterDescription)

        // Release the device on every path, including the early failures below.
        try {
            val size = wgpuBufferGetSize(buffer)
            val mapState = wgpuBufferGetMapState(buffer)
            val mapped = wgpuBufferGetMappedRange(buffer, 0uL, 16uL)
                ?: return failed(target, backendName, "mapped range unavailable", callbackCount, adapterDescription)
            val memory = MemoryBuffer(mapped, 16uL)
            for (index in 0 until 4) {
                memory.writeUInt((index + 1).toUInt(), (index * 4).toULong())
            }
            bufferSize = size.toLong()
            if (size != 16uL) {
                return failed(target, backendName, "unexpected buffer size $size", callbackCount, adapterDescription)
            }
            if (mapState != WGPUBufferMapState_Mapped) {
                return failed(target, backendName, "buffer not mapped: $mapState", callbackCount, adapterDescription)
            }
            wgpuBufferUnmap(buffer)
        } finally {
            wgpuBufferDestroy(buffer)
            wgpuBufferRelease(buffer)
        }
    } finally {
        device?.let { wgpuDeviceRelease(it) }
        requestDeviceRegistration.close()
        uncapturedRegistration.close()
        deviceLostRegistration.close()
        adapterRegistration.close()
        adapter?.let { wgpuAdapterRelease(it) }
        wgpuInstanceRelease(instance)
    }

    if (callbackCount < 2) {
        return failed(target, backendName, "callbackCount=$callbackCount", callbackCount, adapterDescription)
    }
    return SmokeResult(
        target = target,
        revision = DAWN_REVISION,
        backend = backendName,
        adapterDescription = adapterDescription,
        callbackCount = callbackCount,
        bufferSize = bufferSize,
        status = "passed",
        diagnostic = diagnostic,
    )
}

private inline fun pumpUntil(instance: WGPUInstance?, timeout: Duration, done: () -> Boolean) {
    val start = TimeSource.Monotonic.markNow()
    while (!done() && start.elapsedNow() < timeout) {
        wgpuInstanceProcessEvents(instance)
    }
}

private fun failed(
    target: String,
    backend: String,
    diagnostic: String,
    callbackCount: Int = 0,
    adapterDescription: String = "",
): SmokeResult = SmokeResult(
    target = target,
    revision = DAWN_REVISION,
    backend = backend,
    adapterDescription = adapterDescription,
    callbackCount = callbackCount,
    bufferSize = 0,
    status = "failed",
    diagnostic = diagnostic,
)

private fun WGPUStringView.copyString(): String {
    val data = data ?: return ""
    val length = length.toInt()
    if (length <= 0) return ""
    val buffer = MemoryBuffer(data.handler, length.toULong())
    return ByteArray(length) { index -> buffer.readByte(index.toULong()) }.decodeToString()
}
