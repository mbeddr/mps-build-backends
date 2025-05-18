plugins {
    `kotlin-dsl`
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    implementation("org.jetbrains.kotlin.jvm:org.jetbrains.kotlin.jvm.gradle.plugin:2.1.20")
    implementation("com.specificlanguages.mps.artifact-transforms:com.specificlanguages.mps.artifact-transforms.gradle.plugin:1.0.0")
}
