package org.graphiks.dawn4k.demo

import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import java.awt.Rectangle
import kotlin.math.roundToInt

/** Legacy qualification wrapper; controls themselves are common Compose content. */
@Composable
internal fun ParticleControlPanel(
    controls: ParticleControls,
    onClose: () -> Unit,
    onControlBounds: ((String, Rectangle) -> Unit)? = null,
) {
    val density = LocalDensity.current.density
    ParticleControlsContent(controls, onClose,
        Modifier.padding(start = 20.dp, top = 56.dp, end = 20.dp),
        onControlBounds = if (onControlBounds == null) null else { name, b ->
            onControlBounds(name, Rectangle((b.x * density).roundToInt(), (b.y * density).roundToInt(),
                (b.width * density).roundToInt(), (b.height * density).roundToInt()))
        })
}
