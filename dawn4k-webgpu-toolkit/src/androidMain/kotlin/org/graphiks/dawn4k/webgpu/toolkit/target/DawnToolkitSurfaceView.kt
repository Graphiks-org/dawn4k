package org.graphiks.dawn4k.webgpu.toolkit.target

import org.graphiks.dawn4k.webgpu.toolkit.surface.DawnSurfaceView
import org.graphiks.webgpu.toolkit.SurfaceTarget

/**
 * Exposes an Android dawn4k [DawnSurfaceView] as a toolkit [SurfaceTarget].
 *
 * The view keeps its SurfaceHolder lifecycle — generations, availability and asynchronous
 * retirement — and the returned target forwards it, so
 * `withSurface(view.asToolkitTarget()) { _, surface -> ... }` presents into the view like a
 * desktop window. A size change or a destroyed surface publishes a new generation, and the
 * toolkit follows it the same way it follows a window resize.
 *
 * The view must be attached and its surface created before the first frame; until then the
 * toolkit suspends acquisition, exactly as it does for a zero-sized window. The returned target
 * only forwards the view's state and never closes it, so the caller keeps ownership of the view.
 */
fun DawnSurfaceView.asToolkitTarget(): SurfaceTarget = DawnTargetAdapter(
    dawnTarget = target,
    platformType = PlatformSurfaceType.Android,
)
