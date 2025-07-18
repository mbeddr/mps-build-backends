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
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testImplementation("commons-io:commons-io:2.13.0")
    testImplementation("org.mockito:mockito-core:[4.11.0,5)") {
        because("does not support Java 8 from version 5")
    }
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.10.0")
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
