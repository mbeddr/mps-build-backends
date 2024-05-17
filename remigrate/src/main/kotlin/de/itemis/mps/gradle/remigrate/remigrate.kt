package de.itemis.mps.gradle.remigrate

import com.intellij.ide.plugins.PluginManager
import com.intellij.openapi.extensions.PluginId
import de.itemis.mps.gradle.logging.detectLogging
import jetbrains.mps.project.Project
import java.io.File

val logging = detectLogging()
val logger = logging.getLogger("de.itemis.mps.gradle.migrate")

fun remigrate(args: RemigrateArgs) {
    val loader = args.buildLoader()
    val moduleMigrationsToExclude = args.excludeModuleMigrations.toSet()
    val projectMigrationsToExclude = args.excludeProjectMigrations.toSet()

    loader.execute { environment ->
        val pluginId = PluginId.getId(PLUGIN_ID)
        val pluginDescriptor = PluginManager.getInstance()
            .findEnabledPlugin(pluginId)

        if (pluginDescriptor == null) {
            throw Exception("Plugin $pluginId not loaded or not enabled, cannot proceed")
        }

        for (projectDir in args.projects) {
            val project = environment.openProject(File(projectDir))
            try {
                val helperClass =
                    pluginDescriptor.pluginClassLoader!!.loadClass(WorkFromIdeaPlugin.javaClass.name)

                val method = helperClass.getMethod("work",
                    Project::class.java,
                    Set::class.java,
                    Set::class.java)

                method.invoke(null, project, projectMigrationsToExclude, moduleMigrationsToExclude)
            } finally {
                environment.closeProject(project)
            }
            environment.flushAllEvents()
        }
    }
}

internal const val PLUGIN_ID = "de.itemis.mps.buildbackends.remigrate"
