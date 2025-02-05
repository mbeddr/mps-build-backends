package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.checkArgument

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model", help = "list of models to generate")
    val modules by parser.adding("--module", help = "list of modules to generate")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from generation")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from generation")
    val noStrictMode by parser.flagging(
        "--no-strict-mode",
        help = "Disable strict generation mode. Strict mode places additional limitations on generators, but is required for parallel generation"
    )
    val parallelGenerationThreads by parser.storing(
        "--parallel-generation-threads",
        help = "Number of threads to use for parallel generation. Value of 0 means that parallel generation is disabled. Default: 0",
        argName = "THREADS"
    ) { toInt() }
        .default(0)
        .addValidator { checkArgument(value >= 0) { "parallel-generation-threads value $value must be >= 0" } }
        .addValidator {
            checkArgument(!noStrictMode || value == 0) { "strict mode is required for parallel generation" }
        }
    val forceIndexing by parser.storing(
        "--force-indexing", help = "whether to force full indexing at startup to work around MPS-37926." +
                " Supported values: always, never, auto. Default: auto.") {
        when (this) {
            "always" -> true
            "never" -> false
            "auto" -> null
            else -> throw InvalidArgumentException("Unsupported value '$this'. Supported values are always, never, auto")
        }
    }.default(null)
}
