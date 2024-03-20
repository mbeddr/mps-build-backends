plugins {
    id("backend-conventions")
}

mpsZips {
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/platform-impl.jar")
    include("lib/util.jar")

    include("plugins/mps-migration/lib/*.jar")
}
