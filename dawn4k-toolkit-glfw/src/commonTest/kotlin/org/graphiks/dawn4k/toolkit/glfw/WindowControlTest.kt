package org.graphiks.dawn4k.toolkit.glfw

import org.graphiks.dawn4k.toolkit.DawnSession
import org.graphiks.dawn4k.toolkit.surface.DawnPresentation
import kotlin.test.*

class WindowControlTest {
    private fun exercise(partial: Boolean, operation: suspend (DawnWindow) -> Unit) {
        val backend = RecordingBackend()
        DawnMainThreadLoop.run(backend) { main ->
            val window = main.owner.onOwner {
                val runtime = main.runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
                val native = NativeWindowLease(backend, backend.createWindow(DawnWindowOptions()), runtime)
                DawnWindow(main, native, object : WindowSession {
                    override val gpu: DawnSession get() = error("control tests do not access a GPU")
                    override val presentation: DawnPresentation get() = error("control tests do not access a surface")
                    override suspend fun shutdown() {}
                }).also(main::register)
            }
            try {
                if (partial) {
                    backend.beforeTermination = { throw IllegalStateException("runtime still retiring") }
                    assertFailsWith<IllegalStateException> { window.shutdown() }
                } else window.shutdown()
                assertEquals(1, backend.destroys)
                assertFailsWith<IllegalStateException> { operation(window) }
                assertEquals(0, backend.closeRequests)
                assertEquals(0, backend.resizeRequests)
            } finally {
                backend.beforeTermination = {}
                window.shutdown()
            }
        }
        assertEquals(1, backend.destroys)
    }

    @Test fun closeRequestRejectsFullyRetiredWindow() = exercise(false) { it.requestClose() }
    @Test fun closeRequestRejectsDestroyedWindowBeforeRuntimeRetires() = exercise(true) { it.requestClose() }
    @Test fun resizeRejectsDestroyedWindowBeforeRuntimeRetires() = exercise(true) { it.resize(40, 40) }
}
