package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.internal.copyToString
import org.graphiks.dawn4k.native.WGPUCompilationInfo
import org.graphiks.dawn4k.native.WGPUCompilationMessage
import org.graphiks.dawn4k.native.WGPUCompilationMessageType
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Error
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Info
import org.graphiks.dawn4k.native.WGPUCompilationMessageType_Warning
import org.graphiks.kffi.NativeAddress
import org.graphiks.webgpu.GPUCompilationInfo
import org.graphiks.webgpu.GPUCompilationMessage
import org.graphiks.webgpu.GPUCompilationMessageType

/**
 * Explicit conversion table native `WGPUCompilationMessageType` -> Kotlin
 * `GPUCompilationMessageType`.
 */
internal fun WGPUCompilationMessageType.toCompilationMessageType(): GPUCompilationMessageType = when (this) {
    WGPUCompilationMessageType_Error -> GPUCompilationMessageType.Error
    WGPUCompilationMessageType_Warning -> GPUCompilationMessageType.Warning
    WGPUCompilationMessageType_Info -> GPUCompilationMessageType.Info
    else -> throw IllegalStateException("unknown native compilation message type $this")
}

/** The Kotlin view of one native compilation message; every field is a copied value. */
private class DawnCompilationMessage(
    override val message: String,
    override val type: GPUCompilationMessageType,
    override val lineNum: ULong,
    override val linePos: ULong,
    override val offset: ULong,
    override val length: ULong,
) : GPUCompilationMessage

/** The Kotlin view of a native compilation info: all messages copied, positions included. */
private class DawnCompilationInfo(override val messages: List<GPUCompilationMessage>) : GPUCompilationInfo

/**
 * Copies every compilation message (and its positions) out of the borrowed
 * [compilationInfo] pointer into Kotlin-owned values. Must be called inside the
 * native callback, before the borrowed memory is invalidated.
 */
internal fun readCompilationInfo(compilationInfo: NativeAddress): GPUCompilationInfo {
    val info = WGPUCompilationInfo(compilationInfo)
    val count = info.messageCount
    val messages = ArrayList<GPUCompilationMessage>(count.toInt())
    val base = info.messages
    if (base != null && count > 0uL) {
        val baseAddress = base.handler.rawValue
        for (index in 0 until count.toInt()) {
            val message = WGPUCompilationMessage(NativeAddress(baseAddress + index * WGPU_COMPILATION_MESSAGE_SIZE))
            messages += DawnCompilationMessage(
                message = message.message.copyToString(),
                type = message.type.toCompilationMessageType(),
                lineNum = message.lineNum,
                linePos = message.linePos,
                offset = message.offset,
                length = message.length,
            )
        }
    }
    return DawnCompilationInfo(messages)
}

/**
 * `sizeof(WGPUCompilationMessage)` in bytes: pointer (8) + `WGPUStringView` (16)
 * + type (4, padded to 8) + four uint64 positions (32) = 64. Matches the stride
 * the generated `allocateArray` uses on every target.
 */
private const val WGPU_COMPILATION_MESSAGE_SIZE: Long = 64L
