package org.graphiks.dawn4k.internal

/**
 * A portable monitor: one exclusive critical section per object, callable from
 * common code. [PendingOperation] serializes its wait state machine with it —
 * the waiter's thread, the event-processing caller and the cancelling thread all
 * enter the same monitor, which is what makes the deliver-or-release decision
 * exactly-once by construction.
 *
 * The critical section must stay short and must never call user code —
 * neither [PendingOperation.releaseRejected] callbacks nor continuation
 * resumption — and must never re-enter itself. Callers decide under the lock
 * and perform any user-visible action after releasing it.
 */
internal expect class SynchronizedObject() {

    /**
     * Runs [block] while holding this monitor. The monitor is released when
     * [block] returns or throws; [block] runs exactly once per invocation.
     */
    fun <R> withLock(block: () -> R): R
}
