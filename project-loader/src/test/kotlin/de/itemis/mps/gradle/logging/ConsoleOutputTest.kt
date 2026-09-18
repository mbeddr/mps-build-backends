package de.itemis.mps.gradle.logging

import de.itemis.mps.gradle.project.loader.EnvironmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.logging.ConsoleHandler
import java.util.logging.Filter
import java.util.logging.Level
import java.util.logging.Logger

class ConsoleOutputTest {
    @Test
    fun `quiet output suppresses startup output and restores global state`() {
        val originalOut = System.out
        val originalErr = System.err
        val propertyNames = listOf(
            "idea.log.console",
            "intellij.log.stdout",
            "intellij.console.log.level"
        )
        val originalProperties = propertyNames.associateWith(System::getProperty)
        val rootLogger = Logger.getLogger("")
        val originalHandlers = rootLogger.handlers.toList()
        val originalLevel = rootLogger.level
        val originalFilter = rootLogger.filter
        val originalUseParentHandlers = rootLogger.useParentHandlers
        val consoleHandler = ConsoleHandler()
        val filter = Filter { true }
        val replacementHandler = ConsoleHandler()
        val outputLogger = Logger.getLogger("de.itemis.mps.gradle.output")
        val originalOutputHandlers = outputLogger.handlers.toList()
        val originalOutputLevel = outputLogger.level
        val originalOutputFilter = outputLogger.filter
        val originalOutputUseParentHandlers = outputLogger.useParentHandlers
        val outputHandler = ConsoleHandler()
        val outputFilter = Filter { true }
        rootLogger.addHandler(consoleHandler)
        rootLogger.level = Level.FINE
        rootLogger.filter = filter
        rootLogger.useParentHandlers = false
        outputLogger.handlers.forEach(outputLogger::removeHandler)
        outputLogger.addHandler(outputHandler)
        outputLogger.level = Level.SEVERE
        outputLogger.filter = outputFilter
        outputLogger.useParentHandlers = false
        outputHandler.level = Level.SEVERE

        try {
            System.setProperty("idea.log.console", "true")
            System.setProperty("intellij.log.stdout", "true")
            System.setProperty("intellij.console.log.level", "WARNING")

            consoleOutput(
                verbose = false,
                environmentKind = EnvironmentKind.MPS,
                logLevel = Level.WARNING
            ).use { output ->
                assertEquals("false", System.getProperty("idea.log.console"))
                assertEquals("false", System.getProperty("intellij.log.stdout"))
                assertEquals("OFF", System.getProperty("intellij.console.log.level"))
                assertFalse(rootLogger.handlers.contains(consoleHandler))

                assertNotSame(originalOut, System.out)
                assertNotSame(originalErr, System.err)
                output.environmentCreated()
                rootLogger.addHandler(replacementHandler)

                assertNotSame(originalOut, System.out)
                assertNotSame(originalErr, System.err)

                output.showBackendOutput {
                    assertSame(originalOut, System.out)
                    assertSame(originalErr, System.err)
                }

                assertNotSame(originalOut, System.out)
                assertNotSame(originalErr, System.err)
            }

            assertSame(originalOut, System.out)
            assertSame(originalErr, System.err)
            assertEquals("true", System.getProperty("idea.log.console"))
            assertEquals("true", System.getProperty("intellij.log.stdout"))
            assertEquals("WARNING", System.getProperty("intellij.console.log.level"))
            assertEquals(originalHandlers + consoleHandler, rootLogger.handlers.toList())
            assertSame(Level.FINE, rootLogger.level)
            assertSame(filter, rootLogger.filter)
            assertFalse(rootLogger.useParentHandlers)
            assertEquals(listOf(outputHandler), outputLogger.handlers.toList())
            assertSame(Level.SEVERE, outputLogger.level)
            assertSame(outputFilter, outputLogger.filter)
            assertFalse(outputLogger.useParentHandlers)
            assertSame(Level.SEVERE, outputHandler.level)
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            propertyNames.forEach { name -> restoreProperty(name, originalProperties[name]) }
            rootLogger.handlers.forEach(rootLogger::removeHandler)
            originalHandlers.forEach(rootLogger::addHandler)
            rootLogger.level = originalLevel
            rootLogger.filter = originalFilter
            rootLogger.useParentHandlers = originalUseParentHandlers
            outputLogger.handlers.forEach(outputLogger::removeHandler)
            originalOutputHandlers.forEach(outputLogger::addHandler)
            outputLogger.level = originalOutputLevel
            outputLogger.filter = originalOutputFilter
            outputLogger.useParentHandlers = originalOutputUseParentHandlers
        }
    }

    private fun restoreProperty(name: String, value: String?) {
        if (value == null) {
            System.clearProperty(name)
        } else {
            System.setProperty(name, value)
        }
    }
}
