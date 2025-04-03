package de.itemis.mps.gradle.project.loader.internal

import com.intellij.testFramework.IndexingTestUtil
import jetbrains.mps.project.MPSProject

/**
 * A separate file to avoid NoClassDefFoundError since [IndexingTestUtil] is not present on older MPS versions.
 */
internal fun waitUntilIndexesAreReadyOn241(project: MPSProject) {
    IndexingTestUtil.waitUntilIndexesAreReady(project.project)
}
