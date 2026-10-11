package ygdrasil.conventions

import org.gradle.api.tasks.testing.Test

// The generated bootstrap supports external DLLs through java.library.path.
// Configure the consuming JVM tasks without editing the generated C API sources.
if (System.getProperty("os.name").startsWith("Windows")) {
    val dawnWindowsBin = project(":dawn4k-native").layout.buildDirectory
        .dir("native/mingwX64/shared/bin")
    // The native module may relocate its build directory during configuration,
    // even after a consumer has realized its JVM tasks.
    gradle.projectsEvaluated {
        val bin = dawnWindowsBin.get().asFile.absolutePath
        tasks.withType<Test>().configureEach {
            dependsOn(":dawn4k-native:prepareDawn")
            systemProperty("java.library.path", bin)
            environment("PATH", "$bin;${System.getenv("PATH")}")
        }
        tasks.withType<JavaExec>().configureEach {
            dependsOn(":dawn4k-native:prepareDawn")
            systemProperty("java.library.path", bin)
            environment("PATH", "$bin;${System.getenv("PATH")}")
        }
    }
}
