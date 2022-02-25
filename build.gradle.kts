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
val mpsVersion by extra { "2021.2.3" }

//this version needs to align with the version shiped with MPS found in the /lib folder otherwise, runtime problems will
//surface because mismatching jars on the classpath.
val fastXmlJacksonVersion by extra { "2.11.+" }

allprojects {
    dependencyLocking.lockAllConfigurations()
}

subprojects {
    group = "de.itemis.mps.build-backends"
    version = "1.0.0"

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
}
