package de.itemis.mps.gradle.project.loader

import com.xenomachina.argparser.ArgParser

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
        help = "list of models to generate")
    val modules by parser.adding("--module",
        help = "list of modules to generate")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from generation")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from generation")
}