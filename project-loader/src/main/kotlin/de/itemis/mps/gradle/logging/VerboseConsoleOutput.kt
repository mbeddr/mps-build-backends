package de.itemis.mps.gradle.logging

import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.Logger

// Verbose mode handles the entire project-loader and backend hierarchy, including implementation diagnostics that
// quiet mode deliberately excludes.
private val mpsLogger = Logger.getLogger("de.itemis.mps")

internal class VerboseConsoleOutput(private val logLevel: Level) : ConsoleOutput {
    init {
        // Enable logging before environment creation so startup diagnostics are visible.
        configureLogging(logLevel)
    }

    override fun beforeEnvironmentCreated() = Unit

    override fun environmentCreated() {
        // The platform formatter only becomes available during environment initialization.
        configureLogging(logLevel)
    }

    override fun <T> showBackendOutput(action: () -> T): T = action()

    override fun close() = Unit

    companion object {
        internal fun configureLogging(level: Level) {
            configureLogger(mpsLogger, level) { ConsoleHandler() }
            getRootLoggerConsoleFormatter()?.let { formatter ->
                // Match platform console output and prevent the same record from also reaching the root handler.
                mpsLogger.handlers.filterIsInstance<ConsoleHandler>().forEach { handler ->
                    handler.formatter = formatter
                }
                mpsLogger.useParentHandlers = false
            }
        }
    }
}

private fun getRootLoggerConsoleFormatter(): Formatter? =
    Logger.getLogger("").handlers.asSequence()
        .filterIsInstance<ConsoleHandler>()
        .map { it.formatter }
        .singleOrNull()
