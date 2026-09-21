plugins {
    id("backend-conventions")
}

mpsZips {
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-project-check.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/util.jar")
    include("lib/util-8.jar")
    include("lib/app.jar")

    include("lib/mpsant/mps-tool.jar")

    include("plugins/mps-modelchecker/lib/modelchecker.jar")
}

dependencies {
    testImplementation(libs.xmlunit.core)

    testImplementation(libs.junit.jupiter.api)
    testRuntimeOnly(libs.junit.jupiter.engine)
    testRuntimeOnly(libs.junit.platform.launcher)
}

tasks.test {
    useJUnitPlatform()
}
