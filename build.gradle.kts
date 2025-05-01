plugins {
    id("kotlin-base-conventions") apply false
}

tasks {
    register("setTeamCityBuildNumber") {
        // Empty task for compatibility with TeamCity builds, to be removed at some point
    }
    register("publish") {
        dependsOn(gradle.includedBuild("launcher").task(":publish"))
    }
}
