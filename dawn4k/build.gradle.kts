import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.kmp-desktop-library")
}

val abiHost: String = run {
    val os = System.getProperty("os.name").lowercase()
    val arch = System.getProperty("os.arch").lowercase()
    val osName = when {
        os.contains("mac") -> "macos"
        os.contains("linux") -> "linux"
        else -> os.replace(Regex("[^a-z0-9]+"), "-")
    }
    val archName = when (arch) {
        "aarch64", "arm64" -> "aarch64"
        "x86_64", "amd64" -> "x86-64"
        else -> arch
    }
    "$osName-$archName"
}

kotlin {
    sourceSets.getByName("commonMain").dependencies {
        api(project(":dawn4k-native"))
        api("org.graphiks:webgpu-api:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.core)
    }
    sourceSets.getByName("commonTest").dependencies {
        implementation("org.graphiks:webgpu-descriptors:0.1.0-SNAPSHOT")
        implementation("org.graphiks:suite-acid-tests:0.1.0-SNAPSHOT")
        implementation(libs.kotlinx.coroutines.test)
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}

// The Dawn Linux archives are built against a newer glibc/libstdc++ than the
// Kotlin/Native macOS sysroot, so the Linux test binary can only be linked on a
// Linux host. The Linux klib is still compiled everywhere.
if (!abiHost.startsWith("linux")) {
    tasks.matching { it.name == "linkDebugTestLinuxX64" || it.name == "linuxX64Test" }
        .configureEach { enabled = false }
}
