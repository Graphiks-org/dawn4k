package org.graphiks.dawn4k.testing

import org.graphiks.dawn4k.internal.SynchronizedObject
import org.graphiks.webgpu.GPUError

/** Tests own their error history; the production binding only delivers errors. */
internal class UncapturedErrorCollector : (GPUError) -> Unit {
    private val lock = SynchronizedObject()
    private val received = mutableListOf<GPUError>()

    val errors: List<GPUError> get() = lock.withLock { received.toList() }
    val size: Int get() = lock.withLock { received.size }

    override fun invoke(error: GPUError) {
        lock.withLock { received += error }
    }
}
