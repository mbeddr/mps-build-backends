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

internal class LoggerConfiguration private constructor(
    private val logger: Logger,
    private val handlers: List<HandlerConfiguration>,
    private val level: Level?,
    private val filter: java.util.logging.Filter?,
    private val useParentHandlers: Boolean
) {
    fun restore() {
        val originalHandlers = handlers.map { it.handler }
        logger.handlers.forEach { handler ->
            logger.removeHandler(handler)
            if (originalHandlers.none { it === handler }) handler.close()
        }
        handlers.forEach {
            it.restore()
            logger.addHandler(it.handler)
        }
        logger.level = level
        logger.filter = filter
        logger.useParentHandlers = useParentHandlers
    }

    companion object {
        fun capture(logger: Logger) = LoggerConfiguration(
            logger = logger,
            handlers = logger.handlers.map(::HandlerConfiguration),
            level = logger.level,
            filter = logger.filter,
            useParentHandlers = logger.useParentHandlers
        )
    }
}

private class HandlerConfiguration(val handler: Handler) {
    private val level = handler.level
    private val filter = handler.filter
    private val formatter = handler.formatter
    private val encoding = handler.encoding

    fun restore() {
        handler.level = level
        handler.filter = filter
        if (formatter != null) handler.formatter = formatter
        handler.encoding = encoding
    }
}
