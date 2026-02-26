package de.itemis.mps.gradle.execute

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("execute") {
    val parsed = ArgParser(args).parseInto(::ExecuteArgs)

    logging.configure(parsed.logLevel)

    val result = try {
        parsed.buildLoader().executeWithProject(parsed.project) { environment, project ->
            executeGeneratedCode(parsed, environment, project)
        }
    } catch (t: Throwable) {
        logger.fatal("error executing method", t)
        255
    }

    exitProcess(result)
}
