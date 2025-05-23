import com.specificlanguages.mps.ArtifactTransforms
import de.itemis.mps.buildbackends.computeVersionSuffix

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("kotlin-conventions")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
    id("de.itemis.mps.gradle.launcher")
    id("com.specificlanguages.mps.artifact-transforms")
}

version = "${project.extra["version.project-loader"]}${computeVersionSuffix()}"

// MPS runtime JARs should be available for compiling code and tests but not propagated to consumers.
val mpsRuntime: Configuration by configurations.creating
val mpsZip: Configuration by configurations.creating

@Suppress("UnstableApiUsage")
configurations {
    compileOnly.get().extendsFrom(mpsRuntime)
    testCompileOnly.get().extendsFrom(mpsRuntime)
}

dependencies {
    compileOnly("commons-logging:commons-logging:1.2")

    api("com.xenomachina:kotlin-argparser:2.0.7")

    mpsRuntime(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-core.jar")
        include("lib/mps-environment.jar")
        include("lib/mps-platform.jar")
        include("lib/mps-openapi.jar")
        include("lib/mps-logging.jar")
        include("lib/platform-api.jar")
        include("lib/util.jar")
        include("lib/util-8.jar")
        include("lib/testFramework.jar")
        include("lib/app.jar")
    })

    mpsZip(libs.mps)

    testImplementation("junit:junit:4.13.2")
}

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(11))
    }
}

kotlin {
    explicitApi()
}

publishing {
    publications {
        create<MavenPublication>("projectLoader") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}

tasks {
    val mpsHome = ArtifactTransforms.getMpsRoot(mpsZip)
    test {
        classpath += fileTree(mpsHome) { include("lib/*.jar", "lib/modules/*.jar") }
        mpsBackendLauncher.forMpsHome(mpsHome).configure(this)
    }
}
