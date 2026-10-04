package org.graphiks.dawn4k.native

import org.graphiks.kffi.CallbackPolicy
import org.graphiks.kffi.MemoryBuffer
import org.graphiks.kffi.NativeAddress
import org.graphiks.kffi.engine.JvmDowncallEngine
import org.graphiks.kffi.memoryScope
import java.lang.foreign.Arena
import java.lang.foreign.SymbolLookup
import java.util.concurrent.atomic.AtomicReference
import kotlin.test.Test
import kotlin.test.assertContentEquals
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Proves that a `WGPUBufferMapCallbackInfo` built by the generated factory survives a
 * by-value C call and delivers its callback, its routing userdata and an explicit-length
 * UTF-8 message (embedded NUL included) intact.
 */
class CallbackAbiTest {

    @Test
    fun receivesCallbackInfoByValueWithExplicitLengthMessage() {
        val helperPath = System.getProperty("dawn.abi.helper")
        assertNotNull(helperPath, "dawn.abi.helper must point at the compiled helper library")

        val arena = Arena.ofShared()
        try {
            val helper = SymbolLookup.libraryLookup(helperPath, arena)
                .find("dawn_abi_invoke_buffer_map")
                .orElseThrow { AssertionError("helper symbol not found in $helperPath") }
                .address()

            val seen = AtomicReference<Seen?>(null)
            val appUserdata = NativeAddress(0x1234L)

            memoryScope { allocator ->
                val registration = WGPUBufferMapCallback.register(
                    policy = CallbackPolicy.ONCE,
                    callback = WGPUBufferMapCallback { status, message, userdata1 ->
                        val length = message.length.toInt()
                        val data = message.data
                        val bytes = if (data == null) {
                            ByteArray(0)
                        } else {
                            val buffer = MemoryBuffer(data.handler, message.length)
                            ByteArray(length) { index -> buffer.readByte(index.toULong()) }
                        }
                        seen.set(Seen(status, length, bytes, userdata1?.rawValue))
                    },
                )
                try {
                    val info = WGPUBufferMapCallbackInfo.allocate(
                        allocator = allocator,
                        mode = WGPUCallbackMode_AllowProcessEvents,
                        registration = registration,
                        userdata1 = appUserdata,
                    )
                    JvmDowncallEngine.callGeneric(
                        helper,
                        JvmDowncallEngine.FunctionShape(
                            JvmDowncallEngine.AbiType.I32,
                            listOf(
                                JvmDowncallEngine.AbiType.Struct("WGPUBufferMapCallbackInfo"),
                                JvmDowncallEngine.AbiType.I32,
                            ),
                        ),
                        info.handler.rawValue,
                        7,
                    )
                } finally {
                    registration.close()
                }
            }

            val result = assertNotNull(seen.get(), "the callback was not invoked")
            assertEquals(7u, result.status)
            assertEquals(5, result.length)
            assertContentEquals(
                byteArrayOf(
                    'a'.code.toByte(),
                    'b'.code.toByte(),
                    0,
                    'c'.code.toByte(),
                    'd'.code.toByte(),
                ),
                result.bytes,
            )
            assertEquals(appUserdata.rawValue, result.userdata)
        } finally {
            arena.close()
        }
    }

    private data class Seen(
        val status: UInt,
        val length: Int,
        val bytes: ByteArray,
        val userdata: Long?,
    )
}
