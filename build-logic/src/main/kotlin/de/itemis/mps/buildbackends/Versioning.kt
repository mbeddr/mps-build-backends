package de.itemis.mps.buildbackends

fun computeVersionSuffix(): String {
    val buildCounterStr = System.getenv("BUILD_COUNTER")
    if (buildCounterStr.isNullOrEmpty()) {
        return "-SNAPSHOT"
    } else {
        val gitCommitHash = getCommandOutput("git", "rev-parse", "--short=7", "HEAD")
        return ".$buildCounterStr.$gitCommitHash"
    }
}
