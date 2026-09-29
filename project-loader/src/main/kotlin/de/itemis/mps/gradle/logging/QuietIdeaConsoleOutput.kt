package de.itemis.mps.gradle.logging

import com.intellij.openapi.diagnostic.Logger
import com.intellij.openapi.util.ShutDownTracker
import java.util.logging.ConsoleHandler
import java.util.logging.Level

internal class QuietIdeaConsoleOutput(logLevel: Level) : QuietConsoleOutput(logLevel) {
    override fun environmentCreated() {
        // TestLoggerFactory (used in test mode) configures JUL lazily on the first requested logger.
        // Trigger it before quiet mode removes its console handler.
        Logger.getInstance(QuietIdeaConsoleOutput::class.java)

        super.environmentCreated()
    }

    override fun close() {
        super.close()

        // Cache shutdown tasks run before ordinary shutdown tasks, in reverse registration order.
        // Register after disposal so console logging is disabled before the platform's shutdown diagnostics.
        val rootLogger = java.util.logging.Logger.getLogger("")
        ShutDownTracker.getInstance().registerCacheShutdownTask {
            rootLogger.handlers.filterIsInstance<ConsoleHandler>().forEach {
                rootLogger.removeHandler(it)
            }
        }
    }
}
