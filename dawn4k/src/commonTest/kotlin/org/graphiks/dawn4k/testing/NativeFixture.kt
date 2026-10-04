package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.DawnConfig
import org.graphiks.dawn4k.internal.DawnRuntime
import org.graphiks.dawn4k.internal.DeviceSession

/**
 * One raw GPU session for the tests of this module: a [DawnRuntime] and one
 * already-opened [DeviceSession], closed together in reverse order. Later tasks
 * add their raw creation helpers here as they need them for their tests.
 */
internal class NativeFixture(
    internal val runtime: DawnRuntime,
    internal val session: DeviceSession,
) : AutoCloseable {

    companion object {
        suspend fun open(): NativeFixture {
            val runtime = DawnRuntime(DawnConfig())
            val session = runtime.openSession()
            return NativeFixture(runtime, session)
        }
    }

    override fun close() {
        session.close()
        runtime.close()
    }
}
