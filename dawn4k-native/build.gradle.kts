import org.gradle.api.tasks.testing.Test

plugins {
    id("ygdrasil.conventions.dawn-desktop")
}

kotlin {
    sourceSets {
        commonMain.dependencies {
            api("org.graphiks:kffi:1.0.0-SNAPSHOT")
        }
    }
}

tasks.withType<Test>().configureEach {
    jvmArgs("--enable-native-access=ALL-UNNAMED")
}
