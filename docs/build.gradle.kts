plugins {
    alias(libs.plugins.dokka.mkdocs)
}

val apiModules = listOf("dawn4k-native", "dawn4k", "dawn4k-toolkit-core", "dawn4k-toolkit-surface", "dawn4k-toolkit-glfw")

dependencies {
    apiModules.forEach { dokka(project(":$it")) }
}

tasks.named("dokkaCopyIntoMkDocs") {
    inputs.file(rootProject.layout.projectDirectory.file("scripts/normalize-toolkit-doc-links.py"))
    doLast {
        val normalization = providers.exec {
            commandLine("python3", rootProject.file("scripts/normalize-toolkit-doc-links.py"),
                "--api-root", layout.projectDirectory.dir("docs/api").asFile)
        }
        normalization.result.get().assertNormalExitValue()
        logger.lifecycle(normalization.standardOutput.asText.get().trim())
    }
}

tasks.named("generateMkDocsNavigation") {
    actions.clear()
    doLast {
        // Declaration pages remain linked from Dokka's module and package indexes.
        // Listing every generated declaration repeats a huge sidebar on every page.
        layout.buildDirectory.file("mkdocs/navigation.yaml").get().asFile.writeText(buildString {
            appendLine("  - Reference:")
            appendLine("      - api/index.md")
            apiModules.forEach { appendLine("      - \"$it\": api/$it/index.md") }
        })
    }
}

tasks.register("verifyToolkitApiDocumentation") {
    group = "verification"
    description = "Require generated API pages for every published Dawn and toolkit module."
    dependsOn("embedDokkaIntoMkDocs")
    dependsOn(apiModules.map { ":$it:dokkaGeneratePublicationHtml" })
    doLast {
        for (module in apiModules) {
            val directory = layout.projectDirectory.dir("docs/api/$module").asFile
            check(directory.resolve("index.md").isFile && directory.walkTopDown().any { it.isFile && it.extension == "md" && it.name != "index.md" }) {
                "Missing generated API pages for $module"
            }
            // Dokka's HTML publication renders source links; the MkDocs renderer omits them.
            val html = rootProject.project(":$module").layout.buildDirectory.dir("dokka/html").get().asFile
            check(html.walkTopDown().filter { it.isFile && it.extension == "html" }.any {
                "https://github.com/Graphiks-org/dawn4k/blob/master/$module/" in it.readText()
            }) { "Missing repository source links for $module" }
        }
        logger.lifecycle("Verified generated API pages for all five published modules")
    }
}
