package de.itemis.mps.gradle.project.loader

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.BuildNumber
import jetbrains.mps.project.MPSProject

/**
 * Indicates whether the given MPS version has the indexing bug.
 */
public fun hasIndexingBug(buildNumber: BuildNumber): Boolean {
    return buildNumber.baselineVersion >= 232
}

/**
 * Force full indexing as a workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
 */
public fun forceIndexing(project: MPSProject) {
    val application = ApplicationManager.getApplication()
    application.invokeAndWait({
        application.runWriteAction {
            ProjectRootManagerEx.getInstanceEx(project.project)
                .makeRootsChange({}, RootsChangeRescanningInfo.TOTAL_RESCAN)
        }
    }, ModalityState.defaultModalityState())
}
