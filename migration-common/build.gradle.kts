plugins {
    id("kotlin-conventions")
}

val mpsZip: Configuration by configurations.creating

dependencies {
    compileOnly(libs.commons.logging)
    mpsZip(libs.mps)
    implementation(project(":project-loader"))

    compileOnly(zipTree({ mpsZip.singleFile }).matching {
        include("lib/mps-core.jar")
        include("lib/mps-environment.jar")
        include("lib/mps-platform.jar")
        include("lib/mps-openapi.jar")
        include("lib/util.jar")
        include("lib/util-8.jar")
        include("lib/app.jar")
    })
}
