package de.itemis.mps.gradle.project.loader

import com.xenomachina.argparser.ArgParser
import com.xenomachina.argparser.InvalidArgumentException
import com.xenomachina.argparser.default
import de.itemis.mps.gradle.logging.LogLevel
import java.io.File

private fun <T> splitAndCreate(str: String, creator: (String, String) -> T): T {
    val split = str.split("::", limit = 2)
    checkArgument(split.size == 2) { "String '$str' does not have format <key>::<value>." }
    return creator(split[0], split[1])
}

private fun toMacro(str: String) = splitAndCreate(str, ::Macro)
private fun toPlugin(str: String) = splitAndCreate(str, ::Plugin)

/**
 * Default set of arguments required to start a "headless" MPS. This class should be used by other users of the
 * project-loader in order to establish a somewhat standardised command line interface.
 */
public open class Args(parser: ArgParser) {

    public val plugins: MutableList<Plugin> by parser.adding("--plugin",
            help = "plugin to load. The format is --plugin=<id>::<path>")
    { toPlugin(this) }

    public val macros: MutableList<Macro> by parser.adding("--macro",
            help = "macro to define. The format is --macro=<name>::<value>")
    { toMacro(this) }

    public val pluginLocation: File? by parser.storing("--plugin-location",
            help = "location to load additional plugins from") { File(this) }.default<File?>(null)

    public val buildNumber: String? by parser.storing("--build-number",
            help = "build number used to determine if the plugins are compatible").default<String?>(null)

    public val project: File by parser.storing("--project",
            help = "project to generate from") { File(this) }

    public val testMode: Boolean by parser.flagging("--test-mode", help = "run in test mode")

    public val environmentKind: EnvironmentKind by parser.storing("--environment",
            help = "kind of environment to initialize, supported values are 'idea' (default), 'mps'") {
        EnvironmentKind.valueOf(uppercase())
    }.default(EnvironmentKind.IDEA)

    public val logLevel: LogLevel by parser.storing("--log-level",
        help = "console log level. Supported values: info, warn, error, off. Default: warn.") {
        LogLevel.valueOf(uppercase())
    }.default(LogLevel.WARN)

    public open fun configureProjectLoader(builder: ProjectLoader.Builder) {
        builder.environmentConfig {
            plugins.addAll(this@Args.plugins)
            pluginLocation = this@Args.pluginLocation
            macros.addAll(this@Args.macros)
            testMode = this@Args.testMode
        }
        builder.environmentKind = environmentKind
        builder.buildNumber = buildNumber
        builder.logLevel = de.itemis.mps.gradle.logging.LogLevel.valueOf(logLevel.toString())
    }

    public fun buildLoader(): ProjectLoader = ProjectLoader.build(this::configureProjectLoader)
}

public inline fun checkArgument(isOk: Boolean, message: () -> String) {
    if (!isOk) throw InvalidArgumentException(message())
}
