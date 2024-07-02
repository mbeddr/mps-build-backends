package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.project.loader.Args
import de.itemis.mps.gradle.project.loader.checkArgument

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model", help = "list of models to generate")
    val modules by parser.adding("--module", help = "list of modules to generate")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from generation")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from generation")
    val skipReconciliation by parser.flagging("--skip-reconciliation", help = "skips the Make.reconcile target")
    val skipCompilation by parser.flagging("--skip-compilation", help = "skips the JavaCompile.compile target")
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
}
