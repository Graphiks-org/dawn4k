package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.CoroutineDispatcher
import org.graphiks.dawn4k.toolkit.DawnExecutionOwner

/** A caller-owned, runnable platform thread. Retention may suspend while awaiting platform acknowledgement. */
interface DawnPlatformOwner {
    suspend fun <T> onOwner(block: suspend () -> T): T

    companion object {
        fun fromExecutionOwner(owner: DawnExecutionOwner): DawnPlatformOwner = object : DawnPlatformOwner {
            override suspend fun <T> onOwner(block: suspend () -> T): T = owner.run { block() }
        }
        /** The dispatcher must stay pinned to one OS thread throughout its lifetime. */
        fun fromDispatcher(dispatcher: CoroutineDispatcher): DawnPlatformOwner =
            fromExecutionOwner(DawnExecutionOwner.borrow(dispatcher))
    }
}
