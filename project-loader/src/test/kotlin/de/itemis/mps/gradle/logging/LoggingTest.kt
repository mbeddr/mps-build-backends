package de.itemis.mps.gradle.logging

import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.util.logging.Handler
import java.util.logging.Level
import java.util.logging.Logger

class LoggingTest {
    private val logger = Logger.getLogger("de.itemis.mps")
    private lateinit var previousHandlers: Array<Handler>
    private var previousLevel: Level? = null
    private var previousUseParentHandlers: Boolean = true

    @Before
    fun saveLoggerConfiguration() {
        previousHandlers = logger.handlers
        previousLevel = logger.level
        previousUseParentHandlers = logger.useParentHandlers
        previousHandlers.forEach(logger::removeHandler)
    }

    @After
    fun restoreLoggerConfiguration() {
        logger.handlers.forEach(logger::removeHandler)
        previousHandlers.forEach(logger::addHandler)
        logger.level = previousLevel
        logger.useParentHandlers = previousUseParentHandlers
    }

    @Test
    fun `reconfigures existing handler without adding another one`() {
        configureLogging(Level.WARNING)
        configureLogging(Level.INFO)

        assertEquals(Level.INFO, logger.level)
        assertEquals(1, logger.handlers.size)
        assertEquals(Level.INFO, logger.handlers.single().level)
    }
}
