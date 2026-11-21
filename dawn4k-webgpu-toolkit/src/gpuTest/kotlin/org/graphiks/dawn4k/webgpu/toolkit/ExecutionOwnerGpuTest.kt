package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.webgpu.toolkit.testing.ToolkitGpuEnvironment
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * The foundation the whole toolkit rests on: a dawn4k session must publish its dedicated
 * `dawn4k-toolkit` dispatcher as the execution owner, so GPU work dispatched through
 * `session.executionOwner` runs on the thread that owns the Dawn context.
 */
class ExecutionOwnerGpuTest {
    @Test fun dawnSessionPublishesItsDedicatedOwner() = runBlocking {
        ToolkitGpuEnvironment.withGpuSession { session ->
            val owner = assertNotNull(session.executionOwner, "a dawn4k session must pin an execution owner")
            withContext(owner) {
                assertEquals(
                    true,
                    Thread.currentThread().name.startsWith("dawn4k-toolkit"),
                    "the execution owner must be the dedicated dawn dispatcher, not the caller's " +
                        "(thread: ${Thread.currentThread().name})",
                )
            }
        }
    }
}
