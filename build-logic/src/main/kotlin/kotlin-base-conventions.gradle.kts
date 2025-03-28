import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion

plugins {
    kotlin("jvm")
    // To define repositories for Kotlin libraries
    id("base-conventions")
}

java {
    withSourcesJar()
    targetCompatibility = JavaVersion.VERSION_11
    sourceCompatibility = JavaVersion.VERSION_11
}

kotlin {
    compilerOptions {
        jvmTarget = JvmTarget.JVM_11
        apiVersion = KotlinVersion.KOTLIN_1_6
        languageVersion = KotlinVersion.KOTLIN_1_6
    }
}
