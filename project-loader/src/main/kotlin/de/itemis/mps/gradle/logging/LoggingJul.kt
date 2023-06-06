package de.itemis.mps.gradle.logging

import org.apache.commons.logging.Log
import org.apache.commons.logging.impl.Jdk14Logger
import java.util.logging.ConsoleHandler
import java.util.logging.Level
import java.util.logging.Logger

internal class LoggingJul : Logging {
    val logger = Logger.getLogger("de.itemis.mps")

    override fun getLogger(name: String): Log = Jdk14Logger(name)

    override fun configure(level: LogLevel) {
        logger.level = level.toJul()

        if (logger.handlers.isEmpty()) {
            val handler = ConsoleHandler()
            handler.level = logger.level
            logger.addHandler(handler)
        }
    }

    private fun LogLevel.toJul(): Level =
        when (this) {
            LogLevel.ALL -> Level.ALL
            LogLevel.INFO -> Level.INFO
            LogLevel.WARN -> Level.WARNING
            LogLevel.ERROR -> Level.SEVERE
            LogLevel.OFF -> Level.OFF
        }
}
