plugins {
    id("cmds.base-conventions")
    id("cmds.library-conventions")
}

repositories {
    maven("https://nexus.lucko.me/repository/maven-hytale/")
}

dependencies {
    api(project(":triumph-cmd-core"))
    compileOnly(libs.hytale)
}