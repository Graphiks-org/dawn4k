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
    val platform = detectDemoPlatform(os)
    if (platform == null) {
        System.err.println("dawn4k-demo supports macOS, Windows and Linux/X11. Detected: $os")
        return
    }
    if (platform == DemoPlatform.Linux) {
        check(!System.getenv("DISPLAY").isNullOrBlank()) {
            "missing DISPLAY; launch through dawn4k-demo/docker's /opt/demo/demo.sh"
        }
        initializeLinuxXlibThreading()
    }
    val macos = platform == DemoPlatform.MacOS
    val backend = when (platform) {
        DemoPlatform.MacOS -> "Metal"
        DemoPlatform.Windows -> "D3D12"
        DemoPlatform.Linux -> "Vulkan"
    }
    androidx.compose.ui.window.application {
        androidx.compose.ui.window.Window(
            title = "dawn4k-demo — ParticleScene (Dawn/$backend)",
            onCloseRequest = ::exitApplication,
            undecorated = macos,
            transparent = macos,
        ) {
            Box(Modifier.fillMaxSize()) {
                DemoApp(window, onClose = ::exitApplication)
                if (macos) WindowDraggableArea {
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
