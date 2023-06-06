buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("backend-conventions")
}

val mpsZip by configurations.creating
dependencies {
    compileOnly("commons-logging:commons-logging:1.2")

    mpsZip(libs.mps)

    compileOnly(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-core.jar")
        include("lib/mps-environment.jar")
        include("lib/mps-generator.jar")
        include("lib/mps-platform.jar")
        include("lib/mps-openapi.jar")
        include("lib/mps-logging.jar")
        include("lib/platform-api.jar")
        include("lib/util.jar")

        include("lib/mps-messaging.jar")
        include("lib/app.jar")
        include("lib/mpsant/mps-tool.jar")
    })

    implementation(project(":project-loader"))
}

publishing {
    publications {
        create<MavenPublication>("executeGenerators") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}
