package org.graphiks.dawn4k.demo

import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.InternalComposeUiApi
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.asComposeCanvas
import androidx.compose.ui.input.InputMode
import androidx.compose.ui.input.InputModeManager
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.pointer.PointerButton
import androidx.compose.ui.input.pointer.PointerButtons
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.PointerKeyboardModifiers
import androidx.compose.ui.platform.PlatformContext
import androidx.compose.ui.platform.WindowInfo
import androidx.compose.ui.scene.CanvasLayersComposeScene
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.CoroutineDispatcher
import org.jetbrains.skia.Bitmap
import org.jetbrains.skia.ColorAlphaType
import org.jetbrains.skia.ColorType
import org.jetbrains.skia.ImageInfo
import org.jetbrains.skia.Surface
import java.nio.ByteOrder
import java.util.concurrent.ConcurrentLinkedQueue
import kotlin.coroutines.CoroutineContext

/** No AWT window/toolkit/JAWT: common Compose content renders onto CPU Skia pixels. */
@OptIn(InternalComposeUiApi::class)
internal class ParticleComposeScene(
    controller: DemoSessionController,
    onClose: () -> Unit,
    onViewportBounds: (LogicalViewport) -> Unit,
    onControlBounds: ((String, LogicalViewport) -> Unit)? = null,
) : AutoCloseable {
    private val owner = Thread.currentThread()
    private val dispatcher = SceneOwnerDispatcher(owner)
    private val window = RasterWindowInfo()
    private val platform = object : PlatformContext.Empty() {
        override val windowInfo: WindowInfo = window
        override val inputModeManager = object : InputModeManager {
            override var inputMode: InputMode by mutableStateOf(InputMode.Keyboard)
            override fun requestInputMode(inputMode: InputMode): Boolean {
                this.inputMode = inputMode
                return true
            }
        }
        override fun requestFocus(): Boolean = window.isWindowFocused
    }
    private val scene = CanvasLayersComposeScene(
        density = Density(1f), size = IntSize.Zero,
        coroutineContext = dispatcher, platformContext = platform,
    )
    private var raster: Surface? = null
    private var bitmap: Bitmap? = null
    private var closed = false
    private var scale = 1
    private var size = IntSize.Zero

    init {
        require(ByteOrder.nativeOrder() == ByteOrder.LITTLE_ENDIAN) { "Wayland BGRA frames require little-endian pixels" }
        scene.setContent {
            ParticleDemoContent(controller, false, onClose, onViewportBounds,
                viewport = { Box(it) }, onControlBounds = onControlBounds)
        }
    }

    fun resize(logicalWidth: Int, logicalHeight: Int, scale: Int) {
        requireOwner()
        require(logicalWidth >= 0 && logicalHeight >= 0 && scale > 0) { "invalid raster extent or scale" }
        val width = logicalWidth.toLong() * scale
        val height = logicalHeight.toLong() * scale
        require(width <= Int.MAX_VALUE && height <= Int.MAX_VALUE && width * 4 <= Int.MAX_VALUE &&
            (height == 0L || width * 4 <= Int.MAX_VALUE / height)) { "raster extent/stride overflow" }
        val next = IntSize(width.toInt(), height.toInt())
        if (next == size && scale == this.scale) return
        var replacement: Surface? = null
        var replacementBitmap: Bitmap? = null
        if (next.width > 0 && next.height > 0) {
            replacement = Surface.makeRasterN32Premul(next.width, next.height)
            try {
                replacementBitmap = Bitmap()
                check(replacementBitmap.allocPixels(ImageInfo(next.width, next.height,
                    ColorType.BGRA_8888, ColorAlphaType.PREMUL), next.width * 4)) { "raster pixel allocation failed" }
            } catch (failure: Throwable) {
                replacementBitmap?.close()
                replacement.close()
                throw failure
            }
        }
        bitmap?.close()
        raster?.close()
        raster = replacement
        bitmap = replacementBitmap
        size = next
        this.scale = scale
        window.containerSize = next
        window.containerDpSize = DpSize(logicalWidth.dp, logicalHeight.dp)
        scene.density = Density(scale.toFloat())
        scene.size = next
    }

    fun render(timeNanos: Long): UiPixelFrame {
        requireOwner()
        dispatcher.drain()
        val target = raster ?: return UiPixelFrame(0, 0, 0, byteArrayOf())
        target.canvas.clear(0)
        scene.render(target.canvas.asComposeCanvas(), timeNanos)
        dispatcher.drain()
        val pixels = checkNotNull(bitmap)
        check(target.readPixels(pixels, 0, 0)) { "Skia raster pixel copy failed" }
        val bytes = checkNotNull(pixels.readPixels()) { "Skia BGRA readback failed" }
        return UiPixelFrame(size.width, size.height, size.width * 4, bytes)
    }

    fun pointer(type: PointerEventType, x: Float, y: Float, pressed: Boolean) {
        requireOwner()
        dispatcher.drain()
        if (!window.isWindowFocused || raster == null) return
        scene.sendPointerEvent(type, Offset(x * scale, y * scale),
            buttons = PointerButtons(isPrimaryPressed = pressed),
            button = if (type == PointerEventType.Press || type == PointerEventType.Release) PointerButton.Primary else null)
    }

    fun key(key: Key, type: KeyEventType, shift: Boolean = false) {
        requireOwner()
        dispatcher.drain()
        if (!window.isWindowFocused || raster == null) return
        scene.sendKeyEvent(KeyEvent(key, type, isShiftPressed = shift))
    }

    fun focus(focused: Boolean) {
        requireOwner()
        dispatcher.drain()
        if (!focused) {
            scene.cancelPointerInput()
            scene.focusManager.releaseFocus()
        }
        window.isWindowFocused = focused
    }

    override fun close() {
        check(Thread.currentThread() === owner) { "Compose raster scene accessed off its owner" }
        if (closed) return
        closed = true
        try { scene.close() }
        finally {
            try { bitmap?.close() }
            finally { raster?.close(); dispatcher.close() }
            bitmap = null
            raster = null
        }
    }

    private fun requireOwner() {
        check(Thread.currentThread() === owner) { "Compose raster scene accessed off its owner" }
        check(!closed) { "Compose raster scene is closed" }
    }

    private class RasterWindowInfo : WindowInfo {
        override var isWindowFocused: Boolean by mutableStateOf(false)
        override var containerSize: IntSize by mutableStateOf(IntSize.Zero)
        override var containerDpSize: DpSize by mutableStateOf(DpSize.Zero)
        override val keyboardModifiers = PointerKeyboardModifiers()
    }

    private class SceneOwnerDispatcher(private val owner: Thread) : CoroutineDispatcher() {
        private val pending = ConcurrentLinkedQueue<Runnable>()
        @Volatile private var closed = false
        override fun isDispatchNeeded(context: CoroutineContext) = Thread.currentThread() !== owner
        override fun dispatch(context: CoroutineContext, block: Runnable) {
            if (!closed) pending.add(block)
        }
        fun drain() {
            check(Thread.currentThread() === owner)
            while (true) (pending.poll() ?: return).run()
        }
        fun close() { closed = true; pending.clear() }
    }
}
