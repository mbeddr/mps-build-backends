import com.specificlanguages.mps.ArtifactTransforms
import de.itemis.mps.buildbackends.BackendTesting
import de.itemis.mps.buildbackends.MpsPlatform

plugins {
    `lifecycle-base`
    id("com.specificlanguages.mps.artifact-transforms")
}

fun createMpsPlatforms(): List<MpsPlatform> {
    val supportedMpsVersions = project.findProperty("supportedMpsVersions")?.let { (it as String).split(',') }
        ?: throw GradleException("Property 'supportedMpsVersions' not found")

    return supportedMpsVersions.map { mpsVersion ->
        val mpsConfig = configurations.create("mps$mpsVersion")

        fun mpsDependency(version: String) =
            if (version.length < 4) throw IllegalArgumentException("MPS version must be at least four characters long")
            else if (version[3] == '.') "com.jetbrains.mps:mps-prerelease:$version"
            else "com.jetbrains:mps:$version"

        dependencies.add(mpsConfig.name, mpsDependency(mpsVersion))

        val testTask = tasks.register("testMps$mpsVersion") {
            group = LifecycleBasePlugin.VERIFICATION_GROUP
            description = "Run all tests with MPS $mpsVersion"
        }

        MpsPlatform(mpsVersion, ArtifactTransforms.getMpsRoot(mpsConfig), testTask)
    }
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
