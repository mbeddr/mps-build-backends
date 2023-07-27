import de.itemis.mps.buildbackends.getCommandOutput
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

plugins {
    id("version-conventions")
}

repositories {
    mavenCentral()
    maven("https://artifacts.itemis.cloud/repository/maven-mps")
}

dependencyLocking.lockAllConfigurations()
