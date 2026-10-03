package org.graphiks.dawn4k.smoke

/** Report label for the running target, e.g. `jvm-mac os x-aarch64` or `native-macos-aarch64`. */
expect fun smokeTargetName(): String

/** True on macOS, where the smoke selects the Metal backend. */
expect fun smokeIsMacOs(): Boolean
