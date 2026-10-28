package org.graphiks.dawn4k.toolkit

/**
 * Advanced building block for toolkit integrators: the shutdown bracket shared by
 * [withDawnSession], `withDawnSurface` and `withDawnWindow`.
 *
 * [block] runs first and its failure always propagates. Every exit — success, failure or
 * cancellation — then stops progress ([stopProgress]) before shutting down ([shutdown]), in
 * that order: stopping manual progress first is what keeps shutdown admissible for
 * manual-execution sessions. [stopProgress] is not guarded: if it fails, [shutdown] is
 * skipped and that failure propagates. A [shutdown] failure is suppressed into a pending
 * [block] failure, or thrown when [block] succeeded.
 */
suspend fun <T> withToolkitShutdown(
    stopProgress: suspend () -> Unit,
    shutdown: suspend () -> Unit,
    block: suspend () -> T,
): T {
    var failure: Throwable? = null
    try { return block() }
    catch (caught: Throwable) { failure = caught; throw caught }
    finally {
        stopProgress()
        try { shutdown() }
        catch (cleanup: Throwable) { if (failure == null) throw cleanup else failure.addSuppressed(cleanup) }
    }
}
