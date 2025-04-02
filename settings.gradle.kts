pluginManagement {
    includeBuild("build-logic")
    includeBuild("launcher")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.9.0")
}

rootProject.name = "mps-build-backends"

include("execute-generators")
include("project-loader")
include("modelcheck")
include("execute")
include("remigrate")

include("integration-tests")

includeBuild("launcher")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("mps", "com.jetbrains:mps:2024.3.1")
            library("commons-logging", "commons-logging:commons-logging:1.2")
        }
    }
}
