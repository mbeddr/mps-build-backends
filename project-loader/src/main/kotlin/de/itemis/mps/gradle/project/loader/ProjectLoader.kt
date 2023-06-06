package de.itemis.mps.gradle.project.loader

import de.itemis.mps.gradle.logging.LogLevel
import de.itemis.mps.gradle.logging.detectLogging
import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.environment.EnvironmentConfig
import jetbrains.mps.tool.environment.IdeaEnvironment
import jetbrains.mps.tool.environment.MpsEnvironment
import org.apache.log4j.Logger
import java.io.File

/**
 * Executes an action in the context of an MPS or IDEA environment.
 */
public class ProjectLoader private constructor(
    private val environmentConfig: EnvironmentConfig,
    private val environmentKind: EnvironmentKind,
    private val pluginLocation: File?,
    private val buildNumber: String?,
    private val logLevel: LogLevel
) {
    private val logger = Logger.getLogger("de.itemis.mps.gradle.project.loader")

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

        public fun build(): ProjectLoader = ProjectLoader(
            environmentConfigBuilder.build(),
            environmentKind,
            environmentConfigBuilder.pluginLocation,
            buildNumber,
            logLevel
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
     * Execute [action] in the context of an initialized MPS/IDEA environment and project located in [projectDir].
     * Closes the project and shuts down the environment after the action finishes, even if it throws an exception.
     *
     * @param projectDir the directory to open as a MPS project
     * @param action action to execute.
     */
    public fun <T> executeWithProject(projectDir: File, action: (Environment, Project) -> T): T =
        execute { env -> withOpenProject(env, projectDir, action) }

    /**
     * Opens the project in [projectDir], executes [action] and disposes of the project regardless of whether [action]
     * succeeds or throws an exception.
     */
    private fun<T> withOpenProject(environment: Environment, projectDir: File, action: (Environment, Project) -> T): T {
        logger.info("opening project: ${projectDir.absolutePath}")

        val project = environment.openProject(projectDir)

        logger.info("flushing events")
        environment.flushAllEvents()

        try {
            return action(environment, project)
        } finally {
            logger.info("disposing project")
            project.dispose()
            logger.info("project disposed")
        }
    }
}
