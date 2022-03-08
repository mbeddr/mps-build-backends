package de.itemis.mps.gradle.modelcheck.de.itemis.mps.gradle.modelcheck

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.modelcheck.ReportFormat
import de.itemis.mps.gradle.project.loader.Args
import kotlin.test.fail

class ModelCheckArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
        help = "list of models to check")
    val modules by parser.adding("--module",
        help = "list of modules to check")
    val warningAsError by parser.flagging("--warning-as-error", help = "treat model checker warning as errors")
    val dontFailOnError by parser.flagging("--error-no-fail", help = "report errors but don't fail the build")
    val xmlFile by parser.storing("--result-file", help = "stores the result as an JUnit xml file").default<String?>(null)
    val xmlReportFormat by parser.storing("--result-format", help = "reporting format for the JUnit file") {
        when (this) {
            "model" -> ReportFormat.ONE_TEST_PER_MODEL
            "message" -> ReportFormat.ONE_TEST_PER_FAILED_MESSAGE
            else -> fail("unsupported result format")
        }
    }.default(ReportFormat.ONE_TEST_PER_MODEL)
}
