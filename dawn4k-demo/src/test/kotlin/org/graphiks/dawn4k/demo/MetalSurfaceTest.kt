package org.graphiks.dawn4k.demo

import java.lang.foreign.MemorySegment
import java.lang.foreign.ValueLayout
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertNotNull
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability

/**
 * Headless tests for the ObjC bridge: they create a bare NSView (no window),
 * force its layer, and verify the CAMetalLayer retrieval. macOS-only at runtime;
 * the test class is guarded by an OS check.
 */
@OptIn(PlatformAvailability::class)
class MetalSurfaceTest {

    @Test
    fun nsViewLayerIsRetrievableAfterSetWantsLayer() {
        if (System.getProperty("os.name").lowercase().contains("mac").not()) return
        ObjCRuntime.autoreleasePool {
            // Create a bare NSView via alloc + init (initWithFrame: takes an
            // NSRect by value — ObjCStructArg is internal to kffi-objc).
            val viewClass = ObjCRuntime.getClass("NSView")
            val allocated = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, viewClass, ObjCRuntime.sel("alloc")
            ) as MemorySegment
            assertNotEquals(MemorySegment.NULL, allocated)
            val view = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, allocated, ObjCRuntime.sel("init")
            ) as MemorySegment
            assertNotEquals(MemorySegment.NULL, view)

            // Force layer creation.
            val nsView = NSView(view)
            nsView.setWantsLayer(true)
            val layer = nsView.layer()
            assertNotEquals(MemorySegment.NULL, layer, "setWantsLayer(true) must create a layer")
        }
    }

    @Test
    fun createdCaMetalLayerIsDetectedAsCaMetalLayer() {
        if (System.getProperty("os.name").lowercase().contains("mac").not()) return
        ObjCRuntime.autoreleasePool {
            val metalLayer = MetalSurface.createCaMetalLayer()
            assertNotNull(metalLayer)
            // The class of the created layer must be CAMetalLayer.
            val layerClass = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, metalLayer, ObjCRuntime.sel("class")
            ) as MemorySegment
            val expectedClass = ObjCRuntime.getClass("CAMetalLayer")
            assertEquals(expectedClass, layerClass, "the created layer must be a CAMetalLayer")
        }
    }
}
