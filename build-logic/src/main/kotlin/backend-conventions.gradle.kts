import de.itemis.mps.buildbackends.computeVersionSuffix

plugins {
    id("kotlin-conventions")
    id("application")
}

version = "${project.extra["version.backend"]}${computeVersionSuffix()}"

val mpsZip: Configuration by configurations.creating

val libs: VersionCatalog = extensions.getByType<VersionCatalogsExtension>().named("libs")

val mpsZips = extensions.create("mpsZips", PatternSet::class)

dependencies {
    mpsZip(libs.findLibrary("mps").get())
    implementation(project(":project-loader"))

    val matchingZips = provider { zipTree(mpsZip.singleFile).matching(mpsZips) }
    addProvider("compileOnly", matchingZips)
    addProvider("testImplementation", matchingZips)
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
