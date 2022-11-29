package de.itemis.mps.gradle.modelcheck

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import configureLogging
import de.itemis.mps.gradle.project.loader.executeWithEnvironmentAndProject

fun main(args: Array<String>) = mainBody("modelcheck") {

    val parsed = ArgParser(args).parseInto(::ModelCheckArgs)
    configureLogging(parsed.logLevel)

    var hasErrors = true
    try {
        hasErrors = executeWithEnvironmentAndProject(parsed) { environment, project -> modelCheckProject(parsed, environment, project) }
    } catch (ex: java.lang.Exception) {
        logger.fatal("error model checking", ex)
    } catch (t: Throwable) {
        logger.fatal("error model checking", t)
    }

    if (hasErrors && !parsed.dontFailOnError) {
        System.exit(-1)
    }

    System.exit(0)

}
