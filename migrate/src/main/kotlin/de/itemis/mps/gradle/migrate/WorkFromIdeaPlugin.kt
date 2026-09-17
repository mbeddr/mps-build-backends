package de.itemis.mps.gradle.migrate

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import de.itemis.mps.gradle.migration.forceSaveAllModules
import de.itemis.mps.gradle.migration.getProjectName
import de.itemis.mps.gradle.migration.saveProject
import jetbrains.mps.ide.migration.AntTaskExecutionUtil
import jetbrains.mps.project.Project
import java.util.logging.Level

/**
 * Entry point called by reflection from this JAR loaded as an IDEA plugin.
 * Loading the code as an IDEA plugin allows easy access to the necessary classes in other plugins, in particular
 * [AntTaskExecutionUtil], which lives in the `jetbrains.mps.ide.migration.workbench` plugin.
 *
 * [AntTaskExecutionUtil.migrate] is the same headless entry point MPS's own `<migrate>` Ant task uses; it drives the
 * same code that backs the interactive migration assistant, running project migrations, cleanup migrations, and
 * module (language and refactoring) migrations - including non-rerunnable and data-requiring ones - without showing
 * any UI.
 */
object WorkFromIdeaPlugin {
    @JvmStatic
    fun work(project: Project, haltOnPrecheckFailure: Boolean, haltOnDependencyError: Boolean): Boolean {
        val projectName = getProjectName(project)
        var success = false

        // AntTaskExecutionUtil.migrate() is meant to run on the EDT, same as when it is invoked from MPS's own
        // <migrate> Ant task. The subsequent saving needs to happen on the EDT too - MPS's write actions can only be
        // acquired there - so everything is done inside a single invokeAndWait.
        // invokeAndWait blocks this thread until the lambda below finishes on the EDT, so plain capture-and-mutate
        // of this var is safe - no concurrent access is actually happening.
        ApplicationManager.getApplication().invokeAndWait({
            try {
                logger.info("Running migration assistant on $projectName")
                success = AntTaskExecutionUtil.migrate(project, haltOnPrecheckFailure, haltOnDependencyError) == true
                logger.info("Done running migration assistant on $projectName, success=$success")

                if (success) {
                    // AntTaskExecutionUtil.migrate() already saves the repository, but force-saving all modules
                    // makes sure that module descriptor changes (e.g. bumped language import versions) are written
                    // to disk even if MPS doesn't otherwise consider the module "changed".
                    forceSaveAllModules(project)
                    saveProject(project)
                }
            } catch (e: Exception) {
                logger.log(Level.SEVERE, "Exception while running migration assistant on $projectName", e)
            }
        }, ModalityState.defaultModalityState())

        return success
    }
}
