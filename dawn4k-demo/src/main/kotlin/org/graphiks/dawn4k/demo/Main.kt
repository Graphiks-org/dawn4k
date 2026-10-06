package org.graphiks.dawn4k.demo

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
        ) {
            DemoApp()
        }
    }
}
