package de.itemis.mps.gradle.modelcheck

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.Plugin
import de.itemis.mps.gradle.project.loader.ProjectLoader

@Suppress("DEPRECATION")
class ModelCheckArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
        help = "list of models to check (regexes)")
    val modules by parser.adding("--module",
        help = "list of modules to check (regexes)")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from check (regexes)")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from check (regexes)")
    val parallel by parser.flagging("--parallel", help = "run model checker in parallel")
    val warningAsError by parser.flagging("--warning-as-error", help = "treat model checker warning as errors")
    val dontFailOnError by parser.flagging("--error-no-fail", help = "report errors but don't fail the build")
    val xmlFile by parser.storing("--result-file", help = "stores the result as an JUnit xml file").default<String?>(null)
    val xmlReportFormat by parser.storing("--result-format", help = "reporting format for the JUnit file") {
        when (this) {
            "model" -> ReportFormat.ONE_TEST_PER_MODEL
            "module-and-model" -> ReportFormat.ONE_TEST_PER_MODULE_AND_MODEL
            "message" -> ReportFormat.ONE_TEST_PER_FAILED_MESSAGE
            else -> throw IllegalArgumentException("unsupported result format: $this")
        }
    }.default(ReportFormat.ONE_TEST_PER_MODEL)

    val forceIndexing by parser.storing(
        "--force-indexing", help = "whether to force full indexing at startup to work around MPS-37926." +
                " Supported values: always, never, auto. Default: auto.") {
        when (this) {
            "always" -> true
            "never" -> false
            "auto" -> null
            else -> throw IllegalArgumentException("Unsupported value '$this'. Supported values are always, never, auto")
        }
    }.default(null)

    override fun configureProjectLoader(builder: ProjectLoader.Builder) {
        super.configureProjectLoader(builder)
        builder.environmentConfig {
            plugins.add(Plugin("jetbrains.mps.ide.modelchecker", "mps-modelchecker"))
        }
    }
}
