@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)
package org.graphiks.dawn4k.toolkit.surface

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.interpretCPointer
import kotlinx.cinterop.objcPtr
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.toolkit.surface.internal.NativeSurfaceSource
import org.graphiks.dawn4k.toolkit.surface.internal.TargetRetirement
import org.graphiks.dawn4k.toolkit.surface.internal.retainedHandleTarget
import platform.CoreFoundation.CFRetain
import platform.CoreFoundation.CFRelease
import platform.QuartzCore.CAMetalLayer

/** Retains the caller's layer on the Apple main thread for each GPU surface attachment. */
fun metalLayerTarget(
    layer: CAMetalLayer, states: StateFlow<DawnTargetState>, owner: DawnPlatformOwner = DawnPlatformOwners.appleMain(),
    retention: DawnTargetRetention = EmptyTargetRetention,
): DawnSurfaceTarget = metalLayerTarget(layer, states, retention, null, owner)

internal fun metalLayerTarget(
    layer: CAMetalLayer, states: StateFlow<DawnTargetState>,
    retention: DawnTargetRetention = EmptyTargetRetention, retirement: TargetRetirement?, owner: DawnPlatformOwner = DawnPlatformOwners.appleMain(),
): DawnSurfaceTarget = retainedHandleTarget(
    source = { NativeSurfaceSource.Metal(it) },
    acquire = {
        val pointer = checkNotNull(interpretCPointer<ByteVar>(layer.objcPtr()))
        checkNotNull(CFRetain(pointer))
        layer.objcPtr().toLong()
    },
    release = { handle ->
        check(handle == layer.objcPtr().toLong()) { "released handle does not match the retained layer" }
        CFRelease(checkNotNull(interpretCPointer<ByteVar>(layer.objcPtr())))
    },
    owner = owner,
    states = states,
    retention = retention,
    retirement = retirement,
)
