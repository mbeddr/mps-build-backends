package de.itemis.mps.gradle.modelcheck

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.logging.configureLogging
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("modelcheck") {

    val parsed = ArgParser(args).parseInto(::ModelCheckArgs)
    configureLogging(parsed.logLevel)

    var hasErrors = true
    try {
        hasErrors = parsed.buildLoader().executeWithProject(parsed.project) { environment, project ->
            modelCheckProject(parsed, environment, project)
        }
    } catch (ex: java.lang.Exception) {
        logger.log(Level.SEVERE, "error model checking", ex)
    } catch (t: Throwable) {
        logger.log(Level.SEVERE, "error model checking", t)
    }

    if (hasErrors && !parsed.dontFailOnError) {
        exitProcess(-1)
    }

    exitProcess(0)

}
