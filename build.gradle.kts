import de.itemis.mps.buildbackends.getCommandOutput
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    kotlin("jvm") version "1.6.10" apply false
}

val kotlinVersion by extra { "1.6.10" }

val kotlinArgParserVersion by extra { "2.0.7" }

//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.11.+" }

allprojects {
    dependencyLocking.lockAllConfigurations()
}

val versionMajor = 1
val versionMinor = 10

val suffix = run {
    val buildCounterStr = System.getenv("BUILD_COUNTER")
    if (buildCounterStr.isNullOrEmpty()) {
        return@run "-SNAPSHOT"
    } else {
        val gitCommitHash = getCommandOutput("git", "rev-parse", "--short=7", "HEAD")
        return@run ".$buildCounterStr.$gitCommitHash"
    }
}

allprojects {
    group = "de.itemis.mps.build-backends"
    version = "${versionMajor}.${versionMinor}${suffix}"
}

subprojects {
    repositories {
        mavenCentral()
        maven {
            url = uri("https://artifacts.itemis.cloud/repository/maven-mps")
        }
    }

    plugins.withType<JavaBasePlugin> {
        project.afterEvaluate {
            extensions.configure(JavaPluginExtension::class.java) {
                withSourcesJar()
            }
        }
    }

    tasks.withType<KotlinCompile> {
        kotlinOptions {
            jvmTarget = "11"
            apiVersion = "1.6"
            allWarningsAsErrors = true
        }
    }

    apply<MavenPublishPlugin>()
    extensions.configure(PublishingExtension::class.java) {
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
}

tasks {
    register("setTeamCityBuildNumber") {
        doLast {
            println("##teamcity[buildNumber '$version']")
        }
    }
}
