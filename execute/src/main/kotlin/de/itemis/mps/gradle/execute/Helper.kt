package de.itemis.mps.gradle.execute

import com.intellij.util.containers.orNull
import de.itemis.mps.gradle.logging.detectLogging
import jetbrains.mps.project.Project
import jetbrains.mps.tool.environment.Environment
import jetbrains.mps.tool.run.ModuleClassCode
import org.jetbrains.mps.openapi.module.SModuleReference
import java.lang.reflect.Method
import kotlin.reflect.KClass

val logging = detectLogging()
val logger = logging.getLogger("de.itemis.mps.gradle.execute")

private fun Project.getModuleReference(moduleName: String): SModuleReference {
    lateinit var moduleReference: SModuleReference

    modelAccess.runReadAction {
        val module = repository.modules.find { it.moduleName == moduleName }
            ?: throw IllegalArgumentException("Not found module $moduleName")
        moduleReference = module.moduleReference
    }

    return moduleReference
}

internal fun executeGeneratedCode(arguments: ExecuteArgs, environment: Environment, project: Project) {

    val moduleReference = project.getModuleReference(arguments.module)
    val classCode = try {
        ModuleClassCode(moduleReference.toString())
    } catch (e: NoClassDefFoundError) {
        throw IllegalArgumentException("Can't execute a class method in this MPS version", e)
    }

    try {
        classCode.load(environment.platform, arguments.`class`)
    } catch (e: ClassNotFoundException) {
        val message =
            "Class ${arguments.`class`} not found in module ${arguments.module}. Maybe the solution is not generated"
        throw IllegalArgumentException(message, e)
    }

    val methodName = arguments.method
    fun getMethod(vararg parameterTypes: KClass<*>): Method? {
        return classCode.staticMethod(methodName, *parameterTypes.map { it.java }.toTypedArray()).orNull()
    }

    getMethod(Project::class, Array<String>::class)?.apply {
        invoke(null, project, arguments.methodArguments.toTypedArray())
        return
    }

    getMethod(Project::class)?.apply {
        if (arguments.methodArguments.isEmpty()) {
            invoke(null, project)
        } else {
            throw IllegalArgumentException("Method $methodName in class ${arguments.`class`} only takes a Project, but a string[] arguments provided to the backend")
        }
        return
    }

    throw IllegalArgumentException("No public static method $methodName in class ${arguments.`class`} that takes a Project and optionally a string[]")
}
