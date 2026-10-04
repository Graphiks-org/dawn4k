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

    @Test fun routedBlockReturningNullRunsExactlyOnce() {
        // A null result from a routed block is the block's own result: it must
        // run exactly once and never be re-run inline off the worker.
        var calls = 0
        val inline = ResourceRegistry()
        assertNull(inline.routed {
            calls += 1
            null
        })
        assertEquals(1, calls)

        calls = 0
        createNativeDispatcher().use { dispatcher ->
            val serialized = ResourceRegistry(dispatcher)
            assertNull(serialized.routed {
                calls += 1
                null
            })
            assertEquals(1, calls)
        }
    }
}
