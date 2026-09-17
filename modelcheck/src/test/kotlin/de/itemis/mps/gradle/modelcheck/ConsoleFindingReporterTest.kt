package de.itemis.mps.gradle.modelcheck

import jetbrains.mps.errors.MessageStatus
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class ConsoleFindingReporterTest {
    @Test
    fun `quiet mode suppresses successful findings`() {
        val output = RecordingOutput()
        val reporter = ConsoleFindingReporter(verbose = false, warningAsError = false, output = output)

        reporter.report(MessageStatus.OK, "valid model")
        reporter.finish()

        assertEquals(emptyList<String>(), output.info)
        assertEquals(emptyList<String>(), output.warnings)
        assertEquals(emptyList<String>(), output.errors)
    }

    @Test
    fun `verbose mode reports successful findings`() {
        val output = RecordingOutput()
        val reporter = ConsoleFindingReporter(verbose = true, warningAsError = false, output = output)

        reporter.report(MessageStatus.OK, "valid model")
        reporter.finish()

        assertEquals(listOf("valid model"), output.info)
    }

    @Test
    fun `reports at most fifty findings with errors taking priority over warnings`() {
        val output = RecordingOutput()
        val reporter = ConsoleFindingReporter(verbose = false, warningAsError = false, output = output)

        repeat(40) { reporter.report(MessageStatus.WARNING, "warning $it") }
        repeat(30) { reporter.report(MessageStatus.ERROR, "error $it") }
        reporter.finish()

        assertEquals((0 until 30).map { "error $it" }, output.errors)
        assertEquals((0 until 20).map { "warning $it" }, output.warnings.dropLast(1))
        assertEquals("20 additional model-check warnings were omitted", output.warnings.last())
    }

    @Test
    fun `counts omitted errors and warnings in the truncation warning`() {
        val output = RecordingOutput()
        val reporter = ConsoleFindingReporter(verbose = false, warningAsError = false, output = output)

        repeat(52) { reporter.report(MessageStatus.ERROR, "error $it") }
        repeat(3) { reporter.report(MessageStatus.WARNING, "warning $it") }
        reporter.finish()

        assertEquals(50, output.errors.size)
        assertEquals(listOf("2 additional model-check errors and 3 warnings were omitted"), output.warnings)
    }

    @Test
    fun `warning as error changes the output channel without changing priority`() {
        val output = RecordingOutput()
        val reporter = ConsoleFindingReporter(verbose = false, warningAsError = true, output = output)

        repeat(49) { reporter.report(MessageStatus.ERROR, "error $it") }
        repeat(2) { reporter.report(MessageStatus.WARNING, "warning $it") }
        reporter.finish()

        assertEquals((0 until 49).map { "error $it" } + "warning 0", output.errors)
        assertEquals(listOf("1 additional model-check warning was omitted"), output.warnings)
    }

    private class RecordingOutput : FindingOutput {
        val info = mutableListOf<String>()
        val warnings = mutableListOf<String>()
        val errors = mutableListOf<String>()

        override fun report(severity: MessageStatus, message: String) {
            when (severity) {
                MessageStatus.OK -> info += message
                MessageStatus.WARNING -> warnings += message
                MessageStatus.ERROR -> errors += message
            }
        }
    }
}
