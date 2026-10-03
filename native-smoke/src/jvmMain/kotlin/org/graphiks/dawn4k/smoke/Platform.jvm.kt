package org.graphiks.dawn4k.smoke

actual fun smokeTargetName(): String {
    val os = System.getProperty("os.name").lowercase().replace(Regex("[^a-z0-9]+"), "-").trim('-')
    val arch = System.getProperty("os.arch").lowercase()
    return "jvm-$os-$arch"
}

actual fun smokeIsMacOs(): Boolean =
    System.getProperty("os.name").lowercase().contains("mac")
