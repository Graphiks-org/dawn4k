package org.graphiks.dawn4k.mapper

import org.graphiks.dawn4k.native.WGPUComponentSwizzle
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_A
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_B
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_G
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_One
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_R
import org.graphiks.dawn4k.native.WGPUComponentSwizzle_Zero
import org.graphiks.dawn4k.native.WGPUIndexFormat
import org.graphiks.dawn4k.native.WGPUIndexFormat_Undefined
import org.graphiks.dawn4k.native.WGPULoadOp
import org.graphiks.dawn4k.native.WGPULoadOp_Undefined
import org.graphiks.dawn4k.native.WGPUOptionalBool
import org.graphiks.dawn4k.native.WGPUOptionalBool_False
import org.graphiks.dawn4k.native.WGPUOptionalBool_True
import org.graphiks.dawn4k.native.WGPUOptionalBool_Undefined
import org.graphiks.dawn4k.native.WGPUStoreOp
import org.graphiks.dawn4k.native.WGPUStoreOp_Undefined
import org.graphiks.webgpu.GPUIndexFormat
import org.graphiks.webgpu.GPULoadOp
import org.graphiks.webgpu.GPUStoreOp
import org.graphiks.webgpu.GPUTextureSwizzleSource

/** null is the C default Undefined; a concrete value is True or False. */
internal fun Boolean?.toNativeOptionalBool(): WGPUOptionalBool = when (this) {
    null -> WGPUOptionalBool_Undefined
    true -> WGPUOptionalBool_True
    false -> WGPUOptionalBool_False
}

// Unlike the platform enums, this common enum has no public numeric value.
internal fun GPUTextureSwizzleSource.toNativeComponentSwizzle(): WGPUComponentSwizzle = when (this) {
    GPUTextureSwizzleSource.Red -> WGPUComponentSwizzle_R
    GPUTextureSwizzleSource.Green -> WGPUComponentSwizzle_G
    GPUTextureSwizzleSource.Blue -> WGPUComponentSwizzle_B
    GPUTextureSwizzleSource.Alpha -> WGPUComponentSwizzle_A
    GPUTextureSwizzleSource.Zero -> WGPUComponentSwizzle_Zero
    GPUTextureSwizzleSource.One -> WGPUComponentSwizzle_One
}

/** The native "no explicit load op" value (C `WGPU_LOAD_OP_INIT`). */
internal val GPULoadOp?.toNativeOrUndefined: WGPULoadOp
    get() = this?.value ?: WGPULoadOp_Undefined

/** The native "no explicit store op" value (C `WGPU_STORE_OP_INIT`). */
internal val GPUStoreOp?.toNativeOrUndefined: WGPUStoreOp
    get() = this?.value ?: WGPUStoreOp_Undefined

/** The native "no explicit index format" value (C `WGPU_INDEX_FORMAT_INIT`). */
internal val GPUIndexFormat?.toNativeOrUndefined: WGPUIndexFormat
    get() = this?.value ?: WGPUIndexFormat_Undefined

/** The native "no explicit texture view format" value. */
internal const val WGPU_MIP_LEVEL_COUNT_UNDEFINED: UInt = 0xFFFFFFFFu

/** The native "no explicit array layer count" value. */
internal const val WGPU_ARRAY_LAYER_COUNT_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_WHOLE_SIZE` (SIZE_MAX): the "to the end of the buffer" size sentinel. */
internal const val WGPU_WHOLE_SIZE: ULong = ULong.MAX_VALUE

/** webgpu.h `WGPU_DEPTH_SLICE_UNDEFINED`: only 3D color attachments define a depth slice. */
internal const val WGPU_DEPTH_SLICE_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_COPY_STRIDE_UNDEFINED`: an absent bytesPerRow / rowsPerImage stride. */
internal const val WGPU_COPY_STRIDE_UNDEFINED: UInt = 0xFFFFFFFFu

/** webgpu.h `WGPU_QUERY_SET_INDEX_UNDEFINED`: an absent timestamp write index. */
internal const val WGPU_QUERY_SET_INDEX_UNDEFINED: UInt = 0xFFFFFFFFu
