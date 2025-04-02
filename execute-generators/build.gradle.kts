plugins {
    id("backend-conventions")
}

mpsZips {
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-generator.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/util.jar")
    include("lib/util-8.jar")

    include("lib/mps-messaging.jar")
    include("lib/app.jar")
    include("lib/mpsant/mps-tool.jar")
}
