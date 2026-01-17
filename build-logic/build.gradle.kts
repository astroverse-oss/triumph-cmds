plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // Updated to newer Kotlin version compatible with Java 25
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:2.0.20")
    implementation("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:0.16.1")
}

// Configure build-logic to handle Java 25
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Use Java 21 for build tooling
    }
}