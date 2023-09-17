import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

group = "de.itemis.mps.build-backends"

repositories {
    mavenCentral()
    maven("https://artifacts.itemis.cloud/repository/maven-mps")
}

dependencyLocking.lockAllConfigurations()
