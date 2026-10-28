package org.graphiks.dawn4k.toolkit.surface

import java.lang.foreign.MemorySegment
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.kffi.objc.ObjCRuntime

/** Retains a CAMetalLayer with Objective-C retain/release on its platform owner. */
fun DawnSurfaceTargets.metalLayer(
    layer: MemorySegment, owner: DawnPlatformOwner, states: StateFlow<DawnTargetState>,
): DawnSurfaceTarget = metalLayer(layer.address(), owner, states, object : DawnTargetRetention {
    override suspend fun retain() { ObjCRuntime.msgSend(null, layer, ObjCRuntime.sel("retain")) }
    override suspend fun retire() { ObjCRuntime.msgSend(null, layer, ObjCRuntime.sel("release")) }
})
