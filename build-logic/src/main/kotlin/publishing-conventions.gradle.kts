plugins {
    `maven-publish`
}

publishing {
    repositories {
        maven {
            name = "itemis"
            url = uri("https://artifacts.itemis.cloud/repository/maven-mps-releases")
            if (project.hasProperty("artifacts.itemis.cloud.user") && project.hasProperty("artifacts.itemis.cloud.pw")) {
                credentials {
                    username = project.findProperty("artifacts.itemis.cloud.user") as String?
                    password = project.findProperty("artifacts.itemis.cloud.pw") as String?
                }
            }
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mbeddr/mps-build-backends")
            if (project.hasProperty("gpr.token")) {
                credentials {
                    username = project.findProperty("gpr.user") as String?
                    password = project.findProperty("gpr.token") as String?
                }
            }
        }
    }

    publications.withType<MavenPublication>().configureEach {
        pom {
            url = "https://github.com/mbeddr/mps-build-backends"
            licenses {
                license {
                    name = "The Apache License, Version 2.0"
                    url = "https://www.apache.org/licenses/LICENSE-2.0.txt"
                }
            }
            scm {
                connection = "scm:git:git://github.com/mbeddr/mps-build-backends.git"
                developerConnection = "scm:git:ssh://github.com/mbeddr/mps-build-backends.git"
                url = "https://github.com/mbeddr/mps-build-backends"
            }
        }
    }
}
