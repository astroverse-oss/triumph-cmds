plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
}

dependencies {
    // Using Kotlin 1.9.25 - compatible with Gradle 8.10.2
    implementation("org.jetbrains.kotlin:kotlin-gradle-plugin:1.9.25")
    implementation("gradle.plugin.com.hierynomus.gradle.plugins:license-gradle-plugin:0.16.1")
}

// Configure build-logic to handle Java 21 for Gradle 8.10.2 compatibility
java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21)) // Use Java 21 for build tooling
    }
}