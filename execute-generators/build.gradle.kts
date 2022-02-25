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

val kotlinArgParserVersion: String by project
val mpsVersion: String by project
val kotlinVersion: String   by project

dependencies {
    implementation(kotlin("stdlib-jdk8", version = kotlinVersion))
    implementation("com.xenomachina:kotlin-argparser:$kotlinArgParserVersion")
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-tool:$mpsVersion")
    compileOnly("com.jetbrains:mps-messaging:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    compileOnly("log4j:log4j:1.2.17")
    implementation(project(":project-loader"))
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
