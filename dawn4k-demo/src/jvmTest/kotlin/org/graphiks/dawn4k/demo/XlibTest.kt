package org.graphiks.dawn4k.demo

import java.lang.foreign.Arena
import java.lang.foreign.MemorySegment
import kotlinx.coroutines.CancellationException
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.graphiks.kffi.x11.generated.XDefaultRootWindow
import org.graphiks.kffi.x11.generated.CWOverrideRedirect
import org.graphiks.kffi.x11.generated.XChangeWindowAttributes
import org.graphiks.kffi.x11.generated.XSetWindowAttributes

/** Exercise the demo facade against a real Linux X server, without GPU setup. */
class XlibTest {
    @Test fun childWindowFollowsGeometryVisibilityAndDestruction() = withDisplay { display ->
        val window = Xlib.createChild(display, XDefaultRootWindow(display))
        var destroyed = false
        try {
            // Do not let the desktop window manager resize this test's root child.
            Arena.ofConfined().use { arena ->
                val attributes = XSetWindowAttributes.allocate(arena)
                XSetWindowAttributes().override_redirect(attributes, 1)
                XChangeWindowAttributes(display, window, CWOverrideRedirect(), attributes)
            }
            Xlib.selectStructureEvents(display, window)
            Xlib.moveResize(display, window, 17, 23, 80, 56)
            Xlib.map(display, window)
            Xlib.sync(display)
            assertEquals(80 to 56, Xlib.size(display, window))
            assertTrue(Xlib.isViewable(display, window))
            assertFalse(Xlib.wasDestroyed(display))

            Xlib.unmap(display, window)
            Xlib.sync(display)
            assertFalse(Xlib.isViewable(display, window))

            Xlib.destroy(display, window)
            destroyed = true
            Xlib.sync(display)
            assertTrue(Xlib.wasDestroyed(display))
            assertFalse(Xlib.wasDestroyed(display))
        } finally {
            if (!destroyed) Xlib.destroy(display, window)
        }
    }

    @Test fun destroyedDrawableErrorsCancelPresentation() = withDisplay { display ->
        val window = Xlib.createChild(display, XDefaultRootWindow(display))
        Xlib.destroy(display, window)
        Xlib.sync(display)
        val failure = assertFailsWith<CancellationException> { Xlib.size(display, window) }
        assertTrue(failure.message.orEmpty().contains("error=9")) // BadDrawable
        Xlib.sync(display) // The reported batch was drained.
    }

    @Test fun invalidRequestsAreReportedOnlyForTheirDisplay() = withDisplay { display ->
        val window = Xlib.createChild(display, XDefaultRootWindow(display))
        val otherDisplay = Xlib.openDisplay()
        try {
            Xlib.moveResize(display, window, 0, 0, 0, 16)
            Xlib.sync(otherDisplay)
            val failure = assertFailsWith<IllegalStateException> { Xlib.sync(display) }
            assertTrue(failure.message.orEmpty().contains("error=2, request=12")) // BadValue, ConfigureWindow
            Xlib.sync(display)
        } finally {
            Xlib.closeDisplay(otherDisplay)
            Xlib.destroy(display, window)
        }
    }

    private fun withDisplay(operation: (MemorySegment) -> Unit) {
        if (!System.getProperty("os.name").contains("Linux") || System.getenv("DAWN_DESKTOP_TESTS") != "1") return
        initializeLinuxXlibThreading()
        val display = Xlib.openDisplay()
        try { operation(display) } finally { Xlib.closeDisplay(display) }
    }
}
