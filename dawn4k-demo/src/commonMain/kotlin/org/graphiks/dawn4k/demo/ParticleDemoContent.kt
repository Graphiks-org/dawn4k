package org.graphiks.dawn4k.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

@Composable
internal fun ParticleDemoContent(
    controller: DemoSessionController,
    mobile: Boolean,
    onLifetimeAction: () -> Unit,
    onViewportBounds: (LogicalViewport) -> Unit,
    viewport: @Composable (Modifier) -> Unit,
    onControlBounds: ((String, LogicalViewport) -> Unit)? = null,
) {
    val session by controller.state.collectAsState()
    val density = LocalDensity.current.density
    val lifetimeLabel = if (!mobile) "Close" else when (session.phase) {
        DemoPhase.Stopped, DemoPhase.Failed -> "Restart"
        else -> "Stop"
    }
    val lifetimeEnabled = !mobile || session.phase != DemoPhase.Stopping
    val viewportModifier = Modifier.testTag("particle-viewport").onGloballyPositioned {
        val b = it.boundsInWindow()
        val bounds = LogicalViewport(b.left / density, b.top / density,
            b.width / density, b.height / density)
        controller.evidence.recordViewport(bounds)
        onViewportBounds(bounds)
    }
    val controlBounds: (String, LogicalViewport) -> Unit = { label, bounds ->
        controller.evidence.recordControl(label, bounds)
        onControlBounds?.invoke(label, bounds)
    }
    BoxWithConstraints(Modifier.fillMaxSize()) {
        val compactPanelMaximum = maxHeight * 0.6f
        if (demoLayoutMode(maxWidth.value, maxHeight.value) == LayoutMode.Wide) {
            Row(Modifier.fillMaxSize()) {
                ParticleControlsContent(controller.controls, onLifetimeAction,
                    Modifier.width(300.dp).fillMaxHeight(), mobile, lifetimeLabel, lifetimeEnabled,
                    controlBounds)
                viewport(viewportModifier.weight(1f).fillMaxHeight())
            }
        } else {
            Column(Modifier.fillMaxSize()) {
                viewport(viewportModifier.fillMaxWidth().weight(1f))
                ParticleControlsContent(controller.controls, onLifetimeAction,
                    Modifier.fillMaxWidth().heightIn(max = compactPanelMaximum), mobile,
                    lifetimeLabel, lifetimeEnabled, controlBounds)
            }
        }
    }
}

@Composable
internal fun ParticleControlsContent(
    controls: ParticleControls,
    onLifetimeAction: () -> Unit,
    modifier: Modifier = Modifier,
    mobile: Boolean = false,
    lifetimeLabel: String = "Close",
    lifetimeEnabled: Boolean = true,
    onControlBounds: ((String, LogicalViewport) -> Unit)? = null,
) {
    val state by controls.state.collectAsState()
    Column(modifier.background(Color(0xFF101820)).verticalScroll(rememberScrollState()).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)) {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            ControlButton(if (state.paused) "Resume" else "Pause", state.ready,
                controls::togglePause, mobile, onBounds = onControlBounds)
            ControlButton("Reset", state.ready, controls::reset, mobile, onBounds = onControlBounds)
            ControlButton(lifetimeLabel, lifetimeEnabled, onLifetimeAction, mobile, onBounds = onControlBounds)
        }
        BasicText("Particles: ${state.count}", style = TextStyle(color = Color.White))
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)) {
            state.availableCounts.forEach { count ->
                ControlButton(count.toString(), state.ready, { controls.selectCount(count) }, mobile,
                    selected = count == state.count, onBounds = onControlBounds)
            }
        }
        if (state.error != null) BasicText("Error: ${state.error}", style = TextStyle(color = Color(0xFFFFB4AB)))
        else if (!state.ready) BasicText(if (state.accepting) "Initializing Dawn…" else "Stopped",
            style = TextStyle(color = Color.White))
    }
}

@Composable
private fun ControlButton(
    text: String, enabled: Boolean, onClick: () -> Unit, mobile: Boolean,
    selected: Boolean = false, onBounds: ((String, LogicalViewport) -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    val shape = RoundedCornerShape(6.dp)
    Box(Modifier.testTag("control:$text").onGloballyPositioned {
        val b = it.boundsInWindow()
        onBounds?.invoke(text, LogicalViewport(b.left / density, b.top / density,
            b.width / density, b.height / density))
    }.background(if (selected) Color(0xFF006A75) else Color(0xFF37474F), shape)
        .border(1.dp, if (selected) Color(0xFF80DEEA) else Color(0xFF607D8B), shape)
        .semantics { this.selected = selected }
        .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
        .then(if (mobile) Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp) else Modifier)
        .padding(horizontal = 12.dp, vertical = 10.dp), contentAlignment = androidx.compose.ui.Alignment.Center) {
        BasicText(text, style = TextStyle(color = if (enabled) Color.White else Color(0xFF90A4AE)))
    }
}
