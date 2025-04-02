import org.apache.tools.ant.filters.ReplaceTokens

plugins {
    id("backend-conventions")
}

mpsZips {
    include("lib/app.jar")
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/platform-impl.jar")
    include("lib/util.jar")
    include("lib/util-8.jar")

    include("plugins/mps-migration/lib/*.jar")
}

tasks.processResources {
    filter(ReplaceTokens::class, "tokens" to mapOf("version" to project.version))
}
