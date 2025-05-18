package de.itemis.mps.buildbackends

import org.gradle.api.Task
import org.gradle.api.provider.Provider
import org.gradle.api.tasks.TaskProvider
import java.io.File

data class MpsPlatform(val mpsVersion: String, val mpsHome: Provider<File>, val testTask: TaskProvider<out Task>)