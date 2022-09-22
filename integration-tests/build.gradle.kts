buildscript {
    configurations.classpath {
        resolutionStrategy.activateDependencyLocking()
    }
}

plugins {
    base
}

val executeGenerators by configurations.creating
val modelcheck by configurations.creating

dependencies {
    executeGenerators(project(":execute-generators"))
    modelcheck(project(":modelcheck"))
}

val SUPPORTED_MPS_VERSIONS = arrayOf("2020.3.6", "2021.1.4", "2021.2.5", "2021.3.1")

val GENERATION_TESTS = listOf(
    GenerationTest("generate-build-solution", listOf("--model", "my.build.script")),
    GenerationTest("generate-simple", listOf()))

val MODELCHECK_TESTS = listOf(
    ModelCheckTest("modelcheckSimple",
        project = "modelcheck",
        args = listOf("--module", "my.solution.with.errors"),
        expectSuccess = false),
    ModelCheckTest("modelcheckExcludeModule",
        project = "modelcheck",
        args = listOf("--exclude-module", "my.solution.with.errors")
    ),
    ModelCheckTest("modelcheckExcludeModel",
        project = "modelcheck",
        args = listOf("--exclude-model", "my.solution.with.errors.java")
    )
)

/**
 * Describes a project to generate with the supported MPS versions
 *
 * @param project project folder name (in `projects/`)
 * @param args additional arguments to the command
 */
data class GenerationTest(val project: String, val args: List<Any>) {
    val projectDir = file("projects/$project")
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
        tasks.register("generate${testCase.project.capitalize()}WithMps$mpsVersion", JavaExec::class) {
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

            args("--project", testCase.projectDir)
            args(testCase.args)

            doFirst {
                println("Deleting generated sources in ${testCase.projectDir}")
                delete(fileTree(testCase.projectDir) {
                    include("**/source_gen/**")
                    include("**/source_gen.caches/**")
                    include("**/classes_gen/**")
                })
            }
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
            })

            // MPS creates logs under its working directory so start it from the MPS home directory, to avoid polluting
            // the checkout directory
            workingDir = mpsHome

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
                                " (actual exit value $actualExitValue)")
                }
            }
        }
    }

    return generateTasks + modelcheckTasks
}

val allTestTasks = SUPPORTED_MPS_VERSIONS.flatMap(::tasksForMpsVersion)

tasks.named("check") {
    dependsOn(allTestTasks)
}
