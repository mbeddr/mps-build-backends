package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.logging.printIdeaLogLocation
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("execute-generators") {
    val parsed = ArgParser(args).parseInto(::GenerateArgs)
    var result = GenerationResult.Error

    try {
        result = parsed.buildLoader()
            .executeWithProject(parsed.project) { _, project -> generateProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        outputLogger.log(java.util.logging.Level.SEVERE, "error generating", ex)
    } catch (t: Throwable) {
        outputLogger.log(java.util.logging.Level.SEVERE, "error generating", t)
    }
    if (result.isFailure()) {
        printIdeaLogLocation()
        throw SystemExitException("generation failed", result.exitCode)
    }

    exitProcess(result.exitCode)
}
