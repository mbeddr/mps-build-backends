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
    include("plugins/mps-httpsupport/solutions/jetbrains.mps.ide.httpsupport.runtime.jar")
}

dependencies {
    testImplementation("org.xmlunit:xmlunit-core:2.6.+")

    testImplementation("org.junit.jupiter:junit-jupiter-api:5.10.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.test {
    useJUnitPlatform()
}
