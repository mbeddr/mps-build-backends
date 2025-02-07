package de.itemis.mps.gradle.project.loader

import de.itemis.mps.gradle.logging.detectLogging
import java.io.File

private val logger = detectLogging().getLogger("de.itemis.mps.gradle.project.loader.ProjectLibraries")

internal fun findProjectLibraries(projectLocation: File, macros: List<Macro>, onFound: (Collection<String>) -> Unit) {
    val librariesXml = projectLocation.resolve(".mps/libraries.xml")
    if (!librariesXml.exists()) return

    val macroMap = macros.associate { it.name to it.value }
    val pathRegex = Regex("""<option\s+name=['"]path['"]\s+value=['"](.*?)['"]\s*/>""")
    librariesXml.useLines { lines ->
        val libraryPaths = lines
            .mapNotNull { pathRegex.find(it)?.groupValues?.get(1) }
            .map { expandMacros(macroMap, projectLocation, it) }
            .toList()

        logger.info("Found libraries in $librariesXml: $libraryPaths")
        onFound(libraryPaths)
    }
}

private fun expandMacros(macros: Map<String, String>, projectLocation: File, input: String): String {
    val projectDirPrefix = "\$PROJECT_DIR$"

    if (input.startsWith(projectDirPrefix)) {
        return input.replace(projectDirPrefix, projectLocation.path)
    }

    if (!input.startsWith("\${")) return input

    val macroLastChar = input.indexOf('}')
    if (macroLastChar < 0) return input // malformed

    val macroKey = input.substring(2, macroLastChar)
    val macroValue = macros[macroKey]

    if (macroValue == null) {
        logger.warn("Unknown macro: $macroKey")
        return input
    }

    return macroValue + input.substring(macroLastChar + 1)
}
