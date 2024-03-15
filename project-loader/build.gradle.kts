import de.itemis.mps.buildbackends.computeVersionSuffix

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("kotlin-conventions")
    id("org.jetbrains.kotlinx.binary-compatibility-validator") version "0.13.2"
}

version = "${project.extra["version.project-loader"]}${computeVersionSuffix()}"

// MPS runtime JARs should be available for compiling code and tests but not propagated to consumers.
val mpsRuntime by configurations.creating
val mpsZip by configurations.creating

configurations {
    compileOnly.get().extendsFrom(mpsRuntime)
    testCompileOnly.get().extendsFrom(mpsRuntime)
}

dependencies {
    compileOnly("log4j:log4j:1.2.17")
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
    })

    mpsZip(libs.mps)

    testImplementation("junit:junit:4.13.1")
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
    val mpsHome = layout.buildDirectory.dir("mps")
    val mpsZip by configurations.getting
    val unpackMps by registering(Sync::class) {
        dependsOn(mpsZip)
        from({ mpsZip.resolve().map(project::zipTree) })
        into(mpsHome)
    }
    test {
        dependsOn(unpackMps)
        classpath += mpsHome.get().dir("lib").asFileTree.matching { include("*.jar") }
    }
}
