plugins {
    `java-library`
    `maven-publish`
}

tasks {
    publishing {
        publications {
            create<MavenPublication>("maven") {
                from(components["java"])

                pom {
                    name.set("triumph-cmds")
                    description.set("Multiplatform command framework")
                    url.set("https://github.com/TriumphTeam/triumph-cmds")

                    licenses {
                        license {
                            name.set("MIT License")
                            url.set("http://www.opensource.org/licenses/mit-license.php")
                        }
                    }

                    developers {
                        developer {
                            id.set("matt")
                            name.set("Mateus Moreira")
                        }
                    }

                    scm {
                        connection.set("scm:git:git://github.com/TriumphTeam/triumph-cmds.git")
                        developerConnection.set("scm:git:ssh://github.com:TriumphTeam/triumph-cmds.git")
                        url.set("https://github.com/TriumphTeam/triumph-cmds")
                    }
                }
            }
        }

        val version = project.version.toString()
        val type = if (version.endsWith("SNAPSHOT") || version.endsWith("DEV")) "snapshots" else "releases"

        repositories {
            maven {
                name = "astroverse"
                url = uri("https://repo.astroverse.es/repository/maven-${type}/")
                credentials(PasswordCredentials::class)
            }
        }
    }
}