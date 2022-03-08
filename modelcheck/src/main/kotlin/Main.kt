package de.itemis.mps.gradle.modelcheck

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.modelcheck.de.itemis.mps.gradle.modelcheck.ModelCheckArgs
import de.itemis.mps.gradle.project.loader.executeWithProject

fun main(args: Array<String>) = mainBody("modelcheck") {

    val parsed = ArgParser(args).parseInto(::ModelCheckArgs)
    var hasErrors = true
    try {
        hasErrors = executeWithProject(parsed) { project -> modelCheckProject(parsed, project) }
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
