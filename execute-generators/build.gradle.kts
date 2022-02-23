import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import java.net.URI

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

repositories {
    mavenCentral()
    maven {
        url = URI("https://projects.itemis.de/nexus/content/repositories/mbeddr")
    }
}

val nexusUsername: String? by project
val nexusPassword: String? by project

val kotlinArgParserVersion: String by project
val mpsVersion: String by project

val kotlinApiVersion: String by project
val kotlinVersion: String   by project

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-tool:$mpsVersion")
    compileOnly("com.jetbrains:mps-messaging:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    //compileOnly("com.jetbrains:platform-concurrency:$mpsVersion")
    compileOnly("log4j:log4j:1.2.17")
    implementation(project(":project-loader"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
    kotlinOptions.apiVersion = kotlinApiVersion
    kotlinOptions.allWarningsAsErrors = true
}

publishing {
    publications {
        create<MavenPublication>("executeGenerators") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}
