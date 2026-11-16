package org.graphiks.dawn4k.webgpu.toolkit.interop

import kotlin.coroutines.Continuation
import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Checks that the dawn4k presentation view exposes the manual frame trio, which is what makes a
 * dawn4k render loop written against `acquireFrame` / `present` / `releaseFrame` compile and run
 * on the generic toolkit.
 */
class ToolkitPresentationViewFrameApiTest {

    private val methods = ToolkitPresentationView::class.java.methods

    @Test
    fun `the view exposes acquireFrame present and releaseFrame`() {
        val names = methods.map { it.name }.toSet()

        assertTrue("acquireFrame" in names, "missing acquireFrame: $names")
        assertTrue("present" in names, "missing present: $names")
        assertTrue("releaseFrame" in names, "missing releaseFrame: $names")
        assertTrue("withFrame" in names, "withFrame must stay available: $names")
    }

    @Test
    fun `acquireFrame is suspending and present is not`() {
        val acquire = methods.single { it.name == "acquireFrame" }
        val present = methods.single { it.name == "present" }

        // A suspending function carries a trailing Continuation parameter in its JVM signature.
        assertTrue(
            acquire.parameterTypes.last() == Continuation::class.java,
            "acquireFrame must suspend while the target is unavailable: ${acquire.parameterTypes.toList()}",
        )
        assertTrue(
            present.parameterTypes.none { it == Continuation::class.java },
            "present must not suspend, like dawn4k's DawnFrame.present: ${present.parameterTypes.toList()}",
        )
    }
}
