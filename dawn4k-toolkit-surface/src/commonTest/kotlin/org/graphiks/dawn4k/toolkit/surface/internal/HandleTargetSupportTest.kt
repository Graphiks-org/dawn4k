package org.graphiks.dawn4k.toolkit.surface.internal

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.graphiks.dawn4k.toolkit.surface.CountingTargetRetention
import org.graphiks.dawn4k.toolkit.surface.DawnPlatformOwner
import org.graphiks.dawn4k.toolkit.surface.DawnTargetRetention
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.EmptyTargetRetention
import org.graphiks.dawn4k.toolkit.surface.PixelExtent
import kotlin.test.*

class HandleTargetSupportTest {
    private val owner = DawnPlatformOwner.fromDispatcher(Dispatchers.Unconfined)

    private fun states(generation: Long = 1, available: Boolean = true) =
        MutableStateFlow(DawnTargetState(generation, PixelExtent(16, 16), available))

    private fun handleTarget(
        states: MutableStateFlow<DawnTargetState>,
        acquire: () -> Long = { 11L },
        released: MutableList<Long> = mutableListOf(),
        retention: DawnTargetRetention = EmptyTargetRetention,
        retirement: TargetRetirement? = null,
    ) = retainedHandleTarget(
        source = { NativeSurfaceSource.Metal(it) },
        acquire = acquire,
        release = { released.add(it) },
        owner = owner,
        states = states,
        retention = retention,
        retirement = retirement,
    )

    @Test fun handleIsExposedUntilAcknowledgedRetirement() = runTest {
        val released = mutableListOf<Long>()
        val target = handleTarget(states(), released = released)
        val lease = target.retain() as HandleLease
        assertEquals(11L, lease.retainedHandle)
        assertTrue(released.isEmpty())
        lease.retire()
        lease.retire()
        assertEquals(listOf(11L), released)
        assertEquals(0L, lease.retainedHandle)
    }

    @Test fun unavailableStateFailsRetentionBeforeAcquiring() = runTest {
        var acquisitions = 0
        val target = handleTarget(states(available = false), acquire = { acquisitions++; 11L })
        assertFailsWith<IllegalStateException> { target.retain() }
        assertEquals(0, acquisitions)
    }

    @Test fun changedStateDuringRetentionReleasesTheHandleAndExternalRetention() = runTest {
        val flow = states()
        val retention = CountingTargetRetention()
        val released = mutableListOf<Long>()
        val target = handleTarget(
            flow,
            acquire = { flow.value = flow.value.copy(available = false); 11L },
            released = released,
            retention = retention,
        )
        assertFailsWith<IllegalStateException> { target.retain() }
        assertEquals(listOf(11L), released)
        retention.requireRetired()
    }

    @Test fun externalRetentionCountsTheLeaseLifetime() = runTest {
        val retention = CountingTargetRetention()
        val target = handleTarget(states(), retention = retention)
        val lease = target.retain()
        assertFailsWith<IllegalStateException> { retention.requireRetired() }
        lease.retire()
        retention.requireRetired()
    }

    @Test fun registryAcknowledgementWaitsForHandleRetirement() = runTest {
        val retirement = TargetRetirement()
        val target = handleTarget(states(generation = 4), retirement = retirement)
        val lease = target.retain()
        val acknowledged = async(start = CoroutineStart.UNDISPATCHED) { retirement.await(4) }
        assertFalse(acknowledged.isCompleted)
        lease.retire()
        acknowledged.await()
    }
}
