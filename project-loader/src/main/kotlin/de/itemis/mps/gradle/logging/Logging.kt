package de.itemis.mps.gradle.logging

import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.Logger

// We need to keep a strong reference to the logger to prevent JUL from collecting it and all its handlers.
internal val logger = Logger.getLogger("de.itemis.mps")

/**
 * Configures `de.itemis.mps` loggers to log to [System.err] with the given [level]. May be called multiple times.
 */
public fun configureLogging(level: Level) {
    logger.level = level

    if (logger.handlers.isEmpty()) {
        val handler = ConsoleHandler()

        // IDEA uses a formatter that provides timestamps. Reuse it for consistent formatting and timestamps.
        getRootLoggerConsoleFormatter()?.let {
            handler.formatter = it

            // Avoid duplicate messages from the handler installed on the root logger.
            logger.useParentHandlers = false
        }
        logger.addHandler(handler)
    }

    logger.handlers.forEach { it.level = level }
}

private fun getRootLoggerConsoleFormatter(): Formatter? =
    Logger.getLogger("").handlers.asSequence()
        .filterIsInstance<ConsoleHandler>()
        .map { it.formatter }
        .singleOrNull()
