package de.itemis.mps.gradle.migration

import com.intellij.openapi.application.ex.ApplicationManagerEx
import jetbrains.mps.project.AbstractModule
import jetbrains.mps.project.MPSProject
import jetbrains.mps.project.Project
import java.util.logging.Logger

private val logger = Logger.getLogger("de.itemis.mps.gradle.migration")

/**
 * Shared code used by the `remigrate` and `migrate` backends' `WorkFromIdeaPlugin.work()` implementations, which run
 * migrations of some kind on [project] and then need to make sure the results are actually written to disk.
 */

// A helper function to avoid deprecation warnings everywhere we need the project name
@Suppress("DEPRECATION")
fun getProjectName(project: Project): String = project.name

fun forceSaveAllModules(project: Project) {
    project.modelAccess.runWriteAction {
        val allModules = project.projectModulesWithGenerators
        for (module in allModules.asSequence().filterIsInstance<AbstractModule>()) {
            module.forceSaveRecursively()
        }
    }
}

fun saveProject(project: Project) {
    logger.info("Saving project ${getProjectName(project)}")
    project.modelAccess.runWriteAction {
        project.repository.saveAll()
    }

    val applicationEx = ApplicationManagerEx.getApplicationEx()
    val ideaProject = (project as MPSProject).project

    val saveAllowed: Boolean = applicationEx.isSaveAllowed
    try {
        applicationEx.isSaveAllowed = true
        ideaProject.save()
    } finally {
        applicationEx.isSaveAllowed = saveAllowed
    }
}
