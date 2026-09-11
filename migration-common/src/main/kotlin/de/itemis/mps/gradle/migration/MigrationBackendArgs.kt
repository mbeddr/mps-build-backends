package de.itemis.mps.gradle.migration

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.logging.detectLogging
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import de.itemis.mps.gradle.project.loader.Plugin
import de.itemis.mps.gradle.project.loader.ProjectLoader

private val logger = detectLogging().getLogger("de.itemis.mps.gradle.migration")

/**
 * Command line arguments and environment setup shared by the `remigrate` and `migrate` backends: both operate on
 * one or several projects, and both need the `jetbrains.mps.ide.mpsmigration` plugin (which hosts the migration
 * runtime/registry used by [jetbrains.mps.project.Project]) added to the MPS environment, plus a warning if their
 * own backend plugin isn't loaded.
 */
abstract class MigrationBackendArgs(parser: ArgParser) : EnvironmentArgs(parser) {
    val projects by parser.adding("--project", help = "project to migrate.")

    /** id of this backend's own IDEA plugin, e.g. "de.itemis.mps.buildbackends.migrate" */
    protected abstract val backendPluginId: String

    override fun configureProjectLoader(builder: ProjectLoader.Builder) {
        builder.environmentConfig {
            plugins.add(Plugin("jetbrains.mps.ide.mpsmigration", "mps-migration"))
        }
        super.configureProjectLoader(builder)

        if (!builder.environmentConfigBuilder.plugins.any { it.id == backendPluginId }) {
            logger.warn(
                "Plugin $backendPluginId is missing, the process will likely fail. " +
                        "Specify the plugin location using --plugin=$backendPluginId::<backend jar path>"
            )
        }
    }
}
