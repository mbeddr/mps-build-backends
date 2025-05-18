package de.itemis.mps.buildbackends

import org.gradle.api.file.ProjectLayout
import java.io.File
import java.nio.file.Files
import javax.inject.Inject

abstract class BackendTesting(val mpsPlatforms: List<MpsPlatform>) {
    @get:Inject
    abstract val layout: ProjectLayout

    fun testProjectDir(project: String) = layout.settingsDirectory.dir("test-projects/$project").asFile

    fun copyTestProjectTo(project: String, copiedProjectDir: File) {
        recreateDirectory(copiedProjectDir)
        testProjectDir(project).copyRecursively(copiedProjectDir)
    }
}

fun recreateDirectory(dir: File): File {
    if (dir.exists()) {
        dir.deleteRecursively()
    }
    Files.createDirectories(dir.toPath())
    return dir
}
