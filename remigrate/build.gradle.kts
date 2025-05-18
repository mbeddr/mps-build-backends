import de.itemis.mps.buildbackends.recreateDirectory
import org.apache.commons.io.file.PathUtils
import org.apache.tools.ant.filters.ReplaceTokens

buildscript {
    dependencies {
        classpath("commons-io:commons-io:2.19.0")
    }
}

plugins {
    id("de.itemis.mps.gradle.launcher")
    id("backend-conventions")
    id("backend-testing")
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
    val testSimple = tasks.register("testRemigrateForMps${mpsPlatform.mpsVersion}", JavaExec::class) {
        val projectDir = temporaryDir.resolve("project")

        doFirst {
            backendTesting.copyTestProjectTo("modelcheck", projectDir)
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

        mainClass = "de.itemis.mps.gradle.remigrate.MainKt"

        args("--project", projectDir)

        if (gradle.startParameter.logLevel >= LogLevel.INFO) {
            args("--log-level=info")
        }

        argumentProviders.add(CommandLineArgumentProvider {
            listOf(
                "--plugin=de.itemis.mps.buildbackends.remigrate::${tasks.jar.get().archiveFile.get()}",
                "--plugin-root", mpsPlatform.mpsHome.get().resolve("plugins").toString())
        })

        doLast {
            if (PathUtils.directoryAndFileContentEquals(backendTesting.testProjectDir("modelcheck").toPath(), projectDir.toPath())) {
                throw GradleException("Project directory $projectDir should have changed after remigrate.")
            }
        }
    }

    mpsPlatform.testTask {
        dependsOn(testSimple)
    }
}
