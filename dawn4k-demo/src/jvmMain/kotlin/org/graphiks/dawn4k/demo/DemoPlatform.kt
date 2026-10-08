package org.graphiks.dawn4k.demo

internal enum class DemoPlatform { MacOS, Windows, Linux }

internal fun detectDemoPlatform(osName: String): DemoPlatform? {
    val os = osName.lowercase()
    return when {
        os.contains("mac") -> DemoPlatform.MacOS
        os.startsWith("windows") -> DemoPlatform.Windows
        os.contains("linux") -> DemoPlatform.Linux
        else -> null
    }
}
