package de.itemis.mps.gradle.project.loader

import com.intellij.openapi.application.ApplicationInfo
import com.intellij.openapi.project.impl.P3SupportInstaller
import com.intellij.openapi.util.BuildNumber
import com.intellij.serviceContainer.AlreadyDisposedException
import de.itemis.mps.gradle.logging.LogLevel
import de.itemis.mps.gradle.logging.detectLogging
import jetbrains.mps.project.MPSProject
import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.tool.environment.MpsEnvironment
import java.io.File

/**
 * Executes an action in the context of an MPS or IDEA environment.
 */
public class ProjectLoader private constructor(
    private val environmentConfig: EnvironmentConfig,
    private val environmentKind: EnvironmentKind,
    private val pluginLocation: File?,
    private val buildNumber: String?,
    private val logLevel: LogLevel,
    private val forceIndexing: Boolean?
) {
    private val logger = detectLogging().getLogger("de.itemis.mps.gradle.project.loader")

    public class Builder {
        /**
         * MPS tooling environment configuration builder. Configure plugins, macros, etc. here.
         */
        public val environmentConfigBuilder: EnvironmentConfigBuilder = EnvironmentConfigBuilder()

        /**
         * The kind of environment to set up (IDEA or MPS).
         */
        public var environmentKind: EnvironmentKind = EnvironmentKind.IDEA

        /**
         * Optional build number that is used to determine if the plugins are compatible. Only guaranteed to work with
         * MPS 2018.2+ but might work in earlier versions as well.
         */
        public var buildNumber: String? = null

        public var logLevel: LogLevel = LogLevel.WARN

        /**
         * Whether to wait for indexing to complete after opening the project. Only has an effect in IDEA environments.
         * When null, only wait if running a buggy MPS version (currently 2023.2 and above).
         */
        public var forceIndexing: Boolean? = null

        public fun build(): ProjectLoader = ProjectLoader(
            environmentConfigBuilder.build(),
            environmentKind,
            environmentConfigBuilder.pluginLocation,
            buildNumber,
            logLevel,
            forceIndexing
        )

        /**
         * Convenience method to configure [environmentConfigBuilder].
         */
        public fun environmentConfig(action: EnvironmentConfigBuilder.() -> Unit): Unit =
            environmentConfigBuilder.run(action)
    }

    public companion object {
        public fun build(action: Builder.() -> Unit): ProjectLoader = Builder().apply(action).build()
    }

    /**
     * Executes [action] in the context of an MPS/IDEA environment, initialized according to this instance. Shuts down
     * the environment after the action finishes, even if it throws an exception.
     */
    public fun <T> execute(action: (Environment) -> T): T {
        val logging = detectLogging()

        /**
         *  The Idea platform reads this property first to determine where additional plugins are loaded from.
         */
        val pluginsPathProperty = "idea.plugins.path"

        /**
         * At least starting from MPS 2018.2 this property is read by the platform to check against this value if a plugin
         * is compatible with the application. It seems to override all other means of checks, e.g. build number in
         * ApplicationInfo.
         */
        val pluginsCompatibleBuildProperty = "idea.plugins.compatible.build"

        val propertyOverrides = mutableListOf<Pair<String, String?>>()

        try {
            if (pluginLocation != null) {
                logger.info("overriding plugin location with: ${pluginLocation.absolutePath}")
                propertyOverrides.add(Pair(pluginsPathProperty, System.getProperty(pluginsPathProperty)))
                System.setProperty(pluginsPathProperty, pluginLocation.absolutePath)
            }

            if (buildNumber != null) {
                logger.info("setting build number to \"$buildNumber\"")
                propertyOverrides.add(
                    Pair(
                        pluginsCompatibleBuildProperty,
                        System.getProperty(pluginsCompatibleBuildProperty)
                    )
                )
                System.setProperty(pluginsCompatibleBuildProperty, buildNumber)
            }

            logger.info("creating $environmentKind environment")

            val environment: Environment = when (environmentKind) {
                EnvironmentKind.IDEA -> IdeaEnvironment(environmentConfig).apply {
                    logger.info("initializing IDEA environment")
                    try {
                        // We only need to call seal() for MPS 2025.1, other versions either don't need it or call it
                        // themselves. However, we cannot use ApplicationInfo.getInstance() here to check the build
                        // number because the application hasn't been initialized yet. At the same time, calling seal()
                        // after init() is too late. So we just call it and catch any exceptions, since it appears that
                        // calling it multiple times should do no harm.
                        P3SupportInstaller.seal()
                    } catch (_: NoClassDefFoundError) {
                        // Ignore if no P3SupportInstaller present
                    }

                    init()
                }

                EnvironmentKind.MPS -> MpsEnvironment(environmentConfig).apply {
                    logger.info("initializing MPS environment")
                    init()
                }
            }

            // Configure logging again in case opening the environment has reset it.
            logging.configure(logLevel)

            try {
                logger.info("flushing events")
                environment.flushAllEvents()

                return action(environment)
            } finally {
                logger.info("flushing events before environment disposal")
                environment.flushAllEvents()
                logger.info("disposing environment")
                try {
                    environment.dispose()
                    logger.info("environment disposed")
                } catch (e: Exception) {
                    logger.info("an exception was caught while disposing environment, it will be logged here and ignored", e)
                }
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
     * Execute [action] in the context of an initialized MPS/IDEA environment and project located in [projectDir].
     * Closes the project after the action even if it throws an exception. Shuts down the environment after actions
     * finish, even if it throws an exception.
     *
     * If an action for a project throws an exception, actions for further projects in the list are not performed.
     *
     * @param projectDir the directory to open as a MPS project
     * @param action action to execute.
     */
    public fun <T> executeWithProject(projectDir: File, action: (Environment, Project) -> T): T =
        execute { env -> withOpenProject(env, projectDir, action) }

    /**
     * Execute [action] in the manner of [executeWithProject] for each project in [projectDirs]. The environment is
     * opened once, projects are opened sequentially, each project being closed before opening the next one.
     *
     * If [action] throws an exception for one of the projects, further projects in the list will not be processed.
     * Closes the project and shuts down the environment after the action finishes, even if it throws an exception.
     *
     * @param projectDirs the directories to open as MPS projects
     * @param action action to execute.
     */
    public fun <T> executeForEachProject(projectDirs: List<File>, action: (Environment, Project) -> T): List<T> =
        execute { env ->
            projectDirs.map { withOpenProject(env, it, action) }
        }

    /**
     * Opens the project in [projectDir], executes [action] and disposes of the project regardless of whether [action]
     * succeeds or throws an exception.
     */
    private fun<T> withOpenProject(environment: Environment, projectDir: File, action: (Environment, Project) -> T): T {
        logger.info("opening project: ${projectDir.absolutePath}")

        val project = environment.openProject(projectDir)

        try {
            logger.info("flushing events")
            environment.flushAllEvents()

            // Workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
            if (environment is IdeaEnvironment) {
                val buildNumber = ApplicationInfo.getInstance().build
                if (shouldForceIndexing(buildNumber)) {
                    logger.info("Forcing full indexing. Can be disabled with --force-indexing=never.")
                    forceIndexing(project as MPSProject, buildNumber)
                    logger.info("Full indexing complete")
                }
            }

            return action(environment, project)
        } finally {
            logger.info("disposing project")
            try {
                environment.closeProject(project)
            } catch (re: RuntimeException) {
                if (re.cause is AlreadyDisposedException) {
                    // do nothing
                } else {
                    throw re
                }
            }
            logger.info("project disposed")
        }
    }

    private fun shouldForceIndexing(buildNumber: BuildNumber): Boolean {
        return forceIndexing ?: hasIndexingBug(buildNumber)
    }
}
