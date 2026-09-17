package de.itemis.mps.gradle.migrate

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import de.itemis.mps.gradle.migration.getProjectName
import jetbrains.mps.project.Project
import java.io.File
import java.util.logging.Level
import java.util.logging.Logger

val logger = Logger.getLogger("de.itemis.mps.gradle.migrate")

fun migrate(args: MigrateArgs) {
    val loader = args.buildLoader()
    val projectDirs = args.projects.map(::File)

    var anyFailures = false

    loader.executeForEachProject(projectDirs) { environment, project ->
        try {
            val pluginId = PluginId.getId(PLUGIN_ID)
            val pluginDescriptor = PluginManager.getInstance().findEnabledPlugin(pluginId)
                ?: throw Exception("Plugin $pluginId not loaded or not enabled, cannot proceed")

            val helperClass = pluginDescriptor.pluginClassLoader!!.loadClass(WorkFromIdeaPlugin.javaClass.name)
            val method = helperClass.getMethod(
                "work",
                Project::class.java,
                Boolean::class.javaPrimitiveType!!,
                Boolean::class.javaPrimitiveType!!
            )

            val success = method.invoke(null, project, args.haltOnPrecheckFailure, args.haltOnDependencyError) as Boolean
            if (!success) {
                throw Exception("Migration failed, see the log above for details")
            }
        } catch (e: Exception) {
            if (!args.continueOnError) {
                throw e
            }

            anyFailures = true
            logger.log(
                Level.SEVERE,
                "Migration failed for project ${getProjectName(project)}, continuing with remaining projects",
                e
            )
        }

        environment.flushAllEvents()
    }

    if (anyFailures) {
        throw Exception("Migration failed for one or more projects, see the log above for details")
    }
}

internal const val PLUGIN_ID = "de.itemis.mps.buildbackends.migrate"
