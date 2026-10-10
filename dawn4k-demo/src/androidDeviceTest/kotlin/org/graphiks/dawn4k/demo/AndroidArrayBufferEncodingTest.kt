package org.graphiks.dawn4k.demo

import org.graphiks.webgpu.ArrayBuffer
import kotlin.test.Test
import kotlin.test.assertContentEquals

/** GPU byte contract, not CPU roundtrip (which conceals matching endian mistakes). */
class AndroidArrayBufferEncodingTest {
    @Test fun floatFactoryProducesGpuLittleEndianBytes() {
        val data = ArrayBuffer.of(floatArrayOf(1f))
        assertContentEquals(byteArrayOf(0, 0, -128, 63), data.toByteArray())
    }
    @Test fun uniformBulkWritesProduceGpuLittleEndianBytes() {
        val data = ArrayBuffer.allocate(4uL)
        data.setFloats(0uL, floatArrayOf(1f))
        assertContentEquals(byteArrayOf(0, 0, -128, 63), data.toByteArray())
    }
}
