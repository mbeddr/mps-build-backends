package de.itemis.mps.gradle.project.loader

import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.util.PathManager
import java.io.File

public class EnvironmentConfigBuilder {
    public var initialConfig: EnvironmentConfig = basicEnvironmentConfig()
    public val plugins: MutableList<Plugin> = mutableListOf()
    public var pluginLocation: File? = null
    public var macros: MutableList<Macro> = mutableListOf()
    public var testMode: Boolean = false

    public fun build(): EnvironmentConfig {
        val cfg = initialConfig

        plugins.forEach {
            if (File(it.path).isAbsolute) {
                cfg.addPlugin(it.path, it.id)
            }
            /**
             *  MPS implementation accepts only absolute paths as plugin path:
             *  https://github.com/JetBrains/MPS/blob/2019.3/core/tool/environment/source_gen/jetbrains/mps/tool/environment/EnvironmentConfig.java#L68
             *  therefore we additionally check if the specified path is a subfolder of the optional
             *  pluginLocation
             */
            else if (pluginLocation != null && File(pluginLocation, it.path).exists()) {
                cfg.addPlugin(File(pluginLocation, it.path).absolutePath, it.id)
            } else {
                cfg.addPreInstalledPlugin(it.path, it.id)
            }
        }
        macros.forEach { cfg.addMacro(it.name, File(it.value)) }

        if (testMode) cfg.withTestModeOn()
        return cfg
    }

    private fun basicEnvironmentConfig(): EnvironmentConfig =
    // This is a somewhat "safe" set of default plugins. It should work with most of the projects we have encountered
    // mbeddr projects won't build with this set of plugins for unknown reasons, most probably the runtime
        // dependencies in the mbeddr plugins are so messed up that they simply broken beyond repair.
        EnvironmentConfig.emptyConfig()
            .withDefaultPlugins()
            .withBuildPlugin()
            .withBootstrapLibraries()
            .withWorkbenchPath()
            .withVcsPlugin()
            .withJavaPlugin()
            .addPreInstalledPlugin("mps-httpsupport", "jetbrains.mps.ide.httpsupport")

    /**
     * Since 2019.2 absolute plugin path has to be provided in addPlugin(), but suitable replacement addDistributedPlugin()
     * is private, therefore this extension is used as temporary convenient replacement
     */
    private fun EnvironmentConfig.addPreInstalledPlugin(folder: String, id: String): EnvironmentConfig {
        this.addPlugin(PathManager.getPreInstalledPluginsPath() + "/" + folder, id)
        return this
    }
}