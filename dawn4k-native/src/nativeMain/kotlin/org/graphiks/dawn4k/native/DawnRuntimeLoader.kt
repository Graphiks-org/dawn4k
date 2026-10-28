@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.native

import kotlinx.cinterop.toKString

public actual fun prepareDawnRuntime() {
    webgpu.native.dawn4kDawnLoadError()?.toKString()?.let { error(it) }
}
