package org.graphiks.dawn4k.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertSame

class OwnedReferenceTest {
    @Test
    fun closesOnceAndRejectsHandleAccess() {
        var releases = 0
        val owner = OwnedReference(42, release = { releases++ })
        assertEquals(42, owner.handle)
        owner.close()
        owner.close()
        assertEquals(1, releases)
        assertFailsWith<IllegalStateException> { owner.handle }
    }

    @Test
    fun releasesEvenWhenDestroyFails() {
        val calls = mutableListOf<String>()
        val failure = IllegalArgumentException("destroy")
        val owner = OwnedReference(42,
            destroy = { calls += "destroy"; throw failure },
            release = { calls += "release" })
        assertSame(failure, assertFailsWith<IllegalArgumentException> { owner.close() })
        owner.close()
        assertEquals(listOf("destroy", "release"), calls)
    }

    @Test
    fun releaseFailureIsNotRetried() {
        var calls = 0
        val owner = OwnedReference(42, release = { calls++; error("release") })
        assertFailsWith<IllegalStateException> { owner.close() }
        owner.close()
        assertEquals(1, calls)
    }

    @Test
    fun destroyPrecedesReleaseAndActionsCanReadClosedState() {
        val calls = mutableListOf<String>()
        lateinit var owner: OwnedReference<Int>
        owner = OwnedReference(42,
            destroy = { assertEquals(42, it); calls += "destroy" },
            release = {
                assertEquals(42, it)
                assertFailsWith<IllegalStateException> { owner.handle }
                calls += "release"
            })
        owner.close()
        assertEquals(listOf("destroy", "release"), calls)
    }
}
