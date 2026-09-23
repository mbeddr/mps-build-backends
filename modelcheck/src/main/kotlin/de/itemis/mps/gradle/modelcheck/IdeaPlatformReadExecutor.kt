package de.itemis.mps.gradle.modelcheck

import com.intellij.openapi.application.ReadAction
import jetbrains.mps.smodel.ModelAccessBase
import org.jetbrains.mps.openapi.module.ModelAccess
import java.util.concurrent.Executor
import java.util.concurrent.RejectedExecutionException

/**
 * Adapted to Kotlin from JetBrains MPS's IdeaPlatformReadExecutor (Apache License 2.0):
 * https://github.com/JetBrains/MPS/blob/5d6bed88c917c0df00b985e34fcfb3adb31c6d0d/plugins/mps-modelchecker/platform/source_gen/jetbrains/mps/ide/modelchecker/platform/actions/IdeaPlatformReadExecutor.java
 */
internal class IdeaPlatformReadExecutor(private val modelAccess: ModelAccess) : Executor {
    override fun execute(runnable: Runnable) {
        try {
            ReadAction.computeCancellable<Unit, RuntimeException> {
                (modelAccess as ModelAccessBase).runReadAction(runnable)
            }
        } catch (e: ReadAction.CannotReadException) {
            throw RejectedExecutionException("failed to acquire or retain read lock", e)
        }
    }
}
