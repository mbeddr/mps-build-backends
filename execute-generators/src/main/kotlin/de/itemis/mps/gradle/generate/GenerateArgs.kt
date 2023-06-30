package de.itemis.mps.gradle.generate

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.project.loader.Args

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model", help = "list of models to generate")
    val modules by parser.adding("--module", help = "list of modules to generate")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from generation")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from generation")
    val parallelGenerationThreads by parser.storing(
        "--parallel-generation-threads",
        help = "Number of threads to use for parallel generation. Value of 0 means that parallel generation is disabled. Default: 0"
    ) { toInt() }
        .default(0)
        .addValidator { if (value < 0) throw InvalidArgumentException("parallel-generation-threads must be >= 0") }
}
