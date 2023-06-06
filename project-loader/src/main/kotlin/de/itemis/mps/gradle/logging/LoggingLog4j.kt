package de.itemis.mps.gradle.logging

import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.Log4JLogger
import org.apache.log4j.ConsoleAppender
import org.apache.log4j.Level
import org.apache.log4j.Logger
import org.apache.log4j.SimpleLayout

internal class LoggingLog4j : Logging {

    private fun LogLevel.toLog4j(): Level =
        when (this) {
            LogLevel.ALL -> Level.ALL
            LogLevel.INFO -> Level.INFO
            LogLevel.WARN -> Level.WARN
            LogLevel.ERROR -> Level.ERROR
            LogLevel.OFF -> Level.OFF
        }

    override fun configure(level: LogLevel) {
        val logger = Logger.getLogger("de.itemis.mps")
        logger.level = level.toLog4j()

        if (level != LogLevel.OFF) {
            val hasAppenderForSystemErr =
                logger.allAppenders.asSequence().filterIsInstance<ConsoleAppender>().any { it.target == "System.err" }

            // Avoid adding the same appender twice
            if (!hasAppenderForSystemErr) {
                val appender = ConsoleAppender(SimpleLayout(), "System.err")
                appender.threshold = level.toLog4j()
                logger.addAppender(appender)
            }
        }
    }

    override fun getLogger(name: String): Log = Log4JLogger(name)
}
