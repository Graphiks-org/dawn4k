package org.graphiks.dawn4k.demo

import org.graphiks.dawn4k.toolkit.surface.DawnTargetRetention

/** The application lease keeps its UI host alive until RetiringDemoTarget acknowledges every user. */
internal object BorrowedDemoHostRetention : DawnTargetRetention {
    override suspend fun retain() = Unit
    override suspend fun retire() = Unit
}
