@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.AtomicLong
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFRelease
import platform.CoreFoundation.CFTypeRef
import platform.QuartzCore.CAMetalLayer

/** Retains the caller's layer on the Apple main thread for each GPU surface attachment. */
fun metalLayerTarget(
    layer: CAMetalLayer, states: StateFlow<DawnTargetState>, owner: DawnPlatformOwner = DawnPlatformOwners.appleMain(),
): DawnSurfaceTarget = metalLayerTarget(layer, states, null, owner)

internal fun metalLayerTarget(
    layer: CAMetalLayer, states: StateFlow<DawnTargetState>, retirement: TargetRetirement?, owner: DawnPlatformOwner = DawnPlatformOwners.appleMain(),
): DawnSurfaceTarget = object : DawnSurfaceTarget {
    override val state = states
    override suspend fun retain(): DawnTargetLease {
        val address = atomic(0L)
        var reference: CFTypeRef? = null
        var generation = 0L
        val retention = object : DawnTargetRetention {
            override suspend fun retain() {
                generation = states.value.generation
                val pointer = checkNotNull(interpretCPointer<ByteVar>(layer.objcPtr()))
                val acquired = checkNotNull(CFRetain(pointer))
                try {
                    retirement?.acquired(generation)
                    reference = acquired
                    address.value = layer.objcPtr().toLong()
                } catch (failure: Throwable) { CFRelease(acquired); throw failure }
            }
            override suspend fun retire() {
                reference?.let {
                    CFRelease(it)
                    reference = null
                    address.value = 0L
                    retirement?.retired(generation)
                }
            }
        }
        val target = DawnSurfaceTargets.retained(NativeSurfaceSource.Metal { address.value }, owner, states, retention)
        return AppleTargetLease(target.retain(), address)
    }
}

internal class AppleTargetLease(
    private val delegate: DawnTargetLease,
    private val address: AtomicLong,
) : DawnTargetLease by delegate {
    val retainedLayerAddress: Long get() = address.value
}
