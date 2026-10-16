package org.graphiks.dawn4k.toolkit

import kotlinx.coroutines.runBlocking
import kotlin.test.Test
import kotlin.test.assertEquals

class ShutdownOwnerTest {
    @Test
    fun borrowedOwnerRemainsUsable() = runBlocking {
        val owner = DawnExecutionOwner.dedicated("caller-owned")
        try {
            val session = openDawnSession(DawnSessionOptions(execution = DawnExecutionMode.Managed(owner)))
            session.shutdown()
            assertEquals(42, owner.run { 42 })
        } finally { owner.close() }
    }
}
