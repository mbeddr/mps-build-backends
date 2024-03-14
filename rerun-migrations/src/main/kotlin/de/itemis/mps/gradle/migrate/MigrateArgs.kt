package de.itemis.mps.gradle.migrate

import com.xenomachina.argparser.ArgParser
import de.itemis.mps.gradle.project.loader.EnvironmentArgs
import de.itemis.mps.gradle.project.loader.Plugin
import de.itemis.mps.gradle.project.loader.ProjectLoader

class MigrateArgs(parser: ArgParser) : EnvironmentArgs(parser) {
    val projects by parser.adding("--project", help = "project to migrate.")
    val excludeModuleMigrations by parser.adding(
        "--exclude-module-migration",
        help = "module migration to exclude from execution or check. Format is language:version"
    ) {
        val (ns, ver) = this.split(":", limit = 2)
        ModuleMigration(ns, ver.toInt())
    }

    val excludeProjectMigrations by parser.adding(
        "--exclude-project-migration",
        help = "ID of project migration to exclude from execution."
    ) {
        ProjectMigration(this)
    }

    override fun configureProjectLoader(builder: ProjectLoader.Builder) {
        builder.environmentConfig {
            plugins.add(Plugin("jetbrains.mps.ide.mpsmigration", "mps-migration"))
        }
        super.configureProjectLoader(builder)
    }
}

data class ProjectMigration(val id: String)
data class ModuleMigration(val languageNamespace: String, val version: Int)
