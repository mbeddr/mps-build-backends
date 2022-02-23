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
val fastXmlJacksonVersion: String by project

val kotlinApiVersion: String by project
val kotlinVersion: String by project

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation(kotlin("test", version = kotlinVersion))
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:$fastXmlJacksonVersion")
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-modelchecker:$mpsVersion")
    compileOnly("com.jetbrains:mps-httpsupport-runtime:$mpsVersion")
    compileOnly("com.jetbrains:mps-project-check:$mpsVersion")
    compileOnly("com.jetbrains:mps-platform:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    //compileOnly("com.jetbrains:extensions:$mpsVersion")
    compileOnly("com.jetbrains:util:$mpsVersion")
    compileOnly("log4j:log4j:1.2.17")
    implementation(project(":project-loader"))
}

tasks.withType<KotlinCompile> {
    kotlinOptions.jvmTarget = "1.8"
}

publishing {
    publications {
        create<MavenPublication>("modelcheck") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}