import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.nio.charset.Charset

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
val mpsVersion by extra { "2021.2.3" }

//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.11.+" }

allprojects {
    dependencyLocking.lockAllConfigurations()
}

val versionMajor = 1
val versionMinor = 0

val suffix = run {
    val buildNumberStr = System.getenv("BUILD_NUMBER")
    if (buildNumberStr.isNullOrEmpty()) {
        return@run "-SNAPSHOT"
    } else {
        return@run ".$buildNumberStr"
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
            url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
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
                url = uri("https://projects.itemis.de/nexus/content/repositories/mbeddr")
                if (project.hasProperty("nexusUsername")) {
                    credentials {
                        username = project.findProperty("nexusUsername") as String?
                        password = project.findProperty("nexusPassword") as String?
                    }
                }
            }
            maven {
                name = "GitHubPackages"
                url = uri("https://maven.pkg.github.com/mbeddr/mps-gradle-plugin")
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
