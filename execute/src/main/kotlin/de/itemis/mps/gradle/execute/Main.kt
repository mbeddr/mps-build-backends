package de.itemis.mps.gradle.execute

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("execute") {
    val parsed = ArgParser(args).parseInto(::ExecuteArgs)

    logging.configure(parsed.logLevel)

    try {
        parsed.buildLoader().executeWithProject(parsed.project) { environment, project ->
            executeGeneratedCode(parsed, environment, project)
        }
    } catch (t: Throwable) {
        logger.fatal("error executing method", t)
        exitProcess(255)
    }

    exitProcess(0)
}
