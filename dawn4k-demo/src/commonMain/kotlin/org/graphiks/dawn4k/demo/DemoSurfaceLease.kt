package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.DawnBackend
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.graphiks.dawn4k.toolkit.surface.DawnSurfaceTarget
import org.graphiks.dawn4k.toolkit.surface.DawnTargetLease
import org.graphiks.dawn4k.toolkit.surface.DawnTargetState
import org.graphiks.dawn4k.toolkit.surface.PixelExtent as ToolkitPixelExtent

/** Retains the native target until the controller acknowledges surface cleanup. */
internal interface DemoSurfaceLease : AutoCloseable {
    val generation: Long
    val backend: DawnBackend
    val valid: Boolean
    val target: DawnSurfaceTarget
    suspend fun awaitRetirement()
    fun pixelExtent(): PixelExtent
}

/** Compatibility adapter; native host lifetime transfers to the lease. */
internal class DesktopSurfaceLease(
    override val generation: Long,
    private val host: SurfaceHost,
) : DemoSurfaceLease {
    private val alive = MutableStateFlow(true)
    private val states = MutableStateFlow(DawnTargetState(generation, ToolkitPixelExtent(0, 0), true))
    private val retainedTarget = RetiringDemoTarget(host.createTarget(states))
    override val target: DawnSurfaceTarget get() = retainedTarget
    override val backend get() = host.backend
    override val valid get() = alive.value
    override fun pixelExtent(): PixelExtent {
        check(valid) { "the viewport lease is invalid" }
        val (width, height) = host.pixelSize()
        states.value = states.value.copy(extent = ToolkitPixelExtent(width.coerceAtLeast(0), height.coerceAtLeast(0)))
        return PixelExtent(width, height)
    }
    override fun close() {
        retainedTarget.requireRetired()
        if (alive.compareAndSet(true, false)) {
            states.value = states.value.copy(available = false)
            host.close()
        }
    }
    override suspend fun awaitRetirement() = retainedTarget.awaitRetirement()
}

/** The application owns its UI host until every toolkit target lease acknowledges retirement. */
internal class RetiringDemoTarget(private val delegate: DawnSurfaceTarget) : DawnSurfaceTarget {
    override val state = delegate.state
    private val retiring = MutableStateFlow(false)
    private val references = MutableStateFlow(0)

    override suspend fun retain(): DawnTargetLease {
        check(!retiring.value) { "demo target is retiring" }
        references.update { it + 1 }
        val retained = try {
            check(!retiring.value) { "demo target is retiring" }
            delegate.retain()
        } catch (failure: Throwable) {
            references.update { it - 1 }
            throw failure
        }
        return object : DawnTargetLease by retained {
            private val retirement = Mutex()
            private var done = false
            override suspend fun retire() = withContext(NonCancellable) {
                retirement.withLock {
                    if (!done) {
                        retained.retire()
                        done = true
                        references.update { it - 1 }
                    }
                }
            }
        }
    }

    suspend fun awaitRetirement() {
        retiring.value = true
        references.first { it == 0 }
    }
    fun requireRetired() {
        retiring.value = true
        check(references.value == 0) { "await toolkit target retirement before destroying its UI host" }
    }
}
