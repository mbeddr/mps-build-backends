import de.itemis.mps.buildbackends.CheckRelease

plugins {
    `maven-publish`
}

// Snapshot versions go to the itemis snapshot repository only, release versions to the itemis release repository and
// GitHub Packages.
val snapshotRepositoryName = "itemisSnapshots"

fun MavenArtifactRepository.itemisCredentials() {
    credentials {
        username = providers.gradleProperty("artifacts.itemis.cloud.user")
            .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_USER")).orNull
        password = providers.gradleProperty("artifacts.itemis.cloud.pw")
            .orElse(providers.environmentVariable("ARTIFACTS_ITEMIS_CLOUD_PW")).orNull
    }
}

publishing {
    repositories {
        maven {
            name = "itemis"
            url = uri("https://artifacts.itemis.cloud/repository/maven-mps-releases")
            itemisCredentials()
        }
        maven {
            name = snapshotRepositoryName
            url = uri("https://artifacts.itemis.cloud/repository/maven-mps-snapshots")
            itemisCredentials()
        }
        maven {
            name = "GitHubPackages"
            url = uri("https://maven.pkg.github.com/mbeddr/mps-build-backends")
            credentials {
                username = providers.gradleProperty("gpr.user")
                    .orElse(providers.environmentVariable("GITHUB_ACTOR")).orNull
                password = providers.gradleProperty("gpr.token")
                    .orElse(providers.environmentVariable("GITHUB_TOKEN")).orNull
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

val isSnapshot = provider { version.toString().endsWith("-SNAPSHOT") }

tasks.withType<PublishToMavenRepository>().configureEach {
    val snapshotRepository = snapshotRepositoryName
    val snapshot = isSnapshot
    onlyIf("snapshot versions are published only to the snapshot repository") {
        (repository.name == snapshotRepository) == snapshot.get()
    }
}

tasks.register<CheckRelease>("checkRelease") {
    description = "Checks that this project's version and dependencies can be published as a release."
    projectVersion = provider { version.toString() }
    releaseVersion = providers.gradleProperty("releaseVersion")
    runtimeClasspath = configurations.named("runtimeClasspath").flatMap { it.incoming.resolutionResult.rootComponent }
}
