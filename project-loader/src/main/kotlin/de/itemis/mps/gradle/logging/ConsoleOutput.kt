package de.itemis.mps.gradle.logging

import de.itemis.mps.gradle.project.loader.EnvironmentKind
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

internal interface ConsoleOutput : AutoCloseable {
    fun beforeEnvironmentCreated()

    fun environmentCreated()

    fun <T> showBackendOutput(action: () -> T): T
}

internal fun consoleOutput(
    verbose: Boolean,
    environmentKind: EnvironmentKind,
    logLevel: Level
): ConsoleOutput = when {
    verbose -> VerboseConsoleOutput(logLevel)
    environmentKind == EnvironmentKind.IDEA -> QuietIdeaConsoleOutput(logLevel)
    else -> QuietMpsConsoleOutput(logLevel)
}

internal fun configureLogger(logger: Logger, level: Level, createHandler: () -> Handler) {
    logger.level = level
    // Environment initialization can call this again after the platform has configured JUL.
    // Reuse our handler instead of emitting every record through another identical handler.
    if (logger.handlers.isEmpty()) {
        logger.addHandler(createHandler())
    }
    logger.handlers.forEach { it.level = level }
}
