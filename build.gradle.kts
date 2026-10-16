group = "org.graphiks"
version = (project.findProperty("releaseVersion") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "0.1.0-SNAPSHOT"

val toolkitModules = listOf("dawn4k-toolkit-core", "dawn4k-toolkit-surface", "dawn4k-toolkit-glfw")
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
        val reports = fileTree(reportDirectory.get()) { include("**/summary.json") }.files.sorted()
        check(reports.isNotEmpty()) { "No toolkit execution summaries in ${reportDirectory.get()}" }
        commandLine(buildList {
            addAll(listOf("python3", "scripts/verify-toolkit-coverage.py"))
            reports.forEach { addAll(listOf("--report", it.absolutePath)) }
            subset.orNull?.split(',')?.filter(String::isNotBlank)?.forEach { addAll(listOf("--require", it)) }
        })
    }
}
