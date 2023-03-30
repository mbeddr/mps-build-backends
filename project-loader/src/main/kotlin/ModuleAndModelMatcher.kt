package de.itemis.mps.gradle.project.loader

import jetbrains.mps.smodel.SModelStereotype
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.model.SModelName
import org.jetbrains.mps.openapi.module.SModule

class ModuleAndModelMatcher {
    constructor(modules: Collection<String>, excludeModules: Collection<String>, models: Collection<String>, excludeModels: Collection<String>) {
        this.includeModuleRegex = regexFromAlternativesOrNull(modules)
        this.excludeModuleRegex = regexFromAlternativesOrNull(excludeModules)
        this.includeModelRegex = regexFromAlternativesOrNull(models)
        this.excludeModelRegex = regexFromAlternativesOrNull(excludeModels)
    }

    private val includeModuleRegex: Regex?
    private val excludeModuleRegex: Regex?
    private val includeModelRegex: Regex?
    private val excludeModelRegex: Regex?

    /**
     * Whether the model should be included, according to include/exclude rules. Does NOT check module inclusion rules.
     */
    fun isModelIncluded(model: SModel): Boolean {
        return !SModelStereotype.isDescriptorModel(model)
                && !SModelStereotype.isStubModel(model)
                && isModelNameIncluded(model.name)
    }

    /**
     * Whether the model should be included, according to include/exclude rules. Does NOT check module inclusion rules.
     */
    private fun isModelNameIncluded(modelName: SModelName): Boolean {
        val name = modelName.longName
        if (includeModelRegex != null && !includeModelRegex.matches(name)) {
            return false
        }

        if (excludeModelRegex != null && excludeModelRegex.matches(name)) {
            return false
        }

        return true
    }

    fun isModelAndModuleIncluded(model: SModel): Boolean {
        return isModelIncluded(model) && isModuleIncluded(model.module)
    }

    fun isModuleIncluded(module: SModule): Boolean = isModuleNameIncluded(module.moduleName!!)

    private fun isModuleNameIncluded(name: String): Boolean {
        if (includeModuleRegex != null && !includeModuleRegex.matches(name)) {
            return false
        }

        if (excludeModuleRegex != null && excludeModuleRegex.matches(name)) {
            return false
        }

        return true
    }

}

private fun regexFromAlternativesOrNull(strings: Collection<String>): Regex? {
    if (strings.isEmpty()) return null
    return strings.joinToString(prefix = "(?:", separator = "|", postfix = ")").toRegex()
}
