package de.itemis.mps.gradle

import org.gradle.testkit.runner.GradleRunner
import org.gradle.testkit.runner.TaskOutcome
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.Matchers.equalTo
import org.junit.jupiter.api.io.TempDir
import org.junit.jupiter.api.parallel.Execution
import org.junit.jupiter.api.parallel.ExecutionMode
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.Arguments.arguments
import org.junit.jupiter.params.provider.MethodSource
import java.io.File

@Execution(ExecutionMode.CONCURRENT)
class GenerationTest {

    private fun getSystemProperty(name: String): String = checkNotNull(System.getProperty(name)) {
        "System property '$name' is not set"
    }

    @ParameterizedTest(name = "MPS {0}")
    @MethodSource("getMpsAndJavaVersions")
    fun generateBuildSolution(mpsVersion: String, javaVersion: String, @TempDir tempDir: File) {
        val testProjectsRoot = getSystemProperty("test.projects.root")

        val originalProjectDir = File(testProjectsRoot)
            .resolve("generate-build-solution")
        check(originalProjectDir.isDirectory) { "test project directory not present: $originalProjectDir" }
        originalProjectDir.copyRecursively(tempDir)

        val projectDir = tempDir

        projectDir.resolve("settings.gradle.kts").writeText("""
            pluginManagement {
                repositories {
                    maven("https://artifacts.itemis.cloud/repository/maven-mps")
                    gradlePluginPortal()
                }
            }
        """.trimIndent())

        projectDir.resolve("build.gradle.kts").writeText("""
            plugins {
                id("com.specificlanguages.mps") version "2.0.0-pre3"
                id("de.itemis.mps.gradle.launcher") version "2.5.2.130.94399cc"
            }
            
            repositories {
                mavenCentral()
                maven("https://artifacts.itemis.cloud/repository/maven-mps")
            }
            
            val backend by configurations.registering
            
            dependencies {
                jbr("com.jetbrains.jdk:jbr_jcef:$javaVersion.+")
                mps("com.jetbrains:mps:$mpsVersion")
            }
            
            tasks {
                val generate by registering(JavaExec::class) {
                        mpsBackendLauncher.forMpsHome(mpsDefaults.mpsHome.asFile)
                            .withMpsVersion("$mpsVersion")
                            .withTemporaryDirectory(temporaryDir)
                            .configure(this)

                    javaLauncher = jbrToolchain.javaLauncher

                    classpath(mpsDefaults.mpsHome.asFileTree.matching {
                        include("lib/**/*.jar")
                    })
                    
                    val backendClasspath = (project.property("backendClasspath") as String)
                        .split(File.pathSeparator)
                    
                    classpath(backendClasspath)

                    mainClass = "de.itemis.mps.gradle.generate.MainKt"

                    args("--project", projectDir)
                    args("--model", "my.build.script")
                }
            }
        """.trimIndent())

        val testKitRoot = getSystemProperty("testkit.root").let(::File)
        val testKitDir = testKitRoot.resolve("mps-${mpsVersion}")
        if (!testKitDir.exists()) {
            testKitDir.mkdirs() || throw RuntimeException("Could not create directory $testKitDir")
        }

        val result = GradleRunner.create()
            .withTestKitDir(testKitDir)
            .withProjectDir(projectDir)
            .withArguments(":generate", "-PbackendClasspath=$generateBackendClasspath", "--info")
            .build()

        assertThat(result.task(":generate")?.outcome, equalTo(TaskOutcome.SUCCESS))
    }

    companion object {
        val generateBackendClasspath = File(System.getProperty("generate.backend.classpath.file")).readText().trim()

        @JvmStatic
        val mpsAndJavaVersions = listOf(
            arguments("2022.3.3", "17"),
            arguments("2023.2.2", "17"),
            arguments("2024.1.2", "17"),
            arguments("2024.3.2", "21"),
            arguments("2025.1-RC1", "21"),
        )
    }
}
