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
    val projectDirs = args.projects.map(::File)
    val moduleMigrationsToExclude = args.excludeModuleMigrations.toSet()
    val projectMigrationsToExclude = args.excludeProjectMigrations.toSet()

    loader.executeForEachProject(projectDirs) { environment, project ->
        val pluginId = PluginId.getId(PLUGIN_ID)
        val pluginDescriptor = PluginManager.getInstance().findEnabledPlugin(pluginId)
            ?: throw Exception("Plugin $pluginId not loaded or not enabled, cannot proceed")

        val helperClass = pluginDescriptor.pluginClassLoader!!.loadClass(WorkFromIdeaPlugin.javaClass.name)
        val method = helperClass.getMethod("work", Project::class.java, Set::class.java, Set::class.java)

        method.invoke(null, project, projectMigrationsToExclude, moduleMigrationsToExclude)

        environment.flushAllEvents()
    }
}

internal const val PLUGIN_ID = "de.itemis.mps.buildbackends.remigrate"
