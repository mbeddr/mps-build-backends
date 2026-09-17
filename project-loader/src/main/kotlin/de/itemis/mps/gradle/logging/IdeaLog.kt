package de.itemis.mps.gradle.logging

import com.intellij.openapi.application.PathManager
import java.io.File

public fun ideaLogFile(): File = File(PathManager.getLogPath(), "idea.log").absoluteFile

public fun printIdeaLogLocation() {
    System.err.println("MPS log file: ${ideaLogFile().path}")
}
