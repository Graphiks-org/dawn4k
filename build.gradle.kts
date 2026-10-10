import org.gradle.api.tasks.testing.Test

group = "org.graphiks"
version = (project.findProperty("releaseVersion") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0-SNAPSHOT"

// The generated bootstrap supports external libraries through java.library.path.
// Use that path for Windows without hand-editing the generated C API sources.
if (System.getProperty("os.name").startsWith("Windows")) {
    val dawnWindowsBin = layout.projectDirectory.dir("dawn4k-native/build/native/mingwX64/shared/bin").asFile
    subprojects {
        if (name in setOf("dawn4k", "dawn4k-native", "dawn4k-demo")) {
            tasks.withType<Test>().configureEach {
                dependsOn(":dawn4k-native:prepareDawn")
                systemProperty("java.library.path", dawnWindowsBin.absolutePath)
                environment("PATH", "${dawnWindowsBin.absolutePath};${System.getenv("PATH")}")
            }
            tasks.withType<JavaExec>().configureEach {
                dependsOn(":dawn4k-native:prepareDawn")
                systemProperty("java.library.path", dawnWindowsBin.absolutePath)
                environment("PATH", "${dawnWindowsBin.absolutePath};${System.getenv("PATH")}")
            }
        }
    }
}
