package org.graphiks.dawn4k.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class CallbackMailboxTest {
    @Test fun postNeverExecutesInlineAndDrainIsFinite() {
        val mailbox = CallbackMailbox()
        val seen = mutableListOf<Int>()
        mailbox.post { seen += 1; mailbox.post { seen += 3 } }
        mailbox.post { seen += 2 }
        assertTrue(seen.isEmpty())
        mailbox.drain()
        assertEquals(listOf(1, 2), seen)
        assertFalse(mailbox.isEmpty())
        mailbox.drain()
        assertEquals(listOf(1, 2, 3), seen)
    }

    @Test fun oneFailureDoesNotDropOtherActions() {
        val mailbox = CallbackMailbox()
        var called = false
        mailbox.post { error("boom") }
        mailbox.post { called = true }
        assertFailsWith<IllegalStateException> { mailbox.drain() }
        assertTrue(called)
        assertTrue(mailbox.isEmpty())
    }
}
