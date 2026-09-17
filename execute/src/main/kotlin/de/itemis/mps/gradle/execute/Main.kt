package de.itemis.mps.gradle.execute

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.logging.printIdeaLogLocation
import java.util.logging.Level
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("execute") {
    val parsed = ArgParser(args).parseInto(::ExecuteArgs)

    val result = try {
        parsed.buildLoader().executeWithProject(parsed.project) { environment, project ->
            executeGeneratedCode(parsed, environment, project)
        }
    } catch (t: Throwable) {
        logger.log(Level.SEVERE, "error executing method", t)
        printIdeaLogLocation()
        255
    }

    exitProcess(result)
}
