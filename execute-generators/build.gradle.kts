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
    compileOnly("com.jetbrains:mps-openapi:$mpsVersion")
    compileOnly("com.jetbrains:mps-core:$mpsVersion")
    compileOnly("com.jetbrains:mps-tool:$mpsVersion")
    compileOnly("com.jetbrains:mps-messaging:$mpsVersion")
    compileOnly("com.jetbrains:platform-api:$mpsVersion")
    compileOnly("com.jetbrains:util:$mpsVersion")
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
