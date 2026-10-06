package org.graphiks.dawn4k.demo

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.window.WindowDraggableArea
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp

fun main() {
    val os = System.getProperty("os.name").lowercase()
    if (!os.contains("mac")) {
        System.err.println("dawn4k-demo runs on macOS only (Metal surface). Detected: $os")
        return
    }
    androidx.compose.ui.window.application {
        androidx.compose.ui.window.Window(
            title = "dawn4k-demo — ParticleScene (Dawn/Metal)",
            onCloseRequest = ::exitApplication,
            undecorated = true,
            transparent = true,
        ) {
            Box(Modifier.fillMaxSize()) {
                DemoApp(window, onClose = ::exitApplication)
                WindowDraggableArea {
                    BasicText(
                        "ParticleScene · Dawn / Metal — drag to move",
                        Modifier.background(Color(0xD9263238)).padding(12.dp),
                        style = TextStyle(color = Color.White),
                    )
                }
            }
        }
    }
}
