package org.graphiks.dawn4k.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.boundsInWindow
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import java.awt.Rectangle
import kotlin.math.roundToInt

@Composable
internal fun ParticleControlPanel(
    controls: ParticleControls,
    onClose: () -> Unit,
    onControlBounds: ((String, Rectangle) -> Unit)? = null,
) {
    val state by controls.state.collectAsState()
    Column(
        Modifier.padding(start = 20.dp, top = 56.dp, end = 20.dp)
            .background(Color(0xD9263238), RoundedCornerShape(12.dp)).padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            ControlButton(if (state.paused) "Resume" else "Pause", state.ready, controls::togglePause,
                onBounds = onControlBounds)
            ControlButton("Reset", state.ready, controls::reset, onBounds = onControlBounds)
            ControlButton("Close", true, onClose, onBounds = onControlBounds)
        }
        BasicText("Particles: ${state.count}", style = TextStyle(color = Color.White))
        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            state.availableCounts.forEach { count ->
                ControlButton(
                    count.toString(), state.ready, { controls.selectCount(count) },
                    selected = count == state.count,
                    onBounds = onControlBounds,
                )
            }
        }
        if (state.error != null) {
            BasicText("Error: ${state.error}", style = TextStyle(color = Color(0xFFFFB4AB)))
        } else if (!state.ready) {
            BasicText("Initializing Dawn…", style = TextStyle(color = Color.White))
        }
    }
}

@Composable
private fun ControlButton(
    text: String,
    enabled: Boolean,
    onClick: () -> Unit,
    selected: Boolean = false,
    onBounds: ((String, Rectangle) -> Unit)? = null,
) {
    val shape = RoundedCornerShape(6.dp)
    BasicText(
        text,
        Modifier.testTag("control:$text").then(if (onBounds == null) Modifier else Modifier.onGloballyPositioned {
            val bounds = it.boundsInWindow()
            onBounds(text, Rectangle(bounds.left.roundToInt(), bounds.top.roundToInt(),
                bounds.width.roundToInt(), bounds.height.roundToInt()))
        }).background(if (selected) Color(0xFF006A75) else Color(0xFF37474F), shape)
            .border(1.dp, if (selected) Color(0xFF80DEEA) else Color(0xFF607D8B), shape)
            .semantics { this.selected = selected }
            .clickable(enabled = enabled, role = Role.Button, onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 10.dp),
        style = TextStyle(color = if (enabled) Color.White else Color(0xFF90A4AE)),
    )
}
