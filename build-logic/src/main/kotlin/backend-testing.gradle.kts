import com.specificlanguages.mps.ArtifactTransforms
import de.itemis.mps.buildbackends.BackendTesting
import de.itemis.mps.buildbackends.MpsPlatform

plugins {
    `lifecycle-base`
    id("com.specificlanguages.mps.artifact-transforms")
}

fun createMpsPlatform(mpsVersion: String, dependencyNotation: String): MpsPlatform {
    val mpsConfig = configurations.create("mps$mpsVersion")
    dependencies.add(mpsConfig.name, dependencyNotation)

    val testTask = tasks.register("testMps$mpsVersion") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Run all tests with MPS $mpsVersion"
    }

    return MpsPlatform(mpsVersion, ArtifactTransforms.getMpsRoot(mpsConfig), testTask)
}

fun createMpsPlatforms(): List<MpsPlatform> {
    val supportedMpsVersions = project.findProperty("supportedMpsVersions")?.let { (it as String).split(',') }
        ?: throw GradleException("Property 'supportedMpsVersions' not found")
    val mpsPrereleaseVersion: String by project

    return supportedMpsVersions.map { createMpsPlatform(it, "com.jetbrains:mps:$it") } +
        createMpsPlatform(
            mpsPrereleaseVersion,
            "com.jetbrains.mps:mps-prerelease:$mpsPrereleaseVersion",
        )
}

val backendTesting = extensions.create("backendTesting", BackendTesting::class.java, createMpsPlatforms())

val integrationTest by tasks.registering {
    dependsOn(provider { backendTesting.mpsPlatforms.map { it.testTask } })
}

tasks.check {
    dependsOn(integrationTest)
}

// Save space (assuming that all JavaExec tasks are integration tests).
tasks.withType(JavaExec::class.java).configureEach {
    doLast {
        if (!project.hasProperty("keepMpsDirs")) {
            val systemPath = systemProperties["idea.system.path"]?.toString()
            if (systemPath != null && systemPath.startsWith(temporaryDir.toString())) {
                logger.info("Deleting MPS system directory $systemPath")
                File(systemPath).deleteRecursively()
            }

            val configPath = systemProperties["idea.config.path"]?.toString()
            if (configPath != null && configPath.startsWith(temporaryDir.toString())) {
                logger.info("Deleting MPS config directory $configPath")
                File(configPath).deleteRecursively()
            }
        }
    }
}
