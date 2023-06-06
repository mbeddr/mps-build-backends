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
    mpsZip(libs.mps)

    compileOnly("commons-logging:commons-logging:1.2")

    compileOnly(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-core.jar")
        include("lib/mps-environment.jar")
        include("lib/mps-platform.jar")
        include("lib/mps-project-check.jar")
        include("lib/mps-openapi.jar")
        include("lib/mps-logging.jar")
        include("lib/platform-api.jar")
        include("lib/util.jar")

        include("lib/mps-modelchecker.jar")
        include("lib/mpsant/mps-tool.jar")

        include("plugins/mps-modelchecker/lib/modelchecker.jar")
        include("plugins/mps-httpsupport/solutions/jetbrains.mps.ide.httpsupport.runtime.jar")
    })

    implementation(project(":project-loader"))

    implementation(kotlin("test"))
    testImplementation(kotlin("test"))
}

tasks.test {
    useJUnitPlatform()
}

publishing {
    publications {
        create<MavenPublication>("modelcheck") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}