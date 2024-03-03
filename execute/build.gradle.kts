plugins {
    id("backend-conventions")
}

application {
    mainClass.set("de.itemis.mps.gradle.execute.MainKt")
}

mpsZips {
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/util.jar")

    include("lib/mpsant/mps-tool.jar")
}
