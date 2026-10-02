package de.itemis.mps.gradle.project.loader

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.BuildNumber
import com.intellij.testFramework.IndexingTestUtil
import jetbrains.mps.project.MPSProject

/**
 * Indicates whether the given MPS version has the indexing bug.
 */
internal fun hasIndexingBug(buildNumber: BuildNumber): Boolean {
    // MPS 2023.2 and 2026.1+ need a full rescan; intermediate versions only need to wait for indexing.
    return buildNumber.baselineVersion >= 232
}

/**
 * Force full indexing as a workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
 */
internal fun forceIndexing(project: MPSProject, buildNumber: BuildNumber) {
    try {
        if (buildNumber.baselineVersion >= 261) {
            // MPS-40233: initial indexing can finish without indexing the model roots. Request a full rescan
            // before waiting for readiness: https://youtrack.jetbrains.com/issue/MPS-40233
            forceIndexing232(project)
        }
        forceIndexing241(project)
    } catch (e: NoClassDefFoundError) {
        // We're probably on an earlier version
        forceIndexing232(project)
    }
}

private fun forceIndexing232(project: MPSProject) {
    val application = ApplicationManager.getApplication()
    application.invokeAndWait({
        application.runWriteAction {
            ProjectRootManagerEx.getInstanceEx(project.project)
                .makeRootsChange({}, RootsChangeRescanningInfo.TOTAL_RESCAN)
        }
    }, ModalityState.defaultModalityState())
}

private fun forceIndexing241(project: MPSProject) {
    IndexingTestUtil.waitUntilIndexesAreReady(project.project)
}
