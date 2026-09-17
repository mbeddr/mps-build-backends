package de.itemis.mps.gradle.modelcheck

import jetbrains.mps.errors.MessageStatus

internal fun interface FindingOutput {
    fun report(severity: MessageStatus, message: String)
}

internal class ConsoleFindingReporter(
    private val verbose: Boolean,
    private val warningAsError: Boolean,
    private val output: FindingOutput,
) {
    private val warnings = ArrayList<String>(MAX_FINDINGS)
    private var errorCount = 0
    private var warningCount = 0

    fun report(severity: MessageStatus, message: String) {
        when (severity) {
            MessageStatus.OK -> if (verbose) output.report(MessageStatus.OK, message)
            MessageStatus.WARNING -> {
                warningCount++
                if (warnings.size < MAX_FINDINGS) warnings += message
            }
            MessageStatus.ERROR -> {
                errorCount++
                if (errorCount <= MAX_FINDINGS) output.report(MessageStatus.ERROR, message)
            }
        }
    }

    fun finish() {
        val warningsToReport = (MAX_FINDINGS - errorCount).coerceAtLeast(0)
        warnings.take(warningsToReport).forEach {
            val severity = if (warningAsError) MessageStatus.ERROR else MessageStatus.WARNING
            output.report(severity, it)
        }

        val omittedErrors = (errorCount - MAX_FINDINGS).coerceAtLeast(0)
        val omittedWarnings = (warningCount - warningsToReport).coerceAtLeast(0)
        if (omittedErrors + omittedWarnings > 0) {
            output.report(MessageStatus.WARNING, omissionMessage(omittedErrors, omittedWarnings))
        }
    }

    private fun omissionMessage(errors: Int, warnings: Int): String {
        val parts = buildList {
            if (errors > 0) add("$errors additional model-check ${if (errors == 1) "error" else "errors"}")
            if (warnings > 0) add("$warnings ${if (errors == 0) "additional model-check " else ""}${if (warnings == 1) "warning" else "warnings"}")
        }
        return parts.joinToString(" and ") + if (errors + warnings == 1) " was omitted" else " were omitted"
    }

    private companion object {
        const val MAX_FINDINGS = 50
    }
}
