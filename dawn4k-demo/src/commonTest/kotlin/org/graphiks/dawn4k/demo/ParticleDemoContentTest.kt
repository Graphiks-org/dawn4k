package org.graphiks.dawn4k.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.*
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class ParticleDemoContentTest {
    @Test fun failedSessionDisplaysFirstErrorAndDisablesGpuButtons() = runComposeUiTest {
        val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        try {
            controller.controls.initialize(65536, 1)
            controller.failed(1, "GPU unavailable")
            waitUntil { controller.state.value.phase == DemoPhase.Failed }
            setContent {
                Box(Modifier.requiredSize(320.dp, 480.dp)) {
                    ParticleDemoContent(controller, true, {}, {}, { Box(it) })
                }
            }
            onNodeWithText("Error: GPU unavailable").performScrollTo().assertIsDisplayed()
            onNodeWithText("Pause").assertIsNotEnabled()
            onNodeWithText("Reset").assertIsNotEnabled()
            onNodeWithText("256").assertIsNotEnabled()
            onNodeWithText("Restart").assertIsEnabled()
        } finally { runBlocking { controller.close() } }
    }

    @Test fun layoutThresholdDependsOnBothAvailableDimensions() {
        assertEquals(LayoutMode.Compact, demoLayoutMode(719f, 600f))
        assertEquals(LayoutMode.Compact, demoLayoutMode(720f, 359f))
        assertEquals(LayoutMode.Wide, demoLayoutMode(720f, 360f))
    }

    @Test fun initializingDesktopCloseEnabledButGpuControlsDisabled() = runComposeUiTest {
        val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        var closed = false
        try {
            setContent {
                Box(Modifier.requiredSize(719.dp, 600.dp)) {
                    ParticleDemoContent(controller, false, { closed = true }, {}, { Box(it) })
                }
            }
            onNodeWithText("Pause").assertIsNotEnabled()
            onNodeWithText("Reset").assertIsNotEnabled()
            onNodeWithText("Close").assertIsEnabled().performClick()
            assertTrue(closed)
        } finally { runBlocking { controller.close() } }
    }

    @Test fun mobileTargetsAreTouchSizedAndRealClicksMutateControlsInCompactLayout() = runComposeUiTest {
        val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val buttons = mutableMapOf<String, LogicalViewport>()
        var viewport: LogicalViewport? = null
        try {
            controller.controls.initialize(65536, 1)
            setContent {
                Box(Modifier.requiredSize(320.dp, 480.dp)) {
                    ParticleDemoContent(controller, true, {}, { viewport = it }, { Box(it) },
                        { label, bounds -> buttons[label] = bounds })
                }
            }
            onNodeWithText("Pause").assertHeightIsAtLeast(48.dp).performClick()
            assertTrue(controller.controls.state.value.paused)
            onNodeWithText("Reset").performClick()
            assertEquals(1L, controller.controls.state.value.resetGeneration)
            onNodeWithText("256").performClick()
            assertEquals(256, controller.controls.state.value.count)
            onNodeWithText("65536").performScrollTo().assertIsDisplayed()
            assertTrue(buttons.getValue("Reset").y >= checkNotNull(viewport).y + checkNotNull(viewport).height)
        } finally { runBlocking { controller.close() } }
    }

    @Test fun wideLayoutPlacesAllControlsLeftOfNativeViewport() = runComposeUiTest {
        val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
        val buttons = mutableMapOf<String, LogicalViewport>()
        var viewport: LogicalViewport? = null
        try {
            setContent {
                Box(Modifier.requiredSize(720.dp, 360.dp)) {
                    ParticleDemoContent(controller, false, {}, { viewport = it }, { Box(it) },
                        { label, bounds -> buttons[label] = bounds })
                }
            }
            onNodeWithText("Close").assertIsDisplayed()
            assertEquals(viewport, controller.evidence.snapshot().viewport)
            assertEquals(buttons["Close"], controller.evidence.snapshot().buttons["Close"])
            assertTrue(buttons.getValue("Close").x + buttons.getValue("Close").width <= checkNotNull(viewport).x)
        } finally { runBlocking { controller.close() } }
    }
}
