import de.itemis.mps.buildbackends.computeVersionSuffix

plugins {
    id("kotlin-conventions")
}

version = "${project.extra["version.backend"]}${computeVersionSuffix()}"
