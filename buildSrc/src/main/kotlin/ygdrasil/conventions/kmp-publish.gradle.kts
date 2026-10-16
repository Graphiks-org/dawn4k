package ygdrasil.conventions

plugins {
    id("com.vanniktech.maven.publish")
}

group = rootProject.group.toString()
version = (project.findProperty("releaseVersion") as? String)
    ?.takeIf { it.isNotBlank() }
    ?: "1.0.0-SNAPSHOT"

val libraryDescription = when (project.name) {
    "dawn4k" -> "Dawn (WebGPU) Kotlin Multiplatform backend"
    "dawn4k-native" -> "Generated Dawn (WebGPU) native bindings for Kotlin Multiplatform"
    "dawn4k-toolkit-core" -> "Managed and manually progressed Dawn GPU sessions for Kotlin Multiplatform"
    "dawn4k-toolkit-surface" -> "Typed native presentation targets and frame lifecycles for Dawn GPU sessions"
    "dawn4k-toolkit-glfw" -> "Desktop GLFW windows and main-thread progression for Dawn GPU sessions"
    else -> "Graphiks dawn4k for Kotlin Multiplatform"
}

val isPublishing = project.findProperty("signingInMemoryKey")?.toString()?.isNotBlank() == true
    || project.findProperty("signing.keyId")?.toString()?.isNotBlank() == true

mavenPublishing {
    if (isPublishing) {
        publishToMavenCentral()
        signAllPublications()
    }
    coordinates(group.toString(), project.name, version.toString())

    pom {
        name.set(project.name)
        description.set(libraryDescription)
        url.set("https://github.com/Graphiks-org/dawn4k")
        inceptionYear.set("2026")

        licenses {
            license {
                name.set("MIT")
                url.set("https://opensource.org/license/MIT")
            }
        }

        developers {
            developer {
                id.set("amommers")
                name.set("Alexandre Mommers")
            }
        }

        scm {
            connection.set("scm:git:https://github.com/Graphiks-org/dawn4k.git")
            developerConnection.set("scm:git:ssh://github.com/Graphiks-org/dawn4k.git")
            url.set("https://github.com/Graphiks-org/dawn4k")
        }
    }
}
