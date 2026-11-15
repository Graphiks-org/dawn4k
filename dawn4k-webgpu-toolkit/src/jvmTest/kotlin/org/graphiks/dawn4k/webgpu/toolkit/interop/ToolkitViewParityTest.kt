package org.graphiks.dawn4k.webgpu.toolkit.interop

import kotlin.test.Test
import kotlin.test.assertTrue

/**
 * Checks that the dawn4k-shaped views expose the members a dawn4k session and window publish,
 * which is what lets code written against `DawnSession` and `DawnWindow` run unchanged.
 */
class ToolkitViewParityTest {

    /**
     * Method names without the JVM mangling suffix Kotlin adds for value-class parameters, so
     * `awaitLost-IoAF18A` reads as `awaitLost`.
     */
    private fun members(type: Class<*>): Set<String> =
        type.methods.map { it.name.substringBefore('-') }.toSet()

    @Test
    fun `the session view exposes context adapter state and manualProgress`() {
        val names = members(ToolkitSessionView::class.java)

        for (member in listOf("getContext", "getAdapter", "getState", "getManualProgress")) {
            assertTrue(member in names, "missing $member: $names")
        }
    }

    @Test
    fun `the session view keeps the members it already had`() {
        val names = members(ToolkitSessionView::class.java)

        for (member in listOf("getDevice", "getQueue", "run", "awaitLost", "createRenderTexture")) {
            assertTrue(member in names, "missing $member: $names")
        }
    }

    @Test
    fun `the window view exposes extent`() {
        val names = members(ToolkitWindowView::class.java)

        assertTrue("getExtent" in names, "missing getExtent: $names")
        assertTrue("getNativeHandle" in names, "nativeHandle must stay available: $names")
        assertTrue("getEvents" in names, "events must stay available: $names")
    }
}
