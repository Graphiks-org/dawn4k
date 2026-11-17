package org.graphiks.dawn4k.webgpu.toolkit.surface

internal actual val surfacePlatform: SurfacePlatform get() = SurfacePlatform.Apple

object DawnPlatformOwners {
    fun appleMain(): DawnPlatformOwner = appleMainOwner()
}
