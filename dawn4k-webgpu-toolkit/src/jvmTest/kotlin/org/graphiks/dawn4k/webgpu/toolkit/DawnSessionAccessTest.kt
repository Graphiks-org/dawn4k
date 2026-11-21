package org.graphiks.dawn4k.webgpu.toolkit

import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.graphiks.dawn4k.webgpu.toolkit.internal.Dawn4kToolkitInstance
import org.graphiks.webgpu.GPUDevice
import org.graphiks.webgpu.GPUTextureFormat
import org.graphiks.webgpu.toolkit.Session
import org.graphiks.webgpu.toolkit.SessionConfig
import org.graphiks.webgpu.toolkit.SessionState
import org.graphiks.webgpu.toolkit.ManualProgression
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertNotEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue
import kotlin.time.Duration.Companion.seconds

/**
 * The per-session dawn accessors: a session opened through [openDawnSession] resolves its own
 * dawn instance — never another session's — and a session that skipped it fails fast.
 */
class DawnSessionAccessTest {

    @Test fun accessorsFailFastForUnknownSession() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val session = sessionStub()
        val probes = listOf<(Dawn4kWebGpuToolkitExtension, Session) -> Any?>(
            { extension, session -> extension.dawnContext(session) },
            { extension, session -> extension.dawnAdapter(session) },
            { extension, session -> extension.dawnDevice(session) },
            { extension, session -> extension.dawnQueue(session) },
        )
        for (probe in probes) {
            val failure = assertFailsWith<IllegalStateException> { probe(extension, session) }
            assertTrue(failure.message!!.contains("openDawnSession"), failure.message)
        }
    }

    @Test fun attachThenResolveReturnsTheBoundInstance() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val instance = extension.open(SessionConfig()) as Dawn4kToolkitInstance
        try {
            val session = sessionStub()
            extension.attach(session, instance)
            assertSame(instance.dawnContext, extension.dawnContext(session))
            val noAdapter = assertFailsWith<IllegalStateException> { extension.dawnAdapter(session) }
            assertTrue(noAdapter.message!!.contains("adapter"), noAdapter.message)
        } finally {
            instance.close()
        }
    }

    @Test fun detachReleasesTheSessionBinding() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val instance = extension.open(SessionConfig()) as Dawn4kToolkitInstance
        try {
            val session = sessionStub()
            extension.attach(session, instance)
            assertSame(instance.dawnContext, extension.dawnContext(session))
            extension.detach(session)
            assertFailsWith<IllegalStateException> { extension.dawnContext(session) }
        } finally {
            instance.close()
        }
    }

    @Test fun createRenderTextureRejectsZeroDimensions() {
        val extension = Dawn4kWebGpuToolkitExtension()
        val session = sessionStub()
        val rejected = assertFailsWith<IllegalArgumentException> {
            extension.createRenderTexture(session, width = 0u, height = 16u, format = GPUTextureFormat.RGBA8Unorm)
        }
        assertTrue(rejected.message!!.contains("positive"), rejected.message)
    }

    @Test fun openDawnSessionSerializesStartup() = runBlocking {
        val extension = Dawn4kWebGpuToolkitExtension()
        val first = async { openDawnSession(extension, SessionConfig(startupTimeout = 60.seconds)) }
        val second = async { openDawnSession(extension, SessionConfig(startupTimeout = 60.seconds)) }
        val sessionA = first.await()
        val sessionB = second.await()
        try {
            assertNotEquals(extension.dawnContext(sessionA), extension.dawnContext(sessionB))
        } finally {
            sessionA.manualProgress?.stop(); sessionA.shutdown()
            sessionB.manualProgress?.stop(); sessionB.shutdown()
        }
    }

    /** A session whose device is never reached: the accessors resolve through the extension. */
    private fun sessionStub(): Session {
        val device = java.lang.reflect.Proxy.newProxyInstance(
            GPUDevice::class.java.classLoader,
            arrayOf(GPUDevice::class.java),
        ) { _, method, _ ->
            when (method.name) {
                "getLabel" -> ""
                "hashCode" -> System.identityHashCode(method)
                "toString" -> "device-stub"
                else -> throw AssertionError("the device must not be touched: ${method.name}")
            }
        } as GPUDevice

        return object : Session {
            override val device: GPUDevice = device
            override val queue: org.graphiks.webgpu.GPUQueue
                get() = throw AssertionError("the queue must not be touched")
            override val state: kotlinx.coroutines.flow.StateFlow<SessionState> =
                kotlinx.coroutines.flow.MutableStateFlow(SessionState.Ready)
            override val manualProgress: ManualProgression? = null
            override suspend fun awaitLost(): org.graphiks.webgpu.GPUDeviceLostInfo =
                throw AssertionError("awaitLost must not be called")
            override suspend fun <R> run(block: suspend (Session) -> R): R =
                throw AssertionError("run must not be called")
            override suspend fun shutdown() = throw AssertionError("shutdown must not be called")
        }
    }
}
