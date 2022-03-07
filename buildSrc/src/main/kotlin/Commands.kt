package de.itemis.mps.buildbackends

fun getCommandOutput(vararg args: String): String {
    val p = ProcessBuilder(*args).start()
    return p.inputStream.readAllBytes().decodeToString().trim()
}
