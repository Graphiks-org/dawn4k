package org.graphiks.dawn4k.toolkit.surface

/** Runs on the supplied platform owner. Each successful retain has one acknowledged retire. */
interface DawnTargetRetention {
    suspend fun retain()
    suspend fun retire()
}
