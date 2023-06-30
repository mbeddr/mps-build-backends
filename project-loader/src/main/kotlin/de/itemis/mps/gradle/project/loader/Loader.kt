package de.itemis.mps.gradle.project.loader

import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.tool.environment.MpsEnvironment
import jetbrains.mps.util.PathManager
import org.apache.log4j.Logger
import java.io.File

public data class Plugin(
        val id: String,
        val path: String
)

public data class Macro(
        val name: String,
        val value: String
)

private val logger = Logger.getLogger("de.itemis.mps.gradle.project.loader")

/**
 *  The Idea platform reads this property first to determine where additional plugins are loaded from.
 */
private const val PROPERTY_PLUGINS_PATH = "idea.plugins.path"

/**
 * At least starting from MPS 2018.2 this property is read by the platform to check against this value if a plugin
 * is compatible with the application. It seems to override all other means of checks, e.g. build number in
 * ApplicationInfo.
 */
private const val PROPERTY_PLUGINS_COMPATIBLE_BUILD = "idea.plugins.compatible.build"

/**
 * Since 2019.2 absolute plugin path has to be provided in addPlugin(), but suitable replacement addDistributedPlugin()
 * is private, therefore this extension is used as temporary convenient replacement
 */
private fun EnvironmentConfig.addPreInstalledPlugin(folder: String, id: String): EnvironmentConfig {
    this.addPlugin(PathManager.getPreInstalledPluginsPath() + "/" + folder, id)
    return this
}

private fun basicEnvironmentConfig(): EnvironmentConfig {

    // This is a somewhat "safe" set of default plugins. It should work with most of the projects we have encountered
    // mbeddr projects won't build with this set of plugins for unknown reasons, most probably the runtime
    // dependencies in the mbeddr plugins are so messed up that they simply broken beyond repair.

    val config = EnvironmentConfig
        .emptyConfig()
        .withDefaultPlugins()
        .withBuildPlugin()
        .withBootstrapLibraries()
        .withWorkbenchPath()
        .withVcsPlugin()
        .withJavaPlugin()
        .addPreInstalledPlugin("mps-httpsupport", "jetbrains.mps.ide.httpsupport")
    return config
}

/**
 * Execute [action] in the context of an initialized MPS/IDEA environment. Shuts down the environment after the action
 * finishes, even if it throws an exception.
 */
public fun <T> executeWithEnvironment(
    environmentKind: EnvironmentKind,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    action: (Environment) -> T
): T {

    val propertyOverrides = mutableListOf<Pair<String, String?>>()

    try {
        if (pluginLocation != null) {
            logger.info("overriding plugin location with: ${pluginLocation.absolutePath}")
            propertyOverrides.add(Pair(PROPERTY_PLUGINS_PATH, System.getProperty(PROPERTY_PLUGINS_PATH)))
            System.setProperty(PROPERTY_PLUGINS_PATH, pluginLocation.absolutePath)
        }

        if (buildNumber != null) {
            logger.info("setting build number to \"$buildNumber\"")
            propertyOverrides.add(
                Pair(
                    PROPERTY_PLUGINS_COMPATIBLE_BUILD,
                    System.getProperty(PROPERTY_PLUGINS_COMPATIBLE_BUILD)
                )
            )
            System.setProperty(PROPERTY_PLUGINS_COMPATIBLE_BUILD, buildNumber)
        }

        val cfg = basicEnvironmentConfig()

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

        logger.info("creating $environmentKind environment")

        val environment: Environment = when (environmentKind) {
            EnvironmentKind.IDEA -> IdeaEnvironment(cfg).apply {
                logger.info("initializing IDEA environment")
                init()
            }

            EnvironmentKind.MPS -> MpsEnvironment(cfg).apply {
                logger.info("initializing MPS environment")
                init()
            }
        }

        try {
            logger.info("flushing events")
            environment.flushAllEvents()

            return action(environment)
        } finally {
            logger.info("flushing events before environment disposal")
            environment.flushAllEvents()
            logger.info("disposing environment")
            environment.dispose()
            logger.info("environment disposed")
        }

    } finally {
        // cleanup overridden property values to the state that they were before.
        propertyOverrides.forEach {
            // if a property wasn't set before the value is "null"
            // setting null as a value for a System property will result in a NPE
            if (it.second != null) {
                System.setProperty(it.first, it.second!!)
            } else {
                System.clearProperty(it.first)
            }
        }
    }
}

/**
 * Execute [action] in the context of an initialized MPS/IDEA environment and project located in [projectDir]. Closes
 * the project and shuts down the environment and  after the action finishes, even if it throws an exception.
 *
 * @param projectDir the location of the project to open.
 * @param pluginLocation optional location where plugins lo load are located. This is for additional plugins, the plugins
 * located in the pre-installed pluging location (usual the "plugins" folder of MPS) are still considered.
 * @param plugins optional list of plugins to load. Path is the what MPS calls the `short (folder) name` in it's build
 * language. The id is the plugin id defined in the plugin descriptor. Both are required because MPS supports multiple
 * plugins the same location.
 * @param macros optional list of path macros to define before the project is opened
 * @param buildNumber optional build number that is used to determine if the plugins are compatible. Only guaranteed to
 * work with MPS 2018.2+ but might work in earlier versions as well.
 * @param environmentKind the kind of environment to set up (IDEA or MPS)
 * @param testMode whether to enable IDEA test mode.
 * @param action the action to execute with the project.
 */
public fun <T> executeWithEnvironmentAndProject(
    environmentKind: EnvironmentKind,
    projectDir: File,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    action: (Environment, Project) -> T
): T = executeWithEnvironment(
        environmentKind = environmentKind,
        plugins = plugins,
        macros = macros,
        pluginLocation = pluginLocation,
        buildNumber = buildNumber,
        testMode = testMode
    ) { environment ->
    logger.info("opening project: ${projectDir.absolutePath}")

    val project = environment.openProject(projectDir)

    logger.info("flushing events")
    environment.flushAllEvents()

    try {
        return@executeWithEnvironment action(environment, project)
    } finally {
        logger.info("disposing project")
        project.dispose()
        logger.info("project disposed")
    }
}


/**
 * Execute [action] in the context of an initialized MPS/IDEA environment and project located in [project]. Closes
 * the project and shuts down the environment and  after the action finishes, even if it throws an exception.
 *
 * @param project the location of the project to open.
 * @param pluginLocation optional location where plugins lo load are located. This is for additional plugins, the plugins
 * located in the pre-installed pluging location (usual the "plugins" folder of MPS) are still considered.
 * @param plugins optional list of plugins to load. Path is the what MPS calls the `short (folder) name` in it's build
 * language. The id is the plugin id defined in the plugin descriptor. Both are required because MPS supports multiple
 * plugins the same location.
 * @param macros optional list of path macros to define before the project is opened
 * @param buildNumber optional build number that is used to determine if the plugins are compatible. Only guaranteed to
 * work with MPS 2018.2+ but might work in earlier versions as well.
 * @param environmentKind the kind of environment to set up (IDEA or MPS)
 * @param testMode whether to enable IDEA test mode.
 * @param action the action to execute with the project.
 */
public fun <T> executeWithProject(
    project: File,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    environmentKind: EnvironmentKind = EnvironmentKind.IDEA,
    action: (Project) -> T
): T = executeWithEnvironmentAndProject(
    projectDir = project,
    environmentKind = environmentKind,
    plugins = plugins,
    macros = macros,
    pluginLocation = pluginLocation,
    buildNumber = buildNumber,
    testMode = testMode
) { _, openedProject -> action(openedProject) }


/**
 *  Convenient function to invoke [executeWithProject] with arguments parsed form the command line.
 *
 *  @see [executeWithProject] for more details.
 *
 *  @param parsed parsed arguemnts.
 *  @param action the action to execute with the project.
 *
 */
public fun <T> executeWithProject(parsed: Args, action: (Project) -> T): T = executeWithProject(
    project = parsed.project,
    plugins = parsed.plugins,
    macros = parsed.macros,
    buildNumber = parsed.buildNumber,
    pluginLocation = parsed.pluginLocation,
    testMode = parsed.testMode,
    environmentKind = parsed.environmentKind,
    action = action
)

/**
 *  Convenient function to invoke [executeWithEnvironmentAndProject] with arguments parsed form the command line.
 *
 *  @see [executeWithEnvironmentAndProject] for more details.
 *
 *  @param parsed parsed arguemnts.
 *  @param action the action to execute with the project.
 *
 */
public fun <T> executeWithEnvironmentAndProject(parsed: Args, action: (Environment, Project) -> T): T = executeWithEnvironmentAndProject(
    projectDir = parsed.project,
    plugins = parsed.plugins,
    macros = parsed.macros,
    buildNumber = parsed.buildNumber,
    pluginLocation = parsed.pluginLocation,
    testMode = parsed.testMode,
    environmentKind = parsed.environmentKind,
    action = action
)
