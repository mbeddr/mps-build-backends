package de.itemis.mps.gradle.project.loader

import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.application.ModalityState
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.RootsChangeRescanningInfo
import com.intellij.openapi.roots.ex.ProjectRootManagerEx
import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.vfs.LocalFileSystem
import com.intellij.openapi.vfs.VfsUtilCore
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.testFramework.IndexingTestUtil
import com.intellij.util.indexing.IndexableSetContributor
import jetbrains.mps.project.MPSProject
import java.util.logging.Logger

private val logger = Logger.getLogger("de.itemis.mps.gradle.project.loader")

private const val INDEX_ROOTS_TIMEOUT_MILLIS = 60_000L
private const val INDEX_ROOTS_POLL_MILLIS = 10L

/**
 * Indicates whether the given MPS version has the indexing bug.
 */
internal fun hasIndexingBug(buildNumber: BuildNumber): Boolean {
    // MPS 2023.2 and 2025.2+ need a full rescan; intermediate versions only need to wait for indexing.
    return buildNumber.baselineVersion >= 232
}

/**
 * Force full indexing as a workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
 */
internal fun forceIndexing(project: MPSProject, buildNumber: BuildNumber) {
    try {
        if (buildNumber.baselineVersion >= 252) {
            // MPS-40233: initial indexing can finish without indexing the model roots. Request a full rescan
            // before waiting for readiness: https://youtrack.jetbrains.com/issue/MPS-40233
            waitForProjectIndexRoots(project)
            requestFullRescan(project)
        }
        waitUntilIndexesAreReady(project)
    } catch (e: NoClassDefFoundError) {
        // We're probably on an earlier version
        requestFullRescan(project)
    }
}

/**
 * Waits until the platform's indexable roots cover the project directory.
 *
 * MPS adds a project's model files to the index roots on a background thread after its modules are loaded, and only
 * then asks the platform to rescan. A full rescan requested before that happens does not cover the project's files,
 * and [IndexingTestUtil.waitUntilIndexesAreReady] can then return with the project unindexed. Index-backed searches
 * report unindexed models as searched with no matches, so the gap is invisible to callers. A project without modules
 * contributes no roots, so there is nothing to wait for. Must be called outside any read or write action.
 */
private fun waitForProjectIndexRoots(project: MPSProject) {
    var hasModules = false
    project.modelAccess.runReadAction { hasModules = project.projectModules.isNotEmpty() }
    if (!hasModules) return

    val ideaProject = project.project
    val basePath = ideaProject.basePath ?: return
    val projectDirectory = LocalFileSystem.getInstance().refreshAndFindFileByPath(basePath) ?: return

    val deadline = System.currentTimeMillis() + INDEX_ROOTS_TIMEOUT_MILLIS
    while (!indexableRootsCover(ideaProject, projectDirectory)) {
        if (System.currentTimeMillis() >= deadline) {
            logger.warning("Index roots did not cover $basePath within $INDEX_ROOTS_TIMEOUT_MILLIS ms; rescanning anyway")
            return
        }
        Thread.sleep(INDEX_ROOTS_POLL_MILLIS)
    }
}

private fun indexableRootsCover(project: Project, directory: VirtualFile): Boolean =
    IndexableSetContributor.EP_NAME.extensionList.any { contributor ->
        IndexableSetContributor.getProjectRootsToIndex(contributor, project)
            .any { VfsUtilCore.isAncestor(directory, it, false) }
    }

private fun requestFullRescan(project: MPSProject) {
    val application = ApplicationManager.getApplication()
    application.invokeAndWait({
        application.runWriteAction {
            ProjectRootManagerEx.getInstanceEx(project.project)
                .makeRootsChange({}, RootsChangeRescanningInfo.TOTAL_RESCAN)
        }
    }, ModalityState.defaultModalityState())
}

private fun waitUntilIndexesAreReady(project: MPSProject) {
    IndexingTestUtil.waitUntilIndexesAreReady(project.project)
}
