package org.graphiks.dawn4k.internal

/**
 * JVM and Android actual: the platform monitor of the lock object itself,
 * entered through the `synchronized` intrinsic. Reentrant, like every JVM
 * monitor — the protocol still never re-enters it.
 */
internal actual class SynchronizedObject {

    actual fun <R> withLock(block: () -> R): R = synchronized(this, block)
}
