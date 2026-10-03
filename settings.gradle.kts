pluginManagement {
    repositories {
        google {
            content {
                includeGroupByRegex("com\\.android.*")
                includeGroupByRegex("com\\.google.*")
                includeGroupByRegex("androidx.*")
            }
        }
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositoriesMode.set(RepositoriesMode.FAIL_ON_PROJECT_REPOS)
    repositories {
        google()
        mavenCentral()
        maven {
            name = "GraphiksSnapshots"
            url = uri("https://central.sonatype.com/repository/maven-snapshots/")
            content {
                includeModuleByRegex("org\\.graphiks", "kffi(?:-.*)?")
                includeModuleByRegex("org\\.graphiks", "webgpu-(?:api|descriptors)(?:-.*)?")
                includeModuleByRegex("org\\.graphiks", "suite-(?:core|acid-tests)(?:-.*)?")
            }
        }
    }
}

rootProject.name = "dawn4k"
include(":dawn4k-native")
include(":docs")
