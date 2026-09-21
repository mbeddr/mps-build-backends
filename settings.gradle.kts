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
include("migration-common")
include("modelcheck")
include("execute")
include("remigrate")
include("migrate")

include("integration-tests")

includeBuild("launcher")
