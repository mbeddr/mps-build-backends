package de.itemis.mps.gradle.execute

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.Args

class ExecuteArgs(parser: ArgParser) : Args(parser) {
    val module by parser.storing("--module", help = "name of the module that contains the class")
    val `class` by parser.storing("--class", help = "fully qualified name of the class that contains the method")
    val method by parser.storing(
        "--method",
        help = "name of the method to execute. Must be public and static." +
                "Supported method signatures in order of precedence: (Project, String[]), (Project)"
    )

    val methodArguments by parser.adding(
        "--arg",
        help = "list of strings to pass to the method. Allowed only if the method signature is (Project, String[])"
    )
}
