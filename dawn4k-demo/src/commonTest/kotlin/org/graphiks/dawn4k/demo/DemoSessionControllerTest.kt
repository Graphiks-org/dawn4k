package org.graphiks.dawn4k.demo

import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.yield
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class DemoSessionControllerTest {
    @Test fun closeCannotWaitForeverAfterCallerScopeWasCancelled() = runBlocking {
        val parent = SupervisorJob()
        val scope = CoroutineScope(coroutineContext + parent)
        val controller = DemoSessionController(scope)
        yield()
        parent.cancelAndJoin()
        withTimeout(1000) { controller.close() }
        assertEquals(DemoPhase.Stopped, controller.state.value.phase)
    }

    @Test fun repeatedStopAndRestartCommandsCreateOnlyOneNewSession() = runBlocking {
        val controller = DemoSessionController(this)
        try {
            coroutineScope { List(5) { async { controller.stop() } }.awaitAll() }
            assertEquals(DemoPhase.Stopped, controller.state.value.phase)
            assertFalse(controller.controls.state.value.ready)
            coroutineScope { List(5) { async { controller.restart() } }.awaitAll() }
            assertEquals(2L, controller.state.value.id)
            assertEquals(DemoPhase.Initializing, controller.state.value.phase)
            controller.close()
            controller.restart()
            assertEquals(2L, controller.state.value.id)
            assertEquals(DemoPhase.Stopped, controller.state.value.phase)
        } finally { controller.close() }
    }

    @Test fun lifecycleDoesNotRewriteUserPauseAndStaleCallbacksCannotFailNewSession() = runBlocking {
        val controller = DemoSessionController(this)
        try {
            controller.controls.initialize(65536, 1)
            controller.controls.togglePause()
            controller.lifecycle(false)
            withTimeout(5000) { controller.state.first { !it.lifecycleActive } }
            assertEquals(true, controller.controls.state.value.paused)
            controller.lifecycle(true)
            withTimeout(5000) { controller.state.first { it.lifecycleActive } }
            assertEquals(true, controller.controls.state.value.paused)
            controller.stop()
            controller.restart()
            controller.failed(1, "old error")
            controller.ready(1)
            // Stop is an acknowledged command barrier; stale events precede it.
            controller.stop()
            assertEquals(null, controller.state.value.error)
            assertEquals(2L, controller.state.value.id)
        } finally { controller.close() }
    }

    @Test fun firstSessionErrorWinsAndDisablesControlMutations() = runBlocking {
        val controller = DemoSessionController(this)
        try {
            controller.failed(1, "device lost")
            controller.failed(1, "cleanup failed")
            withTimeout(5000) { controller.state.first { it.phase == DemoPhase.Failed } }
            controller.stop()
            assertEquals("device lost", controller.state.value.error)
            controller.controls.initialize(65536, 1)
            assertFalse(controller.controls.state.value.ready)
        } finally { controller.close() }
    }
}
