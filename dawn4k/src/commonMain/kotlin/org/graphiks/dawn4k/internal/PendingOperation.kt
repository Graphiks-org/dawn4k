package org.graphiks.dawn4k.internal

import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * A single native operation whose completion can race the coroutine waiting
 * for it, without leaking the owned native reference carried by its outcome.
 *
 * The operation tracks two independent facets:
 *  - the waiter facet: `waiting -> delivered | cancelled | owner-closed`,
 *    observed through [waiterFinished];
 *  - the native facet: `not-started -> in-flight -> terminal`, advanced by
 *    [nativeTerminal] and observed through [nativeFinished].
 *
 * Cancelling the awaiting coroutine does NOT terminalize the native side: a
 * native call already in flight keeps running, and its late outcome is handed
 * to [releaseRejected] instead of being delivered. [abandon] ends the wait on
 * the owner's behalf (device loss, runtime shutdown) while leaving the native
 * facet untouched; only [nativeTerminal] authorizes cleaning up the native
 * registration. All transitions are thread-safe: an outcome may complete on the
 * dispatcher worker, on the awaiting coroutine, or on any thread a native
 * callback arrives from.
 *
 * The waiter facet is an explicit state machine serialized by [lock] (see
 * [WaitState]): every outcome is decided — stored for the waiter to take, or
 * released — under that monitor, exactly once, by construction. The waiting
 * coroutine is only ever resumed with a lossy wake-up signal; the outcome
 * itself never travels through the continuation, so no value can vanish in
 * the resume/cancellation seam of `suspendCancellableCoroutine`: whatever the
 * seam does to the signal, the outcome sits in this object until exactly one
 * of the waiter's tail (`await` after the wake-up) and the waiter's
 * cancellation handler takes it out under the monitor.
 */
internal class PendingOperation<T>(val releaseRejected: (T) -> Unit) {

    /**
     * The states of the waiter facet, all transitions under [lock]:
     *  - [EMPTY]: no waiter registered, no outcome decided;
     *  - [REGISTERED]: the waiter registered, no outcome decided;
     *  - [RESERVED]: the outcome is decided and held for the registered
     *    waiter, which either takes it over in [await] or is cancelled, in
     *    which case the cancellation handler takes and releases it;
     *  - [BUFFERED]: the outcome is decided with no waiter yet; a later
     *    [await] reserves and delivers it;
     *  - [FINISHED]: the waiter facet is terminal and holds nothing — the
     *    registered waiter took the outcome (delivered) or vanished (its
     *    handler took a reserved outcome and released it).
     *
     * Invariants: [value] is non-null exactly in [BUFFERED] and [RESERVED];
     * [waiter] is non-null exactly in [REGISTERED]. Every value has exactly
     * one fate — delivered to the waiter, or released through
     * [releaseRejected] — because the take-out transition to [FINISHED]
     * happens at most once, under the monitor.
     */
    private enum class WaitState { EMPTY, REGISTERED, RESERVED, BUFFERED, FINISHED }

    /** Serializes [state], [value] and [waiter]; never held across user code. */
    private val lock = SynchronizedObject()

    private var state: WaitState = WaitState.EMPTY
    private var value: Result<T>? = null
    private var waiter: CancellableContinuation<Unit>? = null

    /** Terminal marker of the native facet, independent from the outcome. */
    private val nativeFacet: CompletableDeferred<Unit> = CompletableDeferred()

    /**
     * Suspends until the waiter facet reaches a terminal state and returns the
     * outcome: a native result, an owner abandonment, or fails fast with this
     * coroutine's cancellation. An outcome that completed before registration
     * (inline native completion) is delivered immediately without suspending.
     *
     * The outcome is NOT delivered through the continuation: a completion
     * stores it in this object and wakes the waiter with a plain signal, and
     * the woken tail reads it back under [lock]. When the waiter's coroutine
     * is cancelled instead, kotlinx-coroutines invokes [waiterVanished]
     * exactly once, and that handler — not a resume-with-value seam — takes
     * the reserved outcome out and releases its owned value. Either path is
     * possible for a given registration, never both: whichever side runs
     * second finds [WaitState.FINISHED] and does nothing.
     */
    suspend fun await(): Result<T> {
        suspendCancellableCoroutine { continuation -> registerWaiter(continuation) }
        return consumeReserved()
    }

    /**
     * Registers the single waiter: [EMPTY] parks it for a later decision,
     * [BUFFERED] reserves the buffered outcome for it and wakes it inline.
     * Any other state means the single waiter already came.
     */
    private fun registerWaiter(continuation: CancellableContinuation<Unit>) {
        var deliverBuffered = false
        lock.withLock {
            when (state) {
                WaitState.EMPTY -> {
                    state = WaitState.REGISTERED
                    waiter = continuation
                }
                WaitState.BUFFERED -> {
                    state = WaitState.RESERVED
                    deliverBuffered = true
                }
                else -> throw IllegalStateException("a pending operation serves a single waiter")
            }
        }
        // The cancellation handler is installed before the inline delivery:
        // a cancellation that already happened fires it right here (possibly
        // on this thread), and the handler takes the reserved outcome itself.
        // A later cancellation fires it from the cancelling thread instead.
        continuation.invokeOnCancellation(::waiterVanished)
        if (deliverBuffered) {
            // Lossy by design: the outcome is already reserved in this
            // object. When the signal is lost to a cancellation,
            // waiterVanished releases the reserved outcome exactly once.
            continuation.resume(Unit)
        }
    }

    /**
     * The waiter's tail after a wake-up: takes the outcome reserved for this
     * waiter. Runs only when the wake-up survived — a cancelled coroutine
     * never reaches it — and never twice for the same registration.
     */
    private fun consumeReserved(): Result<T> =
        lock.withLock {
            when (state) {
                WaitState.RESERVED -> {
                    state = WaitState.FINISHED
                    val delivered = value!!
                    value = null
                    delivered
                }
                else ->
                    // Unreachable by the protocol: the tail runs only after
                    // a wake-up that reserved an outcome for this waiter, and
                    // the only other taker (waiterVanished) excludes the
                    // tail from running. Reported as a failure instead of
                    // crashing the wait, so a protocol regression surfaces.
                    Result.failure(
                        IllegalStateException("pending operation state $state has no reserved outcome"),
                    )
            }
        }

    /**
     * The registered waiter vanished — its coroutine was cancelled, possibly
     * while the wake-up was still in flight. Ends the wait; when an outcome
     * had already been reserved for the vanished waiter, takes it out and
     * releases its owned value, exactly once. A late [complete] that arrives
     * afterwards finds the facet decided and releases its own value itself.
     */
    private fun waiterVanished(cause: Throwable?) {
        var rejected: Result<T>? = null
        lock.withLock {
            when (state) {
                WaitState.REGISTERED -> state = WaitState.FINISHED
                WaitState.RESERVED -> {
                    rejected = value
                    value = null
                    state = WaitState.FINISHED
                }
                else -> {
                    // EMPTY/BUFFERED: no waiter registration ever reached
                    // the monitor, so this handler cannot see them. FINISHED:
                    // the waiter tail or an earlier handler run already
                    // took the outcome. Nothing to do in either case.
                }
            }
        }
        // Outside the monitor: user code must never run under the lock.
        rejected?.onSuccess(releaseRejected)
    }

    /**
     * Delivers the native outcome. The first delivery reaches the waiter (or
     * is buffered until one registers); a further delivery that owns a new
     * native reference has no consumer and is released exactly once, while
     * the delivered one is never released twice.
     *
     * Storing an outcome for a registered waiter does NOT deliver it through
     * the continuation: the waiter is woken with a plain signal and reads the
     * outcome under [lock]; if the signal loses to the waiter's
     * cancellation, [waiterVanished] releases the stored outcome exactly
     * once. A value that arrives for an already-decided facet (delivered,
     * buffered, cancelled, abandoned) never enters this object and is
     * released here.
     */
    fun complete(result: Result<T>) {
        var wake: CancellableContinuation<Unit>? = null
        var rejected = false
        lock.withLock {
            when (state) {
                WaitState.EMPTY -> {
                    state = WaitState.BUFFERED
                    value = result
                }
                WaitState.REGISTERED -> {
                    state = WaitState.RESERVED
                    value = result
                    wake = waiter
                    waiter = null
                }
                else -> rejected = true
            }
        }
        if (rejected) {
            // This value owns a reference nobody will consume.
            result.onSuccess(releaseRejected)
            return
        }
        // Lossy wake-up, deliberately: resuming a cancelled continuation is a
        // documented no-op in kotlinx-coroutines, and the stored outcome is
        // owned by the protocol until the waiter (or its cancellation
        // handler) takes it.
        wake?.resume(Unit)
    }

    /**
     * Terminates the wait on the owner's behalf with [cause]: a suspended
     * waiter resumes with the failure and a later await returns it without
     * suspending. No-op when the outcome is already final: a real buffered
     * outcome can still be consumed by its waiter, and a late value continues
     * to be released by [complete]. An abandonment owns no native reference,
     * so there is nothing to release on the decided paths.
     */
    fun abandon(cause: Throwable) {
        var wake: CancellableContinuation<Unit>? = null
        lock.withLock {
            when (state) {
                WaitState.EMPTY -> {
                    state = WaitState.BUFFERED
                    value = Result.failure(cause)
                }
                WaitState.REGISTERED -> {
                    state = WaitState.RESERVED
                    value = Result.failure(cause)
                    wake = waiter
                    waiter = null
                }
                else -> {}
            }
        }
        wake?.resume(Unit)
    }

    /**
     * Marks the native facet terminal: the native registration can be cleaned
     * up now that the operation is done firing callbacks. Independent of
     * [waiterFinished]; cancelling a coroutine alone must never close a
     * native registration.
     */
    fun nativeTerminal() {
        nativeFacet.complete(Unit)
    }

    /**
     * Whether the wait reached `delivered`, `cancelled` or `owner-closed` for
     * a registered waiter. An outcome buffered before any waiter registered
     * does not finish the wait: it is still waiting to be consumed.
     */
    val waiterFinished: Boolean
        get() = lock.withLock {
            state == WaitState.RESERVED || state == WaitState.FINISHED
        }

    /** Whether the native side reached its terminal state. */
    val nativeFinished: Boolean
        get() = nativeFacet.isCompleted
}
