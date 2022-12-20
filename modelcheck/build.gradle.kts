import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    kotlin("jvm")
    `maven-publish`
}

val mpsVersion: String by project

dependencies {
    compileOnly("com.jetbrains:mps-environment:$mpsVersion")
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-modelchecker:$mpsVersion")
    compileOnly("com.jetbrains:mps-httpsupport-runtime:$mpsVersion")
    compileOnly("com.jetbrains:mps-project-check:$mpsVersion")
    compileOnly("com.jetbrains:mps-platform:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    compileOnly("com.jetbrains:util:$mpsVersion")
    compileOnly("log4j:log4j:1.2.17")
    implementation(project(":project-loader"))

    implementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
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