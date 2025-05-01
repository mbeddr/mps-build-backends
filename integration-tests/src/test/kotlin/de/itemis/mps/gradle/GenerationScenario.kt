package de.itemis.mps.gradle

import java.io.File
import java.nio.file.Files

/**
 * Describes a project to generate with the supported MPS versions
 *
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class GenerationTestScenario(val name: String, val project: String, val args: List<Any>,
                                  val expectation: GenerationTestExpectation = GenerationTestExpectation.Success) {
    val projectDir = File("projects").resolve(project)
}

interface GenerationTestExpectation {
    fun verify(testCase: GenerationTestScenario, exitCode: Int): String?
    fun prepare(testCase: GenerationTestScenario) {}

    object Success : GenerationTestExpectation {
        override fun verify(testCase: GenerationTestScenario, exitCode: Int): String? =
            if (exitCode == 0) null else "generation failed unexpectedly (exit value $exitCode)"
    }

    object Failure : GenerationTestExpectation {
        override fun verify(testCase: GenerationTestScenario, exitCode: Int): String? =
            if (exitCode == 255) null else "generation did not fail unexpectedly (exit value $exitCode)"
    }

    object Empty : GenerationTestExpectation {
        override fun verify(testCase: GenerationTestScenario, exitCode: Int): String? =
            if (exitCode == 254) null else "generation was not empty unexpectedly (exit value $exitCode)"
    }

    data class SuccessWithFiles(val files: Set<File>) : GenerationTestExpectation {
        constructor(vararg fileNames: String) : this(fileNames.map { File(it) }.toSet())

        override fun verify(testCase: GenerationTestScenario, exitCode: Int): String? {
            if (exitCode != 0) return "generation failed unexpectedly (actual exit value $exitCode)"

            val missingFiles = files.filter { !testCase.projectDir.resolve(it).exists() }

            if (missingFiles.isEmpty()) return null

            return missingFiles.joinToString(prefix = "the following files were not generated in ${testCase.projectDir}: ")
        }

        override fun prepare(testCase: GenerationTestScenario) {
            files.forEach {
                val absolutePath = testCase.projectDir.resolve(it).toPath()
                if (Files.exists(absolutePath)) Files.delete(absolutePath)
            }
        }
    }
}
