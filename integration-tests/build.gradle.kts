import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.Multimaps
import de.itemis.mps.buildbackends.MpsPlatform
import java.nio.file.Files
import java.util.*

plugins {
    base
    id("base-conventions")
    id("de.itemis.mps.gradle.launcher")
    id("backend-testing")
}

buildscript {
    dependencies {
        classpath("com.google.guava:guava:33.4.0-jre")
    }
}

val executeGenerators: Configuration by configurations.creating
val modelcheck: Configuration by configurations.creating
val execute: Configuration by configurations.creating

dependencies {
    executeGenerators(project(":execute-generators"))
    modelcheck(project(":modelcheck"))
    execute(project(":execute"))
}

val GENERATION_TESTS = listOf(
    GenerationTest("generateBuildSolution", "generate-build-solution", listOf("--model", "my.build.script"),
        expectation = GenerationTestExpectation.SuccessWithFiles("solutions/my.build/build.xml")),
    GenerationTest("generateBuildSolutionParallel", "generate-build-solution",
        listOf("--model", "my.build.script", "--parallel-generation-threads=4"),
        expectation = GenerationTestExpectation.SuccessWithFiles("solutions/my.build/build.xml")),
    GenerationTest("generateSimple", "generate-simple", listOf()),
    GenerationTest("generateBuildSolutionWithMpsEnvironment",
        "generate-build-solution", listOf("--model", "my.build.script", "--environment", "MPS")),
    GenerationTest("generateEmpty", "generate-simple", listOf("--exclude-model", "my.solution.java"),
        expectation = GenerationTestExpectation.Empty),

    GenerationTest(
        name = "generateIncorrect",
        project = "generate-with-errors",
        args = listOf("--log-level=all"),
        expectation = GenerationTestExpectation.Failure),
    GenerationTest(
        name = "generateExcludeIncorrectModule",
        project = "generate-with-errors",
        args = listOf("--exclude-module", "solution.with.errors")
    ),
    GenerationTest(
        name = "generateExcludeIncorrectModel",
        project = "generate-with-errors",
        args = listOf("--exclude-model", "solution.with.errors.incorrect")
    ),
    GenerationTest(
        name = "generateIncludeCorrectModelOnly",
        project = "generate-with-errors",
        args = listOf("--model", "correct.model")
    )
)

val MODELCHECK_TESTS = listOf(
    ModelCheckTest("modelcheckSimple",
        project = "modelcheck",
        args = listOf("--module", "my.solution.with.errors"),
        expectSuccess = false),
    ModelCheckTest("modelcheckModule",
        project = "modelcheck",
        args = listOf("--module", "my.solution.with.module.errors", "--result-format", "module-and-model"),
        expectSuccess = false),
    ModelCheckTest("modelcheckSimpleWithMpsEnvironment",
        project = "modelcheck",
        args = listOf("--module", "my.solution.with.errors", "--environment", "MPS"),
        expectSuccess = false),
    ModelCheckTest("modelcheckExcludeModule",
        project = "modelcheck",
        args = listOf("--exclude-module", "my.solution.with.*")
    ),
    ModelCheckTest("modelcheckExcludeModuleWithMpsEnvironment",
        project = "modelcheck",
        args = listOf("--exclude-module", "my.solution.with.*", "--environment", "MPS")
    ),
    ModelCheckTest("modelcheckExcludeModel",
        project = "modelcheck",
        args = listOf("--exclude-model", "my.solution.with.errors.java", "--exclude-model", "my.solution.with.errors.brokenref")
    ),
    ModelCheckTest("modelcheckBrokenrefs",
        project = "modelcheck",
        args = listOf("--model", "my.solution.with.errors.brokenref"),
        expectSuccess = false
    ),
    ModelCheckTest("modelcheckParallel",
        project = "modelcheck",
        args = listOf("--module", "my.solution", "--parallel")
    )
)

val EXECUTE_TESTS = run {
    val commonArgs = arrayOf("--module", "my.solution", "--method", "execute")
    listOf(
        ExecuteTest(
            "executeMethodWithArgumentsPassingArguments", "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.WithArguments", "--arg", "arg1", "--arg", "arg2")
        ),
        ExecuteTest(
            "executeMethodWithArgumentsNotPassingArguments",
            "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.WithArguments")
        ),
        ExecuteTest(
            "executeMethodWithoutArgumentsPassingArguments",
            "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.WithoutArguments", "--arg", "arg1", "--arg", "arg2"),
            expectSuccess = false
        ),
        ExecuteTest(
            "executeMethodWithoutArgumentsNotPassingArguments",
            "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.WithoutArguments")
        ),
        ExecuteTest(
            "executeMissingMethod", "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.MissingMethod"),
            expectSuccess = false
        ),
        ExecuteTest(
            "executeMethodInMissingClass", "execute-method",
            listOf(*commonArgs, "--class", "my.solution.java.MissingClass"),
            expectSuccess = false
        )
    )
}

/**
 * Describes a project to generate with the supported MPS versions
 *
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class GenerationTest(val name: String, val project: String, val args: List<Any>,
                          val expectation: GenerationTestExpectation = GenerationTestExpectation.Success)

interface GenerationTestExpectation {
    fun verify(projectDir: File, exitCode: Int): String?

    object Success : GenerationTestExpectation {
        override fun verify(projectDir: File, exitCode: Int): String? =
            if (exitCode == 0) null else "generation failed unexpectedly (exit value $exitCode)"
    }

    object Failure : GenerationTestExpectation {
        override fun verify(projectDir: File, exitCode: Int): String? =
            if (exitCode == 255) null else "generation did not fail unexpectedly (exit value $exitCode)"
    }

    object Empty : GenerationTestExpectation {
        override fun verify(projectDir: File, exitCode: Int): String? =
            if (exitCode == 254) null else "generation was not empty unexpectedly (exit value $exitCode)"
    }

    data class SuccessWithFiles(val files: Set<File>) : GenerationTestExpectation {
        constructor(vararg fileNames: String) : this(fileNames.map { File(it) }.toSet())

        override fun verify(projectDir: File, exitCode: Int): String? {
            if (exitCode != 0) return "generation failed unexpectedly (actual exit value $exitCode)"

            val missingFiles = files.filter { !projectDir.resolve(it).exists() }

            if (missingFiles.isEmpty()) return null

            return missingFiles.joinToString(prefix = "the following files were not generated in ${projectDir}: ")
        }
    }
}

/**
 * Describes a project to check with the supported MPS versions
 *
 * @param name test name
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class ModelCheckTest(val name: String, val project: String, val args: List<Any>, val expectSuccess: Boolean = true)

/**
 * Describes a project to execute code in generated classes with the supported MPS versions
 *
 * @param name test name
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class ExecuteTest(val name: String, val project: String, val args: List<Any>, val expectSuccess: Boolean = true)

enum class TestKind {
    GENERATE,
    MODELCHECK,
    EXECUTE
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

fun copyTestProjectTo(project: String, copiedProjectDir: File) {
    if (copiedProjectDir.exists()) {
        copiedProjectDir.deleteRecursively()
    }
    Files.createDirectories(copiedProjectDir.toPath())

    val originalProjectDir = layout.settingsDirectory.dir("test-projects/$project").asFile
    originalProjectDir.copyRecursively(copiedProjectDir)
}

/**
 * Creates and returns the Gradle tasks to perform all the tests with a single MPS version.
 * Also creates any necessary Gradle objects (configurations, dependencies, etc.)
 */
fun tasksForMpsPlatform(mpsPlatform: MpsPlatform): Multimap<TestKind, TaskProvider<out Task>> {
    val buildDir = project.layout.buildDirectory.get().asFile

    fun JavaExecSpec.configureGenerateTaskForSpec(projectDir: File, temporaryDir: File) {
        mpsBackendLauncher.forMpsHome(mpsPlatform.mpsHome)
            .withMpsVersion(mpsPlatform.mpsVersion)
            .withJetBrainsJvm()
            .withTemporaryDirectory(temporaryDir)
            .configure(this)
        classpath(fileTree(mpsPlatform.mpsHome) {
            include("lib/**/*.jar")
        })
        classpath(executeGenerators)

        mainClass.set("de.itemis.mps.gradle.generate.MainKt")

        args("--project", projectDir)
    }

    fun JavaExec.configureGenerateTask(projectDir: File) {
        val mpsTmpDir = this.temporaryDir.resolve("mps-tmp")
        Files.createDirectories(mpsTmpDir.toPath())

        configureGenerateTaskForSpec(projectDir, mpsTmpDir)
    }

    val generateTasks = GENERATION_TESTS.map { testCase ->
        tasks.register("generate${testCase.name.capitalize()}WithMps${mpsPlatform.mpsVersion}", JavaExec::class) {
            dependsOn(executeGenerators)
            val projectDir = temporaryDir.resolve("project")

            configureGenerateTask(projectDir)

            group = LifecycleBasePlugin.VERIFICATION_GROUP

            args(testCase.args)

            doFirst {
                copyTestProjectTo(testCase.project, projectDir)
            }

            // Check exit value manually
            isIgnoreExitValue = true

            doLast {
                val actualExitValue = executionResult.get().exitValue
                val message = testCase.expectation.verify(projectDir, actualExitValue)
                if (message != null) {
                    throw GradleException("Generation test ${testCase.name} failed: $message")
                }
            }
        }
    }

    val modelcheckTasks = MODELCHECK_TESTS.map { testCase ->
        tasks.register("modelcheckTest${testCase.name.capitalize()}WithMps${mpsPlatform.mpsVersion}", JavaExec::class) {
            dependsOn(modelcheck)
            val projectDir = temporaryDir.resolve("project")
            doFirst {
                copyTestProjectTo(testCase.project, projectDir)
            }

            mpsBackendLauncher.forMpsHome(mpsPlatform.mpsHome)
                .withMpsVersion(mpsPlatform.mpsVersion)
                .withJetBrainsJvm()
                .withTemporaryDirectory(temporaryDir.resolve("mps-tmp").also { Files.createDirectories(it.toPath()) })
                .configure(this)

            group = LifecycleBasePlugin.VERIFICATION_GROUP
            classpath(fileTree(mpsPlatform.mpsHome) {
                include("lib/**/*.jar")
                // modelcheck uses HttpSupportUtil#getURL()
                include("plugins/mps-httpsupport/**/*.jar")
            })
            classpath(modelcheck)

            mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")

            args("--project", projectDir)
            args("--result-file", file("$buildDir/TEST-${testCase.name}-mps-${mpsPlatform.mpsVersion}-results.xml"))

            args(testCase.args)

            // Check exit value manually
            isIgnoreExitValue = true
            doLast {
                val actualExitValue = executionResult.get().exitValue
                val actualSuccess = actualExitValue == 0
                if (actualSuccess != testCase.expectSuccess) {
                    throw GradleException(
                        "Modelcheck outcome: expected success: ${testCase.expectSuccess}, but was: $actualSuccess" +
                                " (actual exit value $actualExitValue)"
                    )
                }
            }
        }
    }

    val executeTasks: List<TaskProvider<out Task>> = EXECUTE_TESTS.map { testCase ->
        tasks.register("executeTest${testCase.name.capitalize()}WithMps${mpsPlatform.mpsVersion}") {
            dependsOn(execute, executeGenerators)

            doLast {
                val projectDir = temporaryDir.resolve("project")
                copyTestProjectTo(testCase.project, projectDir)

                val generationResult = javaexec {
                    val mpsTmpDir = this@register.temporaryDir.resolve("mps-tmp")
                    Files.createDirectories(mpsTmpDir.toPath())

                    configureGenerateTaskForSpec(projectDir, mpsTmpDir)
                }

                if (generationResult.exitValue != 0) {
                    throw GradleException("Generation failed with exit code ${generationResult.exitValue}")
                }

                val executionResult = javaexec {
                    mpsBackendLauncher.forMpsHome(mpsPlatform.mpsHome)
                        .withMpsVersion(mpsPlatform.mpsVersion)
                        .withJetBrainsJvm()
                        .withTemporaryDirectory(temporaryDir)
                        .configure(this)

                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    classpath(fileTree(mpsPlatform.mpsHome) {
                        include("lib/**/*.jar")
                    })
                    classpath(execute)

                    mainClass.set("de.itemis.mps.gradle.execute.MainKt")

                    args("--project", projectDir)
                    args(testCase.args)

                    isIgnoreExitValue = true
                }

                val actualExitValue = executionResult.exitValue
                val actualSuccess = actualExitValue == 0
                val expectedSuccess = testCase.expectSuccess
                if (actualSuccess != expectedSuccess) {
                    throw GradleException(
                        "Execute outcome: expected success: $expectedSuccess, but was: $actualSuccess" +
                                " (actual exit value $actualExitValue)"
                    )
                }
            }
        }
    }

    return ImmutableMultimap.builder<TestKind, TaskProvider<out Task>>()
        .putAll(TestKind.EXECUTE, executeTasks)
        .putAll(TestKind.GENERATE, generateTasks)
        .putAll(TestKind.MODELCHECK, modelcheckTasks)
        .build()
}

data class Key(val kind: TestKind, val mpsVersion: String)

fun buildTestMatrix(): Multimap<Key, TaskProvider<out Task>> {
    val builder = ImmutableMultimap.builder<Key, TaskProvider<out Task>>()

    for (mpsPlatform in backendTesting.mpsPlatforms) {
        val tasksForThisPlatform = tasksForMpsPlatform(mpsPlatform)
        for (entry in tasksForThisPlatform.entries()) {
            builder.put(Key(entry.key, mpsPlatform.mpsVersion), entry.value)
        }
    }

    return builder.build()
}

val testMatrix = buildTestMatrix()

tasks.named("check") {
    dependsOn(testMatrix.values())
}

for (kind in TestKind.values()) {
    tasks.register("test${kind.name.lowercase().capitalize()}") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Run all '${kind.name.lowercase()}' tests"
        dependsOn(Multimaps.filterKeys(testMatrix) { it!!.kind == kind }.values())
    }
}

for (mpsPlatform in backendTesting.mpsPlatforms) {
    mpsPlatform.testTask {
        dependsOn(Multimaps.filterKeys(testMatrix) { it!!.mpsVersion == mpsPlatform.mpsVersion }.values())
    }
}
