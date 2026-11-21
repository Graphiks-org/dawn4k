package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import org.graphiks.dawn4k.webgpu.toolkit.testing.ToolkitGpuEnvironment
import kotlin.test.Test
import kotlin.test.assertNotNull

/**
 * The foundation the whole toolkit rests on: a dawn4k session must publish its dedicated
 * execution owner, and work dispatched through it runs on the thread that owns the Dawn
 * context — [DawnExecutionOwner.checkThread] passes only on that pinned thread.
 */
class ExecutionOwnerGpuTest {
    @Test fun dawnSessionPublishesItsDedicatedOwner() = runBlocking {
        val extension = ToolkitGpuEnvironment.extension()
        ToolkitGpuEnvironment.withGpuSession(extension) { session ->
            val owner = assertNotNull(session.executionOwner, "a dawn4k session must pin an execution owner")
            val instance = assertNotNull(extension.currentInstance(), "the extension opened this session's instance")
            withContext(owner) {
                // The pinned-thread check passes only on the thread that created the Dawn context.
                instance.dawnOwner.checkThread()
            }
        }
    }
}
