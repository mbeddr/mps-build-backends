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

    include("lib/mpsant/mps-tool.jar")

    include("plugins/mps-modelchecker/lib/modelchecker.jar")
    include("plugins/mps-httpsupport/solutions/jetbrains.mps.ide.httpsupport.runtime.jar")
}

dependencies {
    implementation(kotlin("test"))
    implementation("com.fasterxml.jackson.dataformat:jackson-dataformat-xml:2.13.+")

    testImplementation(kotlin("test"))
    testImplementation("org.xmlunit:xmlunit-core:2.6.+")

}

tasks.test {
    useJUnitPlatform()
}
