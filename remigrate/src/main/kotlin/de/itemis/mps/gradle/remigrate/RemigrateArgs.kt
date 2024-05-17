package de.itemis.mps.gradle.remigrate

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import de.itemis.mps.gradle.project.loader.Plugin
import de.itemis.mps.gradle.project.loader.ProjectLoader

class RemigrateArgs(parser: ArgParser) : EnvironmentArgs(parser) {
    val projects by parser.adding("--project", help = "project to migrate.")
    val excludeModuleMigrations by parser.adding(
        "--exclude-module-migration",
        help = "module migration to exclude from execution or check. Format is language:version"
    ) {
        val (ns, ver) = this.split(":", limit = 2)
        Pair(ns, ver.toInt())
    }

    val excludeProjectMigrations by parser.adding(
        "--exclude-project-migration",
        help = "ID of project migration to exclude from execution."
    )

    override fun configureProjectLoader(builder: ProjectLoader.Builder) {
        builder.environmentConfig {
            plugins.add(Plugin("jetbrains.mps.ide.mpsmigration", "mps-migration"))
        }
        super.configureProjectLoader(builder)

        if (!builder.environmentConfigBuilder.plugins.any { it.id == PLUGIN_ID }) {
            logger.warn(
                "Plugin $PLUGIN_ID is missing, the process will likely fail. " +
                        "Specify the plugin location using --plugin=$PLUGIN_ID::<backend jar path>"
            )
        }
    }
}
