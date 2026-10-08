package org.graphiks.dawn4k.demo

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.EncodedImageFormat
import org.jetbrains.skia.Image
import org.jetbrains.skia.ImageInfo
import java.nio.file.Files
import java.nio.file.Path

/** Executes real raster/input qualification in a separate headless, DISPLAY-free JVM. */
fun main() {
    check(System.getProperty("java.awt.headless") == "true") { "probe must be headless" }
    check(System.getenv("DISPLAY") == null) { "probe must not have DISPLAY" }
    val report = Path.of(requireNotNull(System.getenv("DAWN_SCENE_PROBE_REPORT")))
    Files.createDirectories(report.parent)
    val tests = ParticleComposeSceneTest()
    tests.rasterControlsReceiveActualPointerPressAndRelease()
    tests.focusLossCancelsPressedControlAndUnfocusedKeysCannotAct()
    tests.tabAndEnterSpaceActivateFocusedComposeControlsNotGlobalShortcuts()
    tests.scaleTwoPreservesLogicalBoundsAndConvertsPointerExactlyOnce()
    tests.closeControlInvokesLifetimeCallbackAndSceneCloseIsIdempotent()
    tests.zeroSizeSuspendsRasterAndOverflowOrNonpositiveScaleFailsBeforeAllocation()
    tests.offOwnerCallsCannotRenderOrCloseTheNativeScene()
    tests.malformedPackedFramesCannotReachNativeShmUpload()
    tests.letterAndEscapeKeysNeverInvokeOldGlobalActions()
    val controller = DemoSessionController(CoroutineScope(SupervisorJob() + Dispatchers.Default))
    controller.controls.initialize(65536, 1)
    try {
        ParticleComposeScene(controller, {}, {}).use { scene ->
            for ((name, scale) in listOf("scale1" to 1, "scale2" to 2)) {
                scene.resize(800, 600, scale)
                val frame = scene.render(demoMonotonicNanos())
                Image.makeRaster(ImageInfo(frame.width, frame.height, ColorType.BGRA_8888,
                    ColorAlphaType.PREMUL), frame.bgra, frame.stride).use { image ->
                    checkNotNull(image.encodeToData(EncodedImageFormat.PNG)).use { png ->
                        Files.write(report.parent.resolve("$name.png"), png.bytes)
                    }
                }
            }
        }
    } finally { runBlocking { controller.close() } }
    Files.writeString(report, """{"sceneRaster":true,"pointerControls":true,"focusedKeys":true,"scale2":true,"nativeToplevelQualified":false,"dawnPresentationQualified":false,"displayAbsent":true,"headless":true}""" + "\n")
    println("[qualification] sceneRaster=true pointerControls=true focusedKeys=true scale2=true; no native toplevel/Dawn claim")
}
