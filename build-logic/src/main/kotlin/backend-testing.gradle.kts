import com.specificlanguages.mps.ArtifactTransforms
import de.itemis.mps.buildbackends.BackendTesting
import de.itemis.mps.buildbackends.MpsPlatform

plugins {
    `lifecycle-base`
    id("com.specificlanguages.mps.artifact-transforms")
}

fun createMpsPlatform(name: String, mpsVersion: String, dependencyNotation: String): MpsPlatform {
    val mpsConfig = configurations.create("mps$name")
    dependencies.add(mpsConfig.name, dependencyNotation)

    val testTask = tasks.register("testMps$name") {
        group = LifecycleBasePlugin.VERIFICATION_GROUP
        description = "Run all tests with MPS $mpsVersion"
    }

    return MpsPlatform(name, mpsVersion, ArtifactTransforms.getMpsRoot(mpsConfig), testTask)
}

fun createMpsPlatforms(): List<MpsPlatform> {
    val prefix = "supportedMpsVersion."
    val supportedMpsVersions = providers.gradlePropertiesPrefixedBy(prefix).get()
        .mapKeys { it.key.removePrefix(prefix) }
        .toSortedMap()
    if (supportedMpsVersions.isEmpty()) {
        throw GradleException("No '$prefix<line>' properties found")
    }
    for ((line, version) in supportedMpsVersions) {
        if (version != line && !version.startsWith("$line.")) {
            throw GradleException("MPS version $version in property '$prefix$line' is not from release line $line")
        }
    }
    val supportedMpsPrereleaseVersion: String by project

    return supportedMpsVersions.map { (line, version) -> createMpsPlatform(line, version, "com.jetbrains:mps:$version") } +
        createMpsPlatform(
            "Prerelease",
            supportedMpsPrereleaseVersion,
            "com.jetbrains.mps:mps-prerelease:$supportedMpsPrereleaseVersion",
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
