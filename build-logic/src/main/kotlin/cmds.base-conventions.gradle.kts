import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `java-library`
    id("com.github.hierynomus.license")
    kotlin("jvm")
}

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    compileOnly("org.jetbrains:annotations:23.0.0")
}

java {
    sourceCompatibility = JavaVersion.VERSION_11
    targetCompatibility = JavaVersion.VERSION_11
    withSourcesJar()
    withJavadocJar()
}

license {
    header = rootProject.file("LICENSE")
    encoding = "UTF-8"
    mapping("kotlin", "JAVADOC_STYLE")
    mapping("java", "JAVADOC_STYLE")

    include("**/*.kt")
    include("**/*.java")
}

tasks {
    withType<JavaCompile> {
        options.encoding = "UTF-8"
        //options.compilerArgs.add("-Xlint:unchecked")
    }

    withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            javaParameters = true
        }
    }
}