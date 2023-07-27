import de.itemis.mps.buildbackends.getCommandOutput

val versionMajor = 1
val versionMinor = 10

val suffix = run {
    val buildCounterStr = System.getenv("BUILD_COUNTER")
    if (buildCounterStr.isNullOrEmpty()) {
        return@run "-SNAPSHOT"
    } else {
        val gitCommitHash = getCommandOutput("git", "rev-parse", "--short=7", "HEAD")
        return@run ".$buildCounterStr.$gitCommitHash"
    }
}

group = "de.itemis.mps.build-backends"
version = "${versionMajor}.${versionMinor}${suffix}"
