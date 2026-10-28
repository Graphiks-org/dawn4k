plugins {
    `kotlin-dsl`
}

repositories {
    google()
    mavenCentral()
}

dependencies {
    implementation(libs.kotlin.gradle.plugin)
    implementation(libs.android.gradle.plugin)
    implementation(libs.dokka.gradle.plugin)
    implementation(libs.maven.publish.gradle.plugin)
    implementation(libs.commons.compress)
    implementation(libs.serialization.json)
    testImplementation(gradleTestKit())
    testImplementation(kotlin("test-junit"))
}

tasks.test {
    systemProperty("test.gradle.home", gradle.gradleHomeDir!!.absolutePath)
    systemProperty("test.repo.root", rootProject.projectDir.parentFile.absolutePath)
    systemProperty("test.testkit.dir", layout.buildDirectory.dir("test-kit").get().asFile.absolutePath)
}
