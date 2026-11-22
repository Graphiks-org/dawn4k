package org.graphiks.dawn4k.compose

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.StateFlow
import org.graphiks.dawn4k.DawnBackend
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetState

/**
 * The viewport's view of a toolkit [DawnSurfaceTarget]: the backend the target
 * presents through and the target's own state (generation, extent, available).
 *
 * Surface creation is not part of this contract: consumers retain the target
 * through the toolkit and acquire surfaces with
 * [org.graphiks.dawn4k.webgpu.toolkit.surface.DawnTargetLease.createSurface].
 * Closing the host is bookkeeping only — it never retires the target, whose
 * retention the consumer owns.
 */
class DawnViewportHost internal constructor(
    target: DawnSurfaceTarget,
    backend: DawnBackend,
    onClose: () -> Unit = {},
) : AutoCloseable {
    /** The backend the target's native layer presents through. */
    val backend: DawnBackend = backend

    /** The target's state, driven by the view or window behind it. */
    val state: StateFlow<DawnTargetState> = target.state

    private val onClose = onClose.once()

    override fun close() = onClose()

    private fun (() -> Unit).once(): () -> Unit {
        var run = false
        return {
            if (!run) {
                run = true
                this()
            }
        }
    }
}

/**
 * Places the Compose UI over a Dawn presentation layer and reports the
 * [DawnViewportHost] once the platform layer is ready.
 *
 * [target] is the toolkit target the composable hosts on the desktop actual:
 * any `DawnSurfaceTarget` the integrator supplies
 * (`DawnSurfaceTargets.metalLayer/win32/x11/wayland`). The Android and iOS
 * actuals host their own toolkit view (`DawnSurfaceView`, `DawnMetalView`) and
 * forward that view's state through the host; the [target] argument is not
 * read there, and a caller-supplied mobile target is ignored. Pass the view's
 * own `view.target` so the same expression works on every platform.
 *
 * [onHost] runs on the platform's UI thread: the Compose UI dispatcher on the
 * JVM, the main thread on Android, and the main thread on iOS. It receives a
 * host that is valid until the composable leaves the composition or the
 * platform destroys the layer; the consumer retires any toolkit lease it took
 * before the host goes away.
 *
 * [modifier] has no default here on purpose: a default on an `expect`
 * declaration applies to `actual`s that do not repeat it, which makes the
 * signature depend on which actual is compiled. Callers pass `Modifier`.
 */
@Composable
expect fun DawnViewport(
    target: DawnSurfaceTarget,
    modifier: Modifier,
    onHost: (DawnViewportHost) -> Unit,
)
