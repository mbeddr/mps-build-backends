pluginManagement {
    includeBuild("build-logic")
}

plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version ("0.5.0")
}

rootProject.name = "mps-build-backends"

include("execute-generators")
include("project-loader")
include("modelcheck")
include("execute")
include("rerun-migrations")

include("integration-tests")

includeBuild("launcher")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("mps", "com.jetbrains:mps:2021.3.5")
            library("commons-logging", "commons-logging:commons-logging:1.2")
        }
    }
}
