package de.itemis.mps.gradle.logging

import de.itemis.mps.gradle.project.loader.EnvironmentKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Test
import java.util.logging.ConsoleHandler
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
        val consoleHandler = ConsoleHandler()
        rootLogger.addHandler(consoleHandler)

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
        } finally {
            System.setOut(originalOut)
            System.setErr(originalErr)
            propertyNames.forEach { name -> restoreProperty(name, originalProperties[name]) }
            rootLogger.handlers.forEach(rootLogger::removeHandler)
            originalHandlers.forEach(rootLogger::addHandler)
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
