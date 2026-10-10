package org.graphiks.dawn4k.internal

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertNull

/**
 * Ownership bookkeeping of [ResourceRegistry], without any GPU: destruction is
 * immediate and idempotent, the release of an owned reference is deferred to
 * teardown (so validation tests keep a tombstone pointing at valid memory),
 * and a closed owner refuses new acquisitions.
 */
class ResourceRegistryTest {

    @Test fun releasesRunInReverseOrderAndMayReenterRegistry() {
        val registry = ResourceRegistry()
        val calls = mutableListOf<Int>()
        repeat(3) { index ->
            registry.own(Any(), null) {
                assertEquals(0, registry.debugRemainingRefs())
                calls += index
            }
        }
        registry.close()
        assertEquals(listOf(2, 1, 0), calls)
    }

    @Test fun destroyIsIdempotentAndReleaseWaitsForOwner() {
        val calls = mutableListOf<String>()
        val registry = ResourceRegistry()
        val key = Any()
        registry.own(key, destroy = { calls += "destroy" }, release = { calls += "release" })
        registry.destroy(key)
        registry.destroy(key)
        assertEquals(listOf("destroy"), calls)
        registry.close()
        registry.close()
        assertEquals(listOf("destroy", "release"), calls)
    }

    @Test fun closedOwnerRefusesNewAcquisition() {
        val registry = ResourceRegistry()
        registry.close()
        assertFailsWith<IllegalStateException> {
            registry.own(Any(), destroy = null, release = {})
        }
    }

    @Test fun refcountOnlyObjectReleasesWithoutDestroy() {
        val calls = mutableListOf<String>()
        val registry = ResourceRegistry()
        val key = Any()
        registry.own(key, destroy = null, release = { calls += "release" })
        registry.release(key)
        // Releasing again, or destroying afterwards, must not double-release.
        registry.release(key)
        registry.destroy(key)
        registry.close()
        assertEquals(listOf("release"), calls)
    }

}
