package ygdrasil.conventions

import java.net.URI

plugins {
    id("org.jetbrains.dokka")
}

dokka {
    moduleName.set(project.name)
    dokkaSourceSets.configureEach {
        sourceLink {
            localDirectory.set(project.projectDir)
            remoteUrl.set(URI("https://github.com/Graphiks-org/dawn4k/blob/master/${project.name}"))
            remoteLineSuffix.set("#L")
        }
    }
}
