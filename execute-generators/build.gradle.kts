buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    id("kotlin-conventions")
}

val mpsZip by configurations.creating
dependencies {
    compileOnly("log4j:log4j:1.2.17")

    mpsZip(libs.mps)

    compileOnly(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-openapi.jar")
        include("lib/mps-core.jar")
        include("lib/mps-generator.jar")
        include("lib/mps-messaging.jar")
        include("lib/platform-api.jar")
        include("lib/util.jar")

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
