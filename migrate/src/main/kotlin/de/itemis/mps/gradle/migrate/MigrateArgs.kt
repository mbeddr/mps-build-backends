package de.itemis.mps.gradle.migrate

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.migration.MigrationBackendArgs

class MigrateArgs(parser: ArgParser) : MigrationBackendArgs(parser) {
    override val backendPluginId = PLUGIN_ID

    val haltOnPrecheckFailure by parser.flagging(
        "--halt-on-precheck-failure",
        help = "halt migration if the pre-migration consistency check (e.g. unresolved references) reports " +
                "problems. By default such problems are logged and migration proceeds anyway."
    )

    val haltOnDependencyError by parser.flagging(
        "--halt-on-dependency-error",
        help = "halt migration if a dependency (e.g. a library module) hasn't itself been migrated yet. By " +
                "default such problems are logged and migration proceeds anyway."
    )

    val continueOnError by parser.flagging(
        "--continue-on-error",
        help = "when migrating multiple --project entries, continue with the remaining ones even if migration " +
                "fails for one of them. By default the first failure stops the whole run. The process still exits " +
                "with a non-zero code if any project failed."
    )
}
