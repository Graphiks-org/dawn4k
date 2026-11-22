group = "org.graphiks.webgpu"
version = (project.findProperty("releaseVersion") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0-SNAPSHOT"

// The Compose viewport module declares only the targets Compose 1.11.1 can
// compile for it: it has no tvOS host, no macOS native target (the desktop JVM
// actual covers macOS), it publishes no ui/foundation for Kotlin/Native Linux,
// and Compose 1.11 removed the Apple x64 targets. It asks the shared
// kmp-library convention to skip those targets. The property must be
// set before the module's plugins block runs, which is why it is set here rather
// than in the module script.
findProject(":dawn4k-compose")?.extensions?.extraProperties?.set(
    "kmp.skipTargets",
    "tvosArm64,tvosSimulatorArm64,linuxX64,iosX64,macosArm64",
)


val toolkitModules = listOf("dawn4k-webgpu-toolkit")
val toolkitPublicationVersion = providers.gradleProperty("releaseVersion").filter(String::isNotBlank).orElse("1.0.0-SNAPSHOT")

tasks.register<Exec>("verifyToolkitPublication") {
    group = "verification"
    description = "Publish locally and require every declared toolkit variant in Maven metadata."
    dependsOn((listOf("dawn4k-native", "dawn4k") + toolkitModules).map { ":$it:publishToMavenLocal" })
    doFirst {
        commandLine("python3", "scripts/verify-toolkit-coverage.py", "--metadata-root",
            System.getProperty("maven.repo.local", "${System.getProperty("user.home")}/.m2/repository"),
            "--version", toolkitPublicationVersion.get())
    }
}

tasks.register<Exec>("verifyToolkitCoverage") {
    group = "verification"
    description = "Require actual GPU execution for the complete toolkit platform matrix."
    val reportDirectory = providers.gradleProperty("toolkit.coverage.dir").orElse("build/toolkit-coverage")
    val subset = providers.gradleProperty("toolkit.coverage.require")
    inputs.dir(reportDirectory)
    doFirst {
        commandLine(buildList {
            addAll(listOf("python3", "scripts/verify-toolkit-coverage.py", "--report-dir", reportDirectory.get()))
            subset.orNull?.split(',')?.filter(String::isNotBlank)?.forEach { addAll(listOf("--require", it)) }
        })
    }
}
