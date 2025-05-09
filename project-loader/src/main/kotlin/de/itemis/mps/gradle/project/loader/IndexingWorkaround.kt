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
public fun hasIndexingBug(buildNumber: BuildNumber): Boolean {
    // For 2023.2 we need to force indexing, for 2024.1 we only wait for indexing to complete using IndexingTestUtils.
    return buildNumber.baselineVersion >= 232
}

/**
 * Force full indexing as a workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
 */
public fun forceIndexing(project: MPSProject, @Suppress("UNUSED_PARAMETER") buildNumber: BuildNumber) {
    try {
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
