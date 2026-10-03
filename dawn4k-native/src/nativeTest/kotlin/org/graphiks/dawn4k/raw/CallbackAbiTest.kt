package org.graphiks.dawn4k.raw

import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.memoryScope
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Native (Kotlin/Native) counterpart of the JVM [CallbackAbiTest].
 *
 * The generated value type is checked against cinterop: a CallbackInfo built by the
 * generated factory must expose the registered upcall pointer and the application
 * userdata through the underlying `webgpu.native` struct. The cross-FFI by-value
 * invocation itself is exercised on JVM where the helper library can be loaded.
 */
class CallbackAbiTest {

    @Test
    fun callbackInfoByValueCarriesRegistrationAndUserdata() {
        memoryScope { allocator ->
            var invoked = 0
            val registration = WGPUBufferMapCallback.register(
                policy = CallbackPolicy.ONCE,
                callback = WGPUBufferMapCallback { _, _, _ -> invoked++ },
            )
            try {
                val info = WGPUBufferMapCallbackInfo.allocate(
                    allocator = allocator,
                    mode = WGPUCallbackMode_AllowProcessEvents,
                    registration = registration,
                    userdata1 = NativeAddress(0x1234L),
                )

                assertEquals(WGPUCallbackMode_AllowProcessEvents, info.mode)
                assertNotNull(info.callback, "registered callback pointer must be stored by value")
                assertNotNull(info.userdata2, "routing userdata must be stored by value")
                assertEquals(0x1234L, info.userdata1?.rawValue)
                assertEquals(0, invoked)
            } finally {
                registration.close()
            }
        }
    }
}
