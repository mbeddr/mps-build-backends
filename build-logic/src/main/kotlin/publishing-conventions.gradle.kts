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
}
