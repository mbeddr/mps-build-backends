package de.itemis.mps.gradle.migrate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.SystemExitException
import com.xenomachina.argparser.mainBody
import de.itemis.mps.gradle.logging.configureLogging
import kotlin.system.exitProcess

fun main(args: Array<String>): Unit = mainBody("migrate") {
    val parsedArgs = ArgParser(args).parseInto(::MigrateArgs)
    configureLogging(parsedArgs.logLevel)

    try {
        migrate(parsedArgs)
    } catch (e: SystemExitException) {
        throw e
    } catch (e: Throwable) {
        e.printStackTrace(System.err)
        throw SystemExitException("exception occurred while migrating", 1)
    }

    exitProcess(0) // Have to explicitly exit because IDEA leaves non-daemon threads behind.
}
