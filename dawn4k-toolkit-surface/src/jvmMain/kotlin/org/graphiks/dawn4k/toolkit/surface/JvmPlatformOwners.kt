package org.graphiks.dawn4k.toolkit.surface

import java.awt.EventQueue
import kotlinx.coroutines.CoroutineDispatcher
import kotlin.coroutines.CoroutineContext

internal actual val surfacePlatform: SurfacePlatform
    get() {
        val name = System.getProperty("os.name").lowercase()
        return when {
            "mac" in name -> SurfacePlatform.Apple
            "windows" in name -> SurfacePlatform.Windows
            "linux" in name -> SurfacePlatform.Linux
            else -> throw UnsupportedOperationException("unsupported native surface host: $name")
        }
    }

object DawnPlatformOwners {
    /** Reuses the AWT event thread; the toolkit does not own or stop it. */
    fun awt(): DawnPlatformOwner = DawnPlatformOwner.fromDispatcher(object : CoroutineDispatcher() {
        override fun isDispatchNeeded(context: CoroutineContext) = !EventQueue.isDispatchThread()
        override fun dispatch(context: CoroutineContext, block: Runnable) = EventQueue.invokeLater(block)
    })
}
