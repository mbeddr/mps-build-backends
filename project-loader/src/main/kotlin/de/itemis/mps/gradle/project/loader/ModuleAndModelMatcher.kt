package de.itemis.mps.gradle.project.loader

import jetbrains.mps.smodel.SModelStereotype
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.model.SModelName
import org.jetbrains.mps.openapi.module.SModule

public class ModuleAndModelMatcher public constructor(
    modules: Collection<String>,
    excludeModules: Collection<String>,
    models: Collection<String>,
    excludeModels: Collection<String>
) {
    private val includeModuleRegex: Regex? = regexFromAlternativesOrNull(modules)
    private val excludeModuleRegex: Regex? = regexFromAlternativesOrNull(excludeModules)
    private val includeModelRegex: Regex? = regexFromAlternativesOrNull(models)
    private val excludeModelRegex: Regex? = regexFromAlternativesOrNull(excludeModels)

    /**
     * Whether the model should be included, according to include/exclude rules. Does NOT check module inclusion rules.
     */
    public fun isModelIncluded(model: SModel): Boolean {
        return !SModelStereotype.isDescriptorModel(model)
                && !SModelStereotype.isStubModel(model)
                && isModelNameIncluded(model.name)
    }

    /**
     * Whether the model should be included, according to include/exclude rules. Does NOT check module inclusion rules.
     */
    public fun isModelNameIncluded(modelName: SModelName): Boolean {
        val name = modelName.value
        if (includeModelRegex != null && !includeModelRegex.matches(name)) {
            return false
        }

        if (excludeModelRegex != null && excludeModelRegex.matches(name)) {
            return false
        }

        return true
    }

    public fun isModelAndModuleIncluded(model: SModel): Boolean {
        return isModelIncluded(model) && isModuleIncluded(model.module)
    }

    public fun isModuleIncluded(module: SModule): Boolean = isModuleNameIncluded(module.moduleName!!)

    public fun isModuleNameIncluded(name: String): Boolean {
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
