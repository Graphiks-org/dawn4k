package org.graphiks.dawn4k.toolkit.surface

import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runTest
import kotlin.test.*

class CountingTargetRetentionTest {
    @Test fun awaitRetirementSuspendsUntilEveryOutstandingLeaseRetires() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        retention.retain()
        val retired = async(start = CoroutineStart.UNDISPATCHED) { retention.awaitRetirement() }
        assertFalse(retired.isCompleted)
        retention.retire()
        assertFalse(retired.isCompleted)
        retention.retire()
        retired.await()
    }

    @Test fun awaitRetirementCompletesImmediatelyWithoutOutstandingLeases() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        retention.retire()
        async(start = CoroutineStart.UNDISPATCHED) { retention.awaitRetirement() }.await()
    }

    @Test fun retainAfterRetirementStartFailsCleanly() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        val retired = async(start = CoroutineStart.UNDISPATCHED) { retention.awaitRetirement() }
        assertFailsWith<IllegalStateException> { retention.retain() }
        retention.retire()
        retired.await()
    }

    @Test fun retainAfterCompletedRetirementFailsCleanly() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        retention.retire()
        retention.awaitRetirement()
        assertFailsWith<IllegalStateException> { retention.retain() }
    }

    @Test fun requireRetiredThrowsWhileLeasesAreOutstanding() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        assertFailsWith<IllegalStateException> { retention.requireRetired() }
    }

    @Test fun requireRetiredPassesAtZeroAndSealsRetention() = runTest {
        val retention = CountingTargetRetention()
        retention.retain()
        retention.retire()
        retention.requireRetired()
        assertFailsWith<IllegalStateException> { retention.retain() }
    }

    @Test fun unbalancedRetireFails() = runTest {
        val retention = CountingTargetRetention()
        assertFailsWith<IllegalStateException> { retention.retire() }
    }
}
