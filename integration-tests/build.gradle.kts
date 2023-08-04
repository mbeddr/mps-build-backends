import org.gradle.kotlin.dsl.support.serviceOf
import java.nio.file.Files

buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    base
    id("base-conventions")
}

val executeGenerators by configurations.creating
val modelcheck by configurations.creating

dependencies {
    executeGenerators(project(":execute-generators"))
    modelcheck(project(":modelcheck"))
}

val SUPPORTED_MPS_VERSIONS = arrayOf("2021.1.4", "2021.2.6", "2021.3.2", "2022.2", "2022.3")

val GENERATION_TESTS = listOf(
    GenerationTest("generateBuildSolution", "generate-build-solution", listOf("--model", "my.build.script"),
        expectation = GenerationTestExpectation.SuccessWithFiles("solutions/my.build/build.xml")),
    GenerationTest("generateBuildSolutionParallel", "generate-build-solution",
        listOf("--model", "my.build.script", "--parallel-generation-threads=4"),
        expectation = GenerationTestExpectation.SuccessWithFiles("solutions/my.build/build.xml")),
    GenerationTest("generateBuildSolutionNonStrict", "generate-build-solution",
        listOf("--model", "my.build.script", "--disable-strict-mode"),
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
    )
)

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

fun configureJavaForMpsVersion(javaExec: JavaExec, mpsHome: File, mpsVersion: String) {
    val launcher = serviceOf<JavaToolchainService>().launcherFor {
        vendor.set(JvmVendorSpec.matching("JetBrains"))
        languageVersion.set(JavaLanguageVersion.of(if (mpsVersion < "2022") 11 else 17))
    }
    javaExec.javaLauncher.set(launcher)

    if (mpsVersion >= "2022.3") {
        javaExec.systemProperty("jna.boot.library.path", mpsHome.resolve("lib/jna/${System.getProperty("os.arch")}"))
    }

    if (mpsVersion >= "2022") {
        val modules = listOf(
            "java.base/java.io",
            "java.base/java.lang",
            "java.base/java.lang.reflect",
            "java.base/java.net",
            "java.base/java.nio",
            "java.base/java.nio.charset",
            "java.base/java.text",
            "java.base/java.time",
            "java.base/java.util",
            "java.base/java.util.concurrent",
            "java.base/java.util.concurrent.atomic",
            "java.base/jdk.internal.vm",
            "java.base/sun.nio.ch",
            "java.base/sun.nio.fs",
            "java.base/sun.security.ssl",
            "java.base/sun.security.util",
            "java.desktop/java.awt",
            "java.desktop/java.awt.dnd.peer",
            "java.desktop/java.awt.event",
            "java.desktop/java.awt.image",
            "java.desktop/java.awt.peer",
            "java.desktop/javax.swing",
            "java.desktop/javax.swing.plaf.basic",
            "java.desktop/javax.swing.text.html",
            "java.desktop/sun.awt.datatransfer",
            "java.desktop/sun.awt.image",
            "java.desktop/sun.awt",
            "java.desktop/sun.font",
            "java.desktop/sun.java2d",
            "java.desktop/sun.swing",
            "jdk.attach/sun.tools.attach",
            "jdk.compiler/com.sun.tools.javac.api",
            "jdk.internal.jvmstat/sun.jvmstat.monitor",
            "jdk.jdi/com.sun.tools.jdi",
            "java.desktop/sun.lwawt",
            "java.desktop/sun.lwawt.macosx",
            "java.desktop/com.apple.laf",
            "java.desktop/com.apple.eawt",
            "java.desktop/com.apple.eawt.event"
        )

        javaExec.jvmArgs(modules.map { "--add-opens=$it=ALL-UNNAMED" })
    }
}

/**
 * Creates and returns the Gradle tasks to perform all the tests with a single MPS version.
 * Also creates any necessary Gradle objects (configurations, dependencies, etc.)
 */
fun tasksForMpsVersion(mpsVersion: String): List<TaskProvider<out Task>> {
    val configuration = configurations.create("mps$mpsVersion")
    dependencies.add(configuration.name, "com.jetbrains:mps:$mpsVersion@zip")

    val mpsHome = File(buildDir, "mps-$mpsVersion")
    val unpackTask = tasks.register("unpackMps$mpsVersion", Sync::class) {
        dependsOn(configuration)
        from({ configuration.resolve().map(project::zipTree) })
        into(mpsHome)
    }

    val generateTasks = GENERATION_TESTS.map { testCase ->
        tasks.register("generate${testCase.name.capitalize()}WithMps$mpsVersion", JavaExec::class) {
            dependsOn(unpackTask)
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            classpath(executeGenerators)
            classpath(fileTree(mpsHome) {
                include("lib/**/*.jar")
            })

            // MPS creates logs under its working directory so start it from the MPS home directory, to avoid polluting
            // the checkout directory
            workingDir = mpsHome

            mainClass.set("de.itemis.mps.gradle.generate.MainKt")

            // Workaround for https://youtrack.jetbrains.com/issue/MPS-35992/MPSHeadlessPlatformStarter-race-condition-causes-unnecessary-wait
            args("--test-mode")

            args("--project", testCase.projectDir)
            args(testCase.args)

            doFirst {
                println("Deleting generated sources in ${testCase.projectDir}")
                delete(fileTree(testCase.projectDir) {
                    include("**/source_gen/**")
                    include("**/source_gen.caches/**")
                    include("**/classes_gen/**")
                })

                println("Deleting MPS caches in ${mpsHome}/system")
                delete(fileTree(mpsHome) { include("system/**") })
            }

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

            configureJavaForMpsVersion(this, mpsHome, mpsVersion)
        }
    }

    val modelcheckTasks = MODELCHECK_TESTS.map { testCase ->
        tasks.register("modelcheckTest${testCase.name.capitalize()}WithMps$mpsVersion", JavaExec::class) {
            dependsOn(unpackTask)
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            classpath(modelcheck)
            classpath(fileTree(mpsHome) {
                include("lib/**/*.jar")
                // modelcheck uses HttpSupportUtil#getURL()
                include("plugins/mps-httpsupport/**/*.jar")
                include("plugins/mps-modelchecker/**/*.jar")
            })

            // MPS creates logs under its working directory so start it from the MPS home directory, to avoid polluting
            // the checkout directory
            workingDir = mpsHome

            mainClass.set("de.itemis.mps.gradle.modelcheck.MainKt")

            // Workaround for https://youtrack.jetbrains.com/issue/MPS-35992/MPSHeadlessPlatformStarter-race-condition-causes-unnecessary-wait
            args("--test-mode")

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
                                " (actual exit value $actualExitValue)")
                }
            }

            configureJavaForMpsVersion(this, mpsHome, mpsVersion)
        }
    }

    return generateTasks + modelcheckTasks
}

val testTasksByVersion = SUPPORTED_MPS_VERSIONS.map { mpsVersion ->
    val tasksForThisVersion = tasksForMpsVersion(mpsVersion)
    tasks.register("checkMps${mpsVersion}") {
        description = "Runs all tests using MPS $mpsVersion"
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        dependsOn(tasksForThisVersion)
    }
}

tasks.named("check") {
    dependsOn(testTasksByVersion)
}
