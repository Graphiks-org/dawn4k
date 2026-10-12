package org.graphiks.dawn4k.internal

import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class AsyncOperationRegistryTest {
    @Test fun nativeCompletionAloneDoesNotAuthorizeTeardown() {
        val registry = AsyncOperationRegistry()
        val owner = Any()
        val operation = PendingOperation<Unit> { }
        registry.register(owner, operation)
        registry.nativeSettled(operation)
        assertTrue(registry.hasPending(owner))
        registry.ownershipSettled(operation)
        assertFalse(registry.hasPending(owner))
    }

    @Test fun ownershipCompletionAloneDoesNotAuthorizeTeardown() {
        val registry = AsyncOperationRegistry()
        val owner = Any()
        val operation = PendingOperation<Unit> { }
        registry.register(owner, operation)
        registry.ownershipSettled(operation)
        assertTrue(registry.hasPending(owner))
        registry.nativeSettled(operation)
        assertFalse(registry.hasPending())
    }
}
