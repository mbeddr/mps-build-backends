import com.google.common.collect.ImmutableMultimap
import com.google.common.collect.Multimap
import com.google.common.collect.MultimapBuilder
import com.google.common.collect.Multimaps
import java.nio.file.Files
import java.util.*

plugins {
    base
    id("base-conventions")
    id("de.itemis.mps.gradle.launcher")
}

val executeGenerators by configurations.creating
val modelcheck by configurations.creating
val execute by configurations.creating

dependencies {
    executeGenerators(project(":execute-generators"))
    modelcheck(project(":modelcheck"))
    execute(project(":execute"))
}

val SUPPORTED_MPS_VERSIONS = arrayOf("2021.3.5", "2022.2.2", "2022.3")

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
                          val expectation: GenerationTestExpectation = GenerationTestExpectation.Success) {
    val projectDir = file("projects/$project")
}

interface GenerationTestExpectation {
    fun verify(testCase: GenerationTest, exitCode: Int): String?
    fun prepare(testCase: GenerationTest) {}

    object Success : GenerationTestExpectation {
        override fun verify(testCase: GenerationTest, exitCode: Int): String? =
            if (exitCode == 0) null else "generation failed unexpectedly (exit value $exitCode)"
    }

    object Failure : GenerationTestExpectation {
        override fun verify(testCase: GenerationTest, exitCode: Int): String? =
            if (exitCode == 255) null else "generation did not fail unexpectedly (exit value $exitCode)"
    }

    object Empty : GenerationTestExpectation {
        override fun verify(testCase: GenerationTest, exitCode: Int): String? =
            if (exitCode == 254) null else "generation was not empty unexpectedly (exit value $exitCode)"
    }

    data class SuccessWithFiles(val files: Set<File>) : GenerationTestExpectation {
        constructor(vararg fileNames: String) : this(fileNames.map { File(it) }.toSet())

        override fun verify(testCase: GenerationTest, exitCode: Int): String? {
            if (exitCode != 0) return "generation failed unexpectedly (actual exit value $exitCode)"

            val missingFiles = files.filter { !testCase.projectDir.resolve(it).exists() }

            if (missingFiles.isEmpty()) return null

            return missingFiles.joinToString(prefix = "the following files were not generated in ${testCase.projectDir}: ")
        }

        override fun prepare(testCase: GenerationTest) {
            files.forEach {
                val absolutePath = testCase.projectDir.resolve(it).toPath()
                if (Files.exists(absolutePath)) Files.delete(absolutePath)
            }
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
data class ModelCheckTest(val name: String, val project: String, val args: List<Any>, val expectSuccess: Boolean = true) {
    val projectDir = file("projects/$project")
}

/**
 * Describes a project to execute code in generated classes with the supported MPS versions
 *
 * @param name test name
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class ExecuteTest(val name: String, val project: String, val args: List<Any>, val expectSuccess: Boolean = true) {
    val projectDir = file("projects/$project")
}

enum class TestKind {
    GENERATE,
    MODELCHECK,
    EXECUTE
}

fun String.capitalize() = replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }

/**
 * Creates and returns the Gradle tasks to perform all the tests with a single MPS version.
 * Also creates any necessary Gradle objects (configurations, dependencies, etc.)
 */
fun tasksForMpsVersion(mpsVersion: String): Multimap<TestKind, TaskProvider<out Task>> {
    val configuration = configurations.create("mps$mpsVersion")
    dependencies.add(configuration.name, "com.jetbrains:mps:$mpsVersion@zip")

    val mpsHome = File(buildDir, "mps-$mpsVersion")
    val unpackTask = tasks.register("unpackMps$mpsVersion", Sync::class) {
        dependsOn(configuration)
        from({ configuration.resolve().map(project::zipTree) })
        into(mpsHome)
    }

    fun JavaExecSpec.configureGenerateTaskForSpec(projectDir: File, temporaryDir: File) {
        mpsBackendLauncher.forMpsHome(mpsHome)
            .withMpsVersion(mpsVersion)
            .withJetBrainsJvm()
            .withTemporaryDirectory(temporaryDir)
            .configure(this)
        classpath(executeGenerators)
        classpath(fileTree(mpsHome) {
            include("lib/**/*.jar")
        })

        mainClass.set("de.itemis.mps.gradle.generate.MainKt")

        args("--project", projectDir)
    }

    fun cleanBeforeGeneration(projectDir: File) {
        println("Deleting generated sources in $projectDir")
        delete(fileTree(projectDir) {
            include("**/source_gen/**")
            include("**/source_gen.caches/**")
            include("**/classes_gen/**")
        })

        println("Deleting MPS caches in ${mpsHome}/system")
        delete(fileTree(mpsHome) { include("system/**") })
    }

    fun JavaExec.configureGenerateTask(projectDir: File) {
        configureGenerateTaskForSpec(projectDir, this.temporaryDir)
        dependsOn(unpackTask)
        doFirst {
            cleanBeforeGeneration(projectDir)
        }
    }

    val generateTasks = GENERATION_TESTS.map { testCase ->
        tasks.register("generate${testCase.name.capitalize()}WithMps$mpsVersion", JavaExec::class) {
            configureGenerateTask(testCase.projectDir)

            group = LifecycleBasePlugin.VERIFICATION_GROUP

            args(testCase.args)

            doFirst {
                testCase.expectation.prepare(testCase)
            }

            // Check exit value manually
            isIgnoreExitValue = true
            doLast {
                val actualExitValue = executionResult.get().exitValue
                val message = testCase.expectation.verify(testCase, actualExitValue)
                if (message != null) {
                    throw GradleException("Generation test ${testCase.name} failed: $message")
                }
            }
        }
    }

    val modelcheckTasks = MODELCHECK_TESTS.map { testCase ->
        tasks.register("modelcheckTest${testCase.name.capitalize()}WithMps$mpsVersion", JavaExec::class) {
            mpsBackendLauncher.forMpsHome(mpsHome)
                .withMpsVersion(mpsVersion)
                .withJetBrainsJvm()
                .configure(this)

            dependsOn(unpackTask)
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            classpath(modelcheck)
            classpath(fileTree(mpsHome) {
                include("lib/**/*.jar")
                // modelcheck uses HttpSupportUtil#getURL()
                include("plugins/mps-httpsupport/**/*.jar")
                include("plugins/mps-modelchecker/**/*.jar")
            })

            mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")

            args("--project", testCase.projectDir)
            args("--result-file", file("$buildDir/TEST-${testCase.name}-mps-${mpsVersion}-results.xml"))

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
        tasks.register("executeTest${testCase.name.capitalize()}WithMps$mpsVersion") {
            dependsOn(unpackTask)

            doLast {
                cleanBeforeGeneration(testCase.projectDir)

                javaexec {
                    configureGenerateTaskForSpec(testCase.projectDir, temporaryDir)
                }

                val executionResult = javaexec {
                    mpsBackendLauncher.forMpsHome(mpsHome)
                        .withMpsVersion(mpsVersion)
                        .withJetBrainsJvm()
                        .withTemporaryDirectory(temporaryDir)
                        .configure(this)

                    group = LifecycleBasePlugin.VERIFICATION_GROUP
                    classpath(execute)
                    classpath(fileTree(mpsHome) {
                        include("lib/**/*.jar")
                    })

                    mainClass.set("de.itemis.mps.gradle.execute.MainKt")

                    args("--project", testCase.projectDir)
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

    for (mpsVersion in SUPPORTED_MPS_VERSIONS) {
        val tasksForThisVersion = tasksForMpsVersion(mpsVersion)
        for (entry in tasksForThisVersion.entries()) {
            builder.put(Key(entry.key, mpsVersion), entry.value)
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

for (mpsVersion in SUPPORTED_MPS_VERSIONS) {
    tasks.register("testMps$mpsVersion") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Run all tests with MPS $mpsVersion"
        dependsOn(Multimaps.filterKeys(testMatrix) { it!!.mpsVersion == mpsVersion }.values())
    }
}
