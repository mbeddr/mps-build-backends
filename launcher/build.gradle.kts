import de.itemis.mps.buildbackends.computeVersionSuffix

plugins {
    `java-gradle-plugin`
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
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(8))
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
