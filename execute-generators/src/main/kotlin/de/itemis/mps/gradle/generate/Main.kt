package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import configureLogging
import de.itemis.mps.gradle.project.loader.executeWithProject
import org.apache.log4j.Logger

fun main(args: Array<String>) = mainBody("execute-generators") {
    val parsed = ArgParser(args).parseInto(::GenerateArgs)
    var result = GenerationResult.Error

    val logger = Logger.getLogger("de.itemis.mps.gradle.generate")
    configureLogging(parsed.logLevel)

    try {
        result = parsed.buildLoader()
            .executeWithProject(parsed.project) { _, project -> generateProject(parsed, project) }
    } catch (ex: java.lang.Exception) {
        logger.fatal("error generating", ex)
    } catch (t: Throwable) {
        logger.fatal("error generating", t)
    }
    if (result.isFailure()) {
        throw SystemExitException("generation failed", result.exitCode)
    }

    System.exit(result.exitCode)
}
