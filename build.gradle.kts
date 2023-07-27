plugins {
    id("version-conventions")
}
tasks {
    register("setTeamCityBuildNumber") {
        doLast {
            println("##teamcity[buildNumber '$version']")
        }
    }
}
