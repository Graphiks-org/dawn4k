package org.graphiks.dawn4k.demo

import java.awt.Toolkit
import java.lang.foreign.MemorySegment
import java.lang.foreign.Arena
import java.lang.foreign.FunctionDescriptor
import java.lang.foreign.Linker
import java.lang.foreign.ValueLayout
import java.lang.invoke.MethodHandles
import java.lang.invoke.MethodType
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertTrue
import org.graphiks.kffi.objc.CALayer
import org.graphiks.kffi.objc.CGPoint
import org.graphiks.kffi.objc.CGRect
import org.graphiks.kffi.objc.CGSize
import org.graphiks.kffi.objc.NSThread
import org.graphiks.kffi.objc.NSView
import org.graphiks.kffi.objc.ObjCRuntime
import org.graphiks.kffi.objc.PlatformAvailability
import org.graphiks.kffi.objc.CFRelease
import org.graphiks.kffi.objc.CFRunLoopGetMain
import org.graphiks.kffi.objc.CFRunLoopAddSource
import org.graphiks.kffi.objc.CFRunLoopRemoveSource
import org.graphiks.kffi.objc.CFRunLoopRunInMode
import org.graphiks.kffi.objc.CFRunLoopSourceContext
import org.graphiks.kffi.objc.CFRunLoopSourceCreate
import org.graphiks.kffi.objc.NSEventTrackingRunLoopMode

@OptIn(PlatformAvailability::class)
class MetalLayerHostTest {
    private fun isMac(): Boolean = System.getProperty("os.name").lowercase().contains("mac")

    @Test
    fun nativeUiWorkContinuesDuringMouseTracking() {
        if (!isMac()) return
        Toolkit.getDefaultToolkit()
        val enteredTracking = CountDownLatch(1)
        val executor = Executors.newSingleThreadExecutor()
        val tracking = executor.submit {
            onAppKitThread {
                Arena.ofConfined().use { arena ->
                    // An unsignalled source keeps the real tracking-mode run loop alive.
                    val context = CFRunLoopSourceContext.allocate(arena)
                    val callback = Linker.nativeLinker().upcallStub(
                        MethodHandles.lookup().findVirtual(
                            MetalLayerHostTest::class.java, "keepTrackingAlive",
                            MethodType.methodType(Void.TYPE, MemorySegment::class.java)
                        ).bindTo(this),
                        FunctionDescriptor.ofVoid(ValueLayout.ADDRESS), arena,
                    )
                    CFRunLoopSourceContext().perform(context, callback)
                    val source = CFRunLoopSourceCreate(
                        MemorySegment.NULL, 0L, context
                    )
                    val loop = CFRunLoopGetMain()
                    CFRunLoopAddSource(loop, source, NSEventTrackingRunLoopMode)
                    try {
                        enteredTracking.countDown()
                        CFRunLoopRunInMode(NSEventTrackingRunLoopMode, 1.5, 0)
                    } finally {
                        CFRunLoopRemoveSource(loop, source, NSEventTrackingRunLoopMode)
                        CFRelease(source)
                    }
                }
            }
        }
        try {
            assertTrue(enteredTracking.await(5, TimeUnit.SECONDS))
            val started = System.nanoTime()
            assertTrue(onAppKitThread { NSThread.isMainThread() })
            val millis = (System.nanoTime() - started) / 1_000_000
            assertTrue(millis < 750, "native UI work stalled until tracking ended (${millis}ms)")
        } finally {
            tracking.get(5, TimeUnit.SECONDS)
            executor.shutdownNow()
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun keepTrackingAlive(info: MemorySegment) {}

    @Test
    fun nativeUiWorkRunsOnAppKitIncludingNestedCalls() {
        if (!isMac()) return
        Toolkit.getDefaultToolkit()
        assertTrue(onAppKitThread { NSThread.isMainThread() })
        assertTrue(onAppKitThread { onAppKitThread { NSThread.isMainThread() } })
        assertFailsWith<IllegalArgumentException> {
            onAppKitThread { throw IllegalArgumentException("callback failure") }
        }
    }

    @Test
    fun overlayTracksViewBoundsAndDetachesOnClose() {
        if (!isMac()) return
        Toolkit.getDefaultToolkit()
        onAppKitThread {
            val allocated = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, ObjCRuntime.getClass("NSView"), ObjCRuntime.sel("alloc")
            ) as MemorySegment
            val pointer = ObjCRuntime.msgSend(
                ValueLayout.ADDRESS, allocated, ObjCRuntime.sel("init")
            ) as MemorySegment
            val view = NSView(pointer)
            try {
                view.setFrame(CGRect(CGPoint(0.0, 0.0), CGSize(320.0, 240.0)))
                val host = MetalLayerHost.attach(pointer.address())
                val overlay = MemorySegment.ofAddress(host.layerPtr)
                // Keep a test-owned reference to inspect detachment after host.close().
                ObjCRuntime.msgSend(null, overlay, ObjCRuntime.sel("retain"))
                try {
                    assertEquals(320 to 240, host.pixelSize())
                    view.setFrame(CGRect(CGPoint(0.0, 0.0), CGSize(640.0, 360.0)))
                    assertEquals(640 to 360, host.pixelSize())
                    assertEquals(640.0, CALayer(overlay).frame().size.width)
                    assertEquals(360.0, CALayer(overlay).frame().size.height)
                    host.close()
                    host.close()
                    assertEquals(MemorySegment.NULL, CALayer(overlay).superlayer())
                    assertFailsWith<IllegalStateException> { host.pixelSize() }
                } finally {
                    host.close()
                    ObjCRuntime.msgSend(null, overlay, ObjCRuntime.sel("release"))
                }
            } finally {
                ObjCRuntime.msgSend(null, pointer, ObjCRuntime.sel("release"))
            }
        }
    }
}
