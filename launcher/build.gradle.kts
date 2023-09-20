import de.itemis.mps.buildbackends.computeVersionSuffix
import org.jetbrains.kotlin.gradle.dsl.JvmTarget
import org.jetbrains.kotlin.gradle.dsl.KotlinVersion
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    `kotlin-dsl`
    id("base-conventions")
    id("publishing-conventions")
}

repositories {
    gradlePluginPortal()
    mavenCentral()
}

group = "de.itemis.mps.build-backends"
version = "${project.extra["version.launcher"]}${computeVersionSuffix()}"

java {
    targetCompatibility = JavaVersion.VERSION_1_8
}

tasks.withType<KotlinCompile> {
    compilerOptions {
        jvmTarget.set(JvmTarget.JVM_1_8)
        apiVersion.set(KotlinVersion.KOTLIN_1_5)
        allWarningsAsErrors.set(true)
    }
}
