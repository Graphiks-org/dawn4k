@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package org.graphiks.dawn4k.webgpu.toolkit.target

import org.graphiks.dawn4k.toolkit.surface.DawnMetalView
import org.graphiks.webgpu.toolkit.SurfaceTarget

/**
 * Exposes an iOS or tvOS dawn4k [DawnMetalView] as a toolkit [SurfaceTarget].
 *
 * The view keeps its Metal layer, its layout updates and its asynchronous retirement; the
 * returned target forwards its state, so `withSurface(view.asToolkitTarget()) { _, surface ->
 * ... }` presents into the view like a desktop window. UIKit owns layout: a size change
 * publishes a new generation, and the toolkit follows it the same way it follows a window
 * resize.
 *
 * The returned target only forwards the view's state and never closes it, so the caller keeps
 * ownership of the view.
 */
fun DawnMetalView.asToolkitTarget(): SurfaceTarget = DawnTargetAdapter(
    dawnTarget = target,
    platformType = PlatformSurfaceType.MetalLayer,
)
