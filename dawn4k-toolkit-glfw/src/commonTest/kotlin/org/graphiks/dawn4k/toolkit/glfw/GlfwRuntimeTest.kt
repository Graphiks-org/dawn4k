package org.graphiks.dawn4k.toolkit.glfw

import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.glfw.internal.*
import kotlin.test.*

class GlfwRuntimeTest {
    @Test fun lastToolkitWindowTerminatesOwnedRuntime() = runTest {
        val backend = RecordingBackend()
        val runtime = GlfwRuntime(backend)
        val first = runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        val second = runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        first.retire()
        assertEquals(0, backend.terminations)
        second.retire()
        second.retire()
        assertEquals(1, backend.initializations)
        assertEquals(1, backend.terminations)
    }
    @Test fun oneWindowClosePreservesAnother() = runTest {
        val backend = RecordingBackend()
        val runtime = GlfwRuntime(backend)
        val first = runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        val second = runtime.acquire(DawnGlfwRuntimeOwnership.Owned)
        first.retire()
        assertTrue(runtime.isActive)
        assertEquals(0, backend.terminations)
        second.retire()
        assertFalse(runtime.isActive)
    }
    @Test fun borrowedRuntimeIsNeverTerminated() = runTest {
        val backend = RecordingBackend().apply { initialized = true }
        val runtime = GlfwRuntime(backend)
        val lease = runtime.acquire(DawnGlfwRuntimeOwnership.Borrowed)
        lease.retire()
        assertEquals(0, backend.initializations)
        assertEquals(0, backend.terminations)
        assertTrue(backend.initialized)
    }
    @Test fun borrowedRuntimeMustAlreadyExist() = runTest {
        val backend = RecordingBackend()
        assertFailsWith<IllegalStateException> { GlfwRuntime(backend).acquire(DawnGlfwRuntimeOwnership.Borrowed) }
        assertEquals(0, backend.initializations)
    }
    @Test fun activeRuntimeCannotChangeOwnership() = runTest {
        val backend = RecordingBackend().apply { initialized = true }
        val runtime = GlfwRuntime(backend)
        val borrowed = runtime.acquire(DawnGlfwRuntimeOwnership.Borrowed)
        assertFailsWith<IllegalStateException> { runtime.acquire(DawnGlfwRuntimeOwnership.Owned) }
        borrowed.retire()
        assertEquals(0, backend.terminations)
    }
}
