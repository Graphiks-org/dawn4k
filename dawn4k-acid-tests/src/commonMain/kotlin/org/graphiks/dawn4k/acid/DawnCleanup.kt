package org.graphiks.dawn4k.acid

/** A failed guarded close means this context cannot be treated as isolated/recovered. */
internal class UnclosedDawnContextException(cause: Throwable) :
    IllegalStateException("Dawn context could not close; stopping the acid run", cause)

internal suspend fun cleanupDawnContext(
    original: Throwable?,
    stopEvents: suspend () -> Unit,
    drain: () -> Unit,
    close: () -> Unit,
): Throwable? {
    var failure = original
    fun record(next: Throwable) {
        val previous = failure
        if (previous == null) failure = next else preserveFailure(previous, next)
    }
    val stopped = try { stopEvents(); true } catch (join: Throwable) { record(join); false }
    // A failed join must not run progression concurrently with an active pump.
    if (stopped) try { drain() } catch (settlement: Throwable) { record(settlement) }
    // DawnContext.close is guarded: pending operations/open children cause a
    // refusal rather than an unsafe release. Always attempt this guarded close.
    try { close() } catch (closure: Throwable) { record(UnclosedDawnContextException(closure)) }
    return failure
}

internal fun Throwable.hasUnclosedDawnContext(): Boolean {
    val pending = ArrayDeque<Throwable>()
    val visited = mutableSetOf<Throwable>()
    pending.add(this)
    while (pending.isNotEmpty()) {
        val failure = pending.removeFirst()
        if (!visited.add(failure)) continue
        if (failure is UnclosedDawnContextException) return true
        failure.cause?.let { pending.add(it) }
        pending.addAll(failure.suppressedExceptions)
    }
    return false
}
