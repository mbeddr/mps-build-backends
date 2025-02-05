package de.itemis.mps.gradle.generate


import com.intellij.openapi.util.BuildNumber
import com.intellij.openapi.util.IconLoader
import de.itemis.mps.gradle.logging.detectLogging
import de.itemis.mps.gradle.project.loader.EnvironmentKind
import de.itemis.mps.gradle.project.loader.ModuleAndModelMatcher
import de.itemis.mps.gradle.project.loader.forceIndexing
import de.itemis.mps.gradle.project.loader.hasIndexingBug
import jetbrains.mps.generator.GenerationOptions
import jetbrains.mps.generator.GenerationSettingsProvider
import jetbrains.mps.generator.runtime.TemplateModule
import jetbrains.mps.make.MakeSession
import jetbrains.mps.make.facet.FacetRegistry
import jetbrains.mps.make.facet.IFacet
import jetbrains.mps.make.facet.ITarget
import jetbrains.mps.make.script.IScript
import jetbrains.mps.make.script.ScriptBuilder
import jetbrains.mps.messages.IMessage
import jetbrains.mps.messages.IMessageHandler
import jetbrains.mps.messages.MessageKind
import jetbrains.mps.project.MPSProject
import jetbrains.mps.project.Project
import jetbrains.mps.smodel.ModelAccessHelper
import jetbrains.mps.smodel.SLanguageHierarchy
import jetbrains.mps.smodel.language.GeneratorRuntime
import jetbrains.mps.smodel.language.LanguageRegistry
import jetbrains.mps.smodel.language.LanguageRuntime
import jetbrains.mps.smodel.resources.ModelsToResources
import jetbrains.mps.smodel.runtime.MakeAspectDescriptor
import jetbrains.mps.tool.builder.make.BuildMakeService
import jetbrains.mps.util.Computable
import org.jetbrains.mps.openapi.language.SLanguage
import org.jetbrains.mps.openapi.model.SModel
import java.util.*

enum class GenerationResult(val exitCode: Int) {
    Success(0),
    NothingToGenerate(254),
    Error(255);

    fun isFailure() = this != Success
}

val logging = detectLogging()
val logger = logging.getLogger("de.itemis.mps.gradle.generate")

private class MsgHandler : IMessageHandler {
    val logger = logging.getLogger("de.itemis.mps.gradle.generate.messages")
    override fun handle(msg: IMessage) {
        when (msg.kind) {
            MessageKind.INFORMATION -> logger.info(msg.text, msg.exception)
            MessageKind.WARNING -> logger.warn(msg.text, msg.exception)
            MessageKind.ERROR -> logger.error(msg.text, msg.exception)
            null -> logger.error(msg.text, msg.exception)
        }
    }

}

private interface LanguageLookupProblems {
    fun languageNotFoundForNamespace(namespace: SLanguage)
}

private fun allLanguagesToActivateFacets(languageRegistry: LanguageRegistry,
                                         usedLanguages: Iterable<SLanguage>,
                                         problems: LanguageLookupProblems): Set<SLanguage> {
    val result = mutableSetOf<SLanguage>()
    val seen = mutableSetOf<GeneratorRuntime>()
    val nsq: Queue<SLanguage> = ArrayDeque()

    nsq.addAll(usedLanguages.distinct())

    // We need to care about used languages of employed generators as we need to respect
    // all facets of all languages that may appear during generation of a model/module in the make script
    while (nsq.isNotEmpty()) {
        val ns: SLanguage = nsq.remove()
        if (!result.add(ns)) {
            continue
        }

        val lr: LanguageRuntime? = languageRegistry.getLanguage(ns)
        if (lr == null) {
            problems.languageNotFoundForNamespace(ns)
            continue
        }

        for (gr in lr.generators.filterIsInstance<TemplateModule>()) {
            if (seen.add(gr)) {
                nsq.addAll(gr.targetLanguages)
            }
        }
    }

    return result
}

private fun createScript(proj: Project, models: List<SModel>): IScript {

    val registry = LanguageRegistry.getInstance(proj.repository)

    val allUsedLanguages = ModelAccessHelper(proj.modelAccess).runReadAction (Computable {
        val directlyUsedLanguages = models.map { it.module }.distinct().flatMap { it.usedLanguages }.distinct()
        val indirectlyUsedLanguages = SLanguageHierarchy(registry, directlyUsedLanguages).extended

        val result = allLanguagesToActivateFacets(registry, indirectlyUsedLanguages, object : LanguageLookupProblems {
            override fun languageNotFoundForNamespace(namespace: SLanguage) {
                throw IllegalStateException("Language $namespace was not found, cannot analyze it for facets")
            }
        })

        return@Computable result
    })

    val facetRegistry = proj.getComponent(FacetRegistry::class.java)
    val scb = ScriptBuilder(facetRegistry)

    // Map of facet name to source, for logging
    val allFacets = mutableListOf<IFacet.Name>()

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

    // todo: not sure if we really need the final target to be Make.make all the time. The code was taken fom #BuildMakeService.defaultMakeScript
    return scb.withFacetNames(allFacets).withFinalTarget(ITarget.Name("jetbrains.mps.make.facets.Make.make")).toScript()
}

private fun getFacetsForLanguages(facetRegistry: FacetRegistry, languages: Set<SLanguage>) =
    try {
        getFacetsForLanguagesMps20213(facetRegistry, languages)
    } catch (e: NoSuchMethodException) {
        languages.flatMap {
            @Suppress("DEPRECATION", "removal")
            facetRegistry.getFacetsForLanguage(it.qualifiedName)
        }
    }

@Suppress("UNCHECKED_CAST")
private fun getFacetsForLanguagesMps20213(facetRegistry: FacetRegistry, allUsedLanguages: Set<SLanguage>): Iterable<IFacet> =
    facetRegistry.javaClass.getMethod("getFacetsForLanguages", java.lang.Iterable::class.java).invoke(facetRegistry, allUsedLanguages) as Iterable<IFacet>

private fun makeModels(proj: Project, models: List<SModel>): GenerationResult {
    val session = MakeSession(proj, MsgHandler(), true)
    val res = ModelsToResources(models).resources().toList()
    val makeService = BuildMakeService()

    if (res.isEmpty()) {
        logger.warn("nothing to generate")
        return GenerationResult.NothingToGenerate
    }
    logger.info("starting generation")
    val future = makeService.make(session, res, createScript(proj, models))
    try {
        val result = future.get()
        logger.info("generation finished")
        return if (result.isSucessful) {
            logger.info("generation result: successful")
            GenerationResult.Success
        } else {
            logger.error("generation result: failed")
            logger.error(result)
            GenerationResult.Error
        }
    } catch (ex: Exception) {
        logger.error("failed to generate", ex)
    }
    return GenerationResult.Error
}


fun generateProject(parsed: GenerateArgs, project: Project): GenerationResult {

    // Workaround for https://youtrack.jetbrains.com/issue/MPS-37926/Indices-not-built-properly-in-IdeaEnvironment
    if (project is MPSProject && shouldForceIndexing(parsed, BuildNumber.currentVersion())) {
        logger.info("Forcing full indexing to work around MPS-37926. Can be disabled with --force-indexing=never.")
        forceIndexing(project)
        logger.info("Full indexing complete")
    }

    val generationSettings = project.getComponent(GenerationSettingsProvider::class.java).generationSettings
    parsed.parallelGenerationThreads.let {
        when {
            it == 0 -> generationSettings.isParallelGenerator = false
            it > 0 -> {
                logger.warn("Using parallel generation with $it threads")
                generationSettings.isParallelGenerator = true
                generationSettings.numberOfParallelThreads = it
            }
            else -> error("--parallel-generation-threads must be >= 0")
        }
    }
    if (parsed.noStrictMode) {
        generationSettings.isStrictMode = false
    }

    val moduleAndModelMatcher = ModuleAndModelMatcher(parsed.modules, parsed.excludeModules, parsed.models, parsed.excludeModels)

    val (modulesToInclude, modelsToGenerate) = ModelAccessHelper(project.modelAccess).runReadAction (Computable {
        val modules = project.projectModulesWithGenerators.filter(moduleAndModelMatcher::isModuleIncluded)
        val models = modules
            .flatMap { module -> module.models }
            .filter(moduleAndModelMatcher::isModelIncluded)

        modules to models
    })

    if (logger.isInfoEnabled &&
        (parsed.models.isNotEmpty() || parsed.excludeModels.isNotEmpty()
                || parsed.modules.isNotEmpty() || parsed.excludeModules.isNotEmpty())) {
        logger.info("Modules included in generation: $modulesToInclude")
        logger.info("Models included in generation: $modelsToGenerate")
    }

    if (parsed.environmentKind == EnvironmentKind.IDEA) {
        // Activate IconLoader to load icons.
        IconLoader.activate()
    }

    return makeModels(project, modelsToGenerate)
}

private fun shouldForceIndexing(args: GenerateArgs, buildNumber: BuildNumber): Boolean {
    return args.forceIndexing ?: hasIndexingBug(buildNumber)
}
