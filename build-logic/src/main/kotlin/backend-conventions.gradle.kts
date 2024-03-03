import de.itemis.mps.buildbackends.computeVersionSuffix

plugins {
    id("kotlin-conventions")
    id("application")
}

version = "${project.extra["version.backend"]}${computeVersionSuffix()}"

val mpsZip by configurations.creating

val libs = extensions.getByType<VersionCatalogsExtension>().named("libs")

val mpsZips = extensions.create("mpsZips", PatternSet::class)

dependencies {
    compileOnly(libs.findLibrary("commons.logging").get())
    mpsZip(libs.findLibrary("mps").get())
    implementation(project(":project-loader"))

    addProvider("compileOnly", provider { zipTree(mpsZip.singleFile).matching(mpsZips) })
}

publishing {
    publications {
        create<MavenPublication>("backend") {
            from(components["java"])
            versionMapping {
                allVariants {
                    fromResolutionResult()
                }
            }
        }
    }
}
