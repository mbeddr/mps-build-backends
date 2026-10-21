package de.itemis.mps.buildbackends

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.artifacts.result.ResolvedComponentResult
import org.gradle.api.artifacts.result.ResolvedDependencyResult
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.TaskAction

/**
 * Checks that the project can be published as a release:
 *
 * - its version is `MAJOR.MINOR.PATCH` and matches [releaseVersion], if given,
 * - no runtime dependency has a `-SNAPSHOT` version.
 */
abstract class CheckRelease : DefaultTask() {
    @get:Input
    abstract val projectVersion: Property<String>

    @get:Input
    @get:Optional
    abstract val releaseVersion: Property<String>

    @get:Internal
    abstract val runtimeClasspath: Property<ResolvedComponentResult>

    @TaskAction
    fun check() {
        val version = projectVersion.get()
        if (!version.matches(Regex("""\d+\.\d+\.\d+"""))) {
            throw GradleException("Release version must be MAJOR.MINOR.PATCH, got $version")
        }
        val expected = releaseVersion.orNull
        if (expected != null && expected != version) {
            throw GradleException("Release version $expected does not match project version $version")
        }

        val snapshots = dependenciesOf(runtimeClasspath.get())
            .mapNotNull { it.moduleVersion }
            .filter { it.version.endsWith("-SNAPSHOT") }
        if (snapshots.isNotEmpty()) {
            throw GradleException(
                "Cannot release version $version, it depends on snapshots:\n" +
                    snapshots.joinToString("\n") { "- $it" })
        }
    }

    private fun dependenciesOf(root: ResolvedComponentResult): Set<ResolvedComponentResult> {
        val seen = linkedSetOf<ResolvedComponentResult>()
        val queue = ArrayDeque(listOf(root))
        while (queue.isNotEmpty()) {
            for (dependency in queue.removeFirst().dependencies) {
                if (dependency is ResolvedDependencyResult && seen.add(dependency.selected)) {
                    queue.add(dependency.selected)
                }
            }
        }
        return seen
    }
}
