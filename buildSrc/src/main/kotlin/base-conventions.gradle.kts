import de.itemis.mps.buildbackends.getCommandOutput
import org.gradle.kotlin.dsl.maven
import org.gradle.kotlin.dsl.repositories

repositories {
    mavenCentral()
    maven("https://artifacts.itemis.cloud/repository/maven-mps")
}

dependencyLocking.lockAllConfigurations()

val versionMajor = 1
val versionMinor = 10

val suffix = run {
    val buildCounterStr = System.getenv("BUILD_COUNTER")
    if (buildCounterStr.isNullOrEmpty()) {
        return@run "-SNAPSHOT"
    } else {
        val gitCommitHash = getCommandOutput("git", "rev-parse", "--short=7", "HEAD")
        return@run ".$buildCounterStr.$gitCommitHash"
    }
}

group = "de.itemis.mps.build-backends"
version = "${versionMajor}.${versionMinor}${suffix}"
