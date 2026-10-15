package org.graphiks.dawn4k.toolkit.internal

/** Retains acquisitions across withContext's cancellable return boundary. */
internal class BootstrapTransaction {
    private val actions = mutableListOf<suspend () -> Unit>()
    fun onRollback(action: suspend () -> Unit) { actions += action }
    fun commit() { actions.clear() }

    /** Stops at the first failed release; a later attempt resumes there. */
    suspend fun rollback(primary: Throwable? = null): Boolean {
        while (actions.isNotEmpty()) {
            try { actions.last().invoke(); actions.removeAt(actions.lastIndex) }
            catch (failure: Throwable) {
                if (failure !== primary) primary?.addSuppressed(failure)
                return false
            }
        }
        return true
    }
}
