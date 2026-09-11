package de.itemis.mps.gradle.remigrate

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.migration.MigrationBackendArgs

class RemigrateArgs(parser: ArgParser) : MigrationBackendArgs(parser) {
    override val backendPluginId = PLUGIN_ID

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
}
