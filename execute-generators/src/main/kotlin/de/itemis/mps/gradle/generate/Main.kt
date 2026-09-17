package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.logging.configureLogging
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("execute-generators") {
    val parsed = ArgParser(args).parseInto(::GenerateArgs)
    var result = GenerationResult.Error

    configureLogging(parsed.logLevel)

    try {
        result = parsed.buildLoader()
            .executeWithProject(parsed.project) { _, project -> generateProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        logger.log(Level.SEVERE, "error generating", ex)
    } catch (t: Throwable) {
        logger.log(Level.SEVERE, "error generating", t)
    }
    if (result.isFailure()) {
        throw SystemExitException("generation failed", result.exitCode)
    }

    exitProcess(result.exitCode)
}
