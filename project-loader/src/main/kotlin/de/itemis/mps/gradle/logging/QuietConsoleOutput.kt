package de.itemis.mps.gradle.logging

import java.io.OutputStream
import java.io.PrintStream
import java.util.logging.ConsoleHandler
import java.util.logging.Formatter
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

// Backend result and progress loggers use this subtree. Quiet mode can therefore show them without showing the
// implementation diagnostics in the wider de.itemis.mps hierarchy.
private val outputLogger = Logger.getLogger("de.itemis.mps.gradle.output")

internal abstract class QuietConsoleOutput(private val logLevel: Level) : ConsoleOutput {
    private val quietProperties = mapOf(
        "idea.log.console" to "false",
        "intellij.log.stdout" to "false",
        "intellij.console.log.level" to "OFF"
    )
    private val previousPropertyValues = quietProperties.keys.associateWith(System::getProperty)
    private val originalOut: PrintStream = System.out
    private val originalErr: PrintStream = System.err
    private val sink = PrintStream(OutputStream.nullOutputStream())

    init {
        // Install the backend handler before replacing System.out and System.err so it retains the real console streams.
        configureLogging(logLevel)

        // MPS logging initialization installs a new root ConsoleHandler after the existing handlers were removed.
        // It captures the current System.err and can emit startup warnings before environmentCreated() removes it.
        // Redirect both streams during initialization to suppress that handler and any direct platform output.
        quietProperties.forEach(System::setProperty)
        removeRootConsoleHandlers()
        System.setOut(sink)
        System.setErr(sink)
    }

    override fun beforeEnvironmentCreated() = Unit

    override fun environmentCreated() {
        // Platform initialization can install new root handlers and reconfigure JUL, so enforce quiet mode again.
        removeRootConsoleHandlers()
        showBackendOutput {
            configureLogging(logLevel)
        }

        // Propagation keeps backend records in the platform log. Its console handlers have already been removed.
        outputLogger.useParentHandlers = true
    }

    override fun <T> showBackendOutput(action: () -> T): T {
        // Backend actions are the only code allowed to write directly to the console in quiet mode.
        System.setOut(originalOut)
        System.setErr(originalErr)
        try {
            return action()
        } finally {
            System.setOut(sink)
            System.setErr(sink)
        }
    }

    override fun close() {
        removeRootConsoleHandlers()
        System.setOut(originalOut)
        System.setErr(originalErr)
        sink.close()

        previousPropertyValues.forEach { (name, value) ->
            if (value == null) {
                System.clearProperty(name)
            } else {
                System.setProperty(name, value)
            }
        }
    }

    private fun removeRootConsoleHandlers() {
        val rootLogger = Logger.getLogger("")
        rootLogger.handlers.filterIsInstance<ConsoleHandler>().forEach {
            rootLogger.removeHandler(it)
            it.close()
        }
    }

    companion object {
        internal fun configureLogging(level: Level) {
            configureLogger(outputLogger, level) { QuietConsoleHandler() }
            // Before the platform log is available, propagation would send records to a root console handler as well.
            outputLogger.useParentHandlers = false
        }
    }
}

private class QuietConsoleHandler : Handler() {
    // Keep the streams that were active when quiet mode started. System.out and System.err are temporarily replaced
    // with a sink while the platform initializes.
    private val standardOutput = System.out
    private val errorOutput = System.err

    init {
        formatter = object : Formatter() {
            override fun format(record: LogRecord): String = formatMessage(record)
        }
    }

    @Synchronized
    override fun publish(record: LogRecord) {
        if (!isLoggable(record)) return

        // Warnings are diagnostics and belong on stderr; informational output remains safe to pipe from stdout.
        val output = if (record.level.intValue() >= Level.WARNING.intValue()) errorOutput else standardOutput
        output.println(formatter.format(record))
        // The compact formatter removes record metadata, but an attached exception is still needed for diagnosis.
        record.thrown?.printStackTrace(output)
    }

    @Synchronized
    override fun flush() {
        standardOutput.flush()
        errorOutput.flush()
    }

    override fun close() = flush()
}
