@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.cinterop.objcPtr
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.coroutines.flow.StateFlow
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.QuartzCore.CAMetalLayer

internal actual val surfacePlatform: SurfacePlatform get() = SurfacePlatform.Apple

/** Keeps an independent native reference for each retained lease. */
fun DawnSurfaceTargets.metalLayer(
    layer: CAMetalLayer, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>,
): DawnSurfaceTarget {
    val references = mutableListOf<CFTypeRef>()
    return metalLayer(layer.objcPtr().toLong(), owner, states, object : DawnTargetRetention {
        override suspend fun retain() {
            val address = checkNotNull(interpretCPointer<ByteVar>(layer.objcPtr()))
            references += checkNotNull(CFRetain(address))
        }
        override suspend fun retire() { CFRelease(references.removeAt(references.lastIndex)) }
    })
}

object DawnPlatformOwners {
    fun appleMain(): DawnPlatformOwner = appleMainOwner()
}
