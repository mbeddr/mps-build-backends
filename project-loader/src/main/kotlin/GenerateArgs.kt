import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.Args

class GenerateArgs(parser: ArgParser) : Args(parser) {
    val models by parser.adding("--model",
        help = "list of models to check (regexes)")
    val modules by parser.adding("--module",
        help = "list of modules to check (regexes)")
    val excludeModels by parser.adding("--exclude-model", help = "list of models to exclude from check (regexes)")
    val excludeModules by parser.adding("--exclude-module", help = "list of modules to exclude from check (regexes)")
}