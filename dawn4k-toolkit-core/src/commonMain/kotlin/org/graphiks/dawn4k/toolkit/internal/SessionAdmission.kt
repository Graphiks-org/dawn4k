package org.graphiks.dawn4k.toolkit.internal

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlin.coroutines.AbstractCoroutineContextElement
import kotlin.coroutines.CoroutineContext
import kotlin.coroutines.coroutineContext

internal class SessionAdmission {
    private val mutex = Mutex()
    private class Marker(val admission: SessionAdmission) : AbstractCoroutineContextElement(Key) {
        companion object Key : CoroutineContext.Key<Marker>
    }
    suspend fun isAdmitted(): Boolean = coroutineContext[Marker]?.admission === this
    suspend fun <T> run(block: suspend CoroutineScope.() -> T): T {
        if (isAdmitted()) return coroutineScope(block)
        return mutex.withLock { withContext(Marker(this)) { coroutineScope(block) } }
    }
}
