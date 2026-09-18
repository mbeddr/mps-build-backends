package de.itemis.mps.gradle.logging

import java.util.logging.Level

internal class QuietMpsConsoleOutput(logLevel: Level) : QuietConsoleOutput(logLevel) {
    override fun beforeEnvironmentCreated() {
        // Newer MPS versions expose an explicit initializer for platform file logging. Older versions initialize
        // logging elsewhere and do not contain this class.
        val initializer = try {
            Class.forName("jetbrains.mps.core.tool.environment.util.LogInitializer")
        } catch (_: ClassNotFoundException) {
            return
        }
        initializer.getMethod("init").invoke(null)
    }
}
