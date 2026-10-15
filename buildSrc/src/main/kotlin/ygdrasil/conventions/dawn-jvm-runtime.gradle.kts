package ygdrasil.conventions

import org.gradle.api.tasks.JavaExec
import org.gradle.api.tasks.testing.Test

// Every JVM runtime, including Windows, is loaded from verified jar resources.
// Keep FFI access explicit without adding checkout directories to PATH or
// java.library.path; those overrides would hide incomplete Maven packaging.
tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
tasks.withType<JavaExec>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
