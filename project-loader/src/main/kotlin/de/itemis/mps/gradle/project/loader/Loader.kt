package de.itemis.mps.gradle.project.loader

import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.EnvironmentConfig
import java.io.File

/**
 * Executes [action] in the context of an initialized MPS/IDEA environment. Shuts down the environment after the action
 * finishes, even if it throws an exception.
 */
@Deprecated("Use ProjectLoader#execute(action)")
public fun <T> executeWithEnvironment(
    environmentKind: EnvironmentKind,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    action: (Environment) -> T
): T =
    ProjectLoader.build {
        environmentConfig {
            this.plugins.addAll(plugins)
            this.pluginLocation = pluginLocation
            this.macros.addAll(macros)
            this.testMode = testMode
        }
        this.environmentKind = environmentKind
        this.buildNumber = buildNumber
    }.execute(action)

/**
 * Executes [action] in the context of an initialized MPS/IDEA environment. Shuts down the environment after the action
 * finishes, even if it throws an exception.
 */
@Deprecated("Use ProjectLoader#execute(action)")
public fun <T> executeWithEnvironment(
    cfg: EnvironmentConfig,
    environmentKind: EnvironmentKind,
    pluginLocation: File?,
    buildNumber: String?,
    action: (Environment) -> T
): T =
    ProjectLoader.build {
        environmentConfig {
            initialConfig = cfg
            this.pluginLocation = pluginLocation
        }
        this.environmentKind = environmentKind
        this.buildNumber = buildNumber
    }.execute(action)

@Deprecated("Use EnvironmentConfigBuilder class")
public fun buildEnvironmentConfig(
    plugins: List<Plugin> = listOf(),
    pluginLocation: File? = null,
    macros: List<Macro> = listOf(),
    testMode: Boolean = false
): EnvironmentConfig =
    EnvironmentConfigBuilder().let {
        it.plugins.addAll(plugins)
        it.pluginLocation = pluginLocation
        it.macros.addAll(macros)
        it.testMode = testMode
        it.build()
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
@Deprecated("Use ProjectLoader#executeWithProject(action)")
public fun <T> executeWithEnvironmentAndProject(
    environmentKind: EnvironmentKind,
    projectDir: File,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    action: (Environment, Project) -> T
): T {
    val loader = ProjectLoader.build {
        environmentConfig {
            this.plugins.addAll(elements = plugins)
            this.pluginLocation = pluginLocation
            this.macros.addAll(elements = macros)
            this.testMode = testMode
        }
        this.environmentKind = environmentKind
        this.buildNumber = buildNumber
    }
    return loader.executeWithProject(projectDir, action)
}

@Deprecated("Use ProjectLoader#executeWithProject(action)")
public fun <T> executeWithEnvironmentAndProject(
    config: EnvironmentConfig,
    environmentKind: EnvironmentKind,
    projectDir: File,
    pluginLocation: File? = null,
    buildNumber: String? = null,
    action: (Environment, Project) -> T
): T {
    val loader = ProjectLoader.build {
        environmentConfig {
            this.initialConfig = config
            this.pluginLocation = pluginLocation
        }
        this.environmentKind = environmentKind
        this.buildNumber = buildNumber
    }
    return loader.executeWithProject(projectDir, action)
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
@Deprecated("Use ProjectLoader#executeWithProject(action)")
public fun <T> executeWithProject(
    project: File,
    plugins: List<Plugin> = emptyList(),
    macros: List<Macro> = emptyList(),
    pluginLocation: File? = null,
    buildNumber: String? = null,
    testMode: Boolean = false,
    environmentKind: EnvironmentKind = EnvironmentKind.IDEA,
    action: (Project) -> T
): T {
    val loader = ProjectLoader.build {
        environmentConfig {
            this.pluginLocation = pluginLocation
            this.plugins.addAll(plugins)
            this.macros.addAll(macros)
            this.testMode = testMode
        }
        this.environmentKind = environmentKind
        this.buildNumber = buildNumber
    }
    return loader.executeWithProject(project) { _, p -> action(p) }
}


/**
 *  Convenient function to invoke [executeWithProject] with arguments parsed form the command line.
 *
 *  @see [executeWithProject] for more details.
 *
 *  @param parsed parsed arguemnts.
 *  @param action the action to execute with the project.
 *
 */
@Deprecated("Use ProjectLoader#executeWithProject(action)")
public fun <T> executeWithProject(parsed: Args, action: (Project) -> T): T =
    parsed.buildLoader().executeWithProject(parsed.project) { _, p -> action(p) }

/**
 *  Convenient function to invoke [executeWithEnvironmentAndProject] with arguments parsed form the command line.
 *
 *  @see [executeWithEnvironmentAndProject] for more details.
 *
 *  @param parsed parsed arguemnts.
 *  @param action the action to execute with the project.
 *
 */
@Deprecated("Use ProjectLoader#executeWithProject(action)")
public fun <T> executeWithEnvironmentAndProject(parsed: Args, action: (Environment, Project) -> T): T =
    parsed.buildLoader().executeWithProject(parsed.project, action)
