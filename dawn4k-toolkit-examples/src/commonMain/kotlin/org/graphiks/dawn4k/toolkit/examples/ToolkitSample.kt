package org.graphiks.dawn4k.toolkit.examples

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.withDawnSession

/**
 * The shared compute → triangle sample flow behind the Android activity and the Apple view controller.
 *
 * Joins [previousCleanup], runs the compute example and checks its output, then renders the triangle
 * on `target()` (resolved at triangle time) until the caller retires the view. Status updates flow
 * through [onStatus]; the initial "Starting compute…" text stays at the call site so it renders
 * synchronously before launch, exactly as before.
 *
 * Failure handling lives here rather than at the call sites: both platforms' catch blocks were
 * byte-identical (`CancellationException` rethrown, any other failure mapped to
 * `"Failed: ${failure.message}"` through the platform's [onStatus] renderer), and keeping them
 * would reintroduce the duplication this extraction removes. The status update after the first
 * presented frame launches on `this` scope — not on the returned job — so view retirement cannot
 * cancel it, matching the pre-extraction behavior on both platforms.
 */
fun CoroutineScope.launchToolkitSample(
    target: () -> DawnSurfaceTarget,
    previousCleanup: Job?,
    onStatus: suspend (String) -> Unit,
): Job {
    val scope = this
    return scope.launch {
        try {
            previousCleanup?.join()
            val values = withDawnSession { runComputeExample(it) }
            check(values.contentEquals(intArrayOf(2, 4, 6, 8)))
            onStatus("Compute: ${values.contentToString()}")
            runSurfaceTriangleExample(target()) {
                scope.launch { onStatus("Compute: ${values.contentToString()} · triangle presented") }
            }
        } catch (cancelled: CancellationException) { throw cancelled }
        catch (failure: Throwable) { onStatus("Failed: ${failure.message}") }
    }
}
