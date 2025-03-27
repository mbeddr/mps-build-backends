import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm")
    // To define repositories for Kotlin libraries
    id("base-conventions")
}

java {
    withSourcesJar()
}

tasks.withType<KotlinCompile> {
    kotlinOptions {
        jvmTarget = "11"
        apiVersion = "1.6"
        allWarningsAsErrors = true
    }
}
