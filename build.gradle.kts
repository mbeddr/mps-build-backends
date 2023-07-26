tasks {
    register("setTeamCityBuildNumber") {
        doLast {
            println("##teamcity[buildNumber '$version']")
        }
    }
}
