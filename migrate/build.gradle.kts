import de.itemis.mps.buildbackends.recreateDirectory
import org.apache.commons.io.file.PathUtils
import org.apache.tools.ant.filters.ReplaceTokens
import org.gradle.kotlin.dsl.support.serviceOf

buildscript {
    dependencies {
        classpath(libs.commons.io.buildscript)
    }
}

plugins {
    id("de.itemis.mps.gradle.launcher")
    id("backend-conventions")
    id("backend-testing")
}

val executeGenerators: Configuration by configurations.creating

dependencies {
    implementation(project(":migration-common"))
    executeGenerators(project(":execute-generators"))
}

mpsZips {
    include("lib/app.jar")
    include("lib/mps-core.jar")
    include("lib/mps-environment.jar")
    include("lib/mps-platform.jar")
    include("lib/mps-openapi.jar")
    include("lib/mps-logging.jar")
    include("lib/platform-api.jar")
    include("lib/platform-impl.jar")
    include("lib/util.jar")
    include("lib/util-8.jar")

    include("plugins/mps-migration/lib/*.jar")
}

tasks.processResources {
    filter(ReplaceTokens::class, "tokens" to mapOf("version" to project.version))
}

val jarsOnlyRuntimeClasspath = tasks.jar.zip(configurations.runtimeClasspath) { jar, runtimeClasspath -> jar.outputs.files.plus(runtimeClasspath) }

for (mpsPlatform in backendTesting.mpsPlatforms) {
    val testSimple = tasks.register("testMigrateForMps${mpsPlatform.mpsVersion}", JavaExec::class) {
        // executeGenerators is only referenced from inside the nested javaexec {} below, which Gradle can't see at
        // configuration time, so it wouldn't otherwise know to build it first.
        dependsOn(executeGenerators)

        val projectDir = temporaryDir.resolve("project")

        doFirst {
            backendTesting.copyTestProjectTo("migrate", projectDir)

            // migrate does not build the project, use execute-generators to prepare for testing
            val buildResult = serviceOf<ExecOperations>().javaexec {
                mpsBackendLauncher.forMpsHome(mpsPlatform.mpsHome)
                    .withMpsVersion(mpsPlatform.mpsVersion)
                    .withJetBrainsJvm()
                    .withTemporaryDirectory(recreateDirectory(temporaryDir.resolve("build-tmp")))
                    .configure(this)

                classpath(fileTree(mpsPlatform.mpsHome) {
                    include("lib/**/*.jar")
                })
                classpath(executeGenerators)

                mainClass = "de.itemis.mps.gradle.generate.MainKt"

                args("--project", projectDir)
                args("--module", "language.with.migration")

                if (gradle.startParameter.logLevel >= LogLevel.INFO) {
                    args("--log-level=info")
                }
            }

            if (buildResult.exitValue != 0) {
                throw GradleException("Building the test project failed with exit code ${buildResult.exitValue}")
            }

            // The language runtime class is what the migration assistant needs in order to see the language's
            // concepts and migration scripts. Without it migration silently does nothing, so check for it here
            // rather than letting the test fail later with a confusing message.
            val languageRuntimeClass = projectDir.resolve(
                "languages/language.with.migration/classes_gen/language/with/migration/Language.class")
            if (!languageRuntimeClass.isFile) {
                throw GradleException("Building the test project did not produce $languageRuntimeClass")
            }
        }

        mpsBackendLauncher.forMpsHome(mpsPlatform.mpsHome)
            .withMpsVersion(mpsPlatform.mpsVersion)
            .withJetBrainsJvm()
            .withTemporaryDirectory(recreateDirectory(temporaryDir.resolve("mps-tmp")))
            .configure(this)

        group = LifecycleBasePlugin.VERIFICATION_GROUP
        classpath(fileTree(mpsPlatform.mpsHome) {
            include("lib/**/*.jar")
        })
        classpath(jarsOnlyRuntimeClasspath)

        mainClass = "de.itemis.mps.gradle.migrate.MainKt"

        args("--project", projectDir)

        // The test project is built before migrating, so the pre-migration check must pass. Halting on failure
        // reports the actual problem instead of letting migration proceed and do nothing.
        args("--halt-on-precheck-failure")

        if (gradle.startParameter.logLevel >= LogLevel.INFO) {
            args("--log-level=info")
        }

        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "--plugin=de.itemis.mps.buildbackends.migrate::${tasks.jar.get().archiveFile.get()}",
                "--plugin-root", mpsPlatform.mpsHome.get().resolve("plugins").toString())
        })

        doLast {
            val solutionModel = "solutions/language.with.migration.solution/models/language.with.migration.solution.mps"
            if (PathUtils.fileContentEquals(
                    backendTesting.testProjectDir("migrate").resolve(solutionModel).toPath(),
                    projectDir.resolve(solutionModel).toPath())) {
                throw GradleException("$solutionModel should have changed after migrate.")
            }
        }
    }

    mpsPlatform.testTask {
        dependsOn(testSimple)
    }
}
