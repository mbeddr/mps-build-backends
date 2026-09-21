import de.itemis.mps.buildbackends.computeVersionSuffix

plugins {
    `java-gradle-plugin`
    id("base-conventions")
    id("publishing-conventions")
}

group = "de.itemis.mps.build-backends"
version = "${project.extra["version.launcher"]}${computeVersionSuffix()}"

repositories {
    gradlePluginPortal()
    mavenCentral()
}

dependencies {
    testImplementation(libs.junit.jupiter.api)
    testImplementation(libs.commons.io)
    testImplementation(libs.mockito.core) {
        because("does not support Java 8 from version 5")
    }
    testRuntimeOnly(libs.junit.jupiter.engine)
}

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(11)
    }
}

gradlePlugin {
    plugins {
        create("launcher") {
            id = "de.itemis.mps.gradle.launcher"
            implementationClass = "de.itemis.mps.gradle.LauncherPlugin"
        }
    }
}

tasks.test {
    useJUnitPlatform()
}
