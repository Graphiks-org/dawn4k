plugins {
    id("com.android.application")
    alias(libs.plugins.compose.compiler)
}

android {
    namespace = "org.graphiks.dawn4k.demo.app"
    compileSdk = 37
    defaultConfig {
        applicationId = "org.graphiks.dawn4k.demo.app"
        minSdk = 28
        targetSdk = 37
        versionCode = 1
        versionName = "0.1.0"
        ndk { abiFilters += listOf("arm64-v8a", "x86_64") }
    }
    buildFeatures { compose = true }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
}

kotlin { compilerOptions { jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17) } }
base { archivesName.set("androidApp") }

dependencies {
    implementation(project(":dawn4k-demo"))
    implementation(libs.androidx.activity.compose)
}
