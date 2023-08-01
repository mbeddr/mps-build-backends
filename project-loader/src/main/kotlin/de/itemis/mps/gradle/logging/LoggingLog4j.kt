package de.itemis.mps.gradle.logging

import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.Log4JLogger
import org.apache.log4j.*

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

        val existingAppender = logger.allAppenders.asSequence().filterIsInstance<ConsoleAppender>()
            .firstOrNull { it.target == "System.err" }

        val layout = getRootLoggerConsoleLayout() ?: PatternLayout(PatternLayout.TTCC_CONVERSION_PATTERN)

        val appender: ConsoleAppender

        // Avoid adding the same appender twice
        if (existingAppender != null) {
            appender = existingAppender
            appender.layout = layout
        } else {
            appender = ConsoleAppender(layout, "System.err")
            logger.addAppender(appender)
        }
    }

    private fun getRootLoggerConsoleLayout(): Layout? =
        Logger.getRootLogger().allAppenders.asSequence()
            .filterIsInstance(ConsoleAppender::class.java)
            .map { it.layout }
            .firstOrNull()

    override fun getLogger(name: String): Log = Log4JLogger(name)
}
