package de.itemis.mps.gradle.logging

import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.PrintStream
import java.util.logging.Handler
import java.util.logging.Filter
import java.util.logging.Formatter
import java.util.logging.Level
import java.util.logging.LogRecord
import java.util.logging.Logger

class LoggingTest {
    private val logger = Logger.getLogger("de.itemis.mps")
    private lateinit var previousHandlers: Array<Handler>
    private var previousLevel: Level? = null
    private var previousUseParentHandlers: Boolean = true

    @BeforeEach
    fun saveLoggerConfiguration() {
        previousHandlers = logger.handlers
        previousLevel = logger.level
        previousUseParentHandlers = logger.useParentHandlers
        previousHandlers.forEach(logger::removeHandler)
    }

    @AfterEach
    fun restoreLoggerConfiguration() {
        logger.handlers.forEach(logger::removeHandler)
        previousHandlers.forEach(logger::addHandler)
        logger.level = previousLevel
        logger.useParentHandlers = previousUseParentHandlers
    }

    @Test
    fun `reconfigures existing handler without adding another one`() {
        VerboseConsoleOutput.configureLogging(Level.WARNING)
        VerboseConsoleOutput.configureLogging(Level.INFO)

        assertEquals(Level.INFO, logger.level)
        assertEquals(1, logger.handlers.size)
        assertEquals(Level.INFO, logger.handlers.single().level)
    }

    @Test
    fun `verbose output restores logger configuration`() {
        val handler = java.util.logging.ConsoleHandler()
        val filter = Filter { true }
        val formatter = object : Formatter() {
            override fun format(record: LogRecord): String = record.message
        }
        logger.addHandler(handler)
        logger.level = Level.FINE
        logger.filter = filter
        logger.useParentHandlers = true
        handler.level = Level.SEVERE
        handler.filter = filter
        handler.formatter = formatter

        VerboseConsoleOutput(Level.WARNING).use { it.environmentCreated() }

        assertEquals(listOf(handler), logger.handlers.toList())
        assertSame(Level.FINE, logger.level)
        assertSame(filter, logger.filter)
        assertEquals(true, logger.useParentHandlers)
        assertSame(Level.SEVERE, handler.level)
        assertSame(filter, handler.filter)
        assertSame(formatter, handler.formatter)
    }

    @Test
    fun `quiet logging writes messages to the appropriate stream without metadata`() {
        val outputLogger = Logger.getLogger("de.itemis.mps.gradle.output")
        val previousHandlers = outputLogger.handlers
        val previousLevel = outputLogger.level
        val previousUseParentHandlers = outputLogger.useParentHandlers
        val originalOut = System.out
        val originalErr = System.err
        val standardOutput = ByteArrayOutputStream()
        val errorOutput = ByteArrayOutputStream()
        outputLogger.handlers.forEach(outputLogger::removeHandler)

        try {
            System.setOut(PrintStream(standardOutput))
            System.setErr(PrintStream(errorOutput))
            QuietConsoleOutput.configureLogging(Level.FINE)

            outputLogger.fine("details")
            outputLogger.info("progress")
            outputLogger.warning("caution")
            outputLogger.severe("failure")

            assertEquals("details\nprogress\n", standardOutput.toString())
            assertEquals("caution\nfailure\n", errorOutput.toString())
        } finally {
            outputLogger.handlers.forEach(outputLogger::removeHandler)
            previousHandlers.forEach(outputLogger::addHandler)
            outputLogger.level = previousLevel
            outputLogger.useParentHandlers = previousUseParentHandlers
            System.setOut(originalOut)
            System.setErr(originalErr)
        }
    }
}
