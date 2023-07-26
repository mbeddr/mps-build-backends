pluginManagement {
    plugins {
        kotlin("jvm") version "1.6.10" apply false
    }
}

rootProject.name = "mps-build-backends"

include("execute-generators")
include("project-loader")
include("modelcheck")
include("integration-tests")

dependencyResolutionManagement {
    versionCatalogs {
        create("libs") {
            library("mps", "com.jetbrains:mps:2021.2.5")
        }
    }
}
