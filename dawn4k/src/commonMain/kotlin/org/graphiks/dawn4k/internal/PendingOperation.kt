package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.suspendCancellableCoroutine

/**
 * A single native operation whose completion can race the coroutine waiting for
 * it, without leaking the owned native reference carried by its outcome.
 *
 * The operation tracks two independent facets:
 *  - the waiter facet: `waiting -> delivered | cancelled | owner-closed`,
 *    observed through [waiterFinished];
 *  - the native facet: `not-started -> in-flight -> terminal`, advanced by
 *    [nativeTerminal] and observed through [nativeFinished].
 *
 * Cancelling the awaiting coroutine does NOT terminalize the native side: a
 * native call already in flight keeps running, and its late outcome is handed to
 * [releaseRejected] instead of being delivered. [abandon] ends the wait on the
 * owner's behalf (device loss, runtime shutdown) while leaving the native facet
 * untouched; only [nativeTerminal] authorizes cleaning up the native
 * registration. All transitions are thread-safe: an outcome may complete on the
 * dispatcher worker, on the awaiting coroutine, or on any thread a native
 * callback arrives from.
 */
internal class PendingOperation<T>(val releaseRejected: (T) -> Unit) {

    /**
     * The single wait outcome: completed by the first [complete] or [abandon],
     * or cancelled when the registered waiter vanishes. `complete` and `cancel`
     * are atomic exactly-once operations, which is what makes every release
     * below fire at most once per delivered value.
     */
    private val outcome: CompletableDeferred<Result<T>> = CompletableDeferred()

    /** Guards the single-waiter contract of [await]. */
    private val served: CompletableDeferred<Unit> = CompletableDeferred()

    /** Terminal marker of the native facet, independent from the outcome. */
    private val nativeFacet: CompletableDeferred<Unit> = CompletableDeferred()

    /**
     * Suspends until the waiter facet reaches a terminal state and returns the
     * outcome: a native result, an owner abandonment, or fails fast with this
     * coroutine's cancellation. An outcome that completed before registration
     * (inline native completion) is delivered immediately without suspending.
     */
    @OptIn(ExperimentalCoroutinesApi::class)
    suspend fun await(): Result<T> = suspendCancellableCoroutine { continuation ->
        if (!served.complete(Unit)) {
            throw IllegalStateException("a pending operation serves a single waiter")
        }
        // The outcome may already be final: the handler fires immediately then,
        // resuming the waiter inline.
        outcome.invokeOnCompletion { cause ->
            if (cause == null) {
                val result = outcome.getCompleted()
                // resume(value, onCancellation) is atomic against cancellation:
                // a vanished waiter never sees the value, and the value nobody
                // will ever consume is released exactly once instead.
                continuation.resume(result) { _, value, _ ->
                    value.onSuccess(releaseRejected)
                }
            }
        }
        // Losing the waiter fails the outcome: whatever arrives after this
        // point is released, never buffered for a waiter that will not come.
        continuation.invokeOnCancellation { outcome.cancel() }
    }

    /**
     * Delivers the native outcome. The first delivery reaches the waiter (or is
     * buffered until one registers); a further delivery that owns a new native
     * reference has no consumer and is released exactly once, while the
     * delivered one is never released twice.
     */
    fun complete(result: Result<T>) {
        if (!outcome.complete(result)) {
            result.onSuccess(releaseRejected)
        }
    }

    /**
     * Terminates the wait on the owner's behalf with [cause]: a suspended waiter
     * resumes with the failure and a later await returns it without suspending.
     * No-op when the outcome is already final: a real buffered outcome can
     * still be consumed by its waiter, and a late value continues to be
     * released by [complete].
     */
    fun abandon(cause: Throwable) {
        outcome.complete(Result.failure(cause))
    }

    /**
     * Marks the native facet terminal: the native registration can be cleaned up
     * now that the operation is done firing callbacks. Independent of
     * [waiterFinished]; cancelling a coroutine alone must never close a native
     * registration.
     */
    fun nativeTerminal() {
        nativeFacet.complete(Unit)
    }

    /**
     * Whether the wait reached `delivered`, `cancelled` or `owner-closed` for a
     * registered waiter. An outcome buffered before any waiter registered does
     * not finish the wait: it is still waiting to be consumed.
     */
    val waiterFinished: Boolean
        get() = served.isCompleted && outcome.isCompleted

    /** Whether the native side reached its terminal state. */
    val nativeFinished: Boolean
        get() = nativeFacet.isCompleted
}
