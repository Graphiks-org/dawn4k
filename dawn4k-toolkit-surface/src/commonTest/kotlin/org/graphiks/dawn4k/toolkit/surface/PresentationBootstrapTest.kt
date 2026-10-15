package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.DawnContext
import org.graphiks.dawn4k.toolkit.*
import org.graphiks.dawn4k.toolkit.surface.internal.*
import kotlin.test.*

class PresentationBootstrapTest {
    @Test fun capabilityFailureRetiresSurfaceAndTargetBeforeContext() = runTest {
        val target = CountingTarget(1)
        val native = CountingSurface().apply { capabilityFailure = IllegalStateException("unsupported capability") }
        var context: DawnContext? = null
        val factory = SurfaceFactory { lease, suppliedContext ->
            context = suppliedContext
            native.onClose = {
                assertNotEquals(0L, suppliedContext.nativeBridge().instanceHandle())
            }
            target.onRetire = {
                assertEquals(1, native.closes)
                assertNotEquals(0L, suppliedContext.nativeBridge().instanceHandle())
            }
            SurfaceAttachment(lease, null, { native }, native::close)
        }
        assertFailsWith<IllegalStateException> {
            openDawnSurface(target, DawnSessionOptions(), DawnPresentationOptions(), factory)
        }
        assertEquals(1, native.closes)
        assertEquals(1, target.retirements)
        assertEquals(0L, assertNotNull(context).nativeBridge().instanceHandle())
    }
}
