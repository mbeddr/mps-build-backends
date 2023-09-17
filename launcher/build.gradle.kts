import de.itemis.mps.buildbackends.computeVersionSuffix

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
