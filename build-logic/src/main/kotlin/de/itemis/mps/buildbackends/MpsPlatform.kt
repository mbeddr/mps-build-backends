package de.itemis.mps.buildbackends

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

/**
 * An MPS distribution to run tests with.
 *
 * @param name identifies the platform in task and configuration names, such as the MPS release line (`2025.1`).
 * @param mpsVersion the full MPS version (`2025.1.4`).
 */
data class MpsPlatform(
    val name: String,
    val mpsVersion: String,
    val mpsHome: Provider<File>,
    val testTask: TaskProvider<out Task>,
)
