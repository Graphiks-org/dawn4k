package org.graphiks.dawn4k.toolkit.surface

import java.lang.foreign.MemorySegment
import kotlinx.coroutines.flow.StateFlow

/** Surface extent comes from the caller's configure events and buffer scale. */
fun DawnSurfaceTargets.wayland(
    display: MemorySegment, surface: MemorySegment, owner: DawnPlatformOwner,
    states: StateFlow<DawnTargetState>, retention: DawnTargetRetention,
): DawnSurfaceTarget = wayland(display.address(), surface.address(), owner, states, retention)
