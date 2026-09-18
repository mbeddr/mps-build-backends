package de.itemis.mps.gradle.logging

import com.intellij.openapi.diagnostic.Logger
import java.util.logging.Level

internal class QuietIdeaConsoleOutput(logLevel: Level) : QuietConsoleOutput(logLevel) {
    override fun environmentCreated() {
        // TestLoggerFactory (used in test mode) configures JUL lazily on the first requested logger.
        // Trigger it before quiet mode removes its console handler.
        Logger.getInstance(QuietIdeaConsoleOutput::class.java)

        super.environmentCreated()
    }
}
