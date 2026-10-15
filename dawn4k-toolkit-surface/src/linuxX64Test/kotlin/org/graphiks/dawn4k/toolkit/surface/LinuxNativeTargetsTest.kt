package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class LinuxNativeTargetsTest {
    @Test fun linuxFactoriesRejectNullNativeTargets() = runTest {
        val owner = DawnPlatformOwner.fromDispatcher(Dispatchers.Unconfined)
        val states = MutableStateFlow(DawnTargetState(1, PixelExtent(1, 1), true))
        val retention = object : DawnTargetRetention {
            override suspend fun retain() {}
            override suspend fun retire() {}
        }
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.x11(0, 0u, owner, states, retention) }
        assertFailsWith<IllegalArgumentException> { DawnSurfaceTargets.wayland(0, 0, owner, states, retention) }
    }
}
