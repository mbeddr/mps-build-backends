package de.itemis.mps.gradle.generate


import com.intellij.openapi.util.IconLoader
import de.itemis.mps.gradle.project.loader.EnvironmentKind
import de.itemis.mps.gradle.project.loader.ModuleAndModelMatcher
import jetbrains.mps.make.MakeSession
import jetbrains.mps.make.facet.FacetRegistry
import jetbrains.mps.make.facet.IFacet
import jetbrains.mps.make.facet.ITarget
import jetbrains.mps.make.script.IScript
import jetbrains.mps.make.script.ScriptBuilder
import jetbrains.mps.messages.IMessage
import jetbrains.mps.messages.IMessageHandler
import jetbrains.mps.messages.MessageKind
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.SLanguageHierarchy
import jetbrains.mps.smodel.SModelStereotype
import jetbrains.mps.smodel.language.LanguageRegistry
import jetbrains.mps.smodel.resources.ModelsToResources
import jetbrains.mps.smodel.runtime.MakeAspectDescriptor
import jetbrains.mps.tool.builder.make.BuildMakeService
import org.apache.log4j.Logger
import org.jetbrains.concurrency.AsyncPromise
import org.jetbrains.mps.openapi.language.SLanguage
import org.jetbrains.mps.openapi.model.SModel
import org.jetbrains.mps.openapi.module.SModule

private val logger = Logger.getLogger("de.itemis.mps.gradle.generate")

private val DEFAULT_FACETS = listOf(
        IFacet.Name("jetbrains.mps.lang.core.Generate"),
        IFacet.Name("jetbrains.mps.lang.core.TextGen"),
        IFacet.Name("jetbrains.mps.make.facets.Make"),
        IFacet.Name("jetbrains.mps.lang.makeup.Makeup"))

private class MsgHandler : IMessageHandler {
    val logger = Logger.getLogger("de.itemis.mps.gradle.generate.messages")
    override fun handle(msg: IMessage) {
        when (msg.kind) {
            MessageKind.INFORMATION -> logger.info(msg.text, msg.exception)
            MessageKind.WARNING -> logger.warn(msg.text, msg.exception)
            MessageKind.ERROR -> logger.error(msg.text, msg.exception)
            null -> logger.error(msg.text, msg.exception)
        }
    }

}

private fun createScript(proj: Project, models: List<SModel>): IScript {

    val allUsedLanguagesAR: AsyncPromise<Set<SLanguage>> = AsyncPromise()
    val registry = LanguageRegistry.getInstance(proj.repository)

    proj.modelAccess.runReadAction {
        val allDirectlyUsedLanguages = models.map { it.module }.distinct().flatMap { it.usedLanguages }.distinct()
        allUsedLanguagesAR.setResult(SLanguageHierarchy(registry, allDirectlyUsedLanguages).extended)
    }

    val allUsedLanguages = allUsedLanguagesAR.get()

    val facetRegistry = proj.getComponent(FacetRegistry::class.java)
    val scb = ScriptBuilder (facetRegistry)

    // Map of facet name to source, for logging
    val allFacets = DEFAULT_FACETS.toMutableList()
    if (logger.isInfoEnabled) {
        logger.info("Default make facets: $DEFAULT_FACETS")
    }

    when {
        allUsedLanguages == null -> logger.error("failed to retrieve used languages")
        allUsedLanguages.isEmpty() -> logger.warn("no used language is given")
        else -> {
            if (logger.isInfoEnabled) {
                logger.info("All languages used by the models: $allUsedLanguages")
            }

            val facetNamesFromMakeAspect = allUsedLanguages
                .mapNotNull { registry.getLanguage(it) }
                .mapNotNull { it.getAspect(MakeAspectDescriptor::class.java) }
                .flatMap { it.manifest.facets() }
                .map { it.name }

            if (logger.isInfoEnabled) {
                logger.info("Additional facets found in make aspects of used languages: $facetNamesFromMakeAspect")
            }
            allFacets.addAll(facetNamesFromMakeAspect)

            val facetsFromRegistry = getFacetsForLanguages(facetRegistry, allUsedLanguages)
            val facetNamesFromRegistry = facetsFromRegistry.map { it.name }

            if (logger.isInfoEnabled) {
                logger.info("Additional facets found in FacetRegistry for used languages: $facetNamesFromRegistry")
            }

            allFacets.addAll(facetNamesFromRegistry)
        }
    }


    // For some reason MPS doesn't explicitly stat that there is a dependency on Generate, TextGen and Make, so we have
    // to make sure they are always included in the set of facets even if for MPS there is no dependency on them.

    // todo: not sure if we really need the final target to be Make.make all the time. The code was taken fom #BuildMakeService.defaultMakeScript
    return scb.withFacetNames(allFacets).withFinalTarget(ITarget.Name("jetbrains.mps.make.facets.Make.make")).toScript()
}

private fun getFacetsForLanguages(facetRegistry: FacetRegistry, allUsedLanguages: Set<SLanguage>) =
    try {
        getFacetsForLanguagesMps20213(facetRegistry, allUsedLanguages)
    } catch (e: NoSuchMethodException) {
        allUsedLanguages.flatMap { facetRegistry.getFacetsForLanguage(it.qualifiedName) }
    }

@Suppress("UNCHECKED_CAST")
private fun getFacetsForLanguagesMps20213(facetRegistry: FacetRegistry, allUsedLanguages: Set<SLanguage>): Iterable<IFacet> =
    facetRegistry.javaClass.getMethod("getFacetsForLanguages", java.lang.Iterable::class.java).invoke(facetRegistry, allUsedLanguages) as Iterable<IFacet>

private fun makeModels(proj: Project, models: List<SModel>): Boolean {
    val session = MakeSession(proj, MsgHandler(), true)
    val res = ModelsToResources(models).resources().toList()
    val makeService = BuildMakeService()

    if (res.isEmpty()) {
        logger.warn("nothing to generate")
        return false
    }
    logger.info("starting generation")
    val future = makeService.make(session, res, createScript(proj, models))
    try {
        val result = future.get()
        logger.info("generation finished")
        return if (result.isSucessful) {
            logger.info("generation result: successful")
            true
        } else {
            logger.error("generation result: failed")
            logger.error(result)
            false
        }
    } catch (ex: Exception) {
        logger.error("failed to generate", ex)
    }
    return false
}


fun generateProject(parsed: GenerateArgs, project: Project): Boolean {
    val ftr = AsyncPromise<List<SModel>>()
    val modelsList = ArrayList<SModel>()
    val modulesList = ArrayList<SModule>()
    val moduleAndModelMatcher = ModuleAndModelMatcher(parsed.modules, parsed.excludeModules, parsed.models, parsed.excludeModels)

    project.modelAccess.runReadAction {
        modelsList.addAll(
            project.projectModulesWithGenerators
                .filter(moduleAndModelMatcher::isModuleIncluded)
                .flatMap { module -> module.models }
                .filter(moduleAndModelMatcher::isModelIncluded))
        modulesList.addAll(
            project.projectModulesWithGenerators
                .filter(moduleAndModelMatcher::isModuleIncluded)
        )
        val allCheckedModels = modulesList.flatMap { module ->
            module.models.filter { !SModelStereotype.isDescriptorModel(it) }
        }.union(modelsList).toList()
        ftr.setResult(allCheckedModels)
    }

    val modelsToGenerate = ftr.get()

    if (modelsToGenerate == null) {
        logger.error("failed to fetch modelsToGenerate")
        return false
    }

    if (parsed.environmentKind == EnvironmentKind.IDEA) {
        // Activate IconLoader to load icons.
        IconLoader.activate()
    }

    return makeModels(project, modelsToGenerate)
}
